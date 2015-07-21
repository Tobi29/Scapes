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

import org.tobi29.scapes.chunk.terrain.TerrainRenderInfo;
import org.tobi29.scapes.engine.utils.math.FastMath;

public class ClimateInfoLayer implements TerrainRenderInfo.InfoLayer {
    private final ClimateGenerator climateGenerator;
    private int sx, sy;
    private double temperature00, temperature10, temperature01, temperature11;
    private double humidity00, humidity10, humidity01, humidity11;

    public ClimateInfoLayer(ClimateGenerator climateGenerator) {
        this.climateGenerator = climateGenerator;
    }

    @Override
    public void init(int x, int y, int z, int xSize, int ySize, int zSize) {
        sx = x;
        sy = y;
        xSize--;
        ySize--;
        zSize--;
        temperature00 = climateGenerator.temperature(x, y, z);
        temperature10 = climateGenerator.temperature(x + xSize, y, z);
        temperature01 = climateGenerator.temperature(x, y + ySize, z);
        temperature11 = climateGenerator.temperature(x + xSize, y + ySize, z);
        humidity00 = climateGenerator.humidity(x, y, z);
        humidity10 = climateGenerator.humidity(x + xSize, y, z);
        humidity01 = climateGenerator.humidity(x, y + ySize, z);
        humidity11 = climateGenerator.humidity(x + xSize, y + ySize, z);
    }

    public double temperature(int x, int y, int z) {
        x -= sx;
        y -= sy;
        double mixX = x / 15.0;
        double mixY = y / 15.0;
        double temperature0 = FastMath.mix(temperature00, temperature01, mixY);
        double temperature1 = FastMath.mix(temperature10, temperature11, mixY);
        return climateGenerator
                .temperatureD(FastMath.mix(temperature0, temperature1, mixX),
                        z);
    }

    public double humidity(int x, int y) {
        x -= sx;
        y -= sy;
        double mixX = x / 15.0;
        double mixY = y / 15.0;
        double humidity0 = FastMath.mix(humidity00, humidity01, mixY);
        double humidity1 = FastMath.mix(humidity10, humidity11, mixY);
        return FastMath.mix(humidity0, humidity1, mixX);
    }
}
