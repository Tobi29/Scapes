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

package org.tobi29.scapes.vanilla.basics.material.item.tool

import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.engine.utils.math.floorToInt
import org.tobi29.scapes.vanilla.basics.material.ItemDefaultHeatable
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterialType
import org.tobi29.scapes.vanilla.basics.material.item.ItemSimpleData

class ItemMold(type: VanillaMaterialType) : ItemSimpleData(
        type), ItemDefaultHeatable {
    override fun types(): Int {
        return 2
    }

    override fun texture(data: Int): String {
        when (data) {
            0 -> return "VanillaBasics:image/terrain/tools/MoldClay.png"
            1 -> return "VanillaBasics:image/terrain/tools/Mold.png"
            else -> throw IllegalArgumentException("Unknown data: {}" + data)
        }
    }

    override fun name(item: ItemStack): String {
        val name = StringBuilder(40)
        when (item.data()) {
            1 -> {
                name.append("Ceramic Mold")
                name.append("Clay Mold")
            }
            else -> name.append("Clay Mold")
        }
        val temperature = temperature(item)
        name.append("\nTemp.:").append(temperature.floorToInt()).append("°C")
        return name.toString()
    }

    override fun maxStackSize(item: ItemStack): Int {
        return 64
    }

    override fun heatTransferFactor(item: ItemStack) = 0.0004

    override fun temperatureUpdated(item: ItemStack) {
        if (temperature(item) >= 1000.0) {
            item.setData(1)
        }
    }
}
