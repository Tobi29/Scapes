/*
 * Copyright 2012-2016 Tobi29
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

import java8.util.stream.Collectors
import org.tobi29.scapes.block.BlockType
import org.tobi29.scapes.chunk.WorldClient
import org.tobi29.scapes.chunk.terrain.TerrainClient
import org.tobi29.scapes.engine.utils.math.sqr
import org.tobi29.scapes.engine.utils.math.vector.Vector2i
import org.tobi29.scapes.engine.utils.math.vector.distanceSqr
import org.tobi29.scapes.engine.utils.math.vector.lengthSqr
import org.tobi29.scapes.engine.utils.profiler.profilerSection
import org.tobi29.scapes.engine.utils.stream
import org.tobi29.scapes.engine.utils.task.Joiner
import org.tobi29.scapes.engine.utils.task.TaskExecutor
import org.tobi29.scapes.entity.client.EntityClient
import org.tobi29.scapes.entity.client.MobPlayerClientMain
import org.tobi29.scapes.packets.PacketBlockChange
import java.util.*

class TerrainInfiniteClient(override val world: WorldClient, loadingRadius: Int,
                            zSize: Int, taskExecutor: TaskExecutor, air: BlockType) : TerrainInfinite<EntityClient>(
        zSize, taskExecutor, air, air, world.registry.blocks()), TerrainClient {
    override val renderer: TerrainInfiniteRenderer
    override val player: MobPlayerClientMain
    private val sortedLocations: List<Vector2i>
    private val chunkManager: TerrainInfiniteChunkManagerClient
    private val loadingRadiusSqr: Int
    private val joiner: Joiner
    private var requestedChunks = 0

    init {
        loadingRadiusSqr = loadingRadius * loadingRadius
        player = world.player
        chunkManager = TerrainInfiniteChunkManagerClient(loadingRadius)
        val locations = ArrayList<Vector2i>()
        val loadingRadiusSqr = sqr(loadingRadius)
        for (yy in -loadingRadius..loadingRadius) {
            for (xx in -loadingRadius..loadingRadius) {
                locations.add(Vector2i(xx, yy))
            }
        }
        Collections.sort(locations) { vector1, vector2 ->
            val distance1 = vector1.distanceSqr(Vector2i.ZERO)
            val distance2 = vector2.distanceSqr(Vector2i.ZERO)
            if (distance1 == distance2) 0 else if (distance1 > distance2) 1 else -1
        }
        sortedLocations = locations.stream().collect(
                Collectors.toList<Vector2i>())
        renderer = TerrainInfiniteRenderer(this, player,
                loadingRadius.toDouble(),
                sortedLocations)
        joiner = taskExecutor.runThread({ joiner ->
            while (!joiner.marked) {
                var active = false
                val playerPos = player.getCurrentPos()
                val x = playerPos.intX() shr 4
                val y = playerPos.intY() shr 4
                for (pos in sortedLocations) {
                    var chunk = chunkManager[pos.x + x, pos.y + y]
                    if (pos.lengthSqr() < loadingRadiusSqr) {
                        if (chunk == null) {
                            chunk = addChunk(pos.x + x, pos.y + y)
                        }
                        if (chunk != null && chunk.checkLoaded()) {
                            active = true
                        }
                    } else if (chunk != null) {
                        removeChunk(chunk)
                    }
                }
                if (active) {
                    joiner.sleep(10)
                } else {
                    joiner.sleep()
                }
            }
        }, "Chunk-Requests")
    }

    fun requestedChunks(): Int {
        return requestedChunks
    }

    fun changeRequestedChunks(add: Int) {
        requestedChunks += add
    }

    override fun update(delta: Double) {
        profilerSection("Chunks") {
            chunkManager.stream().forEach { it.updateClient(delta) }
        }
        profilerSection("Center") {
            val pos = player.getCurrentPos()
            val xx = pos.intX() shr 4
            val yy = pos.intY() shr 4
            if (chunkManager.setCenter(xx, yy) { it.dispose() }) {
                joiner.wake()
            }
        }
    }

    override fun toggleStaticRenderDistance() {
        renderer.toggleStaticRenderDistance()
    }

    override fun reloadGeometry() {
        renderer.reloadGeometry()
    }

    override fun process(packet: PacketBlockChange) {
        val x = packet.x()
        val y = packet.y()
        val z = packet.z()
        chunkC(x shr 4, y shr 4) { chunk ->
            chunk.typeDataG(x, y, z,
                    world.plugins.registry().block(packet.id()) ?: air,
                    packet.data())
        }
    }

    override fun dispose() {
        joiner.join()
        lighting.dispose()
        chunkManager.stream().forEach { removeChunk(it) }
        renderer.dispose()
    }

    override fun entityAdded(entity: EntityClient) {
        super.entityAdded(entity)
        world.entityAdded(entity)
    }

    override fun entityRemoved(entity: EntityClient) {
        super.entityRemoved(entity)
        world.entityRemoved(entity)
    }

    override fun hasChunk(x: Int,
                          y: Int): Boolean {
        return chunkManager.has(x, y)
    }

    override fun chunk(x: Int,
                       y: Int): TerrainInfiniteChunkClient? {
        return chunkManager[x, y]
    }

    override fun chunkNoLoad(x: Int,
                             y: Int): TerrainInfiniteChunkClient? {
        return chunkManager[x, y]
    }

    override fun loadedChunks() = chunkManager.stream()

    fun addChunk(x: Int,
                 y: Int): TerrainInfiniteChunkClient? {
        if (x < cxMin || x > cxMax || y < cyMin || y > cyMax) {
            return null
        }
        profilerSection("Allocate Chunk") {
            var chunk: TerrainInfiniteChunkClient? = null
            synchronized(chunkManager) {
                chunk = chunkManager[x, y]
                if (chunk == null) {
                    val chunk2 = TerrainInfiniteChunkClient(Vector2i(x, y),
                            this,
                            zSize, renderer)
                    chunkManager.add(chunk2)
                    chunk = chunk2
                }
            }
            return chunk
        }
    }

    private fun removeChunk(chunk: TerrainInfiniteChunkClient) {
        val x = chunk.pos.x
        val y = chunk.pos.y
        chunkManager.remove(x, y)?.let { it.dispose() }
    }

    override fun sunLightReduction(x: Int,
                                   y: Int): Int {
        return world.environment.sunLightReduction(x.toDouble(),
                y.toDouble()).toInt()
    }

    override fun addEntity(entity: EntityClient): Boolean {
        val pos = entity.getCurrentPos()
        val x = pos.intX() shr 4
        val y = pos.intY() shr 4
        return chunkC(x, y, { chunk ->
            chunk.addEntity(entity)
            true
        }) ?: false
    }
}
