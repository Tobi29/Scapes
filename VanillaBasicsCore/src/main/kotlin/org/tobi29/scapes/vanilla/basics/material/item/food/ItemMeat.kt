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
import org.tobi29.scapes.entity.server.MobLivingServer
import org.tobi29.scapes.entity.server.MobPlayerServer
import org.tobi29.scapes.entity.server.MobServer
import org.tobi29.scapes.inventory.*
import org.tobi29.scapes.vanilla.basics.entity.server.ComponentMobLivingServerCondition
import org.tobi29.scapes.vanilla.basics.entity.server.access
import org.tobi29.scapes.vanilla.basics.material.ItemDefaultHeatableI
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterialType
import org.tobi29.scapes.vanilla.basics.material.item.VanillaItemBase
import org.tobi29.io.tag.MutableTag
import org.tobi29.io.tag.toInt
import org.tobi29.io.tag.toTag
import org.tobi29.stdex.math.floorToInt

class ItemMeat(
        type: VanillaMaterialType
) : VanillaItemBase<ItemMeat>(type),
        ItemTypeMeatI<ItemMeat>,
        ItemTypeNamedI<ItemMeat>,
        ItemTypeIconKindsI<ItemMeat, MeatType>,
        ItemTypeStackableDefaultI<ItemMeat>,
        ItemTypeUseableI<ItemMeat>,
        ItemDefaultHeatableI<ItemMeat> {
    override fun textureAsset(kind: MeatType) =
            "${kind.texture}/Raw"

    override fun name(item: TypedItem<ItemMeat>): String {
        val name = StringBuilder(40)
        name.append(kind(item).rawName)
        val temperature = temperature(item)
        name.append("\nTemp.:").append(temperature.floorToInt()).append("°C")
        return name.toString()
    }

    override fun maxStackSize(item: TypedItem<ItemMeat>) = 32

    override fun heatTransferFactor(item: TypedItem<ItemMeat>) = 0.001

    override fun temperatureUpdated(item: TypedItem<ItemMeat>): Item? {
        if (temperature(item) >= 60.0) {
            return item.copy(type = materials.cookedMeat)
        }
        return item
    }

    override fun click(entity: MobPlayerServer,
                       item: TypedItem<ItemMeat>): Item? {
        entity.getOrNull(ComponentMobLivingServerCondition.COMPONENT)?.access {
            stamina -= 0.8
            hunger += 0.1
            thirst -= 0.3
        }
        entity.damage(5.0)
        return item.copy(amount = item.amount - 1).orNull()
    }

    override fun click(entity: MobPlayerServer,
                       item: TypedItem<ItemMeat>,
                       hit: MobServer): Pair<Item?, Double?> =
            (if (hit is MobLivingServer) {
                hit.heal(1.0)
                item.copy(item.amount - 1).orNull()
            } else item) to 0.0
}

interface ItemTypeMeatI<I : ItemType> : ItemTypeKindsI<I, MeatType> {
    override val kinds: Set<MeatType>
        get() = MeatType.values().toSet()

    override fun kind(item: TypedItem<I>): MeatType =
            item.metaData["MeatType"]?.toMeatType()
                    ?: MeatType.PORK

    override fun kind(item: TypedItem<I>,
                      value: MeatType): TypedItem<I> =
            item.copy((item.metaData +
                    ("MeatType" to value.id.toTag())).toTag())
}

enum class MeatType(val id: Int,
                    val texture: String,
                    val rawName: String,
                    val cookedName: String = "Cooked $rawName") {
    PORK(0, "$TEXTURE_ROOT/pork", "Pork")
}

private const val TEXTURE_ROOT = "VanillaBasics:image/terrain/food/meat"

fun MutableTag.toMeatType(): MeatType? = when (toInt()) {
    0 -> MeatType.PORK
    else -> null
}
