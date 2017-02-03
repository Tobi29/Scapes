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

package scapes.plugin.tobi29.vanilla.basics.material.item.food

import org.tobi29.scapes.block.GameRegistry
import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.engine.utils.io.tag.getDouble
import org.tobi29.scapes.engine.utils.io.tag.setDouble
import org.tobi29.scapes.entity.server.MobPlayerServer
import scapes.plugin.tobi29.vanilla.basics.material.CropType
import scapes.plugin.tobi29.vanilla.basics.material.VanillaMaterial
import scapes.plugin.tobi29.vanilla.basics.material.item.ItemSimpleData

class ItemBaked(materials: VanillaMaterial,
                private val cropRegistry: GameRegistry.Registry<CropType>) : ItemSimpleData(
        materials, "vanilla.basics.item.Baked") {
    override fun click(entity: MobPlayerServer,
                       item: ItemStack) {
        val conditionTag = entity.metaData("Vanilla").structure("Condition")
        synchronized(conditionTag) {
            val stamina = conditionTag.getDouble("Stamina") ?: 0.0
            conditionTag.setDouble("Stamina", stamina - 0.1)
            val hunger = conditionTag.getDouble("Hunger") ?: 0.0
            conditionTag.setDouble("Hunger", hunger + 0.1)
            val thirst = conditionTag.getDouble("Thirst") ?: 0.0
            conditionTag.setDouble("Thirst", thirst - 0.1)
        }
        item.setAmount(item.amount() - 1)
    }

    override fun types(): Int {
        return cropRegistry.values().size
    }

    override fun texture(data: Int): String {
        return cropRegistry[data].texture() + "/Baked.png"
    }

    override fun name(item: ItemStack): String {
        return cropRegistry[item.data()].bakedName()
    }

    override fun maxStackSize(item: ItemStack): Int {
        return 16
    }
}
