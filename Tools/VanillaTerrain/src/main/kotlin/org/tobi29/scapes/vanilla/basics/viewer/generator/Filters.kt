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
package org.tobi29.scapes.vanilla.basics.viewer.generator

import org.tobi29.scapes.engine.utils.math.clamp
import org.tobi29.scapes.vanilla.basics.generator.BiomeGenerator
import org.tobi29.scapes.vanilla.basics.generator.ClimateGenerator
import org.tobi29.scapes.vanilla.basics.generator.TerrainGenerator

fun terrain(
        terrainGenerator: TerrainGenerator): TerrainViewerCanvas.ColorSupplier {
    val layer = TerrainGenerator.TerrainGeneratorLayer()
    val output = TerrainGenerator.TerrainGeneratorOutput()
    return object : TerrainViewerCanvas.ColorSupplier {
        override fun color(x: Double,
                           y: Double,
                           o: TerrainViewerCanvas.Output) {
            terrainGenerator.generate(x, y, layer)
            terrainGenerator.generate(x, y, layer, output)
            val height = output.height / 256.0 - 1.0
            if (output.height < -0.0 || output.height >= 500.0) {
                o.h = 0.0
                o.s = 0.0
                o.v = 0.0
            } else if (height < 0.0) {
                o.h = 0.64
                o.s = 1.0
                o.v = 1.0 + height
            } else {
                o.h = 0.29
                o.s = 1.0
                o.v = 0.5 + height * 2.0
                if (!output.soiled) {
                    o.s = 0.0
                }
            }
        }
    }
}

fun biome(
        biomeGenerator: BiomeGenerator): TerrainViewerCanvas.ColorSupplier {
    return object : TerrainViewerCanvas.ColorSupplier {
        override fun color(x: Double,
                           y: Double,
                           o: TerrainViewerCanvas.Output) {
            val biome = biomeGenerator[x, y]
            when (biome) {
                BiomeGenerator.Biome.POLAR -> {
                    o.h = 0.0
                    o.s = 0.0
                    o.v = 1.0
                }
                BiomeGenerator.Biome.TUNDRA -> {
                    o.h = 0.4
                    o.s = 0.2
                    o.v = 0.8
                }
                BiomeGenerator.Biome.TAIGA -> {
                    o.h = 0.4
                    o.s = 0.8
                    o.v = 0.4
                }
                BiomeGenerator.Biome.OCEAN_ARCTIC -> {
                    o.h = 0.6
                    o.s = 0.6
                    o.v = 1.0
                }
                BiomeGenerator.Biome.WASTELAND -> {
                    o.h = 0.15
                    o.s = 0.3
                    o.v = 0.9
                }
                BiomeGenerator.Biome.STEPPE -> {
                    o.h = 0.2
                    o.s = 0.5
                    o.v = 0.7
                }
                BiomeGenerator.Biome.PLAINS -> {
                    o.h = 0.25
                    o.s = 0.7
                    o.v = 0.5
                }
                BiomeGenerator.Biome.FOREST -> {
                    o.h = 0.3
                    o.s = 0.9
                    o.v = 0.3
                }
                BiomeGenerator.Biome.OCEAN_TEMPERATE -> {
                    o.h = 0.6
                    o.s = 0.8
                    o.v = 0.9
                }
                BiomeGenerator.Biome.DESERT -> {
                    o.h = 0.1
                    o.s = 0.3
                    o.v = 1.0
                }
                BiomeGenerator.Biome.XERIC_SHRUBLAND -> {
                    o.h = 0.15
                    o.s = 0.4
                    o.v = 0.9
                }
                BiomeGenerator.Biome.DRY_SAVANNA -> {
                    o.h = 0.2
                    o.s = 0.5
                    o.v = 0.7
                }
                BiomeGenerator.Biome.WET_SAVANNA -> {
                    o.h = 0.3
                    o.s = 0.8
                    o.v = 0.5
                }
                BiomeGenerator.Biome.OASIS -> {
                    o.h = 0.3
                    o.s = 0.8
                    o.v = 1.0
                }
                BiomeGenerator.Biome.OCEAN_SUBTROPIC -> {
                    o.h = 0.6
                    o.s = 0.9
                    o.v = 0.8
                }
                BiomeGenerator.Biome.RAINFOREST -> {
                    o.h = 0.3
                    o.s = 1.0
                    o.v = 0.4
                }
                BiomeGenerator.Biome.OCEAN_TROPIC -> {
                    o.h = 0.6
                    o.s = 1.0
                    o.v = 0.7
                }
                else -> {
                    o.h = 0.0
                    o.s = 0.0
                    o.v = 0.0
                }
            }
        }

        override fun tooltip(x: Double,
                             y: Double): String? {
            return biomeGenerator[x, y].toString()
        }
    }
}

fun climate(
        climateGenerator: ClimateGenerator): TerrainViewerCanvas.ColorSupplier {
    return object : TerrainViewerCanvas.ColorSupplier {
        override fun color(x: Double,
                           y: Double,
                           o: TerrainViewerCanvas.Output) {
            val humidity3 = climateGenerator.humidity3(x, y)
            val temperature2 = climateGenerator.temperature2(x, y)
            val humidity2 = climateGenerator.humidity2D(temperature2,
                    humidity3)
            val riverHumidity = climateGenerator.riverHumidity(x, y)
            val weather = climateGenerator.weatherD(x, y, humidity2)
            val humidity = climateGenerator.humidityD(humidity2,
                    riverHumidity, weather)
            val sunlightReduction = climateGenerator.sunLightReduction(x, y)
            val temperature = climateGenerator.temperatureD(temperature2,
                    sunlightReduction, weather,
                    humidity3)
            o.h = humidity
            o.s = 1.0
            o.v = clamp(temperature / 100.0 + 0.5, 0.5, 1.0)
        }
    }
}

fun mix(
        a: TerrainViewerCanvas.ColorSupplier,
        b: TerrainViewerCanvas.ColorSupplier,
        mix: Double): TerrainViewerCanvas.ColorSupplier {
    return object : TerrainViewerCanvas.ColorSupplier {
        override fun color(x: Double,
                           y: Double,
                           o: TerrainViewerCanvas.Output) {
            a.color(x, y, o)
            val h = o.h
            val s = o.s
            val v = o.v
            b.color(x, y, o)
            o.h = org.tobi29.scapes.engine.utils.math.mix(h, o.h, mix)
            o.s = org.tobi29.scapes.engine.utils.math.mix(s, o.s, mix)
            o.v = org.tobi29.scapes.engine.utils.math.mix(v, o.v, mix)
        }

        override fun tooltip(x: Double,
                             y: Double): String? {
            a.tooltip(x, y)?.let { return it }
            b.tooltip(x, y)?.let { return it }
            return null
        }
    }
}
