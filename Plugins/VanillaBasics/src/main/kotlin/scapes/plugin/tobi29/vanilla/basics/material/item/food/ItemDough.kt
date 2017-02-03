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
import org.tobi29.scapes.engine.utils.io.tag.getFloat
import org.tobi29.scapes.engine.utils.io.tag.setFloat
import org.tobi29.scapes.engine.utils.math.floor
import org.tobi29.scapes.entity.server.MobItemServer
import scapes.plugin.tobi29.vanilla.basics.material.CropType
import scapes.plugin.tobi29.vanilla.basics.material.VanillaMaterial
import scapes.plugin.tobi29.vanilla.basics.material.item.ItemHeatable
import scapes.plugin.tobi29.vanilla.basics.material.item.ItemResearch
import scapes.plugin.tobi29.vanilla.basics.material.item.ItemSimpleData

class ItemDough(materials: VanillaMaterial,
                private val cropRegistry: GameRegistry.Registry<CropType>) : ItemSimpleData(
        materials, "vanilla.basics.item.Dough"), ItemHeatable, ItemResearch {

    override fun types(): Int {
        return cropRegistry.values().size
    }

    override fun texture(data: Int): String {
        return cropRegistry[data].texture() + "/Dough.png"
    }

    override fun name(item: ItemStack): String {
        val name = StringBuilder(40)
        name.append(materials.crop.name(item)).append("Dough")
        val temperature = temperature(item)
        if (temperature > 0.1f) {
            name.append("\nTemp.:").append(floor(temperature)).append(
                    "°C")
        }
        return name.toString()
    }

    override fun maxStackSize(item: ItemStack): Int {
        return if (temperature(item) == 0f) 8 else 1
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
                item.setMaterial(materials.baked)
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
            else -> return 40f
        }
    }

    override fun temperature(item: ItemStack): Float {
        return item.metaData("Vanilla").getFloat("Temperature") ?: 0.0f
    }

    override fun identifiers(item: ItemStack): Array<String> {
        return arrayOf("vanilla.basics.item.Dough",
                "vanilla.basics.item.Dough." + materials.crop.name(item))
    }
}
