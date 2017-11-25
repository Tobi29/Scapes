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

package org.tobi29.scapes.vanilla.basics.world.decorator

import org.tobi29.scapes.chunk.terrain.TerrainServer
import org.tobi29.scapes.engine.math.Random
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial

class BiomeDecorator {
    private val layers = ArrayList<Layer>()
    private var weight = 0
    private var weightCount = 0

    fun addWeight(weight: Int) {
        this.weight += weight
        weightCount++
    }

    fun addLayer(layer: Layer) {
        layers.add(layer)
    }

    fun weight(): Int {
        if (weightCount == 0) {
            return 0
        }
        return weight / weightCount
    }

    fun decorate(terrain: TerrainServer,
                 x: Int,
                 y: Int,
                 materials: VanillaMaterial,
                 random: Random) {
        layers.forEach { it.decorate(terrain, x, y, materials, random) }
    }

    interface Layer {
        fun decorate(terrain: TerrainServer,
                     x: Int,
                     y: Int,
                     materials: VanillaMaterial,
                     random: Random)
    }
}
