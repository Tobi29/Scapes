/*
 * Copyright 2012-2017 Tobi29
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tobi29.scapes.chunk.terrain.infinite

import org.tobi29.scapes.chunk.ChunkMesh
import org.tobi29.scapes.chunk.terrain.TerrainRenderInfo
import org.tobi29.scapes.chunk.terrain.TerrainRenderer
import org.tobi29.scapes.engine.graphics.*
import org.tobi29.scapes.engine.utils.AtomicBoolean
import org.tobi29.scapes.engine.utils.Pool
import org.tobi29.scapes.engine.utils.ThreadLocal
import org.tobi29.scapes.engine.utils.graphics.Cam
import org.tobi29.scapes.engine.utils.math.AABB
import org.tobi29.scapes.engine.utils.math.min
import org.tobi29.scapes.engine.utils.math.sqr
import org.tobi29.scapes.engine.utils.math.sqrt
import org.tobi29.scapes.engine.utils.math.vector.Vector2i
import org.tobi29.scapes.engine.utils.math.vector.div
import org.tobi29.scapes.engine.utils.profiler.profilerSection
import org.tobi29.scapes.engine.utils.task.TaskLock

class TerrainInfiniteRenderer(private val terrain: TerrainInfiniteClient,
                              chunkDistance: Double,
                              private val sortedLocations: List<Vector2i>) : TerrainRenderer {
    private val chunkDistanceMax = chunkDistance * 16.0 - 16.0
    private val chunks = ArrayList<TerrainInfiniteRendererChunk>()
    private val frame: Model
    private val taskLock = TaskLock()
    private val updateVisible = AtomicBoolean()
    private val updatingVisible = AtomicBoolean()
    private var playerX = 0
    private var playerY = 0
    private var playerZ = 0
    private var chunkDistance = 0.0
    private var disposed = false
    private var staticRenderDistance = false
    private var cam = Cam(0.1f, 1.0f)
    private var cullingPool1 = Pool({ VisibleUpdate() })
    private var cullingPool2 = Pool({ VisibleUpdate() })

    init {
        val min = 0.001f
        val max = 0.999f
        frame = terrain.world.game.engine.graphics.createVI(
                floatArrayOf(min, min, min, max, min, min, max, max, min, min,
                        max, min, min, min, max, max, min, max, max, max, max,
                        min, max, max),
                intArrayOf(0, 1, 1, 2, 2, 3, 3, 0, 4, 5, 5, 6, 6, 7, 7, 4, 0, 4,
                        1, 5, 2, 6, 3, 7), RenderType.LINES)
    }

    fun toggleStaticRenderDistance() {
        staticRenderDistance = !staticRenderDistance
    }

    fun reloadGeometry() {
        terrain.loadedChunks().forEach { it.rendererChunk().reset() }
    }

    fun dispose() {
        taskLock.lock()
        disposed = true
    }

    fun addToQueue(chunk: TerrainInfiniteRendererChunk?,
                   i: Int) {
        if (chunk == null || !chunk.chunk().isLoaded || disposed) {
            return
        }
        if (!checkLoaded(chunk)) {
            return
        }
        terrain.world.game.engine.taskExecutor.runTask({
            val threadData = THREAD_DATA.get()
            threadData.process(chunk, i, this)
        }, taskLock, "Update-Chunk-Geometry")
    }

    fun addToQueue(chunk: TerrainInfiniteRendererChunk?) {
        if (chunk == null || !chunk.chunk().isLoaded || disposed) {
            return
        }
        if (!checkLoaded(chunk)) {
            return
        }
        terrain.world.game.engine.taskExecutor.runTask({
            val threadData = THREAD_DATA.get()
            for (i in 0..chunk.zSections() - 1) {
                threadData.process(chunk, i, this)
            }
        }, taskLock, "Update-Chunk-Geometry")
    }

    private fun checkLoaded(chunk: TerrainInfiniteRendererChunk): Boolean {
        val terrainChunk = chunk.chunk()
        val x = terrainChunk.pos.x
        val y = terrainChunk.pos.y
        if (!checkLoaded(terrain, x - 1, y - 1)) {
            return false
        }
        if (!checkLoaded(terrain, x, y - 1)) {
            return false
        }
        if (!checkLoaded(terrain, x - 1, y - 1)) {
            return false
        }
        if (!checkLoaded(terrain, x - 1, y)) {
            return false
        }
        if (!checkLoaded(terrain, x - 1, y)) {
            return false
        }
        if (!checkLoaded(terrain, x - 1, y + 1)) {
            return false
        }
        if (!checkLoaded(terrain, x, y + 1)) {
            return false
        }
        if (!checkLoaded(terrain, x - 1, y + 1)) {
            return false
        }
        return true
    }

    override fun renderUpdate(cam: Cam) {
        if (disposed) {
            return
        }
        this.cam = cam
        val camPos = cam.position.now().div(16.0)
        val newPlayerX = camPos.intX()
        val newPlayerY = camPos.intY()
        val newPlayerZ = camPos.intZ()
        if (!updatingVisible.get() && (updateVisible.getAndSet(
                false) || playerX != newPlayerX ||
                playerY != newPlayerY || playerZ != newPlayerZ)) {
            updatingVisible.set(true)
            playerX = newPlayerX
            playerY = newPlayerY
            playerZ = newPlayerZ
            terrain.world.game.engine.taskExecutor.runTask({
                profilerSection("Update-Visible") {
                    updateVisible()
                }
                updatingVisible.set(false)
            }, taskLock, "Update-Visible-Chunks")
        }
        val camX = camPos.intX()
        val camY = camPos.intY()
        val offsetX = camX - camPos.x
        val offsetY = camY - camPos.y
        chunks.clear()
        chunkDistance = chunkDistanceMax
        for (pos in sortedLocations) {
            val chunk = terrain.chunkNoLoad(camX + pos.x,
                    camY + pos.y)
            if (chunk != null) {
                val rendererChunk = chunk.rendererChunk()
                if (rendererChunk.isLoaded) {
                    chunks.add(rendererChunk)
                    continue
                }
            }
            chunkDistance = min(chunkDistance,
                    sqrt(sqr(offsetX + pos.x) + sqr(offsetY + pos.y)) * 16.0)
        }
    }

    override fun render(gl: GL,
                        shader1: Shader,
                        shader2: Shader,
                        cam: Cam,
                        debug: Boolean) {
        if (disposed) {
            return
        }
        chunks.forEach { it.render(gl, shader1, shader2, cam) }
        if (debug) {
            gl.engine.graphics.textureEmpty().bind(gl)
            chunks.forEach { it.renderFrame(gl, frame, shader1, cam) }
        }
    }

    override fun renderAlpha(gl: GL,
                             shader1: Shader,
                             shader2: Shader,
                             cam: Cam) {
        if (disposed) {
            return
        }
        val iterator = chunks.listIterator(chunks.size)
        while (iterator.hasPrevious()) {
            iterator.previous().renderAlpha(gl, shader1, shader2, cam)
        }
    }

    override fun blockChange(x: Int,
                             y: Int,
                             z: Int) {
        val xx = x shr 4
        val yy = y shr 4
        val zz = z shr 4
        val xxx = x - (xx shl 4)
        val yyy = y - (yy shl 4)
        val zzz = z - (zz shl 4)
        terrain.chunkC(xx, yy) { chunk ->
            val rendererChunk = chunk.rendererChunk()
            rendererChunk.dirtyX(zz, xxx, yyy, zzz)
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun TerrainInfiniteRendererChunk.dirtyX(zz: Int,
                                                           xxx: Int,
                                                           yyy: Int,
                                                           zzz: Int) {
        if (xxx == 0) {
            dirtyY(zz, yyy, zzz, -1)
        } else if (xxx == 15) {
            dirtyY(zz, yyy, zzz, 1)
        }
        dirtyY(zz, yyy, zzz, 0)
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun TerrainInfiniteRendererChunk.dirtyY(zz: Int,
                                                           yyy: Int,
                                                           zzz: Int,
                                                           xxxx: Int) {
        if (yyy == 0) {
            dirtyZ(zz, zzz, xxxx, -1)
        } else if (yyy == 15) {
            dirtyZ(zz, zzz, xxxx, 1)
        }
        dirtyZ(zz, zzz, xxxx, 0)
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun TerrainInfiniteRendererChunk.dirtyZ(zz: Int,
                                                           zzz: Int,
                                                           xxxx: Int,
                                                           yyyy: Int) {
        if (zzz == 0 && zz > 0) {
            dirty(zz, xxxx, yyyy, -1)
        } else if (zzz == 15 && zz < zSections() - 1) {
            dirty(zz, xxxx, yyyy, 1)
        }
        dirty(zz, xxxx, yyyy, 0)
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun TerrainInfiniteRendererChunk.dirty(zz: Int,
                                                          xxxx: Int,
                                                          yyyy: Int,
                                                          zzzz: Int) {
        val z = zz + zzzz
        if (xxxx == 0 && yyyy == 0) {
            if (z >= 0 && z < zSections()) {
                setGeometryDirty(z)
            }
        } else {
            val pos = chunk().pos
            terrain.chunkC(pos.x + xxxx, pos.y + yyyy) {
                val rendererChunk = it.rendererChunk()
                if (z >= 0 && z < rendererChunk.zSections()) {
                    rendererChunk.setGeometryDirty(z)
                }
            }
        }
    }

    override fun actualRenderDistance(): Double {
        if (staticRenderDistance) {
            return 4096.0
        }
        return chunkDistance
    }

    private fun updateVisible() {
        profilerSection("Reset") {
            terrain.loadedChunks().forEach { it.rendererChunk().resetPrepareVisible() }
        }
        profilerSection("Check") {
            checkVisible(playerX, playerY, playerZ,
                    terrain.chunkNoLoad(playerX, playerY))
        }
        profilerSection("Update") {
            terrain.loadedChunks().forEach { it.rendererChunk().updateVisible() }
        }
    }

    private fun checkVisible(x: Int,
                             y: Int,
                             z: Int,
                             chunk: TerrainInfiniteChunkClient?) {
        if (chunk != null) {
            chunk.rendererChunk().setPrepareVisible(z)
            cullingPool1.push().set(x, y, z + 1, chunk, chunk.rendererChunk())
            cullingPool1.push().set(x, y, z - 1, chunk, chunk.rendererChunk())
        }
        terrain.chunkNoLoad(x, y - 1)?.let { chunk ->
            cullingPool1.push().set(x, y - 1, z, chunk, chunk.rendererChunk())
        }
        terrain.chunkNoLoad(x + 1, y)?.let { chunk ->
            cullingPool1.push().set(x + 1, y, z, chunk, chunk.rendererChunk())
        }
        terrain.chunkNoLoad(x, y + 1)?.let { chunk ->
            cullingPool1.push().set(x, y + 1, z, chunk, chunk.rendererChunk())
        }
        terrain.chunkNoLoad(x - 1, y)?.let { chunk ->
            cullingPool1.push().set(x - 1, y, z, chunk, chunk.rendererChunk())
        }
        while (cullingPool1.isNotEmpty()) {
            cullingPool1.asSequence().filter {
                !it.rendererChunk.isCulled(it.z)
            }.forEach { checkVisible(it, cullingPool2) }
            val swap = cullingPool1
            swap.reset()
            cullingPool1 = cullingPool2
            cullingPool2 = swap
        }
        terrain.loadedChunks().forEach { it.rendererChunk().setCulled(false) }
    }

    private fun checkVisible(update: VisibleUpdate,
                             pool: Pool<VisibleUpdate>) {
        update.rendererChunk.setPrepareVisible(update.z)
        if (update.rendererChunk.setCulled(update.z,
                true) && !update.rendererChunk.isSolid(update.z)) {
            val x = update.x - playerX
            val y = update.y - playerY
            val z = update.z - playerZ
            if (z >= 0) {
                val rendererChunk = update.chunk.rendererChunk()
                if (!rendererChunk.isCulled(update.z + 1)) {
                    pool.push().set(update.x, update.y, update.z + 1,
                            update.chunk, rendererChunk)
                }
            }
            if (z <= 0) {
                val rendererChunk = update.chunk.rendererChunk()
                if (!rendererChunk.isCulled(update.z - 1)) {
                    pool.push().set(update.x, update.y, update.z - 1,
                            update.chunk, rendererChunk)
                }
            }
            if (y <= 0) {
                val chunk = terrain.chunkNoLoad(update.x, update.y - 1)
                if (chunk != null) {
                    val rendererChunk = chunk.rendererChunk()
                    if (!rendererChunk.isCulled(update.z)) {
                        pool.push().set(update.x, update.y - 1, update.z, chunk,
                                rendererChunk)
                    }
                }
            }
            if (x >= 0) {
                val chunk = terrain.chunkNoLoad(update.x + 1, update.y)
                if (chunk != null) {
                    val rendererChunk = chunk.rendererChunk()
                    if (!rendererChunk.isCulled(update.z)) {
                        pool.push().set(update.x + 1, update.y, update.z, chunk,
                                rendererChunk)
                    }
                }
            }
            if (y >= 0) {
                val chunk = terrain.chunkNoLoad(update.x, update.y + 1)
                if (chunk != null) {
                    val rendererChunk = chunk.rendererChunk()
                    if (!rendererChunk.isCulled(update.z)) {
                        pool.push().set(update.x, update.y + 1, update.z, chunk,
                                rendererChunk)
                    }
                }
            }
            if (x <= 0) {
                val chunk = terrain.chunkNoLoad(update.x - 1, update.y)
                if (chunk != null) {
                    val rendererChunk = chunk.rendererChunk()
                    if (!rendererChunk.isCulled(update.z)) {
                        pool.push().set(update.x - 1, update.y, update.z, chunk,
                                rendererChunk)
                    }
                }
            }
        }
    }

    private fun checkLoaded(terrain: TerrainInfiniteClient,
                            x: Int,
                            y: Int): Boolean {
        val chunk = terrain.chunkNoLoad(x, y)
        return chunk != null && chunk.isLoaded
    }

    private class VisibleUpdate {
        var x = 0
        var y = 0
        var z = 0
        lateinit var chunk: TerrainInfiniteChunkClient
        lateinit var rendererChunk: TerrainInfiniteRendererChunk

        fun set(x: Int,
                y: Int,
                z: Int,
                chunk: TerrainInfiniteChunkClient,
                rendererChunk: TerrainInfiniteRendererChunk) {
            this.x = x
            this.y = y
            this.z = z
            this.chunk = chunk
            this.rendererChunk = rendererChunk
        }
    }

    private class ThreadLocalData {
        val arrays = ChunkMesh.VertexArrays()
        val arraysAlpha = ChunkMesh.VertexArrays()
        val section = TerrainInfiniteRenderSection()

        fun process(chunk: TerrainInfiniteRendererChunk,
                    i: Int,
                    renderer: TerrainInfiniteRenderer) {
            if (!chunk.unsetGeometryDirty(i)) {
                return
            }
            var model: TerrainInfiniteChunkModel? = null
            val terrainChunk = chunk.chunk()
            if (terrainChunk.isEmpty(i)) {
                chunk.setSolid(i, false)
            } else {
                val terrain = chunk.chunk().terrain
                val air = terrain.air
                section.init(terrain, terrainChunk.posBlock.x - 16,
                        terrainChunk.posBlock.y - 16, 0, 48, 48,
                        terrainChunk.zSize, true)
                val bx = terrainChunk.posBlock.x
                val by = terrainChunk.posBlock.y
                val bz = i shl 4
                var solid = true
                var empty = true
                profilerSection("CheckSolid") {
                    for (xxx in 0..15) {
                        val bxx = bx + xxx
                        for (yyy in 0..15) {
                            val byy = by + yyy
                            for (zzz in 0..15) {
                                val bzz = bz + zzz
                                val type = terrainChunk.typeL(xxx, yyy, bzz)
                                if (type == air) {
                                    solid = false
                                } else {
                                    empty = false
                                    if (solid && type.connectStage(section, bxx,
                                            byy, bzz) < 4) {
                                        solid = false
                                    }
                                }
                                if (!solid && !empty) {
                                    break
                                }
                            }
                        }
                    }
                }
                if (chunk.isSolid(i) != solid || !chunk.isLoaded) {
                    chunk.setSolid(i, solid)
                    renderer.updateVisible.set(true)
                }
                if (!empty && chunk.isVisible(i)) {
                    val relativeX = terrainChunk.posBlock.x - renderer.cam.position.doubleX()
                    val relativeY = terrainChunk.posBlock.y - renderer.cam.position.doubleY()
                    val relativeZ = (i shl 4) - renderer.cam.position.doubleZ()
                    val lod = sqr(relativeX + 8) +
                            sqr(relativeY + 8) +
                            sqr(relativeZ + 8) < 9216
                    val mesh = ChunkMesh(arrays)
                    val meshAlpha = ChunkMesh(arraysAlpha)
                    val info = TerrainRenderInfo(terrain.world.infoLayers())
                    info.init(bx, by, bz, 16, 16, 16)
                    profilerSection("GenerateMesh") {
                        for (xxx in 0..15) {
                            val bxx = bx + xxx
                            for (yyy in 0..15) {
                                val byy = by + yyy
                                for (zzz in 0..15) {
                                    val bzz = bz + zzz
                                    val block = terrainChunk.blockL(xxx, yyy,
                                            bzz)
                                    val type = terrain.type(block)
                                    val data = terrain.data(block)
                                    type.addToChunkMesh(mesh, meshAlpha, data,
                                            section, info, bxx, byy, bzz,
                                            xxx.toDouble(), yyy.toDouble(),
                                            zzz.toDouble(), lod)
                                }
                            }
                        }
                    }
                    profilerSection("AssembleMesh") {
                        val engine = terrain.world.game.engine
                        val vao: Pair<Model, AABB>?
                        val vaoAlpha: Pair<Model, AABB>?
                        if (mesh.size() > 0) {
                            vao = Pair(mesh.finish(engine), mesh.aabb())
                        } else {
                            vao = null
                        }
                        if (meshAlpha.size() > 0) {
                            vaoAlpha = Pair(meshAlpha.finish(engine),
                                    meshAlpha.aabb())
                        } else {
                            vaoAlpha = null
                        }
                        model = TerrainInfiniteChunkModel(vao, vaoAlpha, lod)
                    }
                }
                section.clear()
            }
            chunk.replaceMesh(i, model)
        }
    }

    companion object {
        private val THREAD_DATA = ThreadLocal { ThreadLocalData() }
    }
}
