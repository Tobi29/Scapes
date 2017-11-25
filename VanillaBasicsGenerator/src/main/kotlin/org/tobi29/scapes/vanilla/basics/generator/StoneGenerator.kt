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

import org.tobi29.scapes.engine.math.Random
import org.tobi29.scapes.engine.utils.generation.layer.RandomPermutation
import org.tobi29.scapes.engine.utils.generation.layer.random
import org.tobi29.scapes.engine.utils.generation.layer.randomOffset
import org.tobi29.scapes.engine.utils.generation.value.SimplexNoise
import org.tobi29.scapes.engine.utils.math.floorToInt
import org.tobi29.scapes.engine.utils.toArray

class StoneGenerator(random: Random,
                     stoneLayers: Sequence<Sequence<StoneType>>) {
    private val stoneLayers = stoneLayers.map { it.toArray() }.map {
        Layer(random, it)
    }.toArray()

    fun stoneType(x: Int,
                  y: Int,
                  i: Int) = stoneLayers[i].stoneType(x, y)

    class Layer(random: Random,
                private val types: Array<StoneType>) {
        private val base = RandomPermutation(random)
        private val swirl = SimplexNoise(random.nextLong())
        private val noise = RandomPermutation(random)

        fun stoneType(x: Int,
                      y: Int) = run {
            noise.randomOffset(2, x, y) { x, y ->
                swirl.randomOffset(1024.0, x.toDouble(),
                        y.toDouble()) { x, y ->
                    types[base.random(types.size, x.floorToInt() shr 11,
                            y.floorToInt() shr 11)]
                }
            }
        }
    }
}
