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

package scapes.plugin.tobi29.vanilla.basics.generator

class BiomeGenerator(climateGenerator: ClimateGenerator,
                     private val terrainGenerator: TerrainGenerator) {
    private val climateGenerator: ClimateGenerator

    init {
        this.climateGenerator = climateGenerator.at(-100, 0.0)
    }

    operator fun get(x: Double,
                     y: Double): Biome {
        val humidity3 = climateGenerator.humidity3(x, y)
        val temperature2 = climateGenerator.temperature2D(x, y, humidity3)
        val riverHumidity = climateGenerator.riverHumidity(x, y)
        val humidity2 = climateGenerator.humidity2D(temperature2, humidity3)
        val terrainFactor = terrainGenerator.generateTerrainFactorLayer(x, y)
        val mountainFactor = terrainGenerator.generateMountainFactorLayer(x, y,
                terrainFactor)
        return getD(humidity2, riverHumidity, temperature2, terrainFactor,
                mountainFactor)
    }

    fun getD(humidity2: Double,
             riverHumidity: Double,
             temperature: Double,
             terrainFactor: Double,
             mountainFactor: Double): Biome {
        if (terrainFactor + mountainFactor * 3.2 < 0.21) {
            if (temperature < -20.0) {
                return Biome.OCEAN_ARCTIC
            } else if (temperature < 20.0) {
                return Biome.OCEAN_TEMPERATE
            } else if (humidity2 < 0.8) {
                return Biome.OCEAN_SUBTROPIC
            } else {
                return Biome.OCEAN_TROPIC
            }
        }
        if (temperature < 0.0) {
            if (humidity2 < 0.2) {
                if (temperature < -20.0) {
                    return Biome.POLAR
                }
                return Biome.TUNDRA
            } else {
                return Biome.TAIGA
            }
        }
        if (humidity2 < 0.2) {
            if (temperature < 30.0) {
                return Biome.WASTELAND
            } else {
                if (riverHumidity > 0.2) {
                    return Biome.OASIS
                }
                return Biome.DESERT
            }
        } else if (humidity2 < 0.3) {
            if (temperature < 30.0) {
                return Biome.STEPPE
            } else {
                if (riverHumidity > 0.2) {
                    return Biome.OASIS
                }
                return Biome.XERIC_SHRUBLAND
            }
        } else if (humidity2 < 0.5) {
            if (temperature < 30.0) {
                return Biome.PLAINS
            } else {
                if (riverHumidity > 0.2) {
                    return Biome.OASIS
                }
                return Biome.DRY_SAVANNA
            }
        } else if (humidity2 < 0.7) {
            if (temperature < 30.0) {
                return Biome.FOREST
            } else {
                return Biome.WET_SAVANNA
            }
        } else {
            return Biome.RAINFOREST
        }
    }

    enum class Zone {
        ARCTIC,
        TEMPERATE,
        SUBTROPIC,
        TROPIC
    }

    enum class Biome constructor(private val zone: Zone,
                                 val isValidSpawn: Boolean) {
        POLAR(Zone.ARCTIC, false),
        TUNDRA(Zone.ARCTIC, false),
        TAIGA(Zone.ARCTIC, false),
        OCEAN_ARCTIC(Zone.ARCTIC, false),
        WASTELAND(Zone.TEMPERATE, false),
        STEPPE(Zone.TEMPERATE, false),
        PLAINS(Zone.TEMPERATE, true),
        FOREST(Zone.TEMPERATE, false),
        OCEAN_TEMPERATE(Zone.TEMPERATE, false),
        DESERT(Zone.SUBTROPIC, false),
        XERIC_SHRUBLAND(Zone.SUBTROPIC, false),
        DRY_SAVANNA(Zone.SUBTROPIC, false),
        WET_SAVANNA(Zone.SUBTROPIC, false),
        OASIS(Zone.SUBTROPIC, true),
        OCEAN_SUBTROPIC(Zone.SUBTROPIC, false),
        RAINFOREST(Zone.TROPIC, false),
        OCEAN_TROPIC(Zone.TROPIC, false);

        fun zone(): Zone {
            return zone
        }
    }
}
