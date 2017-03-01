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

package org.tobi29.scapes.vanilla.basics.material.item.food

import org.tobi29.scapes.block.GameRegistry
import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.engine.utils.math.floor
import org.tobi29.scapes.vanilla.basics.material.CropType
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial
import org.tobi29.scapes.vanilla.basics.material.ItemDefaultHeatable
import org.tobi29.scapes.vanilla.basics.material.ItemResearch
import org.tobi29.scapes.vanilla.basics.material.item.ItemSimpleData

class ItemDough(materials: VanillaMaterial,
                private val cropRegistry: GameRegistry.Registry<CropType>) : ItemSimpleData(
        materials,
        "vanilla.basics.item.Dough"), ItemDefaultHeatable, ItemResearch {

    override fun types(): Int {
        return cropRegistry.values().size
    }

    override fun texture(data: Int): String {
        return cropRegistry[data].texture() + "/Dough.png"
    }

    override fun name(item: ItemStack): String {
        val name = StringBuilder(40)
        name.append(materials.crop.name(item)).append("Dough")
        val temperature = temperature(item)
        name.append("\nTemp.:").append(floor(temperature)).append("°C")
        return name.toString()
    }

    override fun maxStackSize(item: ItemStack): Int {
        return 8
    }

    override fun heatTransferFactor(item: ItemStack) = 0.001

    override fun temperatureUpdated(item: ItemStack) {
        if (temperature(item) >= 40.0) {
            item.setMaterial(materials.baked)
        }
    }

    override fun identifiers(item: ItemStack): Array<String> {
        return arrayOf("vanilla.basics.item.Dough",
                "vanilla.basics.item.Dough." + materials.crop.name(item))
    }
}
