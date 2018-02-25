/*
 * Copyright 2012-2018 Tobi29
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

import org.tobi29.arrays.DoubleArray2
import org.tobi29.generation.value.OpenSimplexNoise
import org.tobi29.graphics.Image
import org.tobi29.graphics.MutableImage
import org.tobi29.graphics.toImage
import org.tobi29.math.Random
import org.tobi29.math.vector.Vector3d
import org.tobi29.math.vector.cross
import org.tobi29.math.vector.normalizedSafe
import org.tobi29.stdex.combineToInt
import org.tobi29.stdex.math.clamp
import org.tobi29.stdex.math.remP
import kotlin.math.roundToInt

class CloudGenerator(val densityNoise: OpenSimplexNoise) {
    constructor(seed: Long) : this(Random(seed))
    constructor(random: Random) : this(OpenSimplexNoise(random.nextLong()))

    fun generate(
        weather: Double,
        time: Double,
        x: Double,
        y: Double,
        size: Int
    ): Image {
        val t = time * 3.0
        val density = DoubleArray2(size + 2, size + 2)
        for (yy in 0 until size + 2) {
            for (xx in 0 until size + 2) {
                val xxx = (x + ((xx - 2).toDouble() / (size - 2))) * 1024.0
                val yyy = (y + ((yy - 2).toDouble() / (size - 2))) * 1024.0
                density[xx, yy] = noiseOctave(
                    xxx, yyy, 6, 0.3, 2.4
                ) { xxxx, yyyy -> densityNoise.noise(xxxx, yyyy, t) } *
                        3.0 - 2.5 + weather * 6.5
            }
        }

        val image = MutableImage(size, size)
        for (yy in 0 until size) {
            for (xx in 0 until size) {
                val vecX = Vector3d(0.025, 0.0, density.gradX(xx + 1, yy + 1))
                val vecY = Vector3d(0.0, 0.025, density.gradY(xx + 1, yy + 1))
                val normal = (vecX cross vecY).normalizedSafe()
                val r = (normal.x * 0.5 + 0.5).toPixel()
                val g = (normal.y * 0.5 + 0.5).toPixel()
                val b = (normal.z * 0.5 + 0.5).toPixel()
                val a = (density[xx + 1, yy + 1] * 0.5).toPixel()
                image[xx, yy] = combineToInt(r, g, b, a)
            }
        }
        return image.toImage()
    }
}

private fun Double.toPixel(): Byte =
    clamp((this * 255.0).roundToInt(), 0, 255).toByte()

private fun DoubleArray2.gradX(
    x: Int,
    y: Int
): Double =
    (-this[(x - 1) remP width, y] + this[(x + 1) remP width, y]) * 0.5

private fun DoubleArray2.gradY(
    x: Int,
    y: Int
): Double =
    (-this[x, (y - 1) remP height] + this[x, (y + 1) remP height]) * 0.5

inline fun noiseOctave(
    x: Double,
    y: Double,
    octaves: Int,
    frequency: Double,
    amplitude: Double,
    offsetX: Double = 1.0,
    offsetY: Double = offsetX,
    noise: (Double, Double) -> Double
): Double {
    var out = 0.0
    var cFrequency = 1.0
    var cAmplitude = 1.0
    var normal = 0.0
    var i = 0
    while (i < octaves) {
        out += noise(
            x * cFrequency + offsetX * i,
            y * cFrequency + offsetY * i
        ) * cAmplitude
        normal += cAmplitude
        i++
        cFrequency *= frequency
        cAmplitude *= amplitude
    }
    return out / normal
}
