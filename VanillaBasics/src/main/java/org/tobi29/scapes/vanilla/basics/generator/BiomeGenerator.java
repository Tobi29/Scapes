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
        double humidity3 = climateGenerator.getHumidity3(x, y);
        double temperature2 =
                climateGenerator.getTemperature2D(x, y, humidity3);
        double riverHumidity = climateGenerator.getRiverHumidity(x, y);
        double humidity2 =
                climateGenerator.getHumidity2D(temperature2, humidity3);
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
        POLAR(Zone.ARCTIC),
        TUNDRA(Zone.ARCTIC),
        TAIGA(Zone.ARCTIC),
        WASTELAND(Zone.TEMPERATE),
        STEPPE(Zone.TEMPERATE),
        FOREST(Zone.TEMPERATE),
        DESERT(Zone.SUBTROPIC),
        SAVANNA(Zone.SUBTROPIC),
        OASIS(Zone.SUBTROPIC),
        RAINFOREST(Zone.TROPIC);
        private final Zone zone;

        Biome(Zone zone) {
            this.zone = zone;
        }

        public Zone getZone() {
            return zone;
        }
    }
}
