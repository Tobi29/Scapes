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

import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.engine.utils.io.tag.set
import org.tobi29.scapes.engine.utils.io.tag.syncMapMut
import org.tobi29.scapes.engine.utils.io.tag.toDouble
import org.tobi29.scapes.engine.utils.math.floor
import org.tobi29.scapes.entity.server.MobLivingServer
import org.tobi29.scapes.entity.server.MobPlayerServer
import org.tobi29.scapes.entity.server.MobServer
import org.tobi29.scapes.vanilla.basics.material.ItemDefaultHeatable
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterialType
import org.tobi29.scapes.vanilla.basics.material.item.ItemSimpleData

class ItemCookedMeat(type: VanillaMaterialType) : ItemSimpleData(
        type), ItemDefaultHeatable {
    override fun click(entity: MobPlayerServer,
                       item: ItemStack) {
        entity.metaData("Vanilla").syncMapMut("Condition") { conditionTag ->
            val stamina = conditionTag["Stamina"]?.toDouble() ?: 0.0
            val hunger = conditionTag["Hunger"]?.toDouble() ?: 0.0
            val thirst = conditionTag["Thirst"]?.toDouble() ?: 0.0
            if (temperature(item) >= 30) {
                conditionTag["Stamina"] = stamina - 0.04
                conditionTag["Hunger"] = hunger + 0.4
                conditionTag["Thirst"] = thirst - 0.1
            } else {
                conditionTag["Stamina"] = stamina - 0.4
                conditionTag["Hunger"] = hunger + 0.2
                conditionTag["Thirst"] = thirst - 0.2
            }
        }
        item.setAmount(item.amount() - 1)
    }

    override fun click(entity: MobPlayerServer,
                       item: ItemStack,
                       hit: MobServer): Double {
        if (temperature(item) >= 120) {
            if (hit is MobLivingServer) {
                hit.heal(10.0)
                item.setAmount(item.amount() - 1)
            }
            return 0.0
        } else {
            return materials.meat.click(entity, item, hit)
        }
    }

    override fun types(): Int {
        return 1
    }

    override fun texture(data: Int): String {
        when (data) {
            0 -> return "VanillaBasics:image/terrain/food/meat/pork/Cooked.png"
            else -> throw IllegalArgumentException("Unknown data: {}" + data)
        }
    }

    override fun name(item: ItemStack): String {
        val name = StringBuilder(40)
        when (item.data()) {
            else -> name.append("Cooked Porkchop")
        }
        val temperature = temperature(item)
        if (temperature > 0.1f) {
            name.append("\nTemp.:").append(floor(temperature)).append(
                    "°C")
        }
        return name.toString()
    }

    override fun maxStackSize(item: ItemStack): Int {
        return 1
    }

    override fun heatTransferFactor(item: ItemStack) = 0.001

    override fun temperatureUpdated(item: ItemStack) {
        if (temperature(item) >= 120.0) {
            item.setAmount(0)
        }
    }
}
