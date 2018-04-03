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
import org.tobi29.scapes.block.ItemTypeUseableI
import org.tobi29.scapes.entity.server.MobPlayerServer
import org.tobi29.scapes.inventory.*
import org.tobi29.scapes.vanilla.basics.entity.server.ComponentMobLivingServerCondition
import org.tobi29.scapes.vanilla.basics.entity.server.access
import org.tobi29.scapes.vanilla.basics.material.CropType
import org.tobi29.scapes.vanilla.basics.material.ItemResearchI
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterialType
import org.tobi29.scapes.vanilla.basics.material.item.VanillaItemBase

class ItemBaked(
        type: VanillaMaterialType
) : VanillaItemBase<ItemBaked>(type),
        ItemTypeKindsRegistryI<ItemBaked, CropType>,
        ItemTypeNamedI<ItemBaked>,
        ItemTypeIconKindsI<ItemBaked, CropType>,
        ItemTypeStackableDefaultI<ItemBaked>,
        ItemTypeUseableI<ItemBaked>,
        ItemResearchI<ItemBaked> {
    override val registry =
            plugins.registry.get<CropType>("VanillaBasics", kindTag)
    override val kindTag get() = "CropType"

    override fun textureAsset(kind: CropType) =
            "${kind.texture}/Baked"

    override fun name(item: TypedItem<ItemBaked>) = kind(item).bakedName

    override fun maxStackSize(item: TypedItem<ItemBaked>) = 16

    override fun click(entity: MobPlayerServer,
                       item: TypedItem<ItemBaked>): Item? {
        entity.getOrNull(ComponentMobLivingServerCondition.COMPONENT)?.access {
            stamina -= 0.1
            hunger += 0.1
            thirst -= 0.1
        }
        entity.damage(5.0)
        return item.copy(amount = item.amount - 1).orNull()
    }

    override fun identifiers(item: TypedItem<ItemBaked>): Array<String> {
        return arrayOf("vanilla.basics.item.Baked",
                "vanilla.basics.item.Baked." + kind(item).name)
    }
}
