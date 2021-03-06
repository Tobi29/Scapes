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

import org.tobi29.io.tag.*
import org.tobi29.math.Random
import org.tobi29.math.cosTable
import org.tobi29.math.sinTable
import org.tobi29.math.threadLocalRandom
import org.tobi29.math.vector.Vector3d
import org.tobi29.math.vector.Vector3i
import org.tobi29.math.vector.normalizedSafe
import org.tobi29.math.vector.times
import org.tobi29.scapes.block.inventories
import org.tobi29.scapes.chunk.EnvironmentServer
import org.tobi29.scapes.chunk.EnvironmentType
import org.tobi29.scapes.chunk.MobSpawner
import org.tobi29.scapes.chunk.WorldServer
import org.tobi29.scapes.chunk.generator.ChunkGenerator
import org.tobi29.scapes.chunk.generator.ChunkPopulator
import org.tobi29.scapes.chunk.terrain.TerrainServer
import org.tobi29.scapes.chunk.terrain.block
import org.tobi29.scapes.entity.CreatureType
import org.tobi29.scapes.entity.ListenerToken
import org.tobi29.scapes.entity.server.MobPlayerServer
import org.tobi29.scapes.entity.server.MobServer
import org.tobi29.scapes.inventory.kind
import org.tobi29.scapes.server.MessageLevel
import org.tobi29.scapes.server.extension.event.MessageEvent
import org.tobi29.scapes.terrain.TerrainChunk
import org.tobi29.scapes.vanilla.basics.VanillaBasics
import org.tobi29.scapes.vanilla.basics.entity.server.MobItemServer
import org.tobi29.scapes.vanilla.basics.generator.BiomeGenerator
import org.tobi29.scapes.vanilla.basics.generator.ClimateGenerator
import org.tobi29.scapes.vanilla.basics.generator.TerrainGenerator
import org.tobi29.scapes.vanilla.basics.material.ItemTypeHeatable
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial
import org.tobi29.scapes.vanilla.basics.material.heat
import org.tobi29.scapes.vanilla.basics.packet.PacketDayTimeSync
import org.tobi29.scapes.vanilla.basics.packet.PacketLightning
import org.tobi29.stdex.math.HALF_PI
import org.tobi29.stdex.math.clamp
import org.tobi29.stdex.math.floorToInt
import org.tobi29.stdex.math.mix
import kotlin.collections.set
import kotlin.math.max

class EnvironmentOverworldServer(override val type: EnvironmentType,
                                 private val world: WorldServer,
                                 plugin: VanillaBasics) : EnvironmentServer,
        EnvironmentClimate {
    private val materials: VanillaMaterial
    private val gen: ChunkGeneratorOverworld
    private val pop: ChunkPopulatorOverworld
    private val terrainGenerator: TerrainGenerator
    private val climateGenerator: ClimateGenerator
    private val biomeGenerator: BiomeGenerator
    private var simulationCount = 0L
    private var syncWait = 2.0
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
                if (terrain.light(x, y, z) < 7 &&
                        terrain.block(x, y, z) {
                            !isSolid(it) && isTransparent(it)
                        } &&
                        terrain.block(x, y, z + 1) {
                            !isSolid(it) && isTransparent(it)
                        } &&
                        terrain.block(x, y, z - 1) {
                            isSolid(it) && !isTransparent(it)
                        }) {
                    return true
                }
                return false
            }

            override fun spawn(terrain: TerrainServer,
                               x: Int,
                               y: Int,
                               z: Int): MobServer {
                val random = threadLocalRandom()
                return plugin.entityTypes.zombie.createServer(
                        terrain.world).apply {
                    setPos(Vector3d(x + 0.5, y + 0.5, z + 1.0))
                    setRot(Vector3d(0.0, 0.0, random.nextDouble() * 360.0))
                }
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
                if (terrain.light(x, y, z) < 7 &&
                        terrain.block(x, y, z) {
                            !isSolid(it) && isTransparent(it)
                        } &&
                        terrain.block(x, y, z + 1) {
                            !isSolid(it) && isTransparent(it)
                        } &&
                        terrain.block(x, y, z - 1) {
                            isSolid(it) && !isTransparent(it)
                        }) {
                    return true
                }
                return false
            }

            override fun spawn(terrain: TerrainServer,
                               x: Int,
                               y: Int,
                               z: Int): MobServer {
                val random = threadLocalRandom()
                return plugin.entityTypes.skeleton.createServer(
                        terrain.world).apply {
                    setPos(Vector3d(x + 0.5, y + 0.5, z + 1.0))
                    setRot(Vector3d(0.0, 0.0, random.nextDouble() * 360.0))
                }
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
                if (terrain.light(x, y, z) >= 7 &&
                        terrain.block(x, y, z) {
                            !isSolid(it) && isTransparent(it)
                        } &&
                        terrain.block(x, y, z - 1) {
                            isSolid(it) && !isTransparent(it)
                        }) {
                    return true
                }
                return false
            }

            override fun spawn(terrain: TerrainServer,
                               x: Int,
                               y: Int,
                               z: Int): MobServer {
                val random = threadLocalRandom()
                return plugin.entityTypes.pig.createServer(
                        terrain.world).apply {
                    setPos(Vector3d(x + 0.5, y + 0.5, z + 0.6875))
                    setRot(Vector3d(0.0, 0.0, random.nextDouble() * 360.0))
                }
            }

            override fun creatureType(): CreatureType {
                return CreatureType.CREATURE
            }
        })
        world.entityListener({ entity ->
            var itemUpdateWait = 1.0
            entity.onUpdate[ITEMS_LISTENER_TOKEN] = { delta ->
                itemUpdateWait -= delta
                while (itemUpdateWait <= 0.0) {
                    itemUpdateWait += 1.0
                    val inventories = entity.inventories
                    val pos = entity.getCurrentPos()
                    val temperature = climateGenerator.temperature(
                            pos.x.floorToInt(), pos.y.floorToInt(),
                            pos.z.floorToInt())
                    inventories.forEachModify { id, inventory ->
                        var flag = false
                        for (i in 0 until inventory.size()) {
                            inventory[i].kind<ItemTypeHeatable>()?.let { itemHeatable ->
                                inventory[i] = itemHeatable.heat(temperature)
                                flag = true
                            }
                        }
                        flag
                    }
                }
            }
            if (entity is MobItemServer) {
                var itemUpdateWait = 1.0
                val pos = entity.getCurrentPos()
                val temperature = climateGenerator.temperature(
                        pos.x.floorToInt(), pos.y.floorToInt(),
                        pos.z.floorToInt())
                entity.onUpdate[ITEMS_LISTENER_TOKEN] = { delta ->
                    itemUpdateWait -= delta
                    while (itemUpdateWait <= 0.0) {
                        itemUpdateWait += 1.0
                        entity.item.kind<ItemTypeHeatable>()?.let { itemHeatable ->
                            entity.item = itemHeatable.heat(temperature)
                        }
                    }
                }
            }
        })
        world.entityListener({ entity ->
            if (entity is MobPlayerServer) {
                entity.onDeath[DEATH_MESSAGE_TOKEN] = {
                    world.connection.server.events.fire(
                            MessageEvent(entity.connection(), MessageLevel.CHAT,
                                    "${entity.nickname()} died!",
                                    targetWorld = world))
                }
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
        val y = -12000
        var flag = false
        while (!flag) {
            if (!terrainGenerator.isValidSpawn(x.toDouble(),
                    y.toDouble()) || !biomeGenerator.get(x.toDouble(),
                    y.toDouble()).isValidSpawn) {
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
        map["DayTime"] = climateGenerator.dayTime().toTag()
        map["Day"] = climateGenerator.day().toTag()
        map["SimulationCount"] = simulationCount.toTag()
    }

    override fun tick(delta: Double) {
        climateGenerator.add(0.000277777777778 * delta)
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
            val random = threadLocalRandom()
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
                    world.addEntityNew(
                            materials.plugin.entityTypes.tornado.createServer(
                                    world).apply {
                                setPos(Vector3d(x.toDouble(), y.toDouble(),
                                        terrain.highestTerrainBlockZAt(x,
                                                y).toDouble()))
                            })
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
        return Vector3d(rx, ry, rz).normalizedSafe().times(mix)
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
            val random = threadLocalRandom()
            if (delta < 360 + random.nextInt(80)) {
                return
            }
            count = max(20480 / delta.toInt(), 1)
        }
        tagStructure["SimulationCount"] = simulationCount.toTag()
        simulateSeason(terrain, chunk.posBlock.x,
                chunk.posBlock.y, chunk.posBlock.z, chunk.size.x, chunk.size.y,
                chunk.size.z, count)
    }

    private fun simulateSeason(terrain: TerrainServer,
                               x: Int,
                               y: Int,
                               z: Int,
                               dx: Int,
                               dy: Int,
                               dz: Int,
                               chance: Int) {
        val random = threadLocalRandom()
        val humidity00 = climateGenerator.humidity(x.toDouble(),
                y.toDouble())
        val temperature00 = climateGenerator.temperature(x.toDouble(),
                y.toDouble())
        val humidity10 = climateGenerator.humidity((x + 15).toDouble(),
                y.toDouble())
        val temperature10 = climateGenerator.temperature(
                (x + 15).toDouble(), y.toDouble())
        val humidity01 = climateGenerator.humidity(x.toDouble(),
                (y + 15).toDouble())
        val temperature01 = climateGenerator.temperature(x.toDouble(),
                (y + 15).toDouble())
        val humidity11 = climateGenerator.humidity((x + 15).toDouble(),
                (y + 15).toDouble())
        val temperature11 = climateGenerator.temperature(
                (x + 15).toDouble(), (y + 15).toDouble())
        for (yy in 0 until dx) {
            val yyy = yy + y
            val mixY = yy / 15.0
            val humidity0 = mix(humidity00, humidity01, mixY)
            val humidity1 = mix(humidity10, humidity11, mixY)
            val temperature0 = mix(temperature00, temperature01, mixY)
            val temperature1 = mix(temperature10, temperature11, mixY)
            for (xx in 0 until dy) {
                if (random.nextInt(chance) == 0) {
                    val xxx = xx + x
                    val zz = terrain.highestTerrainBlockZAt(xxx, yyy)
                    val mixX = xx / 15.0
                    val humidity = mix(humidity0, humidity1, mixX)
                    val temperature = climateGenerator.temperatureD(
                            mix(temperature0, temperature1, mixX), zz)
                    terrain.modify(xxx, yyy, zz - 1, 1, 1, 2) { terrain ->
                        val spaceBlock = terrain.block(xxx, yyy, zz)
                        val spaceType = terrain.type(spaceBlock)
                        val spaceData = terrain.data(spaceBlock)
                        val groundType = terrain.type(xxx, yyy, zz - 1)
                        if (humidity > 0.2) {
                            if (groundType == materials.dirt) {
                                terrain.typeData(xxx, yyy, zz - 1,
                                        materials.grass, random.nextInt(4))
                            } else if (groundType == materials.grass && random.nextInt(
                                    20) == 0) {
                                terrain.data(xxx, yyy, zz - 1,
                                        random.nextInt(9))
                            }
                        } else {
                            if (groundType == materials.grass) {
                                terrain.typeData(xxx, yyy, zz - 1,
                                        materials.dirt, 0)
                            }
                        }
                        if (temperature > 1.0) {
                            if (spaceType == materials.snow) {
                                if (spaceData < 8) {
                                    terrain.data(xxx, yyy, zz, spaceData + 1)
                                } else {
                                    terrain.typeData(xxx, yyy, zz,
                                            materials.air, 0)
                                }
                            }
                        } else {
                            val weather = climateGenerator.weather(
                                    xxx.toDouble(), yyy.toDouble())
                            if (temperature < 0.0 && (weather > 0.5 || chance == 1)) {
                                if (spaceType == materials.air ||
                                        spaceType == materials.flower ||
                                        spaceType == materials.stoneRock) {
                                    terrain.typeData(xxx, yyy, zz,
                                            materials.snow, 8)
                                } else if (spaceType == materials.snow && spaceData > 0) {
                                    terrain.data(xxx, yyy, zz, spaceData - 1)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private val ITEMS_LISTENER_TOKEN = ListenerToken("VanillaBasics:Items")
private val DEATH_MESSAGE_TOKEN = ListenerToken("VanillaBasics:DeathMessage")
