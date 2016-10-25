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
package org.tobi29.scapes.chunk

import java8.util.stream.Stream
import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.chunk.generator.ChunkGenerator
import org.tobi29.scapes.chunk.generator.ChunkPopulator
import org.tobi29.scapes.chunk.terrain.TerrainServer
import org.tobi29.scapes.connection.PlayConnection
import org.tobi29.scapes.engine.utils.Sync
import org.tobi29.scapes.engine.utils.io.tag.MultiTag
import org.tobi29.scapes.engine.utils.io.tag.TagStructure
import org.tobi29.scapes.engine.utils.io.tag.getLong
import org.tobi29.scapes.engine.utils.io.tag.setLong
import org.tobi29.scapes.engine.utils.math.floor
import org.tobi29.scapes.engine.utils.math.sqrt
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.engine.utils.math.vector.distanceSqr
import org.tobi29.scapes.engine.utils.profiler.profilerSection
import org.tobi29.scapes.engine.utils.stream
import org.tobi29.scapes.engine.utils.task.Joiner
import org.tobi29.scapes.engine.utils.task.TaskExecutor
import org.tobi29.scapes.entity.CreatureType
import org.tobi29.scapes.entity.server.*
import org.tobi29.scapes.packets.PacketClient
import org.tobi29.scapes.packets.PacketEntityAdd
import org.tobi29.scapes.packets.PacketEntityDespawn
import org.tobi29.scapes.packets.PacketSoundEffect
import org.tobi29.scapes.server.connection.PlayerConnection
import org.tobi29.scapes.server.connection.ServerConnection
import org.tobi29.scapes.server.format.WorldFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ThreadLocalRandom

class WorldServer(worldFormat: WorldFormat, val id: String, seed: Long,
                  val connection: ServerConnection, taskExecutor: TaskExecutor,
                  terrainSupplier: (WorldServer) -> TerrainServer,
                  environmentSupplier: (WorldServer) -> EnvironmentServer) : World<EntityServer>(
        worldFormat.plugins(), taskExecutor, worldFormat.plugins().registry(),
        seed), MultiTag.ReadAndWrite, PlayConnection<PacketClient> {
    private val entityListeners = Collections.newSetFromMap(
            ConcurrentHashMap<(EntityServer) -> Unit, Boolean>())
    private val spawners = Collections.newSetFromMap(
            ConcurrentHashMap<MobSpawner, Boolean>())
    private val populators = ArrayList<ChunkPopulator>()
    val terrain: TerrainServer
    val environment: EnvironmentServer
    private val sync = Sync(20.0, 5000000000L, true, "Server-Update")
    private val players = ConcurrentHashMap<String, MobPlayerServer>()
    private val generator: ChunkGenerator
    private var joiner: Joiner? = null

    init {
        environment = environmentSupplier(this)
        terrain = terrainSupplier(this)
        generator = environment.generator()
        populators.add(environment.populator())
        worldFormat.plugins().plugins().forEach { plugin ->
            plugin.worldInit(this)
        }
    }

    fun connection(): ServerConnection {
        return connection
    }

    override fun read(tagStructure: TagStructure) {
        tagStructure.getLong("Tick")?.let { tick = it }
        tagStructure.getStructure("Environment")?.let { environment.load(it) }
    }

    override fun write(): TagStructure {
        val tagStructure = TagStructure()
        tagStructure.setLong("Tick", tick)
        tagStructure.setStructure("Environment", environment.save())
        return tagStructure
    }

    fun calculateSpawn() {
        spawn = environment.calculateSpawn(terrain)
    }

    override fun addEntity(entity: EntityServer): Boolean {
        initEntity(entity)
        return terrain.addEntity(entity)
    }

    override fun removeEntity(entity: EntityServer): Boolean {
        return terrain.removeEntity(entity)
    }

    override fun hasEntity(entity: EntityServer): Boolean {
        return getEntity(entity.getUUID()) != null || terrain.hasEntity(entity)
    }

    override fun getEntity(uuid: UUID): EntityServer? {
        val player = players.values.stream().filter { entity -> entity.getUUID() == uuid }.findAny()
        if (player.isPresent) {
            return player.get()
        }
        return terrain.getEntity(uuid)
    }

    override fun getEntities(consumer: (Stream<EntityServer>) -> Unit) {
        terrain.getEntities(consumer)
        consumer(players.values.stream())
    }

    @Suppress("USELESS_CAST")
    override fun getEntities(x: Int,
                             y: Int,
                             z: Int,
                             consumer: (Stream<EntityServer>) -> Unit) {
        terrain.getEntities(x, y, z, consumer)
        consumer(
                players.values.stream().map { it as EntityServer }.filter { entity ->
                    val pos = entity.getCurrentPos()
                    pos.intX() == x && pos.intY() == y && pos.intZ() == z
                })
    }

    override fun getEntitiesAtLeast(minX: Int,
                                    minY: Int,
                                    minZ: Int,
                                    maxX: Int,
                                    maxY: Int,
                                    maxZ: Int,
                                    consumer: (Stream<EntityServer>) -> Unit) {
        terrain.getEntitiesAtLeast(minX, minY, minZ, maxX, maxY,
                maxZ) { consumer(it) }
        consumer(players.values.stream())
    }

    override fun entityAdded(entity: EntityServer) {
        initEntity(entity)
        send(PacketEntityAdd(entity, plugins.registry()))
    }

    override fun entityRemoved(entity: EntityServer) {
        send(PacketEntityDespawn(entity))
    }

    fun addEntityNew(entity: EntityServer) {
        initEntity(entity)
        entity.onSpawn()
        terrain.addEntity(entity)
    }

    fun initEntity(entity: EntityServer) {
        entityListeners.forEach { it(entity) }
    }

    fun players(): Collection<MobPlayerServer> {
        return players.values
    }

    fun addPlayer(player: MobPlayerServer,
                  isNew: Boolean) {
        initEntity(player)
        if (isNew) {
            player.onSpawn()
        }
        players.put(player.nickname(), player)
        entityAdded(player)
    }

    @Synchronized fun removePlayer(player: MobPlayerServer) {
        players.remove(player.nickname())
        entityRemoved(player)
    }

    fun mobs(creatureType: CreatureType): Int {
        // TODO: Optimize: Keep track using add and remove
        var count = 0
        getEntities { stream ->
            count += stream.filter(
                    { entity -> entity is MobLivingServer }).filter(
                    { entity -> (entity as MobLivingServer).creatureType() === creatureType }).count().toInt()
        }
        return count
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
                        player.onDeath()
                    }
                }
            }
            profilerSection("Environment") {
                environment.tick(delta)
            }
            profilerSection("Tasks") { taskExecutor.tick() }
            tick++
        }
    }

    fun dropItems(items: List<ItemStack>,
                  x: Int,
                  y: Int,
                  z: Int,
                  despawntime: Double = 600.0) {
        val pos = Vector3d(x + 0.5, y + 0.5, z + 0.5)
        for (item in items) {
            dropItem(item, pos, despawntime)
        }
    }

    fun dropItem(item: ItemStack,
                 x: Int,
                 y: Int,
                 z: Int,
                 despawntime: Double = 600.0) {
        dropItem(item, Vector3d(x + 0.5, y + 0.5, z + 0.5), despawntime)
    }

    fun dropItem(item: ItemStack,
                 pos: Vector3d,
                 despawntime: Double = 600.0) {
        val random = ThreadLocalRandom.current()
        val entity = MobItemServer(this, pos,
                Vector3d(-2.0 + random.nextDouble() * 4.0,
                        -2.0 + random.nextDouble() * 4.0,
                        random.nextDouble() * 1.0 + 0.5), item, despawntime)
        addEntityNew(entity)
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
            val x = floor(x1 + dx * i)
            val y = floor(y1 + dy * i)
            val z = floor(z1 + dz * i)
            if (!terrain.type(x, y, z).isTransparent(terrain, x, y, z)) {
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

    fun environment(): EnvironmentServer {
        return environment
    }

    fun id(): String {
        return id
    }

    fun addPopulator(populator: ChunkPopulator) {
        populators.add(populator)
    }

    fun generator(): ChunkGenerator {
        return generator
    }

    fun populators(): Stream<ChunkPopulator> {
        return populators.stream()
    }

    fun dispose() {
        terrain.dispose()
    }

    fun playSound(audio: String,
                  entity: EntityServer,
                  pitch: Float = 1.0f,
                  gain: Float = 1.0f,
                  range: Float = 16.0f) {
        if (entity is MobServer) {
            playSound(audio, entity.getCurrentPos(), entity.speed(), pitch,
                    gain, range)
        } else {
            playSound(audio, entity.getCurrentPos(), Vector3d.ZERO, pitch, gain,
                    range)
        }
    }

    fun playSound(name: String,
                  position: Vector3d,
                  velocity: Vector3d) {
        playSound(name, position, velocity, 1.0f, 1.0f)
    }

    fun playSound(audio: String,
                  position: Vector3d,
                  velocity: Vector3d,
                  range: Float) {
        playSound(audio, position, velocity, 1.0f, 1.0f, range)
    }

    fun playSound(audio: String?,
                  position: Vector3d,
                  velocity: Vector3d,
                  pitch: Float,
                  gain: Float,
                  range: Float = 16.0f) {
        if (audio != null) {
            if (!audio.isEmpty()) {
                send(PacketSoundEffect(audio, position, velocity, pitch,
                        gain, range))
            }
        }
    }

    fun entityListener(listener: (EntityServer) -> Unit) {
        entityListeners.add(listener)
    }

    fun stop(dropWorld: WorldServer?) {
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
        joiner?.join()
    }

    fun start() {
        joiner = taskExecutor.runThread({ joiner ->
            thread = Thread.currentThread()
            sync.init()
            while (!joiner.marked) {
                if (!players.isEmpty()) {
                    profilerSection("Tick") { update(0.05) }
                    if (players.values.stream().filter(
                            { it.isActive() }).findAny().isPresent) {
                        sync.cap()
                    } else {
                        sync.tick()
                    }
                } else {
                    sync.cap()
                }
            }
            thread = null
        }, "Tick", TaskExecutor.Priority.MEDIUM)
    }

    override fun send(packet: PacketClient) {
        players.values.forEach { it.connection().send(packet) }
    }

    fun send(packet: PacketClient,
             exceptions: List<PlayerConnection>) {
        players.values.stream().map { it.connection() }.filter {
            !exceptions.contains(it)
        }.forEach { it.send(packet) }
    }

    override fun getWorldEntities(): Stream<MobPlayerServer> {
        return players.values.stream()
    }
}