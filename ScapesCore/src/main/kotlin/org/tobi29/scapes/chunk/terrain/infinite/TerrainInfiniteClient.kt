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
import kotlinx.coroutines.experimental.channels.ActorJob
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import org.tobi29.scapes.block.BlockType
import org.tobi29.scapes.chunk.WorldClient
import org.tobi29.scapes.chunk.terrain.TerrainClient
import org.tobi29.scapes.engine.math.vector.MutableVector2i
import org.tobi29.scapes.engine.math.vector.Vector2i
import org.tobi29.scapes.engine.math.vector.distanceSqr
import org.tobi29.scapes.engine.math.vector.lengthSqr
import org.tobi29.scapes.engine.utils.AtomicBoolean
import org.tobi29.scapes.engine.utils.ConcurrentHashSet
import org.tobi29.scapes.engine.utils.ThreadLocal
import org.tobi29.scapes.engine.utils.logging.KLogging
import org.tobi29.scapes.engine.utils.math.sqr
import org.tobi29.scapes.engine.utils.profiler.profilerSection
import org.tobi29.scapes.engine.utils.tag.TagMap
import org.tobi29.scapes.engine.utils.task.Timer
import org.tobi29.scapes.engine.utils.task.loop
import org.tobi29.scapes.entity.client.EntityClient
import org.tobi29.scapes.packets.PacketBlockChange
import org.tobi29.scapes.packets.PacketRequestChunk
import org.tobi29.scapes.terrain.infinite.TerrainInfiniteChunkManagerStatic
import kotlinx.coroutines.experimental.TimeUnit
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.buildSequence

class TerrainInfiniteClient private constructor(override val world: WorldClient,
                                                private val loadingRadius: Int,
                                                zSize: Int,
                                                private val taskExecutor: CoroutineContext,
                                                air: BlockType,
                                                private val center: MutableVector2i) : TerrainInfinite<EntityClient, TerrainInfiniteChunkClient>(
        zSize, taskExecutor, air, world.registry,
        TerrainInfiniteChunkManagerStatic(center,
                loadingRadius)), TerrainClient {
    override val renderer: TerrainInfiniteRenderer
    private val sortedLocations: List<Vector2i>
    private var requestActor: ActorJob<Unit>? = null
    private var requestHeartbeatJob: Pair<Job, AtomicBoolean>? = null
    private val chunkRequests = ConcurrentHashSet<Vector2i>()

    constructor(world: WorldClient,
                loadingRadius: Int,
                zSize: Int,
                taskExecutor: CoroutineContext,
                air: BlockType) : this(world, loadingRadius, zSize,
            taskExecutor, air, MutableVector2i())

    init {
        sortedLocations = buildSequence {
            for (yy in -loadingRadius..loadingRadius) {
                for (xx in -loadingRadius..loadingRadius) {
                    yield(Vector2i(xx, yy))
                }
            }
        }.sortedBy { it.distanceSqr(Vector2i.ZERO) }.toList()
        renderer = TerrainInfiniteRenderer(this, loadingRadius.toDouble(),
                sortedLocations)
    }

    override fun getThreadContext(): TerrainInfiniteClientSection = SECTION.get()

    override fun update(delta: Double) {
        profilerSection("Chunks") {
            chunkManager.stream().forEach { it.updateClient(delta) }
        }
        profilerSection("ChunkManager") {
            val pos = world.player.getCurrentPos()
            val xx = pos.intX() shr 4
            val yy = pos.intY() shr 4
            center.set(xx, yy)
            if (chunkManager.update()) {
                requestActor?.offer(Unit)
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
            chunk.lockWrite {
                chunk.typeDataG(x, y, z, type(packet.id()), packet.data())
            }
        }
    }

    override fun init() {
        val loadingRadiusSqr = sqr(loadingRadius)
        requestActor = actor(taskExecutor + CoroutineName("Load-Requests")) {
            for (msg in channel) {
                var isLoading = false
                val playerPos = world.player.getCurrentPos()
                val x = playerPos.intX() shr 4
                val y = playerPos.intY() shr 4
                for (pos in sortedLocations) {
                    val xx = pos.x + x
                    val yy = pos.y + y
                    val chunk = chunkManager[xx, yy]
                    if (pos.lengthSqr() < loadingRadiusSqr) {
                        if (chunk == null && !isLoading) {
                            val p = Vector2i(xx, yy)
                            if (chunkRequests.size < 3
                                    && !chunkRequests.contains(p)) {
                                chunkRequests.add(p)
                                world.send(
                                        PacketRequestChunk(
                                                world.plugins.registry, xx, yy))
                            } else {
                                isLoading = true
                            }
                        }
                    } else if (chunk != null) {
                        removeChunk(chunk)
                    }
                }
            }
        }
        val stop = AtomicBoolean(false)
        requestHeartbeatJob = launch(
                taskExecutor + CoroutineName("Load-Requests-Heartbeat")) {
            Timer().apply { init() }.loop(Timer.toDiff(100.0),
                    { delay(it, TimeUnit.NANOSECONDS) }) {
                if (stop.get()) return@loop false

                requestActor?.offer(Unit)

                true
            }
        } to stop
    }

    override suspend fun dispose() {
        requestActor?.let {
            it.close()
            it.join()
        }
        lighting.dispose()
        chunkManager.stream().forEach { removeChunk(it) }
        renderer.dispose()
    }

    override fun entityAdded(entity: EntityClient,
                             spawn: Boolean) {
        super.entityAdded(entity, spawn)
        world.entityAdded(entity, spawn)
    }

    override fun entityRemoved(entity: EntityClient) {
        super.entityRemoved(entity)
        world.entityRemoved(entity)
    }

    override fun addChunk(x: Int,
                          y: Int) = null

    fun loadChunk(x: Int,
                  y: Int,
                  tagMap: TagMap) {
        synchronized(chunkManager) {
            if (chunkManager[x, y] != null) {
                logger.warn { "Chunk received twice: $x/$y" }
                chunkManager.remove(x, y)
            }
            val chunk = TerrainInfiniteChunkClient(Vector2i(x, y), this, zSize,
                    renderer)
            chunk.lockWrite {
                chunk.read(tagMap)
                chunk.setLoaded()
            }
            chunkManager.add(chunk)
        }
        for (xx in -1..1) {
            val xxx = x + xx
            for (yy in -1..1) {
                val yyy = y + yy
                chunkNoLoad(xxx, yyy)?.rendererChunk()?.setGeometryDirty()
            }
        }
        chunkRequests.remove(Vector2i(x, y))
    }

    fun failedChunk(x: Int,
                    y: Int) {
        chunkRequests.remove(Vector2i(x, y))
    }

    private fun removeChunk(chunk: TerrainInfiniteChunkClient) {
        val x = chunk.pos.x
        val y = chunk.pos.y
        chunkManager.remove(x, y)
        chunk.dispose()
    }

    override fun sunLightReduction(x: Int,
                                   y: Int): Int {
        return world.environment.sunLightReduction(x.toDouble(),
                y.toDouble()).toInt()
    }

    override fun addEntity(entity: EntityClient,
                           spawn: Boolean): Boolean {
        val pos = entity.getCurrentPos()
        val x = pos.intX() shr 4
        val y = pos.intY() shr 4
        return chunkC(x, y, { chunk ->
            chunk.addEntity(entity, spawn)
            true
        }) ?: false
    }

    companion object : KLogging() {
        val SECTION = ThreadLocal { TerrainInfiniteClientSection() }
    }
}
