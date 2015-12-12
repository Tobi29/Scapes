/*
 * Copyright 2012-2015 Tobi29
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
package org.tobi29.scapes.vanilla.basics.generator;

public class BiomeGenerator {
    private final ClimateGenerator climateGenerator;

    public BiomeGenerator(ClimateGenerator climateGenerator) {
        this.climateGenerator = climateGenerator.at(-100, 0.0);
    }

    public Biome get(double x, double y) {
        double humidity3 = climateGenerator.humidity3(x, y);
        double temperature2 = climateGenerator.temperature2D(x, y, humidity3);
        double riverHumidity = climateGenerator.riverHumidity(x, y);
        double humidity2 = climateGenerator.humidity2D(temperature2, humidity3);
        return getD(humidity2, riverHumidity, temperature2);
    }

    public Biome getD(double humidity2, double riverHumidity,
            double temperature) {
        if (temperature < 0.0) {
            if (humidity2 < 0.2) {
                if (temperature < -20.0) {
                    return Biome.POLAR;
                }
                return Biome.TUNDRA;
            }
            return Biome.TAIGA;
        }
        if (humidity2 < 0.2) {
            if (temperature < 30.0) {
                return Biome.WASTELAND;
            } else if (riverHumidity > 0.2) {
                return Biome.OASIS;
            }
            return Biome.DESERT;
        } else if (humidity2 < 0.4) {
            if (temperature < 30.0) {
                return Biome.STEPPE;
            }
            return Biome.SAVANNA;
        } else if (humidity2 < 0.7) {
            return Biome.FOREST;
        }
        return Biome.RAINFOREST;
    }

    public enum Zone {
        ARCTIC,
        TEMPERATE,
        SUBTROPIC,
        TROPIC
    }

    public enum Biome {
        POLAR(Zone.ARCTIC, false),
        TUNDRA(Zone.ARCTIC, false),
        TAIGA(Zone.ARCTIC, false),
        WASTELAND(Zone.TEMPERATE, false),
        STEPPE(Zone.TEMPERATE, true),
        FOREST(Zone.TEMPERATE, false),
        DESERT(Zone.SUBTROPIC, false),
        SAVANNA(Zone.SUBTROPIC, false),
        OASIS(Zone.SUBTROPIC, true),
        RAINFOREST(Zone.TROPIC, false);
        private final Zone zone;
        private final boolean spawn;

        Biome(Zone zone, boolean spawn) {
            this.zone = zone;
            this.spawn = spawn;
        }

        public Zone zone() {
            return zone;
        }

        public boolean isValidSpawn() {
            return spawn;
        }
    }
}
