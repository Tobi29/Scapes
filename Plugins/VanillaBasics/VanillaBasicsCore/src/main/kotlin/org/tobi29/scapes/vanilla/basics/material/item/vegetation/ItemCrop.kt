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

package org.tobi29.scapes.vanilla.basics.material.item.vegetation

import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.block.Registries
import org.tobi29.scapes.vanilla.basics.material.CropType
import org.tobi29.scapes.vanilla.basics.material.ItemResearch
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial
import org.tobi29.scapes.vanilla.basics.material.item.ItemSimpleData

class ItemCrop(materials: VanillaMaterial,
               private val cropRegistry: Registries.Registry<CropType>) : ItemSimpleData(
        materials, "vanilla.basics.item.Crop"), ItemResearch {
    override fun types(): Int {
        return cropRegistry.values().size
    }

    override fun texture(data: Int): String {
        return "${cropRegistry[data].texture}/Drop.png"
    }

    override fun name(item: ItemStack): String {
        return materials.crop.name(item)
    }

    override fun maxStackSize(item: ItemStack): Int {
        return 16
    }

    override fun identifiers(item: ItemStack): Array<String> {
        return arrayOf("vanilla.basics.item.Crop",
                "vanilla.basics.item.Crop." + materials.crop.name(item))
    }
}
