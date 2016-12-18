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

package scapes.plugin.tobi29.vanilla.basics.material.item.vegetation

import org.tobi29.scapes.block.ItemStack
import scapes.plugin.tobi29.vanilla.basics.material.VanillaMaterial
import scapes.plugin.tobi29.vanilla.basics.material.item.ItemFuel
import scapes.plugin.tobi29.vanilla.basics.material.item.ItemSimpleData

class ItemGrassBundle(materials: VanillaMaterial) : ItemSimpleData(materials,
        "vanilla.basics.item.GrassBundle"), ItemFuel {
    override fun types(): Int {
        return 2
    }

    override fun texture(data: Int): String {
        when (data) {
            0 -> return "VanillaBasics:image/terrain/other/GrassBundle.png"
            1 -> return "VanillaBasics:image/terrain/other/Straw.png"
            else -> throw IllegalArgumentException("Unknown data: {}" + data)
        }
    }

    override fun name(item: ItemStack): String {
        when (item.data()) {
            1 -> return "Straw"
            else -> return "Grass Bundle"
        }
    }

    override fun maxStackSize(item: ItemStack): Int {
        return 128
    }

    override fun fuelTemperature(item: ItemStack): Float {
        return 0.06f
    }

    override fun fuelTime(item: ItemStack): Float {
        return 1600.0f
    }

    override fun fuelTier(item: ItemStack): Int {
        return 0
    }
}
