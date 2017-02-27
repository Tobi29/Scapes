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

class StoneType(private val name: String,
                private val textureRoot: String,
                private val resistance: Double) {
    private val texture: String

    init {
        texture = name.replace(" ", "")
    }

    fun name(): String {
        return name
    }

    fun texture(): String {
        return texture
    }

    fun textureRoot(): String {
        return textureRoot
    }

    fun resistance(): Double {
        return resistance
    }

    fun data(registry: GameRegistry): Int {
        return registry.get<Any>("VanillaBasics", "StoneType")[this]
    }

    companion object {
        private val ROOT = "VanillaBasics:image/terrain/stone"
        val DIRT_STONE = StoneType("Dirt Stone", ROOT, 0.1)
        val FLINT = StoneType("Flint", ROOT, 2.1)
        val CHALK = StoneType("Chalk", ROOT, 0.4)
        val CHERT = StoneType("Chert", ROOT, 0.4)
        val CLAYSTONE = StoneType("Claystone", ROOT, 0.4)
        val CONGLOMERATE = StoneType("Conglomerate", ROOT, 0.4)
        val MARBLE = StoneType("Marble", ROOT, 0.6)
        val ANDESITE = StoneType("Andesite", ROOT, 1.4)
        val BASALT = StoneType("Basalt", ROOT, 1.4)
        val DACITE = StoneType("Dacite", ROOT, 1.4)
        val RHYOLITE = StoneType("Rhyolite", ROOT, 1.4)
        val DIORITE = StoneType("Diorite", ROOT, 1.2)
        val GABBRO = StoneType("Gabbro", ROOT, 1.3)
        val GRANITE = StoneType("Granite", ROOT, 1.5)

        operator fun get(registry: GameRegistry,
                         data: Int): StoneType {
            return registry.get<StoneType>("VanillaBasics", "StoneType")[data]
        }
    }
}
