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
package org.tobi29.scapes.chunk

import kotlinx.coroutines.experimental.*
import org.tobi29.coroutines.Timer
import org.tobi29.coroutines.launchThread
import org.tobi29.logging.KLogging
import org.tobi29.scapes.chunk.terrain.TerrainServer
import org.tobi29.scapes.chunk.terrain.isTransparent
import org.tobi29.scapes.connection.PlayConnection
import org.tobi29.math.vector.Vector3d
import org.tobi29.math.vector.distanceSqr
import org.tobi29.profiler.profilerSection
import org.tobi29.utils.sleepNanos
import org.tobi29.scapes.entity.CreatureType
import org.tobi29.scapes.entity.server.EntityServer
import org.tobi29.scapes.entity.server.MobLivingServer
import org.tobi29.scapes.entity.server.MobPlayerServer
import org.tobi29.scapes.entity.server.MobServer
import org.tobi29.scapes.packets.PacketClient
import org.tobi29.scapes.packets.PacketEntityAdd
import org.tobi29.scapes.packets.PacketEntityDespawn
import org.tobi29.scapes.packets.PacketSoundEffect
import org.tobi29.scapes.server.connection.PlayerConnection
import org.tobi29.scapes.server.connection.ServerConnection
import org.tobi29.scapes.server.format.WorldFormat
import org.tobi29.io.tag.*
import org.tobi29.stdex.ConcurrentHashMap
import org.tobi29.stdex.ConcurrentHashSet
import org.tobi29.stdex.EnumMap
import org.tobi29.stdex.atomic.AtomicInt
import org.tobi29.stdex.math.floorToInt
import org.tobi29.uuid.Uuid
import java.util.concurrent.locks.LockSupport
import kotlin.collections.set
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.math.sqrt

class WorldServer(
        worldFormat: WorldFormat,
        val id: String,
        seed: Long,
        val connection: ServerConnection,
        taskExecutor: CoroutineContext,
        terrainSupplier: (WorldServer) -> TerrainServer,
        environmentSupplier: (WorldServer) -> EnvironmentServer
) : World<EntityServer>(worldFormat.plugins, taskExecutor,
        worldFormat.plugins.registry, seed),
        TagMapWrite,
        PlayConnection<PacketClient> {
    private var updateJob: Job? = null
    private val entityListeners = ConcurrentHashSet<(EntityServer) -> Unit>()
    private val spawners = ConcurrentHashSet<MobSpawner>()
    private val players = ConcurrentHashMap<String, MobPlayerServer>()
    private val entityCounts: Map<CreatureType, AtomicInt> = EnumMap<CreatureType, AtomicInt>().apply {
        CreatureType.values().forEach { put(it, AtomicInt()) }
    }
    val environment: EnvironmentServer = environmentSupplier(this)
    val terrain: TerrainServer = terrainSupplier(this)

    init {
        worldFormat.plugins.plugins.forEach { it.worldInit(this) }
    }

    fun connection(): ServerConnection {
        return connection
    }

    fun read(tagStructure: TagMap) {
        tagStructure["Tick"]?.toLong()?.let { tick = it }
        tagStructure["Environment"]?.toMap()?.let { environment.read(it) }
    }

    override fun write(map: ReadWriteTagMap) {
        map["Tick"] = tick.toTag()
        map["Environment"] = environment.toTag()
    }

    fun calculateSpawn() {
        spawn = environment.calculateSpawn(terrain)
    }

    override fun addEntity(entity: EntityServer,
                           spawn: Boolean): Boolean {
        return terrain.addEntity(entity, spawn)
    }

    override fun removeEntity(entity: EntityServer): Boolean {
        return terrain.removeEntity(entity)
    }

    override fun hasEntity(entity: EntityServer): Boolean {
        return getEntity(entity.uuid) != null || terrain.hasEntity(entity)
    }

    override fun getEntity(uuid: Uuid): EntityServer? {
        return players.values.asSequence()
                .filter { entity -> entity.uuid == uuid }
                .firstOrNull() ?: terrain.getEntity(uuid)
    }

    override fun getEntities(): Sequence<EntityServer> {
        return terrain.getEntities() + players.values.asSequence()
    }

    override fun getEntities(x: Int,
                             y: Int,
                             z: Int): Sequence<EntityServer> {
        return terrain.getEntities(x, y,
                z) + players.values.asSequence().filter { entity ->
            val pos = entity.getCurrentPos()
            pos.x.floorToInt() == x && pos.y.floorToInt() == y && pos.z.floorToInt() == z
        }
    }

    override fun getEntitiesAtLeast(minX: Int,
                                    minY: Int,
                                    minZ: Int,
                                    maxX: Int,
                                    maxY: Int,
                                    maxZ: Int): Sequence<EntityServer> {
        return terrain.getEntitiesAtLeast(minX, minY, minZ, maxX, maxY,
                maxZ) + players.values.asSequence()
    }

    override fun entityAdded(entity: EntityServer,
                             spawn: Boolean) {
        initEntity(entity)
        if (spawn) {
            entity.spawn()
        }
        entity.addedToWorld()
        if (entity is MobLivingServer) {
            entity.getOrNull(CreatureType.COMPONENT)
                    ?.let { entityCounts[it] }?.getAndIncrement()
        }
        send(PacketEntityAdd(plugins.registry, entity))
    }

    override fun entityRemoved(entity: EntityServer) {
        entity.removedFromWorld()
        if (entity is MobLivingServer) {
            entity.getOrNull(CreatureType.COMPONENT)
                    ?.let { entityCounts[it] }?.getAndDecrement()
        }
        send(PacketEntityDespawn(plugins.registry, entity))
    }

    fun addEntityNew(entity: EntityServer) {
        terrain.addEntity(entity, true)
    }

    fun initEntity(entity: EntityServer) {
        entityListeners.forEach { it(entity) }
    }

    fun players(): Collection<MobPlayerServer> {
        return players.values
    }

    fun addPlayer(player: MobPlayerServer,
                  isNew: Boolean) {
        entityAdded(player, isNew)
        players.put(player.nickname(), player)
    }

    @Synchronized
    fun removePlayer(player: MobPlayerServer) {
        players.remove(player.nickname())
        entityRemoved(player)
    }

    fun mobs(creatureType: CreatureType): Int {
        return entityCounts[creatureType]?.get() ?: 0
    }

    private fun update(delta: Double) {
        synchronized(terrain) {
            profilerSection("Terrain") {
                terrain.update(delta, spawners)
            }
            profilerSection("Entities") {
                players.values.forEach { player ->
                    player.update(delta)
                    player.updateListeners(delta)
                    player.move(delta)
                    if (player.isDead) {
                        player.death()
                    }
                }
            }
            profilerSection("Environment") {
                environment.tick(delta)
            }
            profilerSection("Tasks") {
                process()
            }
            tick++
        }
    }

    fun checkBlocked(x1: Int,
                     y1: Int,
                     z1: Int,
                     x2: Int,
                     y2: Int,
                     z2: Int): Boolean {
        val dx = x2 - x1
        val dy = y2 - y1
        val dz = z2 - z1
        val step = (1 / sqrt(dx * dx + dy * dy + dz * dz.toFloat())).toDouble()
        var i = 0.0
        while (i <= 1) {
            val x = (x1 + dx * i).floorToInt()
            val y = (y1 + dy * i).floorToInt()
            val z = (z1 + dz * i).floorToInt()
            if (!terrain.isTransparent(x, y, z)) {
                return true
            }
            i += step
        }
        return false
    }

    fun nearestPlayer(pos: Vector3d): MobPlayerServer? {
        var player: MobPlayerServer? = null
        var distance = -1.0
        for (playerCheck in players.values) {
            val distanceCheck = playerCheck.getCurrentPos().distanceSqr(pos)
            if (distanceCheck < distance || distance == -1.0) {
                player = playerCheck
                distance = distanceCheck
            }
        }
        return player
    }

    fun addSpawner(spawner: MobSpawner) {
        synchronized(spawners) {
            spawners.add(spawner)
        }
    }

    fun playSound(audio: String,
                  entity: EntityServer,
                  pitch: Double = 1.0,
                  gain: Double = 1.0,
                  referenceDistance: Double = 1.0,
                  rolloffFactor: Double = 1.0) {
        if (entity is MobServer) {
            playSound(audio, entity.getCurrentPos(), entity.speed(), pitch,
                    gain, referenceDistance, rolloffFactor)
        } else {
            playSound(audio, entity.getCurrentPos(), Vector3d.ZERO, pitch, gain,
                    referenceDistance, rolloffFactor)
        }
    }

    fun playSound(audio: String?,
                  position: Vector3d,
                  velocity: Vector3d,
                  pitch: Double = 1.0,
                  gain: Double = 1.0,
                  referenceDistance: Double = 1.0,
                  rolloffFactor: Double = 1.0) {
        if (audio != null) {
            if (!audio.isEmpty()) {
                send(PacketSoundEffect(plugins.registry, audio, position,
                        velocity, pitch, gain, referenceDistance,
                        rolloffFactor))
            }
        }
    }

    fun entityListener(listener: (EntityServer) -> Unit) {
        entityListeners.add(listener)
    }

    suspend fun stop(dropWorld: WorldServer?) {
        if (dropWorld != null && dropWorld != this) {
            while (!players.isEmpty()) {
                players.values.forEach { player ->
                    player.connection().setWorld(dropWorld)
                }
            }
        } else {
            while (!players.isEmpty()) {
                players.values.forEach { player ->
                    player.connection().disconnect("World closed", 5.0)
                }
            }
        }
        updateJob?.cancelAndJoin()
        terrain.dispose()
        clearComponents()
        taskExecutor[Job]?.cancelAndJoin()
    }

    fun start() {
        updateJob = launchThread("State") {
            thread = Thread.currentThread()
            val timer = Timer()
            val maxDiff = Timer.toDiff(20.0)
            timer.init()
            try {
                while (true) {
                    val freewheel = withContext(NonCancellable) {
                        if (!players.isEmpty()) {
                            profilerSection("Tick") { update(0.05) }
                            !players.values.asSequence().filter { it.isActive() }.any()
                        } else {
                            false
                        }
                    }
                    if (freewheel) {
                        timer.tick()
                    } else {
                        timer.cap(maxDiff, ::sleepNanos, 5000000000L,
                                logSkip = { skip ->
                                    logger.warn { "World $id is skipping $skip nanoseconds!" }
                                })
                    }
                    yield() // Allow cancel after non-standard sleep
                }
            } finally {
                thread = null
            }
        }.apply {
            invokeOnCompletion(true, false) {
                LockSupport.unpark(thread)
            }
        }
        updateJob?.invokeOnCompletion(true, false) {}
    }

    override fun send(packet: PacketClient) {
        players.values.forEach { it.connection().send(packet) }
    }

    fun send(packet: PacketClient,
             exceptions: List<PlayerConnection>) {
        players.values.asSequence().map { it.connection() }.filter {
            !exceptions.contains(it)
        }.forEach { it.send(packet) }
    }

    companion object : KLogging()
}
