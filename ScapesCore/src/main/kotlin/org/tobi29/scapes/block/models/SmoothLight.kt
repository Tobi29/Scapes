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
package org.tobi29.scapes.block.models

import org.tobi29.scapes.chunk.terrain.Terrain
import org.tobi29.scapes.engine.utils.math.Face
import org.tobi29.scapes.engine.utils.math.clamp
import org.tobi29.scapes.engine.utils.math.round

object SmoothLight {
    private val SIDES = intArrayOf(0, 4, 2, 6, 1, 5, 3, 7, 2, 6, 3, 7, 0, 2, 1,
            3, 0, 4, 1, 5, 4, 6, 5, 7, 0, 4, 2, 6, 1, 5, 3, 7)

    fun calcLight(triple: FloatTriple,
                  side: Face,
                  x: Int,
                  y: Int,
                  z: Int,
                  terrain: Terrain) {
        var light = 0
        var lights = 0
        var sunLight = 0
        var sunLights = 0
        var ssaoLights = 0
        var i: Int
        val limit: Int
        if (side == Face.NONE) {
            i = 24
            limit = 30
        } else {
            i = side.data.toInt() shl 2
            limit = i + 4
        }
        while (i < limit) {
            val offset = SIDES[i]
            val xx = x - (offset shr 2 and 1)
            val yy = y - (offset shr 1 and 1)
            val zz = z - (offset and 1)
            val block = terrain.block(xx, yy, zz)
            val type = terrain.type(block)
            val data = terrain.data(block)
            if (!type.isSolid(data)) {
                var tempLight = terrain.blockLight(xx, yy, zz).toFloat()
                if (tempLight > 0) {
                    light += tempLight.toInt()
                    lights++
                }
                tempLight = terrain.sunLight(xx, yy, zz).toFloat()
                if (tempLight > 0) {
                    sunLight += tempLight.toInt()
                    sunLights++
                }
                ssaoLights++
            }
            i++
        }
        if (lights == 0) {
            triple.a = 0.0f
        } else {
            triple.a = light.toFloat() / lights.toFloat() / 15.0f
        }
        if (sunLights == 0) {
            triple.b = 0.0f
        } else {
            triple.b = sunLight.toFloat() / sunLights.toFloat() / 15.0f
        }
        triple.c = clamp(0.3f * ssaoLights - 0.2f, 0.0f, 1.0f)
    }

    fun calcLight(triple: FloatTriple,
                  side: Face,
                  x: Double,
                  y: Double,
                  z: Double,
                  terrain: Terrain) {
        calcLight(triple, side, round(x), round(y),
                round(z), terrain)
    }

    class FloatTriple {
        var a = 0.0f
        var b = 0.0f
        var c = 0.0f
    }
}
