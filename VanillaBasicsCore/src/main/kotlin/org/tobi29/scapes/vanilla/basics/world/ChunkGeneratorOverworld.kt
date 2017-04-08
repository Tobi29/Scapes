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

import org.tobi29.scapes.block.BlockType
import org.tobi29.scapes.chunk.generator.ChunkGenerator
import org.tobi29.scapes.chunk.generator.GeneratorOutput
import org.tobi29.scapes.engine.utils.math.abs
import org.tobi29.scapes.engine.utils.math.clamp
import org.tobi29.scapes.engine.utils.reduceOrNull
import org.tobi29.scapes.vanilla.basics.VanillaBasics
import org.tobi29.scapes.vanilla.basics.generator.*
import org.tobi29.scapes.vanilla.basics.material.OreType
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial
import org.tobi29.scapes.vanilla.basics.material.update.UpdateLavaFlow
import java.util.*

class ChunkGeneratorOverworld(random: Random,
                              private val terrainGenerator: TerrainGenerator,
                              private val materials: VanillaMaterial) : ChunkGenerator {
    private val random = Random()
    private val generator = TerrainGenerator.TerrainGeneratorOutput()
    private val layer = TerrainGenerator.TerrainGeneratorLayer()
    private val beachGenerator = BeachGenerator(random)
    private val stoneGenerator: StoneGenerator
    private val sandstoneGenerator = SandstoneGenerator(random)
    private val sandstoneLayers: IntArray
    private val seedInt: Long

    init {
        val stoneTypes = materials.plugin.stoneTypes
        val stoneLayers = sequenceOf(
                sequenceOf(stoneTypes.GRANITE,
                        stoneTypes.GABBRO,
                        stoneTypes.DIORITE),
                sequenceOf(stoneTypes.ANDESITE,
                        stoneTypes.BASALT,
                        stoneTypes.DACITE,
                        stoneTypes.RHYOLITE,
                        stoneTypes.MARBLE,
                        stoneTypes.GRANITE,
                        stoneTypes.GABBRO,
                        stoneTypes.DIORITE),
                sequenceOf(stoneTypes.ANDESITE,
                        stoneTypes.BASALT,
                        stoneTypes.DACITE,
                        stoneTypes.RHYOLITE,
                        stoneTypes.DIRT_STONE),
                sequenceOf(stoneTypes.DIRT_STONE,
                        stoneTypes.CHALK,
                        stoneTypes.CLAYSTONE,
                        stoneTypes.CHERT,
                        stoneTypes.CONGLOMERATE))
        stoneGenerator = StoneGenerator(random, stoneLayers)
        seedInt = random.nextInt().toLong() shl 32
        sandstoneLayers = IntArray(512)
        for (i in sandstoneLayers.indices) {
            sandstoneLayers[i] = random.nextInt(6)
        }
    }

    fun randomOreType(plugin: VanillaBasics,
                      stoneType: Int,
                      random: Random): OreType? {
        return plugin.oreTypes.asSequence().filter {
            it.stoneTypes.contains(stoneType)
        }.map {
            Pair(it, random.nextInt(it.rarity))
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
           zzz: Int): StoneType {
        var zzz = zzz
        val terrainFactor = terrainGenerator.generateTerrainFactorLayer(
                xxx.toDouble(), yyy.toDouble())
        zzz += (terrainGenerator.generateMountainFactorLayer(xxx.toDouble(),
                yyy.toDouble(), terrainFactor) * 300).toInt()
        if (zzz <= 96) {
            return stoneGenerator.stoneType(xxx, yyy, 0)
        } else if (zzz <= 240) {
            return stoneGenerator.stoneType(xxx, yyy, 1)
        } else if (terrainGenerator.generateVolcanoFactorLayer(xxx.toDouble(),
                yyy.toDouble()) > 0.4f) {
            return stoneGenerator.stoneType(xxx, yyy, 2)
        } else {
            return stoneGenerator.stoneType(xxx, yyy, 3)
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
        var stoneType: StoneType
        val sandType = if (generator.beach) beachGenerator.sandType(x, y) else 4
        val sandstone = sandstoneGenerator.sandstone(x, y)
        val stoneTypeZShift = random.nextInt(
                6) - (generator.mountainFactor * 300).toInt()
        var lastFree = clamp(generator.height.toInt(),
                generator.waterHeight.toInt(), dz - 1)
        val waterLevel = generator.waterHeight.toInt() - 1
        val riverBedLevel = waterLevel - 14
        var zzzShifted = lastFree + stoneTypeZShift
        if (zzzShifted <= 96) {
            stoneType = stoneGenerator.stoneType(x, y, 0)
        } else if (zzzShifted <= 240) {
            stoneType = stoneGenerator.stoneType(x, y, 1)
        } else if (generator.volcanoFactor > 0.4f) {
            stoneType = stoneGenerator.stoneType(x, y, 2)
        } else {
            stoneType = stoneGenerator.stoneType(x, y, 3)
        }
        for (zz in dz - 1 downTo lastFree + 1) {
            output.type(zz, materials.air)
            output.data(zz, 0)
        }
        for (zz in lastFree downTo z) {
            zzzShifted = zz + stoneTypeZShift
            if (zzzShifted == 240) {
                stoneType = stoneGenerator.stoneType(x, y, 1)
            } else if (zzzShifted == 96) {
                stoneType = stoneGenerator.stoneType(x, y, 0)
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
                                    output.updates.add {
                                        UpdateLavaFlow(it).set(x, y, zz, 0.0)
                                    }
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
                if (type == materials.stoneRaw) {
                    if (sandstone && zz > 240) {
                        type = materials.sandstone
                        data = sandstoneLayers[zz]
                    } else {
                        data = stoneType.id
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
