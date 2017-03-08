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

import mu.KLogging
import org.tobi29.scapes.block.BlockType
import org.tobi29.scapes.block.Update
import org.tobi29.scapes.chunk.MobSpawner
import org.tobi29.scapes.chunk.WorldServer
import org.tobi29.scapes.chunk.generator.ChunkGenerator
import org.tobi29.scapes.chunk.generator.ChunkPopulator
import org.tobi29.scapes.chunk.generator.GeneratorOutput
import org.tobi29.scapes.chunk.terrain.TerrainChunk
import org.tobi29.scapes.chunk.terrain.TerrainServer
import org.tobi29.scapes.engine.utils.io.tag.TagMap
import org.tobi29.scapes.engine.utils.limit
import org.tobi29.scapes.engine.utils.math.abs
import org.tobi29.scapes.engine.utils.math.max
import org.tobi29.scapes.engine.utils.math.vector.*
import org.tobi29.scapes.engine.utils.profiler.profilerSection
import org.tobi29.scapes.engine.utils.task.ThreadJoiner
import org.tobi29.scapes.entity.server.EntityServer
import org.tobi29.scapes.entity.server.MobPlayerServer
import org.tobi29.scapes.server.format.TerrainInfiniteFormat
import java.io.IOException
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ThreadLocalRandom

class TerrainInfiniteServer(override val world: WorldServer,
                            zSize: Int,
                            private val format: TerrainInfiniteFormat,
                            override val generator: ChunkGenerator,
                            internal val populators: Array<ChunkPopulator>,
                            air: BlockType) : TerrainInfinite<EntityServer>(
        zSize, world.taskExecutor, air, air,
        world.registry), TerrainServer.TerrainMutable {
    private val blockChanges = ConcurrentLinkedQueue<(TerrainServer.TerrainMutable) -> Unit>()
    private val chunkUnloadQueue = ConcurrentLinkedQueue<TerrainInfiniteChunkServer>()
    private val chunkManager = TerrainInfiniteChunkManagerServer()
    private val generatorOutput = GeneratorOutput(zSize)
    private val loadJoiner: ThreadJoiner
    private val updateJoiner: ThreadJoiner

    init {
        loadJoiner = world.taskExecutor.runThread({ joiner ->
            val requiredChunks = HashSet<Vector2i>()
            val loadingChunks = ArrayList<Vector2i>()
            while (!joiner.marked) {
                val players = world.players()
                if (players.isEmpty()) {
                    chunkManager.stream().forEach {
                        chunkUnloadQueue.add(it)
                    }
                    removeChunks()
                    joiner.sleep(100)
                } else {
                    for (player in players) {
                        val pos = player.getCurrentPos()
                        val xx = pos.intX() shr 4
                        val yy = pos.intY() shr 4
                        val loadingRadius = (player.connection().loadingRadius() shr 4) + 2
                        // Add 48 to provide circular sendable area
                        // Note: 48 was found to be just enough to avoid request spam
                        val loadingRadiusSqr = loadingRadius * loadingRadius + 48
                        for (x in -loadingRadius..loadingRadius) {
                            val xxx = x + xx
                            for (y in -loadingRadius..loadingRadius) {
                                val yyy = y + yy
                                if (xxx in cxMin..cxMax &&
                                        yyy >= cyMin && yyy <= cyMax) {
                                    if (x * x + y * y <= loadingRadiusSqr) {
                                        requiredChunks.add(Vector2i(x + xx,
                                                y + yy))
                                    }
                                }
                            }
                        }
                    }
                    for (player in players) {
                        val loadArea = Vector3i(player.getCurrentPos() / 16.0)
                        requiredChunks.asSequence().filter {
                            !hasChunk(it.x, it.y)
                        }.forEach { loadingChunks.add(it) }
                        val newChunks: List<Vector2i>
                        if (loadingChunks.size > 64) {
                            newChunks = loadingChunks.asSequence().sortedBy {
                                it.distanceSqr(loadArea)
                            }.limit(32).toList()
                        } else {
                            newChunks = loadingChunks.asSequence().limit(
                                    32).toList()
                        }
                        addChunks(newChunks)
                        chunkManager.stream().filter(
                                { it.shouldPopulate() }).limit(
                                32).forEach({ it.populate() })
                        chunkManager.stream().filter(
                                TerrainInfiniteChunkServer::shouldFinish).forEach(
                                TerrainInfiniteChunkServer::finish)
                        val time = System.currentTimeMillis() - 2000
                        chunkManager.stream().filter { chunk ->
                            chunk.lastAccess() < time && !requiredChunks.contains(
                                    chunk.pos)
                        }.forEach { chunkUnloadQueue.add(it) }
                        chunkManager.stream().forEach { it.updateAdjacent() }
                        if (removeChunks() && loadingChunks.isEmpty()) {
                            joiner.sleep(100)
                        }
                        loadingChunks.clear()
                    }
                    requiredChunks.clear()
                }
            }
        }, "Chunk-Loading")
        updateJoiner = world.taskExecutor.runThread({ joiner ->
            while (!joiner.marked) {
                var idle = true
                while (!blockChanges.isEmpty()) {
                    blockChanges.poll()(this)
                    idle = false
                }
                if (idle) {
                    joiner.sleep()
                }
            }
        }, "Chunk-Updating")
    }

    override fun sunLightReduction(x: Int,
                                   y: Int): Int {
        return world.environment.sunLightReduction(x.toDouble(),
                y.toDouble()).toInt()
    }

    override fun addEntity(entity: EntityServer): Boolean {
        val pos = entity.getCurrentPos()
        val x = pos.intX() shr 4
        val y = pos.intY() shr 4
        return chunk(x, y, { chunk ->
            chunk.addEntity(entity)
            true
        }) ?: false
    }

    override fun entityAdded(entity: EntityServer) {
        super.entityAdded(entity)
        world.entityAdded(entity)
    }

    override fun entityRemoved(entity: EntityServer) {
        super.entityRemoved(entity)
        world.entityRemoved(entity)
    }

    override fun hasChunk(x: Int,
                          y: Int): Boolean {
        return chunkManager.has(x, y)
    }

    override fun chunk(x: Int,
                       y: Int): TerrainInfiniteChunkServer? {
        val chunk = chunkManager[x, y]
        if (chunk != null) {
            chunk.accessed()
            return chunk
        }
        return addChunk(x, y)
    }

    override fun chunkNoLoad(x: Int,
                             y: Int): TerrainInfiniteChunkServer? {
        return chunkManager[x, y]
    }

    override fun loadedChunks() = chunkManager.stream()

    fun addChunk(x: Int,
                 y: Int): TerrainInfiniteChunkServer? {
        return addChunks(listOf(Vector2i(x, y)))[0]
    }

    fun addChunks(
            positions: List<Vector2i>): List<TerrainInfiniteChunkServer?> {
        val chunks = ArrayList<TerrainInfiniteChunkServer?>(
                positions.size)
        val chunkMaps = format.chunkTags(positions)
        for (i in positions.indices) {
            val pos = positions[i]
            val x = pos.x
            val y = pos.y
            if (x < cxMin || x > cxMax || y < cyMin || y > cyMax) {
                chunks.add(null)
                continue
            }
            var chunk: TerrainInfiniteChunkServer? = null
            synchronized(chunkManager) {
                chunk = chunkManager[x, y]
                if (chunk == null) {
                    val map = chunkMaps[i]
                    if (map != null) {
                        profilerSection("Load") {
                            var time = System.currentTimeMillis()
                            val chunk2 = TerrainInfiniteChunkServer(
                                    Vector2i(x, y), this, zSize, map)
                            time = System.currentTimeMillis() - time
                            logger.debug { "Chunk loaded in ${time}ms" }
                            chunkManager.add(chunk2)
                            updateAdjacent(chunk2.pos.x, chunk2.pos.y)
                            chunk = chunk2
                        }
                    } else {
                        profilerSection("Generate") {
                            var time = System.currentTimeMillis()
                            val chunk2 = TerrainInfiniteChunkServer(
                                    Vector2i(x, y), this, zSize,
                                    generator, generatorOutput)
                            time = System.currentTimeMillis() - time
                            logger.debug { "Chunk generated in ${time}ms" }
                            chunkManager.add(chunk2)
                            updateAdjacent(chunk2.pos.x, chunk2.pos.y)
                            chunk = chunk2
                        }
                    }
                }
            }
            chunks.add(chunk)
        }
        return chunks
    }

    override fun update(delta: Double,
                        spawners: Collection<MobSpawner>) {
        profilerSection("Chunks") {
            chunkManager.stream().forEach { it.updateServer(delta) }
        }
        profilerSection("Spawning") {
            val random = ThreadLocalRandom.current()
            for (spawner in spawners) {
                if (world.mobs(
                        spawner.creatureType()) < chunkManager.chunks() * spawner.mobsPerChunk()) {
                    for (chunk in chunkManager.stream()) {
                        if (random.nextInt(
                                spawner.chunkChance()) == 0 && chunk.isLoaded) {
                            for (i in 0..spawner.spawnAttempts() - 1) {
                                val x = random.nextInt(chunk.size.x)
                                val y = random.nextInt(chunk.size.y)
                                val z = random.nextInt(chunk.size.z)
                                val xx = x + chunk.posBlock.x
                                val yy = y + chunk.posBlock.y
                                if (spawner.creatureType().doesDespawn()) {
                                    val player = world.nearestPlayer(
                                            Vector3d(xx.toDouble(),
                                                    yy.toDouble(),
                                                    z.toDouble()))
                                    if (player == null) {
                                        continue
                                    } else {
                                        val distance = player.getCurrentPos() distanceSqr Vector3d(
                                                xx + 0.5, yy + 0.5, z + 0.5)
                                        if (distance > 9216.0 || distance < 256.0) {
                                            continue
                                        }
                                    }
                                }
                                if (spawner.canSpawn(this, xx, yy, z)) {
                                    val entity = spawner.spawn(this, xx, yy, z)
                                    world.addEntityNew(entity)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun queue(blockChanges: (TerrainServer.TerrainMutable) -> Unit) {
        assert(world.checkThread() || Thread.currentThread().let { it == loadJoiner.thread || it == updateJoiner.thread })
        this.blockChanges.add(blockChanges)
        updateJoiner.wake()
    }

    override fun addDelayedUpdate(update: Update) {
        chunkS(update.x() shr 4, update.y() shr 4,
                { chunk -> chunk.addDelayedUpdate(update) })
    }

    override fun hasDelayedUpdate(x: Int,
                                  y: Int,
                                  z: Int,
                                  clazz: Class<out Update>): Boolean {
        if (z < 0 || z >= zSize) {
            return false
        }
        return chunkS(x shr 4, y shr 4,
                { chunk -> chunk.hasDelayedUpdate(x, y, z, clazz) }) ?: false
    }

    override fun isBlockSendable(player: MobPlayerServer,
                                 x: Int,
                                 y: Int,
                                 z: Int,
                                 chunkContent: Boolean): Boolean {
        if (z < 0 || z >= zSize) {
            return false
        }
        val loadingRadius = player.connection().loadingRadius() shr 4
        val xx = x shr 4
        val yy = y shr 4
        val pos = player.getCurrentPos()
        val x2 = pos.intX() shr 4
        val y2 = pos.intY() shr 4
        val chunk = chunkNoLoad(xx, yy)
        val dis = max(abs(xx - x2), abs(yy - y2))
        if (chunk != null) {
            if (dis > loadingRadius || !chunk.isSendable && chunkContent) {
                return false
            }
        } else {
            if (dis > loadingRadius) {
                return false
            }
        }
        return true
    }

    override fun chunks(consumer: (TerrainChunk) -> Unit) {
        chunkManager.stream().filter { it.isLoaded }.forEach { consumer(it) }
    }

    override fun dispose() {
        loadJoiner.join()
        updateJoiner.join()
        lighting.dispose()
        chunkManager.stream().forEach { chunkUnloadQueue.add(it) }
        removeChunks()
        format.dispose()
    }

    override fun type(x: Int,
                      y: Int,
                      z: Int,
                      type: BlockType) {
        if (z < 0 || z >= zSize) {
            return
        }
        chunk(x shr 4, y shr 4) { chunk -> chunk.blockTypeG(x, y, z, type) }
    }

    override fun data(x: Int,
                      y: Int,
                      z: Int,
                      data: Int) {
        if (z < 0 || z >= zSize) {
            return
        }
        chunk(x shr 4, y shr 4) { chunk -> chunk.dataG(x, y, z, data) }
    }

    override fun typeData(x: Int,
                          y: Int,
                          z: Int,
                          block: BlockType,
                          data: Int) {
        if (z < 0 || z >= zSize) {
            return
        }
        chunk(x shr 4, y shr 4) { chunk ->
            chunk.typeDataG(x, y, z, block, data)
        }
    }

    fun updateAdjacent(x: Int,
                       y: Int) {
        for (xx in -1..1) {
            val xxx = xx + x
            for (yy in -1..1) {
                if (xx != 0 || yy != 0) {
                    chunkManager[xxx, yy + y]?.updateAdjacent()
                }
            }
        }
    }

    fun checkBorder(chunk: TerrainInfiniteChunkServer,
                    radius: Int): Boolean {
        for (x in -radius..radius) {
            val xx = chunk.pos.x + x
            for (y in -radius..radius) {
                val yy = chunk.pos.y + y
                if (xx in cxMin..cxMax && yy >= cyMin && yy <= cyMax) {
                    if (chunkNoLoad(xx, yy) == null) {
                        return false
                    }
                }
            }
        }
        return true
    }

    fun checkLoaded(chunk: TerrainInfiniteChunkServer,
                    radius: Int): Boolean {
        for (x in -radius..radius) {
            val xx = chunk.pos.x + x
            for (y in -radius..radius) {
                val yy = chunk.pos.y + y
                if (xx in cxMin..cxMax && yy >= cyMin && yy <= cyMax) {
                    val check = chunkNoLoad(xx, yy)
                    if (check == null || !check.isLoaded) {
                        return false
                    }
                }
            }
        }
        return true
    }

    private fun removeChunks(): Boolean {
        val chunks = ArrayList<Pair<Vector2i, TagMap>>()
        while (!chunkUnloadQueue.isEmpty()) {
            synchronized(this) {
                chunks.add(removeChunk(chunkUnloadQueue.poll()))
            }
        }
        if (chunks.isEmpty()) {
            return true
        }
        try {
            format.putChunkTags(chunks)
        } catch (e: IOException) {
            logger.error(e) { "Failed to store chunks" }
        }
        return false
    }

    private fun removeChunk(
            chunk: TerrainInfiniteChunkServer): Pair<Vector2i, TagMap> {
        val x = chunk.pos.x
        val y = chunk.pos.y
        chunkManager.remove(x, y)
        val tagStructure = chunk.dispose()
        while (chunkUnloadQueue.remove(chunk)) {
            logger.warn { "Chunk queued for unloading twice!" }
        }
        updateAdjacent(chunk.pos.x, chunk.pos.y)
        return Pair(Vector2i(x, y), tagStructure)
    }

    companion object : KLogging()
}
