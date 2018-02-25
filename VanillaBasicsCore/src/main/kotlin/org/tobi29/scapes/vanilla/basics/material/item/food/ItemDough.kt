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

import org.tobi29.scapes.block.ItemTypeIconKindsI
import org.tobi29.scapes.block.ItemTypeKindsRegistryI
import org.tobi29.stdex.math.floorToInt
import org.tobi29.scapes.inventory.Item
import org.tobi29.scapes.inventory.ItemTypeNamedI
import org.tobi29.scapes.inventory.ItemTypeStackableDefaultI
import org.tobi29.scapes.inventory.TypedItem
import org.tobi29.scapes.vanilla.basics.material.CropType
import org.tobi29.scapes.vanilla.basics.material.ItemDefaultHeatableI
import org.tobi29.scapes.vanilla.basics.material.ItemResearchI
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterialType
import org.tobi29.scapes.vanilla.basics.material.item.VanillaItemBase

class ItemDough(
        type: VanillaMaterialType
) : VanillaItemBase<ItemDough>(type),
        ItemTypeKindsRegistryI<ItemDough, CropType>,
        ItemTypeNamedI<ItemDough>,
        ItemTypeIconKindsI<ItemDough, CropType>,
        ItemTypeStackableDefaultI<ItemDough>,
        ItemDefaultHeatableI<ItemDough>,
        ItemResearchI<ItemDough> {
    override val registry =
            plugins.registry.get<CropType>("VanillaBasics", kindTag)
    override val kindTag get() = "CropType"

    override fun textureAsset(kind: CropType) =
            "${kind.texture}/Dough"

    override fun name(item: TypedItem<ItemDough>): String {
        val name = StringBuilder(40)
        name.append(kind(item).name).append("Dough")
        val temperature = temperature(item)
        name.append("\nTemp.:").append(temperature.floorToInt()).append("°C")
        return name.toString()
    }

    override fun maxStackSize(item: TypedItem<ItemDough>) = 8

    override fun heatTransferFactor(item: TypedItem<ItemDough>) = 0.001

    override fun temperatureUpdated(item: TypedItem<ItemDough>): Item? {
        if (temperature(item) >= 40.0) {
            return item.copy(type = materials.baked)
        }
        return item
    }

    override fun identifiers(item: TypedItem<ItemDough>): Array<String> {
        return arrayOf("vanilla.basics.item.Dough",
                "vanilla.basics.item.Dough." + kind(item).name)
    }
}
