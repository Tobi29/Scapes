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

package org.tobi29.scapes.vanilla.basics.world

import org.tobi29.scapes.chunk.EnvironmentServer
import org.tobi29.scapes.chunk.MobSpawner
import org.tobi29.scapes.chunk.WorldServer
import org.tobi29.scapes.chunk.generator.ChunkGenerator
import org.tobi29.scapes.chunk.generator.ChunkPopulator
import org.tobi29.scapes.chunk.terrain.TerrainChunk
import org.tobi29.scapes.chunk.terrain.TerrainServer
import org.tobi29.scapes.engine.utils.io.tag.*
import org.tobi29.scapes.engine.utils.math.*
import org.tobi29.scapes.engine.utils.math.vector.*
import org.tobi29.scapes.entity.CreatureType
import org.tobi29.scapes.entity.WieldMode
import org.tobi29.scapes.entity.server.EntityContainerServer
import org.tobi29.scapes.entity.server.MobItemServer
import org.tobi29.scapes.entity.server.MobPlayerServer
import org.tobi29.scapes.entity.server.MobServer
import org.tobi29.scapes.packets.PacketEntityMetaData
import org.tobi29.scapes.server.MessageLevel
import org.tobi29.scapes.server.extension.event.MessageEvent
import org.tobi29.scapes.vanilla.basics.VanillaBasics
import org.tobi29.scapes.vanilla.basics.entity.server.EntityTornadoServer
import org.tobi29.scapes.vanilla.basics.entity.server.MobPigServer
import org.tobi29.scapes.vanilla.basics.entity.server.MobSkeletonServer
import org.tobi29.scapes.vanilla.basics.entity.server.MobZombieServer
import org.tobi29.scapes.vanilla.basics.generator.BiomeGenerator
import org.tobi29.scapes.vanilla.basics.generator.ClimateGenerator
import org.tobi29.scapes.vanilla.basics.generator.TerrainGenerator
import org.tobi29.scapes.vanilla.basics.material.ItemHeatable
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial
import org.tobi29.scapes.vanilla.basics.packet.PacketDayTimeSync
import org.tobi29.scapes.vanilla.basics.packet.PacketLightning
import java.util.*
import java.util.concurrent.ThreadLocalRandom

class EnvironmentOverworldServer(private val world: WorldServer,
                                 plugin: VanillaBasics) : EnvironmentServer, EnvironmentClimate {
    private val materials: VanillaMaterial
    private val gen: ChunkGeneratorOverworld
    private val pop: ChunkPopulatorOverworld
    private val terrainGenerator: TerrainGenerator
    private val climateGenerator: ClimateGenerator
    private val biomeGenerator: BiomeGenerator
    private var simulationCount = 0L
    private var syncWait = 2.0
    private var playerUpdateWait = 0.25
    private var tickWait = 0.05

    init {
        materials = plugin.materials
        val random = Random(world.seed)
        terrainGenerator = TerrainGenerator(random)
        climateGenerator = ClimateGenerator(random, terrainGenerator)
        climateGenerator.setDayTime(0.1)
        climateGenerator.setDay(4)
        biomeGenerator = BiomeGenerator(climateGenerator, terrainGenerator)
        gen = ChunkGeneratorOverworld(random, terrainGenerator,
                plugin.materials)
        pop = ChunkPopulatorOverworld(world, plugin, biomeGenerator)
        world.addSpawner(object : MobSpawner {
            override fun mobsPerChunk(): Double {
                return 0.1
            }

            override fun spawnAttempts(): Int {
                return 1
            }

            override fun chunkChance(): Int {
                return 2
            }

            override fun canSpawn(terrain: TerrainServer,
                                  x: Int,
                                  y: Int,
                                  z: Int): Boolean {
                if (terrain.light(x, y, z) < 7) {
                    if (!terrain.type(x, y, z).isSolid(terrain, x, y, z) &&
                            terrain.type(x, y, z).isTransparent(terrain, x, y,
                                    z) &&
                            !terrain.type(x, y, z + 1).isSolid(terrain, x, y,
                                    z + 1) &&
                            terrain.type(x, y, z + 1).isTransparent(terrain, x,
                                    y, z + 1) &&
                            terrain.type(x, y, z - 1).isSolid(terrain, x, y,
                                    z - 1) &&
                            !terrain.type(x, y, z - 1).isTransparent(terrain, x,
                                    y, z - 1)) {
                        return true
                    }
                }
                return false
            }

            override fun spawn(terrain: TerrainServer,
                               x: Int,
                               y: Int,
                               z: Int): MobServer {
                val random = ThreadLocalRandom.current()
                return MobZombieServer(terrain.world,
                        Vector3d(x + 0.5, y + 0.5, z + 1.0), Vector3d.ZERO,
                        0.0, random.nextDouble() * 360.0)
            }

            override fun creatureType(): CreatureType {
                return CreatureType.MONSTER
            }
        })
        world.addSpawner(object : MobSpawner {
            override fun mobsPerChunk(): Double {
                return 0.1
            }

            override fun spawnAttempts(): Int {
                return 1
            }

            override fun chunkChance(): Int {
                return 2
            }

            override fun canSpawn(terrain: TerrainServer,
                                  x: Int,
                                  y: Int,
                                  z: Int): Boolean {
                if (terrain.light(x, y, z) < 7) {
                    if (!terrain.type(x, y, z).isSolid(terrain, x, y, z) &&
                            terrain.type(x, y, z).isTransparent(terrain, x, y,
                                    z) &&
                            !terrain.type(x, y, z + 1).isSolid(terrain, x, y,
                                    z + 1) &&
                            terrain.type(x, y, z + 1).isTransparent(terrain, x,
                                    y, z + 1) &&
                            terrain.type(x, y, z - 1).isSolid(terrain, x, y,
                                    z - 1) &&
                            !terrain.type(x, y, z - 1).isTransparent(terrain, x,
                                    y, z - 1)) {
                        return true
                    }
                }
                return false
            }

            override fun spawn(terrain: TerrainServer,
                               x: Int,
                               y: Int,
                               z: Int): MobServer {
                val random = ThreadLocalRandom.current()
                return MobSkeletonServer(terrain.world,
                        Vector3d(x + 0.5, y + 0.5, z + 1.0), Vector3d.ZERO,
                        0.0, random.nextDouble() * 360.0)
            }

            override fun creatureType(): CreatureType {
                return CreatureType.MONSTER
            }
        })
        world.addSpawner(object : MobSpawner {
            override fun mobsPerChunk(): Double {
                return 0.01
            }

            override fun chunkChance(): Int {
                return 10
            }

            override fun spawnAttempts(): Int {
                return 1
            }

            override fun canSpawn(terrain: TerrainServer,
                                  x: Int,
                                  y: Int,
                                  z: Int): Boolean {
                if (terrain.light(x, y, z) >= 7) {
                    if (!terrain.type(x, y, z).isSolid(terrain, x, y, z) &&
                            terrain.type(x, y, z).isTransparent(terrain, x, y,
                                    z) &&
                            terrain.type(x, y, z - 1) === materials.grass) {
                        return true
                    }
                }
                return false
            }

            override fun spawn(terrain: TerrainServer,
                               x: Int,
                               y: Int,
                               z: Int): MobServer {
                val random = ThreadLocalRandom.current()
                return MobPigServer(terrain.world,
                        Vector3d(x + 0.5, y + 0.5, z + 0.6875),
                        Vector3d.ZERO, 0.0, random.nextDouble() * 360.0)
            }

            override fun creatureType(): CreatureType {
                return CreatureType.CREATURE
            }
        })
        world.entityListener({ entity ->
            if (entity is EntityContainerServer) {
                var itemUpdateWait = 1.0
                entity.onUpdate("VanillaBasics:Items", { delta ->
                    itemUpdateWait -= delta
                    while (itemUpdateWait <= 0.0) {
                        itemUpdateWait += 1.0
                        val inventories = entity.inventories()
                        val pos = entity.getCurrentPos()
                        val temperature = climateGenerator.temperature(
                                pos.intX(), pos.intY(), pos.intZ())
                        inventories.forEachModify { id, inventory ->
                            var flag = false
                            for (i in 0..inventory.size() - 1) {
                                val type = inventory.item(i).material()
                                if (type is ItemHeatable) {
                                    type.heat(inventory.item(i), temperature)
                                    flag = true
                                }
                            }
                            flag
                        }
                    }
                })
            }
            if (entity is MobItemServer) {
                var itemUpdateWait = 1.0
                val pos = entity.getCurrentPos()
                val temperature = climateGenerator.temperature(
                        pos.intX(), pos.intY(), pos.intZ())
                entity.onUpdate("VanillaBasics:Items", { delta ->
                    itemUpdateWait -= delta
                    while (itemUpdateWait <= 0.0) {
                        itemUpdateWait += 1.0
                        val type = entity.item().material()
                        if (type is ItemHeatable) {
                            type.heat(entity.item(), temperature, 20.0)
                        }
                    }
                })
            }
        })
        world.entityListener({ entity ->
            if (entity is MobPlayerServer) {
                entity.onSpawn("VanillaBasics:Condition", {
                    entity.metaData("Vanilla").syncMapMut(
                            "Condition") { conditionTag ->
                        conditionTag["Stamina"] = 1.0
                        conditionTag["Wake"] = 1.0
                        conditionTag["Hunger"] = 1.0
                        conditionTag["Thirst"] = 1.0
                        conditionTag["BodyTemperature"] = 37.0
                    }
                })
                entity.onJump("VanillaBasics:Condition", {
                    entity.metaData("Vanilla").syncMapMut(
                            "Condition") { conditionTag ->
                        val stamina = conditionTag["Stamina"]?.toDouble() ?: 0.0
                        val bodyTemperature = conditionTag["BodyTemperature"]?.toDouble() ?: 0.0
                        conditionTag["Stamina"] = stamina - 0.15
                        conditionTag["BodyTemperature"] = bodyTemperature + 0.1
                    }
                })
                entity.onPunch("VanillaBasics:Condition", { strength ->
                    var attackStrength = strength
                    if (entity.wieldMode() != WieldMode.DUAL) {
                        attackStrength *= 1.7
                    }
                    entity.metaData("Vanilla").syncMapMut(
                            "Condition") { conditionTag ->
                        val stamina = conditionTag["Stamina"]?.toDouble() ?: 0.0
                        val bodyTemperature = conditionTag["BodyTemperature"]?.toDouble() ?: 0.0
                        conditionTag["Stamina"] = stamina - 0.04 * attackStrength
                        conditionTag["BodyTemperature"] = bodyTemperature + 0.03 * attackStrength
                    }
                })
                entity.onDeath("VanillaBasics:DeathMessage", {
                    world.connection.events.fireLocal(
                            MessageEvent(entity.connection(), MessageLevel.CHAT,
                                    "${entity.nickname()} died!"))
                })
            }
        })
    }

    override fun climate(): ClimateGenerator {
        return climateGenerator
    }

    override fun generator(): ChunkGenerator {
        return gen
    }

    override fun populator(): ChunkPopulator {
        return pop
    }

    override fun calculateSpawn(terrain: TerrainServer): Vector3i {
        var x = 0
        val y = -11250
        var flag = false
        while (!flag) {
            if (!terrainGenerator.isValidSpawn(x.toDouble(),
                    y.toDouble()) || !biomeGenerator[x.toDouble(), y.toDouble()].isValidSpawn) {
                x += 512
            } else {
                flag = true
            }
        }
        val z = terrain.highestTerrainBlockZAt(x, y)
        return Vector3i(x, y, z)
    }

    override fun read(map: TagMap) {
        map["DayTime"]?.toDouble()?.let { climateGenerator.setDayTime(it) }
        map["Day"]?.toLong()?.let { climateGenerator.setDay(it) }
        map["SimulationCount"]?.toLong()?.let { simulationCount = it }
    }

    override fun write(map: ReadWriteTagMap) {
        map["DayTime"] = climateGenerator.dayTime()
        map["Day"] = climateGenerator.day()
        map["SimulationCount"] = simulationCount
    }

    override fun tick(delta: Double) {
        climateGenerator.add(0.000277777777778 * delta)
        playerUpdateWait -= delta
        while (playerUpdateWait <= 0.0) {
            playerUpdateWait += 0.25
            val random = ThreadLocalRandom.current()
            world.players().forEach {
                val health = it.health()
                val maxHealth = it.maxHealth()
                it.metaData("Vanilla").syncMapMut("Condition") { conditionTag ->
                    var stamina = conditionTag["Stamina"]?.toDouble() ?: 0.0
                    var wake = conditionTag["Wake"]?.toDouble() ?: 0.0
                    var hunger = conditionTag["Hunger"]?.toDouble() ?: 0.0
                    var thirst = conditionTag["Thirst"]?.toDouble() ?: 0.0
                    var bodyTemperature = conditionTag["BodyTemperature"]?.toDouble() ?: 0.0
                    var sleeping = conditionTag["Sleeping"]?.toBoolean() ?: false
                    val ground = it.isOnGround
                    val inWater = it.isInWater
                    val pos = it.getCurrentPos()
                    val temperature = climateGenerator.temperature(pos.intX(),
                            pos.intY(), pos.intZ())
                    val regenFactor = if (sleeping) 1.5 else 1.0
                    val depleteFactor = if (sleeping) 0.05 else 1.0
                    if (stamina > 0.2 && health < maxHealth) {
                        val rate = stamina * 0.5
                        it.heal(rate)
                        stamina -= rate * 0.1
                    }
                    if (inWater) {
                        val rate = clamp(it.speed().lengthSqr() * 0.00125, 0.0,
                                0.05)
                        stamina -= rate
                        bodyTemperature += rate
                        thirst -= rate * 0.075
                    } else if (ground) {
                        val rate = clamp(it.speed().lengthSqr() * 0.00025, 0.0,
                                0.05)
                        stamina -= rate
                        bodyTemperature += rate
                    }
                    stamina -= depleteFactor * 0.00025
                    if (inWater && thirst < 1.0) {
                        thirst += 0.025
                    }
                    if (stamina < 1.0) {
                        val rate = regenFactor * hunger * thirst * 0.05 *
                                (1 - stamina)
                        stamina += rate
                        wake -= rate * 0.005
                        hunger -= rate * 0.003
                        thirst -= rate * 0.01
                    }
                    bodyTemperature += (temperature - bodyTemperature) / 2000.0
                    if (bodyTemperature < 37.0) {
                        var rate = max(37.0 - bodyTemperature, 0.0)
                        rate = min(rate * 8.0 * stamina, 1.0) * 0.04
                        bodyTemperature += rate
                        stamina -= rate * 0.5
                    } else if (bodyTemperature > 37.0) {
                        var rate = max(bodyTemperature - 37.0, 0.0)
                        rate = min(rate * thirst, 1.0) * 0.06
                        bodyTemperature -= rate
                        thirst -= rate * 0.05
                    }
                    if (sleeping) {
                        wake += 0.0002
                        val wakeChance = 7.0 - wake * 7.0
                        if (random.nextDouble() > wakeChance) {
                            sleeping = false
                        }
                    } else {
                        val sleepChance = wake * 10.0
                        if (random.nextDouble() > sleepChance) {
                            sleeping = true
                        }
                    }
                    stamina = min(stamina, 1.0)
                    if (stamina <= 0.0) {
                        it.damage(5.0)
                    }
                    wake = clamp(wake, 0.0, 1.0)
                    hunger = clamp(hunger, 0.0, 1.0)
                    thirst = clamp(thirst, 0.0, 1.0)
                    conditionTag["Stamina"] = stamina
                    conditionTag["Wake"] = wake
                    conditionTag["Hunger"] = hunger
                    conditionTag["Thirst"] = thirst
                    conditionTag["BodyTemperature"] = bodyTemperature
                    conditionTag["Sleeping"] = sleeping
                }
                it.connection().send(
                        PacketEntityMetaData(world.registry, it, "Vanilla"))
            }
        }
        syncWait -= delta
        while (syncWait <= 0.0) {
            syncWait += 4.0
            world.send(PacketDayTimeSync(world.registry,
                    climateGenerator.dayTime(),
                    climateGenerator.day()))
        }
        tickWait -= delta
        while (tickWait <= 0.0) {
            tickWait += 0.05
            simulationCount++
            if (simulationCount >= Long.MAX_VALUE - 10) {
                simulationCount = 0
            }
            val random = ThreadLocalRandom.current()
            val terrain = world.terrain
            world.terrain.chunks({ chunk ->
                simulateSeason(terrain, chunk)
                val x = chunk.posBlock.x + random.nextInt(16)
                val y = chunk.posBlock.y + random.nextInt(16)
                val weather = climateGenerator.weather(x.toDouble(),
                        y.toDouble())
                if (random.nextInt((513.0 - weather * 512.0).toInt()) == 0 &&
                        random.nextInt(1000) == 0 && weather > 0.7) {
                    world.send(PacketLightning(world.registry, x.toDouble(),
                            y.toDouble(),
                            terrain.highestTerrainBlockZAt(x, y).toDouble()))
                } else if (random.nextInt(
                        (513.0 - weather * 512.0).toInt()) == 0 &&
                        random.nextInt(10000) == 0 && weather > 0.85) {
                    val entity = EntityTornadoServer(world,
                            Vector3d(x.toDouble(), y.toDouble(),
                                    terrain.highestTerrainBlockZAt(x,
                                            y).toDouble()))
                    world.addEntityNew(entity)
                }
            })
        }
    }

    override fun sunLightReduction(x: Double,
                                   y: Double): Float {
        return climateGenerator.sunLightReduction(x, y).toFloat()
    }

    override fun sunLightNormal(x: Double,
                                y: Double): Vector3d {
        val latitude = climateGenerator.latitude(y)
        val elevation = climateGenerator.sunElevationD(latitude)
        var azimuth = climateGenerator.sunAzimuthD(elevation, latitude)
        azimuth += HALF_PI
        val rz = sinTable(elevation)
        val rd = cosTable(elevation)
        val rx = cosTable(azimuth) * rd
        val ry = sinTable(azimuth) * rd
        val mix = clamp(elevation * 100.0, -1.0, 1.0)
        return Vector3d(rx, ry, rz).normalizeSafe().times(mix)
    }

    fun simulateSeason(terrain: TerrainServer,
                       chunk: TerrainChunk) {
        val tagStructure = chunk.metaData("Vanilla")
        val chunkSimulationCount = tagStructure["SimulationCount"]?.toLong() ?: 0L
        val count: Int
        if (chunkSimulationCount <= 0) {
            count = 1
        } else {
            val delta = simulationCount - chunkSimulationCount
            val random = ThreadLocalRandom.current()
            if (delta < 180 + random.nextInt(40)) {
                return
            }
            count = max(10240 / delta.toInt(), 1)
        }
        tagStructure["SimulationCount"] = simulationCount
        terrain.queue { handler ->
            simulateSeason(handler, chunk.posBlock.x,
                    chunk.posBlock.y, chunk.size.x, chunk.size.y,
                    count)
        }
    }

    private fun simulateSeason(chunk: TerrainServer.TerrainMutable,
                               x: Int,
                               y: Int,
                               dx: Int,
                               dy: Int,
                               chance: Int) {
        val random = ThreadLocalRandom.current()
        val humidity00 = climateGenerator.humidity(x.toDouble(), y.toDouble())
        val temperature00 = climateGenerator.temperature(x.toDouble(),
                y.toDouble())
        val humidity10 = climateGenerator.humidity((x + 15).toDouble(),
                y.toDouble())
        val temperature10 = climateGenerator.temperature((x + 15).toDouble(),
                y.toDouble())
        val humidity01 = climateGenerator.humidity(x.toDouble(),
                (y + 15).toDouble())
        val temperature01 = climateGenerator.temperature(x.toDouble(),
                (y + 15).toDouble())
        val humidity11 = climateGenerator.humidity((x + 15).toDouble(),
                (y + 15).toDouble())
        val temperature11 = climateGenerator.temperature((x + 15).toDouble(),
                (y + 15).toDouble())
        for (yy in 0..dx - 1) {
            val yyy = yy + y
            val mixY = yy / 15.0
            val humidity0 = mix(humidity00, humidity01, mixY)
            val humidity1 = mix(humidity10, humidity11, mixY)
            val temperature0 = mix(temperature00, temperature01, mixY)
            val temperature1 = mix(temperature10, temperature11, mixY)
            for (xx in 0..dy - 1) {
                if (random.nextInt(chance) == 0) {
                    val xxx = xx + x
                    val z = chunk.highestTerrainBlockZAt(xxx, yyy)
                    val mixX = xx / 15.0
                    val humidity = mix(humidity0, humidity1, mixX)
                    val temperature = climateGenerator.temperatureD(
                            mix(temperature0, temperature1, mixX), z)
                    val spaceBlock = chunk.block(xxx, yyy, z)
                    val spaceType = chunk.type(spaceBlock)
                    val spaceData = chunk.data(spaceBlock)
                    val groundType = chunk.type(xxx, yyy, z - 1)
                    if (humidity > 0.2) {
                        if (groundType === materials.dirt) {
                            chunk.typeData(xxx, yyy, z - 1, materials.grass,
                                    random.nextInt(4))
                        } else if (groundType === materials.grass && random.nextInt(
                                20) == 0) {
                            chunk.data(xxx, yyy, z - 1, random.nextInt(9))
                        }
                    } else {
                        if (groundType === materials.grass) {
                            chunk.typeData(xxx, yyy, z - 1, materials.dirt, 0)
                        }
                    }
                    if (temperature > 1.0) {
                        if (spaceType === materials.snow) {
                            if (spaceData < 8) {
                                chunk.data(xxx, yyy, z, spaceData + 1)
                            } else {
                                chunk.typeData(xxx, yyy, z, materials.air, 0)
                            }
                        }
                    } else {
                        val weather = climateGenerator.weather(xxx.toDouble(),
                                yyy.toDouble())
                        if (temperature < 0.0 && (weather > 0.5 || chance == 1)) {
                            if (spaceType === materials.air ||
                                    spaceType === materials.flower ||
                                    spaceType === materials.stoneRock) {
                                chunk.typeData(xxx, yyy, z, materials.snow, 8)
                            } else if (spaceType === materials.snow && spaceData > 0) {
                                chunk.data(xxx, yyy, z, spaceData - 1)
                            }
                        }
                    }
                }
            }
        }
    }
}
