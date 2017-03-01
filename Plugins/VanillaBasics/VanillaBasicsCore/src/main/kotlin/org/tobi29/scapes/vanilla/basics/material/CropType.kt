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

package org.tobi29.scapes.vanilla.basics.material

import org.tobi29.scapes.block.GameRegistry

class CropType(private val name: String,
               private val bakedName: String,
               textureRoot: String,
               private val time: Double,
               private val nutrient: Int) {
    private val texture: String

    init {
        texture = "$textureRoot/${name.replace(" ", "").toLowerCase()}"
    }

    fun name(): String {
        return name
    }

    fun bakedName(): String {
        return bakedName
    }

    fun texture(): String {
        return texture
    }

    fun time(): Double {
        return time
    }

    fun nutrient(): Int {
        return nutrient
    }

    fun data(registry: GameRegistry): Int {
        return registry.get<Any>("VanillaBasics", "CropType")[this]
    }

    companion object {
        private val ROOT = "VanillaBasics:image/terrain/crops"
        val WHEAT = CropType("Wheat", "Bread", ROOT, 4000.0, 0)

        operator fun get(registry: GameRegistry,
                         data: Int): CropType {
            return registry.get<CropType>("VanillaBasics", "CropType")[data]
        }

        operator fun get(registry: GameRegistry,
                         data: Int?): CropType? {
            if (data == null) {
                return null
            }
            return registry.get<CropType>("VanillaBasics", "CropType")[data]
        }
    }
}
