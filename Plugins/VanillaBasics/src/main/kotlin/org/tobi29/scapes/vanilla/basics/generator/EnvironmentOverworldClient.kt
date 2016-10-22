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

package org.tobi29.scapes.vanilla.basics.generator

import org.tobi29.scapes.chunk.EnvironmentClient
import org.tobi29.scapes.chunk.WorldClient
import org.tobi29.scapes.chunk.WorldSkybox
import org.tobi29.scapes.engine.utils.io.tag.TagStructure
import org.tobi29.scapes.engine.utils.io.tag.getDouble
import org.tobi29.scapes.engine.utils.io.tag.getLong
import org.tobi29.scapes.engine.utils.math.HALF_PI
import org.tobi29.scapes.engine.utils.math.clamp
import org.tobi29.scapes.engine.utils.math.cosTable
import org.tobi29.scapes.engine.utils.math.sinTable
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.engine.utils.math.vector.normalizeSafe
import org.tobi29.scapes.engine.utils.math.vector.times
import java.util.*

class EnvironmentOverworldClient(world: WorldClient) : EnvironmentClient, EnvironmentClimate {
    private val climateGenerator: ClimateGenerator
    private val biomeGenerator: BiomeGenerator

    init {
        val random = Random(world.seed)
        val terrainGenerator = TerrainGenerator(random)
        climateGenerator = ClimateGenerator(random, terrainGenerator)
        biomeGenerator = BiomeGenerator(climateGenerator, terrainGenerator)
    }

    override fun climate(): ClimateGenerator {
        return climateGenerator
    }

    override fun load(tagStructure: TagStructure) {
        tagStructure.getDouble("DayTime")?.let {
            climateGenerator.setDayTime(it)
        }
        tagStructure.getLong("Day")?.let { climateGenerator.setDay(it) }
    }

    override fun tick(delta: Double) {
        climateGenerator.add(0.000277777777778 * delta)
    }

    override fun sunLightReduction(x: Double,
                                   y: Double): Float {
        return climateGenerator.sunLightReduction(x, y).toFloat()
    }

    override fun sunLightNormal(x: Double,
                                y: Double): Vector3d {
        val latitude = climateGenerator.latitude(y)
        val elevation = climateGenerator.sunElevationD(latitude)
        var azimuth = climateGenerator.sunAzimuthD(elevation, latitude)
        azimuth += HALF_PI
        val rz = sinTable(elevation)
        val rd = cosTable(elevation)
        val rx = cosTable(azimuth) * rd
        val ry = sinTable(azimuth) * rd
        val mix = clamp(elevation * 100.0, -1.0, 1.0)
        return Vector3d(rx, ry, rz).normalizeSafe().times(mix)
    }

    override fun createSkybox(world: WorldClient): WorldSkybox {
        return WorldSkyboxOverworld(climateGenerator, biomeGenerator,
                world)
    }
}
