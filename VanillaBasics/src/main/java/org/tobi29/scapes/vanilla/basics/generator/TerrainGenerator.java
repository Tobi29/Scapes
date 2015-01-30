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

import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.noise.value.SimplexNoise;
import org.tobi29.scapes.engine.utils.math.noise.value.ValueNoise;

import java.util.Random;

public class TerrainGenerator {
    private final ValueNoise terrainNoise, mountainNoise, mountainHeightNoise,
            volcanoNoise, volcanoHeightNoise, riverNoise, canyonNoise,
            canyonDepthNoise, caveRiverNoise, caveNoise, caveHeightNoise,
            magmaNoise;

    public TerrainGenerator(Random random) {
        terrainNoise = new SimplexNoise(random.nextLong());
        mountainNoise = new SimplexNoise(random.nextLong());
        mountainHeightNoise = new SimplexNoise(random.nextLong());
        volcanoNoise = new SimplexNoise(random.nextLong());
        volcanoHeightNoise = new SimplexNoise(random.nextLong());
        riverNoise = new SimplexNoise(random.nextLong());
        canyonNoise = new SimplexNoise(random.nextLong());
        canyonDepthNoise = new SimplexNoise(random.nextLong());
        caveRiverNoise = new SimplexNoise(random.nextLong());
        caveNoise = new SimplexNoise(random.nextLong());
        caveHeightNoise = new SimplexNoise(random.nextLong());
        magmaNoise = new SimplexNoise(random.nextLong());
    }

    public void generate(double x, double y, TerrainGeneratorOutput output) {
        double terrainFactor = generateTerrainFactorLayer(x, y);
        double terrainHeight = terrainFactor * 64.0d + 224.0d;
        double mountainFactor = generateMountainFactorLayer(x, y);
        double mountain, mountainHeight;
        if (mountainFactor > 0.0d) {
            mountain = 1.0d - FastMath.abs(mountainNoise
                    .noiseOctave(x / 512.0d, y / 512.0d, 4, 4.0d, 0.2));
            mountainHeight = mountain * mountainFactor * 192.0d;
        } else {
            mountain = 0.0d;
            mountainHeight = 0.0d;
        }
        double volcanoFactor = generateVolcanoFactorLayer(x, y);
        double volcano, volcanoHeight;
        if (volcanoFactor > 0.0d) {
            volcano = FastMath.sqr(FastMath.clamp(volcanoNoise
                    .noiseOctave(x / 512.0d, y / 512.0d, 4, 8.0d, 0.08) * 2.0d -
                    1.0d, 0.0d, 1.0d));
            volcano *= volcano;
            if (volcano > 0.5) {
                volcanoHeight = (1.0d - volcano) * volcanoFactor * 192.0d;
            } else {
                volcanoHeight = volcano * volcanoFactor * 192.0d;
            }
        } else {
            volcano = 0.0d;
            volcanoHeight = 0.0d;
        }
        double river = generateRiverLayer(x, y, mountainFactor, 32.0d);
        double canyon;
        double canyonFactor = FastMath.sqr(FastMath.clamp(
                canyonDepthNoise.noise(x / 4096.0d, y / 4096.0d) * 5.0d - 3.0d,
                0.0d, 1.0d));
        if (canyonFactor > 0.0d) {
            canyon = FastMath.clamp(FastMath.sqr(1.0d - FastMath.abs(canyonNoise
                    .noiseOctave(x / 1024.0d, y / 1024.0d, 2, 8.0d, 0.1))) *
                    4.0d * canyonFactor - 3.0d, 0.0d, 1.0d);
        } else {
            canyon = 0.0d;
        }
        double groundHeight = terrainHeight + mountainHeight + volcanoHeight;
        if (groundHeight > 244.0d) {
            groundHeight -= 244.0d;
            groundHeight *= river;
            groundHeight += 244.0d;
        }
        groundHeight -= canyon * 40.0d;
        double cave = FastMath.clamp(FastMath.sqr(1.0d - FastMath.abs(
                caveNoise.noiseOctave(x / 128.0d, y / 128.0d, 2, 8.0d, 0.1))) *
                8.0d - 6.0d, 0.0d, 1.0d);
        double caveHeight =
                128.0d + caveHeightNoise.noise(x / 512.0d, y / 512.0d) * 96.0d;
        double caveRiver = FastMath.clamp(FastMath.sqr(1.0d - FastMath.abs(
                caveRiverNoise
                        .noiseOctave(x / 512.0d, y / 512.0d, 2, 8.0d, 0.1))) *
                8.0d - 7.0d, 0.0d, 1.0d);
        double magmaHeight =
                7.0d + magmaNoise.noise(x / 512.0d, y / 512.0d) * 5.0d;
        output.height = groundHeight;
        output.mountainFactor = mountainFactor;
        output.volcanoFactor = volcanoFactor;
        output.waterHeight = 256.0d;
        output.cave = cave;
        output.caveHeight = caveHeight;
        output.caveRiver = caveRiver;
        output.caveRiverHeight = 128.0d;
        output.magmaHeight = magmaHeight;
        output.river = river;
        output.soiled = mountain < 1.3 - mountainFactor && volcanoHeight < 18;
        output.beach = groundHeight > 250.0d && groundHeight <= 259.0d;
        output.lavaChance = volcano > 0.5 ? 10000 : volcano > 0.2 ? 10000 : 0;
    }

    public double generateTerrainFactorLayer(double x, double y) {
        return terrainNoise
                .noiseOctave(x / 4096.0d, y / 4096.0d, 4, 8.0d, 0.1d);
    }

    public double generateMountainFactorLayer(double x, double y) {
        return FastMath.sqr((1.0d - FastMath.clamp(FastMath.abs(
                mountainHeightNoise.noise(x / 8192.0d, y / 8192.0d)) * 3.0d -
                0.25, 0.0d, 1.0d)) *
                (mountainHeightNoise.noise(x / 4096.0d, y / 4096.0d) * 0.5 +
                        0.5));
    }

    public double generateVolcanoFactorLayer(double x, double y) {
        return FastMath.sqr(1.0d - FastMath.clamp(FastMath.abs(
                volcanoHeightNoise.noise(x / 16384.0d, y / 16384.0d)) * 3.0d -
                0.25, 0.0d, 1.0d));
    }

    public double generateRiverLayer(double x, double y, double mountainFactor,
            double limit) {
        double riverFactor =
                FastMath.clamp(3.0d - mountainFactor * 10.0d, 0.0d, 1.0d);
        if (riverFactor > 0.0d) {
            double riverN = riverNoise
                    .noiseOctave(x / 4096.0d, y / 4096.0d, 4, 4.0d, 0.2);
            return FastMath.clamp(limit -
                    FastMath.sqr(1.0d - FastMath.abs(riverN)) * limit *
                            riverFactor, 0.0d, 1.0d);
        } else {
            return 1.0d;
        }
    }

    public static class TerrainGeneratorOutput {
        public double height, mountainFactor, volcanoFactor, waterHeight, cave,
                caveHeight, caveRiver, caveRiverHeight, magmaHeight, river;
        public boolean soiled, beach;
        public int lavaChance;
    }
}
