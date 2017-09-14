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

package org.tobi29.scapes.vanilla.basics.generator

import org.tobi29.scapes.engine.utils.ThreadLocal
import org.tobi29.scapes.engine.utils.generation.value.SimplexNoise
import org.tobi29.scapes.engine.utils.math.*

class TerrainGenerator(random: Random) {
    private val terrainNoise = SimplexNoise(random.nextLong())
    private val oceanNoise = SimplexNoise(random.nextLong())
    private val mountainNoise = SimplexNoise(random.nextLong())
    private val mountainCarveNoise = SimplexNoise(random.nextLong())
    private val mountainHeightNoise = SimplexNoise(random.nextLong())
    private val volcanoNoise = SimplexNoise(random.nextLong())
    private val volcanoHeightNoise = SimplexNoise(random.nextLong())
    private val riverNoise = SimplexNoise(random.nextLong())
    private val canyonNoise = SimplexNoise(random.nextLong())
    private val canyonDepthNoise = SimplexNoise(random.nextLong())
    private val caveRiverNoise = SimplexNoise(random.nextLong())
    private val caveNoise = SimplexNoise(random.nextLong())
    private val caveHeightNoise = SimplexNoise(random.nextLong())
    private val magmaNoise = SimplexNoise(random.nextLong())

    fun generate(x: Double,
                 y: Double,
                 output: TerrainGeneratorLayer) {
        val terrainFactor = generateTerrainFactorLayer(x, y)
        val terrainHeight = terrainFactor * 128.0 + output.terrainBase
        val mountainFactor = generateMountainFactorLayer(x, y, terrainFactor)
        val mountain: Double
        val mountainCarve: Double
        val mountainHeight: Double
        if (mountainFactor > 0.0) {
            mountain = 1.0 - abs(
                    mountainNoise.noiseOctave(x / 512.0, y / 512.0, 2, 4.0,
                            0.2))
            val mountainCarveFactor = 1.0 - sqr(1.0 - mountain)
            mountainCarve = abs(
                    mountainCarveNoise.noiseOctave(x / 96.0, y / 96.0, 4, 4.0,
                            0.2)) * mountainCarveFactor
            mountainHeight = (mountain - mountainCarve * 0.17) * mountainFactor * 256.0
        } else {
            mountain = 0.0
            mountainCarve = 0.0
            mountainHeight = 0.0
        }
        val volcanoFactor = generateVolcanoFactorLayer(x, y)
        var volcano: Double
        val volcanoHeight: Double
        if (volcanoFactor > 0.0) {
            volcano = sqr(clamp(
                    volcanoNoise.noiseOctave(x / 512.0, y / 512.0, 4, 8.0,
                            0.08) * 2.0 - 1.0, 0.0, 1.0))
            volcano *= volcano
            if (volcano > 0.5) {
                volcanoHeight = (1.0 - volcano) * volcanoFactor * 192.0
            } else {
                volcanoHeight = volcano * volcanoFactor * 192.0
            }
        } else {
            volcano = 0.0
            volcanoHeight = 0.0
        }
        val river = generateRiverLayer(x, y, mountainFactor, 32.0)
        val canyon: Double
        val canyonFactor = sqr(clamp(
                canyonDepthNoise.noise(x / 4096.0, y / 4096.0) * 5.0 - 3.0, 0.0,
                1.0))
        if (canyonFactor > 0.0) {
            canyon = clamp(sqr(1.0 - abs(
                    canyonNoise.noiseOctave(x / 1024.0, y / 1024.0, 2, 8.0,
                            0.1))) *
                    4.0 * canyonFactor - 3.0, 0.0, 1.0)
        } else {
            canyon = 0.0
        }
        var groundHeight = terrainHeight + mountainHeight + volcanoHeight
        if (groundHeight > output.riverBottom) {
            groundHeight -= output.riverBottom
            groundHeight *= river
            groundHeight += output.riverBottom
        }
        groundHeight -= canyon * 40.0
        output.height = groundHeight
        output.mountain = mountain
        output.mountainCarve = mountainCarve
        output.mountainFactor = mountainFactor
        output.volcano = volcano
        output.volcanoHeight = volcanoHeight
        output.volcanoFactor = volcanoFactor
        output.river = river
    }

    fun generate(x: Double,
                 y: Double,
                 layer: TerrainGeneratorLayer,
                 output: TerrainGeneratorOutput) {
        val cave = clamp(sqr(1.0 - abs(
                caveNoise.noiseOctave(x / 128.0, y / 128.0, 2, 8.0,
                        0.1))) * 8.0 - 6.0, 0.0, 1.0)
        val caveHeight = output.caveAverageHeight + caveHeightNoise.noise(
                x / 512.0, y / 512.0) * output.caveHeightVary
        val caveRiver = clamp(sqr(1.0 - abs(
                caveRiverNoise.noiseOctave(x / 512.0, y / 512.0, 2, 8.0,
                        0.1))) * 8.0 - 7.0, 0.0, 1.0)
        val magmaHeight = 7.0 + magmaNoise.noise(x / 512.0, y / 512.0) * 5.0
        output.height = layer.height
        output.mountainFactor = layer.mountainFactor
        output.volcanoFactor = layer.volcanoFactor
        output.cave = cave
        output.caveHeight = caveHeight
        output.caveRiver = caveRiver
        output.magmaHeight = magmaHeight
        output.river = layer.river
        output.soiled = layer.mountain - layer.mountainCarve * 0.4 < 1.3 - layer.mountainFactor && layer.volcanoHeight < 18
        output.beach = layer.height > output.beachMinHeight && layer.height <= output.beachMaxHeight
        output.lavaChance = if (layer.volcano > 0.5) 10000 else if (layer.volcano > 0.2) 10000 else 0
    }

    fun generateTerrainFactorLayer(x: Double,
                                   y: Double): Double {
        var terrainFactor = sqr(
                terrainNoise.noiseOctave(x / 16384.0, y / 16384.0, 4, 8.0, 0.1))
        if (terrainFactor > 0.3) {
            terrainFactor -= (terrainFactor - 0.3) * 0.3
            if (terrainFactor > 0.35) {
                terrainFactor -= (terrainFactor - 0.35) * 0.6
                if (terrainFactor > 0.4) {
                    terrainFactor -= (terrainFactor - 0.4) * 0.9
                }
            }
        }
        if (terrainFactor < 0.2) {
            val oceanDepth = oceanNoise.noise(x / 8192.0,
                    y / 8192.0) * 0.5 + 0.5
            terrainFactor -= min(0.2 - terrainFactor,
                    0.02) * oceanDepth * 30.0
            if (terrainFactor < 0.15) {
                terrainFactor -= min(0.15 - terrainFactor, 0.04) * 4.0
            }
        }
        return terrainFactor
    }

    fun generateMountainFactorLayer(x: Double,
                                    y: Double,
                                    terrainFactor: Double): Double {
        return cbe(clamp(1.0 - abs(
                mountainHeightNoise.noise(x / 12288.0, y / 12288.0)) * 1.1, 0.0,
                1.0) * min(
                mountainHeightNoise.noise(x / 4096.0, y / 4096.0) * 0.6 + 0.8,
                0.9)) * clamp(terrainFactor * 4.0 - 0.4, 0.0, 1.0)
    }

    fun generateVolcanoFactorLayer(x: Double,
                                   y: Double): Double {
        return sqr(1.0 - clamp(abs(
                volcanoHeightNoise.noise(x / 16384.0,
                        y / 16384.0)) * 3.0 - 0.25, 0.0, 1.0))
    }

    fun generateRiverLayer(x: Double,
                           y: Double,
                           mountainFactor: Double,
                           limit: Double): Double {
        val riverFactor = clamp(3.0 - mountainFactor * 10.0, 0.0, 1.0)
        if (riverFactor > 0.0) {
            val riverN = riverNoise.noiseOctave(x / 4096.0, y / 4096.0, 4, 4.0,
                    0.2)
            return clamp(
                    limit - sqr(1.0 - abs(riverN)) * limit *
                            riverFactor, 0.0, 1.0)
        } else {
            return 1.0
        }
    }

    fun isValidSpawn(x: Double,
                     y: Double): Boolean {
        val layer = TerrainGeneratorLayer()
        generate(x, y, layer)
        if (layer.mountainFactor > 0.2) {
            return false
        }
        if (layer.volcanoFactor > 0.4) {
            return false
        }
        val output = TerrainGeneratorOutput()
        generate(x, y, layer, output)
        return output.height > output.waterHeight
    }

    class TerrainGeneratorLayer {
        val terrainBase = SEA_LEVEL - 31
        val riverBottom = terrainBase + 20.0
        var height = 0.0
        var mountain = 0.0
        var mountainCarve = 0.0
        var mountainFactor = 0.0
        var volcano = 0.0
        var volcanoHeight = 0.0
        var volcanoFactor = 0.0
        var river = 0.0

        companion object {
            private val TL = ThreadLocal { TerrainGeneratorLayer() }

            fun current(): TerrainGeneratorLayer = TL.get()
        }
    }

    class TerrainGeneratorOutput {
        val waterHeight = SEA_LEVEL
        val caveAverageHeight = waterHeight * 0.5
        val caveHeightVary = caveAverageHeight * 0.75
        val caveRiverHeight = waterHeight * 0.5
        val beachMinHeight = SEA_LEVEL - 6.0
        val beachMaxHeight = SEA_LEVEL + 2.0
        var height = 0.0
        var mountainFactor = 0.0
        var volcanoFactor = 0.0
        var cave = 0.0
        var caveHeight = 0.0
        var caveRiver = 0.0
        var magmaHeight = 0.0
        var river = 0.0
        var soiled = false
        var beach = false
        var lavaChance = 0

        companion object {
            private val TL = ThreadLocal { TerrainGeneratorOutput() }

            fun current(): TerrainGeneratorOutput = TL.get()
        }
    }

    companion object {
        private val SEA_LEVEL = 256.0
    }
}
