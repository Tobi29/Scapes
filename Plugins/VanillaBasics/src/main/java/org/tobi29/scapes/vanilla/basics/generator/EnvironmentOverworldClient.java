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

import org.tobi29.scapes.chunk.EnvironmentClient;
import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.chunk.WorldSkybox;
import org.tobi29.scapes.engine.opengl.GL;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;

import java.util.Random;

public class EnvironmentOverworldClient
        implements EnvironmentClient, EnvironmentClimate {
    private final ClimateGenerator climateGenerator;
    private final BiomeGenerator biomeGenerator;

    public EnvironmentOverworldClient(WorldClient world) {
        Random random = new Random(world.seed());
        TerrainGenerator terrainGenerator = new TerrainGenerator(random);
        climateGenerator = new ClimateGenerator(random, terrainGenerator);
        biomeGenerator = new BiomeGenerator(climateGenerator);
    }

    @Override
    public ClimateGenerator climate() {
        return climateGenerator;
    }

    @Override
    public void load(TagStructure tagStructure) {
        if (tagStructure.has("DayTime")) {
            climateGenerator.setDayTime(tagStructure.getDouble("DayTime"));
        } else {
            climateGenerator.setDayTime(0.1);
        }
        if (tagStructure.has("Day")) {
            climateGenerator.setDay(tagStructure.getLong("Day"));
        } else {
            climateGenerator.setDay(4);
        }
    }

    @Override
    public void tick(double delta) {
        climateGenerator.add(0.000277777777778 * delta);
    }

    @Override
    public float sunLightReduction(double x, double y) {
        return (float) climateGenerator.sunLightReduction(x, y);
    }

    @Override
    public Vector3 sunLightNormal(double x, double y) {
        double latitude = climateGenerator.latitude(y);
        double elevation = climateGenerator.sunElevationD(latitude);
        double azimuth = climateGenerator.sunAzimuthD(elevation, latitude);
        azimuth += FastMath.HALF_PI;
        double rz = FastMath.sinTable(elevation);
        double rd = FastMath.cosTable(elevation);
        double rx = FastMath.cosTable(azimuth) * rd;
        double ry = FastMath.sinTable(azimuth) * rd;
        double mix = FastMath.clamp(elevation * 100.0, -1.0, 1.0);
        return FastMath.normalizeSafe(new Vector3d(rx, ry, rz)).multiply(mix);
    }

    @Override
    public WorldSkybox createSkybox(WorldClient world, GL gl) {
        return new WorldSkyboxOverworld(climateGenerator, biomeGenerator, world,
                gl);
    }
}
