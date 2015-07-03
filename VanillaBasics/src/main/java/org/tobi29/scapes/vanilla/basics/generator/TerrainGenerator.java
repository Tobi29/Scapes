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
    private final ValueNoise terrainNoise, mountainNoise, mountainCarveNoise,
            mountainHeightNoise, volcanoNoise, volcanoHeightNoise, riverNoise,
            canyonNoise, canyonDepthNoise, caveRiverNoise, caveNoise,
            caveHeightNoise, magmaNoise;

    public TerrainGenerator(Random random) {
        terrainNoise = new SimplexNoise(random.nextLong());
        mountainNoise = new SimplexNoise(random.nextLong());
        mountainCarveNoise = new SimplexNoise(random.nextLong());
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

    public void generate(double x, double y, TerrainGeneratorLayer output) {
        double terrainFactor = generateTerrainFactorLayer(x, y);
        double terrainHeight = terrainFactor * 64.0 + 224.0;
        double mountainFactor = generateMountainFactorLayer(x, y);
        double mountain, mountainCarve, mountainHeight;
        if (mountainFactor > 0.0) {
            mountain = 1.0 - FastMath.abs(mountainNoise
                    .noiseOctave(x / 512.0, y / 512.0, 2, 4.0, 0.2));
            double mountainCarveFactor = 1.0 - FastMath.sqr(1.0 - mountain);
            mountainCarve = FastMath.abs(mountainCarveNoise
                    .noiseOctave(x / 96.0, y / 96.0, 4, 4.0, 0.2)) *
                    mountainCarveFactor;
            mountainHeight =
                    (mountain - mountainCarve * 0.17) * mountainFactor * 256.0;
        } else {
            mountain = 0.0;
            mountainCarve = 0.0;
            mountainHeight = 0.0;
        }
        double volcanoFactor = generateVolcanoFactorLayer(x, y);
        double volcano, volcanoHeight;
        if (volcanoFactor > 0.0) {
            volcano = FastMath.sqr(FastMath.clamp(volcanoNoise
                    .noiseOctave(x / 512.0, y / 512.0, 4, 8.0, 0.08) * 2.0 -
                    1.0, 0.0, 1.0));
            volcano *= volcano;
            if (volcano > 0.5) {
                volcanoHeight = (1.0 - volcano) * volcanoFactor * 192.0;
            } else {
                volcanoHeight = volcano * volcanoFactor * 192.0;
            }
        } else {
            volcano = 0.0;
            volcanoHeight = 0.0;
        }
        double river = generateRiverLayer(x, y, mountainFactor, 32.0);
        double canyon;
        double canyonFactor = FastMath.sqr(FastMath.clamp(
                canyonDepthNoise.noise(x / 4096.0, y / 4096.0) * 5.0 - 3.0, 0.0,
                1.0));
        if (canyonFactor > 0.0) {
            canyon = FastMath.clamp(FastMath.sqr(1.0 - FastMath.abs(canyonNoise
                    .noiseOctave(x / 1024.0, y / 1024.0, 2, 8.0, 0.1))) *
                    4.0 * canyonFactor - 3.0, 0.0, 1.0);
        } else {
            canyon = 0.0;
        }
        double groundHeight = terrainHeight + mountainHeight + volcanoHeight;
        if (groundHeight > 244.0) {
            groundHeight -= 244.0;
            groundHeight *= river;
            groundHeight += 244.0;
        }
        groundHeight -= canyon * 40.0;
        output.height = groundHeight;
        output.mountain = mountain;
        output.mountainCarve = mountainCarve;
        output.mountainFactor = mountainFactor;
        output.volcano = volcano;
        output.volcanoHeight = volcanoHeight;
        output.volcanoFactor = volcanoFactor;
        output.river = river;
    }

    public void generate(double x, double y, TerrainGeneratorLayer layer,
            TerrainGeneratorOutput output) {
        double cave = FastMath.clamp(FastMath.sqr(1.0 - FastMath.abs(
                caveNoise.noiseOctave(x / 128.0, y / 128.0, 2, 8.0, 0.1))) *
                8.0 - 6.0, 0.0, 1.0);
        double caveHeight =
                128.0 + caveHeightNoise.noise(x / 512.0, y / 512.0) * 96.0;
        double caveRiver = FastMath.clamp(FastMath.sqr(1.0 - FastMath.abs(
                caveRiverNoise
                        .noiseOctave(x / 512.0, y / 512.0, 2, 8.0, 0.1))) *
                8.0 - 7.0, 0.0, 1.0);
        double magmaHeight = 7.0 + magmaNoise.noise(x / 512.0, y / 512.0) * 5.0;
        output.height = layer.height;
        output.mountainFactor = layer.mountainFactor;
        output.volcanoFactor = layer.volcanoFactor;
        output.waterHeight = 256.0;
        output.cave = cave;
        output.caveHeight = caveHeight;
        output.caveRiver = caveRiver;
        output.caveRiverHeight = 128.0;
        output.magmaHeight = magmaHeight;
        output.river = layer.river;
        output.soiled = layer.mountain - layer.mountainCarve * 0.4 <
                1.3 - layer.mountainFactor && layer.volcanoHeight < 18;
        output.beach = layer.height > 250.0 && layer.height <= 259.0;
        output.lavaChance =
                layer.volcano > 0.5 ? 10000 : layer.volcano > 0.2 ? 10000 : 0;
    }

    public double generateTerrainFactorLayer(double x, double y) {
        return terrainNoise.noiseOctave(x / 4096.0, y / 4096.0, 4, 8.0, 0.1d);
    }

    public double generateMountainFactorLayer(double x, double y) {
        return FastMath.sqr((1.0 - FastMath.clamp(FastMath.abs(
                mountainHeightNoise.noise(x / 8192.0, y / 8192.0)) * 3.0 - 0.25,
                0.0, 1.0)) *
                (mountainHeightNoise.noise(x / 4096.0, y / 4096.0) * 0.5 +
                        0.5));
    }

    public double generateVolcanoFactorLayer(double x, double y) {
        return FastMath.sqr(1.0 - FastMath.clamp(FastMath.abs(
                volcanoHeightNoise.noise(x / 16384.0, y / 16384.0)) * 3.0 -
                0.25, 0.0, 1.0));
    }

    public double generateRiverLayer(double x, double y, double mountainFactor,
            double limit) {
        double riverFactor =
                FastMath.clamp(3.0 - mountainFactor * 10.0, 0.0, 1.0);
        if (riverFactor > 0.0) {
            double riverN =
                    riverNoise.noiseOctave(x / 4096.0, y / 4096.0, 4, 4.0, 0.2);
            return FastMath.clamp(limit -
                    FastMath.sqr(1.0 - FastMath.abs(riverN)) * limit *
                            riverFactor, 0.0, 1.0);
        } else {
            return 1.0;
        }
    }

    public static class TerrainGeneratorLayer {
        public double height, mountain, mountainCarve, mountainFactor, volcano,
                volcanoHeight, volcanoFactor, river;
    }

    public static class TerrainGeneratorOutput {
        public double height, mountainFactor, volcanoFactor, waterHeight, cave,
                caveHeight, caveRiver, caveRiverHeight, magmaHeight, river;
        public boolean soiled, beach;
        public int lavaChance;
    }
}
