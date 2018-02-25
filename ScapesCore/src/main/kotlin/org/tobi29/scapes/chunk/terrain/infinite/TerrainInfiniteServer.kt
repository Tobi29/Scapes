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

import kotlinx.coroutines.experimental.CoroutineName
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import org.tobi29.coroutines.Timer
import org.tobi29.coroutines.loop
import org.tobi29.logging.KLogging
import org.tobi29.scapes.block.BlockType
import org.tobi29.scapes.block.Update
import org.tobi29.scapes.chunk.MobSpawner
import org.tobi29.scapes.chunk.WorldServer
import org.tobi29.scapes.chunk.generator.ChunkGenerator
import org.tobi29.scapes.chunk.generator.ChunkPopulator
import org.tobi29.scapes.chunk.terrain.TerrainMutableServer
import org.tobi29.scapes.chunk.terrain.TerrainServer
import org.tobi29.math.threadLocalRandom
import org.tobi29.math.vector.Vector2i
import org.tobi29.math.vector.Vector3d
import org.tobi29.math.vector.distanceSqr
import org.tobi29.io.IOException
import org.tobi29.profiler.profilerSection
import org.tobi29.scapes.entity.server.EntityServer
import org.tobi29.scapes.entity.server.MobPlayerServer
import org.tobi29.scapes.server.format.TerrainInfiniteFormat
import org.tobi29.scapes.terrain.TerrainChunk
import org.tobi29.scapes.terrain.infinite.TerrainInfiniteChunkManagerDynamic
import org.tobi29.io.tag.TagMap
import org.tobi29.stdex.ThreadLocal
import org.tobi29.stdex.atomic.AtomicBoolean
import org.tobi29.stdex.math.floorToInt
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.max

class TerrainInfiniteServer(override val world: WorldServer,
                            zSize: Int,
                            private val format: TerrainInfiniteFormat,
                            override val generator: ChunkGenerator,
                            internal val populators: Array<ChunkPopulator>,
                            air: BlockType) : TerrainInfinite<EntityServer, TerrainInfiniteChunkServer>(
        zSize, world.taskExecutor, air, world.registry,
        TerrainInfiniteChunkManagerDynamic<TerrainInfiniteChunkServer>()),
        TerrainServer {
    // TODO: Port away
    private val chunkUnloadQueue = ConcurrentLinkedQueue<TerrainInfiniteChunkServer>()
    private var loadJob: Pair<Job, AtomicBoolean>? = null

    init {
        val stop = AtomicBoolean(false)
        loadJob = launch(world.taskExecutor + CoroutineName("Chunk-Loading")) {
            val loader = TerrainInfiniteChunkLoader(loadedChunks(), cxMin,
                    cxMax, cyMin, cyMax)

            Timer().apply { init() }.loop(Timer.toDiff(10.0),
                    { delay(it, TimeUnit.NANOSECONDS) }) {
                if (stop.get()) return@loop false

                while (true) {
                    for (player in world.players()) {
                        val pos = player.getCurrentPos()
                        val xx = pos.x.floorToInt() shr 4
                        val yy = pos.y.floorToInt() shr 4
                        val loadingRadius = (player.connection().loadingRadius() shr 4) + 2
                        loader.requireCircle(xx, yy, loadingRadius)
                    }

                    val activeRemove = removeChunks(loader.outsideChunks)
                    val activeAdd = addChunks(loader.requiredChunks {
                        !chunkManager.has(it.x, it.y)
                    }.map { it.now() }.toList()).isNotEmpty()
                    chunkManager.stream().forEach { it.updateAdjacent() }
                    chunkManager.stream().filter { it.shouldPopulate() }
                            .forEach { it.populate() }
                    chunkManager.stream().filter { it.shouldFinish() }
                            .forEach(TerrainInfiniteChunkServer::finish)
                    val active = activeAdd || activeRemove
                    loader.reset()

                    if (!active) break
                }

                true
            }
        } to stop
    }

    override fun getThreadContext(): TerrainInfiniteServerSection = SECTION.get()

    override fun sunLightReduction(x: Int,
                                   y: Int): Int {
        return world.environment.sunLightReduction(x.toDouble(),
                y.toDouble()).toInt()
    }

    override fun addEntity(entity: EntityServer,
                           spawn: Boolean): Boolean {
        val pos = entity.getCurrentPos()
        val x = pos.x.floorToInt() shr 4
        val y = pos.y.floorToInt() shr 4
        return chunk(x, y, { chunk ->
            chunk.addEntity(entity)
            true
        }) ?: false
    }

    override fun entityAdded(entity: EntityServer,
                             spawn: Boolean) {
        super.entityAdded(entity, spawn)
        world.entityAdded(entity, spawn)
    }

    override fun entityRemoved(entity: EntityServer) {
        super.entityRemoved(entity)
        world.entityRemoved(entity)
    }

    override fun addChunk(x: Int,
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
            chunks.add(synchronized(chunkManager) {
                var chunk = chunkManager[x, y]
                if (chunk == null) {
                    val map = chunkMaps[i]
                    chunk = TerrainInfiniteChunkServer(Vector2i(x, y), this,
                            zSize)
                    if (map == null) {
                        chunk.generate(generator)
                    } else {
                        chunk.load(map)
                    }
                    chunkManager.add(chunk)
                    updateAdjacent(chunk.pos.x, chunk.pos.y)
                }
                chunk
            })
        }
        return chunks
    }

    override fun update(delta: Double,
                        spawners: Collection<MobSpawner>) {
        profilerSection("Chunks") {
            chunkManager.stream().forEach { it.updateServer(delta) }
        }
        profilerSection("Spawning") {
            val random = threadLocalRandom()
            for (spawner in spawners) {
                if (world.mobs(
                        spawner.creatureType()) < chunkManager.chunks * spawner.mobsPerChunk()) {
                    for (chunk in chunkManager.stream()) {
                        if (random.nextInt(
                                spawner.chunkChance()) == 0 && chunk.isLoaded) {
                            for (i in 0 until spawner.spawnAttempts()) {
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
        profilerSection("ChunkManager") {
            chunkManager.update()
        }
    }

    override fun <R> modify(x: Int,
                            y: Int,
                            z: Int,
                            dx: Int,
                            dy: Int,
                            dz: Int,
                            block: (TerrainMutableServer) -> R): R {
        val section = SECTION.get()
        profilerSection("Terrain-Modify-Init") {
            section.init(this, x, y, z, dx, dy, dz)
            section.lockChunks()
        }
        try {
            profilerSection("Terrain-Modify-Block") {
                return block(section)
            }
        } finally {
            profilerSection("Terrain-Modify-Flush") {
                section.unlockChunks()
                section.flushPackets()
                section.flushUpdates()
                section.clear()
            }
        }
    }

    override fun hasDelayedUpdate(x: Int,
                                  y: Int,
                                  z: Int,
                                  clazz: Class<out Update>): Boolean {
        if (z < 0 || z >= zSize) {
            return false
        }
        return chunkS(x shr 4, y shr 4) {
            it.hasDelayedUpdate(x, y, z, clazz)
        } ?: false
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
        val x2 = pos.x.floorToInt() shr 4
        val y2 = pos.y.floorToInt() shr 4
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

    override suspend fun dispose() {
        loadJob?.let { (job, stop) ->
            stop.set(true)
            job.join()
        }
        lighting.dispose()
        chunkManager.stream().forEach { chunkUnloadQueue.add(it) }
        removeChunks()
        format.dispose()
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
                chunkUnloadQueue.poll()?.let { chunks.add(removeChunk(it)) }
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

    private fun removeChunks(oi: Sequence<TerrainInfiniteChunkServer>): Boolean {
        val chunks = ArrayList<Pair<Vector2i, TagMap>>()
        synchronized(this) {
            oi.forEach { chunks.add(removeChunk(it)) }
        }
        if (chunks.isEmpty()) {
            return false
        }
        try {
            format.putChunkTags(chunks)
        } catch (e: IOException) {
            logger.error(e) { "Failed to store chunks" }
        }
        return true
    }

    private fun removeChunk(
            chunk: TerrainInfiniteChunkServer): Pair<Vector2i, TagMap> {
        val x = chunk.pos.x
        val y = chunk.pos.y
        chunkManager.remove(x, y)
        val tagStructure = chunk.disposeAndWrite()
        while (chunkUnloadQueue.remove(chunk)) {
            logger.warn { "Chunk queued for unloading twice!" }
        }
        updateAdjacent(chunk.pos.x, chunk.pos.y)
        return Pair(Vector2i(x, y), tagStructure)
    }

    companion object : KLogging() {
        val SECTION = ThreadLocal { TerrainInfiniteServerSection() }
    }
}
