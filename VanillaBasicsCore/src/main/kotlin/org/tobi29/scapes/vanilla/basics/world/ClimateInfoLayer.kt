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

package org.tobi29.scapes.vanilla.basics.world

import org.tobi29.scapes.chunk.terrain.TerrainRenderInfo
import org.tobi29.scapes.engine.utils.math.mix
import org.tobi29.scapes.vanilla.basics.generator.ClimateGenerator

class ClimateInfoLayer(private val climateGenerator: ClimateGenerator) : TerrainRenderInfo.InfoLayer {
    private var sx = 0
    private var sy = 0
    private var temperature00 = 0.0
    private var temperature10 = 0.0
    private var temperature01 = 0.0
    private var temperature11 = 0.0
    private var humidity00 = 0.0
    private var humidity10 = 0.0
    private var humidity01 = 0.0
    private var humidity11 = 0.0

    override fun init(x: Int,
                      y: Int,
                      z: Int,
                      xSize: Int,
                      ySize: Int,
                      zSize: Int) {
        val xs = xSize - 1
        val ys = ySize - 1
        sx = x
        sy = y
        temperature00 = climateGenerator.temperature(x, y, z)
        temperature10 = climateGenerator.temperature(x + xs, y, z)
        temperature01 = climateGenerator.temperature(x, y + ys, z)
        temperature11 = climateGenerator.temperature(x + xs, y + ys, z)
        humidity00 = climateGenerator.humidity(x, y, z)
        humidity10 = climateGenerator.humidity(x + xs, y, z)
        humidity01 = climateGenerator.humidity(x, y + ys, z)
        humidity11 = climateGenerator.humidity(x + xs, y + ys, z)
    }

    fun temperature(x: Int,
                    y: Int,
                    z: Int): Double {
        val xx = x - sx
        val yy = y - sy
        val mixX = xx / 15.0
        val mixY = yy / 15.0
        val temperature0 = mix(temperature00, temperature01, mixY)
        val temperature1 = mix(temperature10, temperature11, mixY)
        return climateGenerator.temperatureD(
                mix(temperature0, temperature1, mixX), z)
    }

    fun humidity(x: Int,
                 y: Int): Double {
        val xx = x - sx
        val yy = y - sy
        val mixX = xx / 15.0
        val mixY = yy / 15.0
        val humidity0 = mix(humidity00, humidity01, mixY)
        val humidity1 = mix(humidity10, humidity11, mixY)
        return mix(humidity0, humidity1, mixX)
    }
}
