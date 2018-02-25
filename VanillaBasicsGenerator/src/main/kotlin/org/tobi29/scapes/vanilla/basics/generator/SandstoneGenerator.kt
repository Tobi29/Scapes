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

package org.tobi29.scapes.vanilla.basics.generator

import org.tobi29.generation.layer.RandomPermutation
import org.tobi29.generation.layer.random
import org.tobi29.generation.layer.randomOffset
import org.tobi29.generation.value.OpenSimplexNoise
import org.tobi29.math.Random
import org.tobi29.stdex.math.floorToInt

class SandstoneGenerator(random: Random) {
    private val base = RandomPermutation(random)
    private val swirl = OpenSimplexNoise(random.nextLong())
    private val noise = RandomPermutation(random)

    fun sandstone(x: Int,
                  y: Int) = run {
        noise.randomOffset(3, x, y) { x, y ->
            swirl.randomOffset(1024.0, x.toDouble(),
                    y.toDouble()) { x, y ->
                base.random(4, x.floorToInt() shr 11,
                        y.floorToInt() shr 11) == 0
            }
        }
    }
}
