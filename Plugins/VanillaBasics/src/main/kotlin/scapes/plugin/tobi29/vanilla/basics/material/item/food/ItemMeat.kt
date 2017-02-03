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

import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.engine.utils.io.tag.getDouble
import org.tobi29.scapes.engine.utils.io.tag.getFloat
import org.tobi29.scapes.engine.utils.io.tag.setDouble
import org.tobi29.scapes.engine.utils.io.tag.setFloat
import org.tobi29.scapes.engine.utils.math.floor
import org.tobi29.scapes.entity.server.MobItemServer
import org.tobi29.scapes.entity.server.MobLivingServer
import org.tobi29.scapes.entity.server.MobPlayerServer
import org.tobi29.scapes.entity.server.MobServer
import scapes.plugin.tobi29.vanilla.basics.material.VanillaMaterial
import scapes.plugin.tobi29.vanilla.basics.material.item.ItemHeatable
import scapes.plugin.tobi29.vanilla.basics.material.item.ItemSimpleData

class ItemMeat(materials: VanillaMaterial) : ItemSimpleData(materials,
        "vanilla.basics.item.Meat"), ItemHeatable {
    override fun click(entity: MobPlayerServer,
                       item: ItemStack) {
        val conditionTag = entity.metaData("Vanilla").structure("Condition")
        synchronized(conditionTag) {
            val stamina = conditionTag.getDouble("Stamina") ?: 0.0
            conditionTag.setDouble("Stamina", stamina - 0.8)
            val hunger = conditionTag.getDouble("Hunger") ?: 0.0
            conditionTag.setDouble("Hunger", hunger + 0.1)
            val thirst = conditionTag.getDouble("Thirst") ?: 0.0
            conditionTag.setDouble("Thirst", thirst - 0.3)
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
        if (temperature > 0.1f) {
            name.append("\nTemp.:").append(floor(temperature)).append(
                    "°C")
        }
        return name.toString()
    }

    override fun maxStackSize(item: ItemStack): Int {
        return if (temperature(item) == 0f) 32 else 1
    }

    override fun heat(item: ItemStack,
                      temperature: Float) {
        var currentTemperature = temperature(item)
        if (currentTemperature < 1 && temperature < currentTemperature) {
            item.metaData("Vanilla").setFloat("Temperature", 0.0f)
        } else {
            currentTemperature += (temperature - currentTemperature) / 400.0f
            item.metaData("Vanilla").setFloat("Temperature", currentTemperature)
            if (currentTemperature >= meltingPoint(item)) {
                item.setMaterial(materials.cookedMeat)
            }
        }
    }

    override fun cool(item: ItemStack) {
        val currentTemperature = temperature(item)
        if (currentTemperature < 1) {
            item.metaData("Vanilla").setFloat("Temperature", 0.0f)
        } else {
            item.metaData("Vanilla").setFloat("Temperature",
                    currentTemperature / 1.002f)
        }
    }

    override fun cool(item: MobItemServer) {
        val currentTemperature = temperature(item.item())
        if (currentTemperature < 1) {
            item.item().metaData("Vanilla").setFloat("Temperature", 0.0f)
        } else {
            if (item.isInWater) {
                item.item().metaData("Vanilla").setFloat("Temperature",
                        currentTemperature / 4.0f)
            } else {
                item.item().metaData("Vanilla").setFloat("Temperature",
                        currentTemperature / 1.002f)
            }
        }
    }

    override fun meltingPoint(item: ItemStack): Float {
        when (item.data()) {
            else -> return 60f
        }
    }

    override fun temperature(item: ItemStack): Float {
        return item.metaData("Vanilla").getFloat("Temperature") ?: 0.0f
    }
}
