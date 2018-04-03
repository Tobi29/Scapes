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

import org.tobi29.scapes.block.ItemTypeIconKindsI
import org.tobi29.scapes.block.ItemTypeKindsRegistryI
import org.tobi29.scapes.inventory.ItemTypeNamedI
import org.tobi29.scapes.inventory.ItemTypeStackableDefaultI
import org.tobi29.scapes.inventory.TypedItem
import org.tobi29.scapes.vanilla.basics.material.CropType
import org.tobi29.scapes.vanilla.basics.material.ItemResearchI
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterialType
import org.tobi29.scapes.vanilla.basics.material.item.VanillaItemBase

class ItemCropDrop(
        type: VanillaMaterialType
) : VanillaItemBase<ItemCropDrop>(type),
        ItemTypeKindsRegistryI<ItemCropDrop, CropType>,
        ItemTypeNamedI<ItemCropDrop>,
        ItemTypeIconKindsI<ItemCropDrop, CropType>,
        ItemTypeStackableDefaultI<ItemCropDrop>,
        ItemResearchI<ItemCropDrop> {
    override val registry =
            plugins.registry.get<CropType>("VanillaBasics", kindTag)
    override val kindTag get() = "CropType"

    override fun textureAsset(kind: CropType) =
            "${kind.texture}/Drop"

    override fun name(item: TypedItem<ItemCropDrop>) = kind(item).name

    override fun maxStackSize(item: TypedItem<ItemCropDrop>) = 16

    override fun identifiers(item: TypedItem<ItemCropDrop>): Array<String> {
        return arrayOf("vanilla.basics.item.Drop",
                "vanilla.basics.item.Drop." + kind(item).name)
    }
}
