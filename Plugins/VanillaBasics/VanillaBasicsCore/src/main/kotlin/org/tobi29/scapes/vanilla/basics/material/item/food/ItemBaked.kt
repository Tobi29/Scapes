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
import org.tobi29.scapes.entity.server.MobPlayerServer
import org.tobi29.scapes.vanilla.basics.material.CropType
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterialType
import org.tobi29.scapes.vanilla.basics.material.item.ItemSimpleData

class ItemBaked(type: VanillaMaterialType) : ItemSimpleData(type) {
    val cropRegistry = plugins.registry.get<CropType>("VanillaBasics",
            "CropType")

    override fun click(entity: MobPlayerServer,
                       item: ItemStack) {
        entity.metaData("Vanilla").syncMapMut("Condition") { conditionTag ->
            val stamina = conditionTag["Stamina"]?.toDouble() ?: 0.0
            val hunger = conditionTag["Hunger"]?.toDouble() ?: 0.0
            val thirst = conditionTag["Thirst"]?.toDouble() ?: 0.0
            conditionTag["Stamina"] = stamina - 0.1
            conditionTag["Hunger"] = hunger + 0.1
            conditionTag["Thirst"] = thirst - 0.1
        }
        item.setAmount(item.amount() - 1)
    }

    override fun types(): Int {
        return cropRegistry.values().size
    }

    override fun texture(data: Int): String {
        return "${cropRegistry[data].texture}/Baked.png"
    }

    override fun name(item: ItemStack): String {
        return cropRegistry[item.data()].bakedName
    }

    override fun maxStackSize(item: ItemStack): Int {
        return 16
    }
}
