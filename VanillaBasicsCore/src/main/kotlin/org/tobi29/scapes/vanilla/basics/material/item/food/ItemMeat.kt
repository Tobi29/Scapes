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
import org.tobi29.scapes.engine.utils.tag.set
import org.tobi29.scapes.engine.utils.tag.syncMapMut
import org.tobi29.scapes.engine.utils.tag.toDouble
import org.tobi29.scapes.engine.utils.math.floor
import org.tobi29.scapes.entity.server.MobLivingServer
import org.tobi29.scapes.entity.server.MobPlayerServer
import org.tobi29.scapes.entity.server.MobServer
import org.tobi29.scapes.vanilla.basics.material.ItemDefaultHeatable
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterialType
import org.tobi29.scapes.vanilla.basics.material.item.ItemSimpleData

class ItemMeat(type: VanillaMaterialType) : ItemSimpleData(
        type), ItemDefaultHeatable {
    override fun click(entity: MobPlayerServer,
                       item: ItemStack) {
        entity.metaData("Vanilla").syncMapMut("Condition") { conditionTag ->
            val stamina = conditionTag["Stamina"]?.toDouble() ?: 0.0
            val hunger = conditionTag["Hunger"]?.toDouble() ?: 0.0
            val thirst = conditionTag["Thirst"]?.toDouble() ?: 0.0
            conditionTag["Stamina"] = stamina - 0.8
            conditionTag["Hunger"] = hunger + 0.1
            conditionTag["Thirst"] = thirst - 0.3
        }
        entity.damage(5.0)
        item.setAmount(item.amount() - 1)
    }

    override fun click(entity: MobPlayerServer,
                       item: ItemStack,
                       hit: MobServer): Double {
        if (hit is MobLivingServer) {
            hit.damage(1.0)
            item.setAmount(item.amount() - 1)
        }
        return 0.0
    }

    override fun types(): Int {
        return 1
    }

    override fun texture(data: Int): String {
        when (data) {
            0 -> return "VanillaBasics:image/terrain/food/meat/pork/Raw.png"
            else -> throw IllegalArgumentException("Unknown data: {}" + data)
        }
    }

    override fun name(item: ItemStack): String {
        val name = StringBuilder(40)
        when (item.data()) {
            else -> name.append("Porkchop")
        }
        val temperature = temperature(item)
        name.append("\nTemp.:").append(floor(temperature)).append("°C")
        return name.toString()
    }

    override fun maxStackSize(item: ItemStack): Int {
        return 32
    }

    override fun heatTransferFactor(item: ItemStack) = 0.001

    override fun temperatureUpdated(item: ItemStack) {
        if (temperature(item) >= 60.0) {
            item.setMaterial(materials.cookedMeat)
        }
    }
}
