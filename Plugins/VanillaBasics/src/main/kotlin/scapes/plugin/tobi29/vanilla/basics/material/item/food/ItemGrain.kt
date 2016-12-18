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

package scapes.plugin.tobi29.vanilla.basics.material.item.food

import org.tobi29.scapes.block.GameRegistry
import org.tobi29.scapes.block.ItemStack
import scapes.plugin.tobi29.vanilla.basics.material.CropType
import scapes.plugin.tobi29.vanilla.basics.material.VanillaMaterial
import scapes.plugin.tobi29.vanilla.basics.material.item.ItemResearch
import scapes.plugin.tobi29.vanilla.basics.material.item.ItemSimpleData

class ItemGrain(materials: VanillaMaterial,
                private val cropRegistry: GameRegistry.Registry<CropType>) : ItemSimpleData(
        materials, "vanilla.basics.item.Grain"), ItemResearch {

    override fun types(): Int {
        return cropRegistry.values().size
    }

    override fun texture(data: Int): String {
        return cropRegistry[data].texture() + "/Grain.png"
    }

    override fun name(item: ItemStack): String {
        return materials.cropDrop.name(item)
    }

    override fun maxStackSize(item: ItemStack): Int {
        return 64
    }

    override fun identifiers(item: ItemStack): Array<String> {
        return arrayOf("vanilla.basics.item.Grain",
                "vanilla.basics.item.Grain." + materials.crop.name(item))
    }
}
