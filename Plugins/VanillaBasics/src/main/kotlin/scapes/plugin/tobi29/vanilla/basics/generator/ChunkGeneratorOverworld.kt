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

package scapes.plugin.tobi29.vanilla.basics.generator

import org.tobi29.scapes.block.BlockType
import org.tobi29.scapes.chunk.generator.ChunkGenerator
import org.tobi29.scapes.chunk.generator.GeneratorOutput
import org.tobi29.scapes.engine.utils.math.abs
import org.tobi29.scapes.engine.utils.math.clamp
import org.tobi29.scapes.engine.utils.math.noise.layer.*
import org.tobi29.scapes.engine.utils.reduceOrNull
import scapes.plugin.tobi29.vanilla.basics.VanillaBasics
import scapes.plugin.tobi29.vanilla.basics.material.OreType
import scapes.plugin.tobi29.vanilla.basics.material.StoneType
import scapes.plugin.tobi29.vanilla.basics.material.VanillaMaterial
import scapes.plugin.tobi29.vanilla.basics.material.update.UpdateLavaFlow
import java.util.*

class ChunkGeneratorOverworld(random: Random,
                              private val terrainGenerator: TerrainGenerator, private val materials: VanillaMaterial) : ChunkGenerator {
    private val random = Random()
    private val sandLayer: RandomNoiseLayer
    private val sandstoneLayer: RandomNoiseLayer
    private val stoneLayers: Array<RandomNoiseLayer>
    private val layer = TerrainGenerator.TerrainGeneratorLayer()
    private val generator = TerrainGenerator.TerrainGeneratorOutput()
    private val sandstoneLayers: ShortArray
    private val seedInt: Long

    init {
        val stoneTypes = arrayOf(
                intArrayOf(StoneType.GRANITE.data(materials.registry),
                        StoneType.GABBRO.data(materials.registry),
                        StoneType.DIORITE.data(materials.registry)),
                intArrayOf(StoneType.ANDESITE.data(materials.registry),
                        StoneType.BASALT.data(materials.registry),
                        StoneType.DACITE.data(materials.registry),
                        StoneType.RHYOLITE.data(materials.registry),
                        StoneType.MARBLE.data(materials.registry),
                        StoneType.GRANITE.data(materials.registry),
                        StoneType.GABBRO.data(materials.registry),
                        StoneType.DIORITE.data(materials.registry)),
                intArrayOf(StoneType.ANDESITE.data(materials.registry),
                        StoneType.BASALT.data(materials.registry),
                        StoneType.DACITE.data(materials.registry),
                        StoneType.RHYOLITE.data(materials.registry),
                        StoneType.DIRT_STONE.data(materials.registry)),
                intArrayOf(StoneType.DIRT_STONE.data(materials.registry),
                        StoneType.CHALK.data(materials.registry),
                        StoneType.CLAYSTONE.data(materials.registry),
                        StoneType.CHERT.data(materials.registry),
                        StoneType.CONGLOMERATE.data(
                                materials.registry)))
        seedInt = random.nextInt().toLong() shl 32
        sandstoneLayers = ShortArray(512)
        for (i in sandstoneLayers.indices) {
            sandstoneLayers[i] = random.nextInt(6).toShort()
        }
        val sandBase = RandomNoiseRandomLayer(random.nextLong(), 6)
        val sandZoom = RandomNoiseZoomLayer(sandBase, 1024.0)
        val sandNoise = RandomNoiseSimplexNoiseLayer(sandZoom,
                random.nextLong(), 128.0)
        sandLayer = RandomNoiseNoiseLayer(sandNoise, random.nextLong(), 2)
        stoneLayers = stoneTypes.map {
            val stoneBase = RandomNoiseRandomLayer(random.nextLong(), it.size)
            val stoneFilter = RandomNoiseFilterLayer(stoneBase, *it)
            val stoneZoom = RandomNoiseZoomLayer(stoneFilter, 2048.0)
            RandomNoiseSimplexNoiseLayer(stoneZoom, random.nextLong(), 1024.0)
        }.toTypedArray()
        val sandstoneBase = RandomNoiseRandomLayer(random.nextLong(), 4)
        val sandstoneZoom = RandomNoiseZoomLayer(sandstoneBase, 2048.0)
        val sandstoneNoise = RandomNoiseSimplexNoiseLayer(sandstoneZoom,
                random.nextLong(), 1024.0)
        sandstoneLayer = RandomNoiseNoiseLayer(sandstoneNoise,
                random.nextLong(), 3)
    }

    fun randomOreType(plugin: VanillaBasics,
                      stoneType: Int,
                      random: Random): OreType? {
        return plugin.oreTypes.asSequence().filter { oreType ->
            oreType.stoneTypes().contains(stoneType)
        }.map { oreType ->
            Pair(oreType, random.nextInt(oreType.rarity()))
        }.reduceOrNull { first, second ->
            if (first.second == second.second && random.nextBoolean()) {
                first
            } else if (first.second > second.second) {
                first
            } else {
                second
            }
        }?.first
    }

    fun it(xxx: Int,
           yyy: Int,
           zzz: Int): Int {
        var zzz = zzz
        val terrainFactor = terrainGenerator.generateTerrainFactorLayer(
                xxx.toDouble(), yyy.toDouble())
        zzz += (terrainGenerator.generateMountainFactorLayer(xxx.toDouble(),
                yyy.toDouble(), terrainFactor) * 300).toInt()
        if (zzz <= 96) {
            return stoneLayers[0].getInt(xxx, yyy)
        } else if (zzz <= 240) {
            return stoneLayers[1].getInt(xxx, yyy)
        } else if (terrainGenerator.generateVolcanoFactorLayer(xxx.toDouble(),
                yyy.toDouble()) > 0.4f) {
            return stoneLayers[2].getInt(xxx, yyy)
        } else {
            return stoneLayers[3].getInt(xxx, yyy)
        }
    }

    override fun seed(x: Int,
                      y: Int) {
        var hash = 17
        hash = 31 * hash + x
        hash = 31 * hash + y
        random.setSeed(hash + seedInt)
    }

    override fun makeLand(x: Int,
                          y: Int,
                          z: Int,
                          dz: Int,
                          output: GeneratorOutput) {
        terrainGenerator.generate(x.toDouble(), y.toDouble(), layer)
        terrainGenerator.generate(x.toDouble(), y.toDouble(), layer, generator)
        var stoneType: Int
        val sandType = if (generator.beach) sandLayer.getInt(x, y) else 4
        val sandstone = sandstoneLayer.getInt(x, y) == 0
        val stoneTypeZShift = random.nextInt(
                6) - (generator.mountainFactor * 300).toInt()
        var lastFree = clamp(generator.height.toInt(),
                generator.waterHeight.toInt(), dz - 1)
        val waterLevel = generator.waterHeight.toInt() - 1
        val riverBedLevel = waterLevel - 14
        var zzzShifted = lastFree + stoneTypeZShift
        if (zzzShifted <= 96) {
            stoneType = stoneLayers[0].getInt(x, y)
        } else if (zzzShifted <= 240) {
            stoneType = stoneLayers[1].getInt(x, y)
        } else if (generator.volcanoFactor > 0.4f) {
            stoneType = stoneLayers[2].getInt(x, y)
        } else {
            stoneType = stoneLayers[3].getInt(x, y)
        }
        for (zz in dz - 1 downTo lastFree + 1) {
            output.type(zz, materials.air)
            output.data(zz, 0)
        }
        for (zz in lastFree downTo z) {
            zzzShifted = zz + stoneTypeZShift
            if (zzzShifted == 240) {
                stoneType = stoneLayers[1].getInt(x, y).toShort().toInt()
            } else if (zzzShifted == 96) {
                stoneType = stoneLayers[0].getInt(x, y).toShort().toInt()
            }
            var type: BlockType
            var data = 0
            if (zz < generator.height) {
                if (zz == 0) {
                    type = materials.bedrock
                } else if (zz < generator.magmaHeight) {
                    type = materials.lava
                } else if (generator.caveRiver > abs(
                        zz - generator.caveRiverHeight) / 16.0) {
                    if (zz < generator.caveRiverHeight) {
                        type = materials.water
                    } else {
                        type = materials.air
                    }
                } else if (generator.cave > abs(
                        zz - generator.caveHeight) / 8.0) {
                    type = materials.air
                } else {
                    if (sandType < 3) {
                        if (lastFree - zz < 9 && lastFree - zz < 5 + random.nextInt(
                                3)) {
                            type = materials.sand
                            data = sandType
                        } else {
                            type = materials.stoneRaw
                        }
                    } else if (lastFree >= waterLevel) {
                        if (zz == lastFree) {
                            if (generator.soiled) {
                                type = materials.grass
                                data = random.nextInt(9)
                            } else {
                                if (generator.lavaChance > 0 && random.nextInt(
                                        generator.lavaChance) == 0) {
                                    type = materials.lava
                                    output.updates.add(
                                            UpdateLavaFlow().set(x, y, zz, 0.0))
                                } else {
                                    type = materials.stoneRaw
                                }
                            }
                        } else if (lastFree - zz < 9) {
                            if (lastFree - zz < 5 + random.nextInt(5)) {
                                if (generator.soiled) {
                                    type = materials.dirt
                                } else {
                                    type = materials.stoneRaw
                                }
                            } else {
                                type = materials.stoneRaw
                            }
                        } else {
                            type = materials.stoneRaw
                        }
                    } else if (lastFree > riverBedLevel && lastFree - zz < 9 &&
                            generator.river < 0.9 &&
                            lastFree - zz < 5 + random.nextInt(3)) {
                        type = materials.sand
                        data = 2
                    } else {
                        type = materials.stoneRaw
                    }
                }
                if (type === materials.stoneRaw) {
                    if (sandstone && zz > 240) {
                        type = materials.sandstone
                        data = sandstoneLayers[zz].toInt()
                    } else {
                        data = stoneType
                    }
                }
            } else {
                if (zz < generator.waterHeight) {
                    type = materials.water
                } else {
                    type = materials.air
                }
                lastFree = zz - 1
            }
            output.type(zz, type)
            output.data(zz, data)
        }
    }
}
