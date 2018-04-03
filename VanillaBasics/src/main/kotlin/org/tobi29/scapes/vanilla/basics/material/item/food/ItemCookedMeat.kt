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
import org.tobi29.scapes.block.ItemTypeUseableI
import org.tobi29.scapes.block.copy
import org.tobi29.stdex.math.floorToInt
import org.tobi29.scapes.entity.server.MobLivingServer
import org.tobi29.scapes.entity.server.MobPlayerServer
import org.tobi29.scapes.entity.server.MobServer
import org.tobi29.scapes.inventory.*
import org.tobi29.scapes.vanilla.basics.entity.server.ComponentMobLivingServerCondition
import org.tobi29.scapes.vanilla.basics.entity.server.access
import org.tobi29.scapes.vanilla.basics.material.ItemDefaultHeatableI
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterialType
import org.tobi29.scapes.vanilla.basics.material.item.VanillaItemBase

class ItemCookedMeat(
        type: VanillaMaterialType
) : VanillaItemBase<ItemCookedMeat>(type),
        ItemTypeMeatI<ItemCookedMeat>,
        ItemTypeNamedI<ItemCookedMeat>,
        ItemTypeIconKindsI<ItemCookedMeat, MeatType>,
        ItemTypeStackableDefaultI<ItemCookedMeat>,
        ItemTypeUseableI<ItemCookedMeat>,
        ItemDefaultHeatableI<ItemCookedMeat> {
    override fun textureAsset(kind: MeatType) =
            "${kind.texture}/Raw"

    override fun name(item: TypedItem<ItemCookedMeat>): String {
        val name = StringBuilder(40)
        name.append(kind(item).cookedName)
        val temperature = temperature(item)
        name.append("\nTemp.:").append(temperature.floorToInt()).append("°C")
        return name.toString()
    }

    override fun maxStackSize(item: TypedItem<ItemCookedMeat>) = 32

    override fun heatTransferFactor(item: TypedItem<ItemCookedMeat>) = 0.001

    override fun temperatureUpdated(item: TypedItem<ItemCookedMeat>): Item? {
        if (temperature(item) >= 120.0) {
            return null
        }
        return item
    }

    override fun click(entity: MobPlayerServer,
                       item: TypedItem<ItemCookedMeat>): Item? {
        entity.getOrNull(ComponentMobLivingServerCondition.COMPONENT)?.access {
            if (temperature(item) >= 30) {
                stamina -= 0.04
                hunger += 0.4
                thirst -= 0.1
            } else {
                stamina -= 0.4
                hunger += 0.2
                thirst -= 0.2
            }
        }
        return item.copy(amount = item.amount - 1).orNull()
    }

    override fun click(entity: MobPlayerServer,
                       item: TypedItem<ItemCookedMeat>,
                       hit: MobServer): Pair<Item?, Double?> =
            (if (hit is MobLivingServer) {
                if (temperature(item) >= 120) hit.heal(10.0)
                else hit.heal(1.0)
                item.copy(item.amount - 1).orNull()
            } else item) to 0.0
}
