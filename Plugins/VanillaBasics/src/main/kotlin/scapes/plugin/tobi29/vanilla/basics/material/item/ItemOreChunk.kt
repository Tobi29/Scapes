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
package scapes.plugin.tobi29.vanilla.basics.material.item

import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.engine.utils.io.tag.set
import org.tobi29.scapes.engine.utils.io.tag.toFloat
import org.tobi29.scapes.engine.utils.math.floor
import org.tobi29.scapes.entity.server.MobItemServer
import scapes.plugin.tobi29.vanilla.basics.material.MetalType
import scapes.plugin.tobi29.vanilla.basics.material.VanillaMaterial
import scapes.plugin.tobi29.vanilla.basics.util.createIngot

class ItemOreChunk(materials: VanillaMaterial) : ItemSimpleData(materials,
        "vanilla.basics.item.OreChunk"), ItemHeatable, ItemResearch {

    override fun name(item: ItemStack): String {
        val name = StringBuilder(50)
        name.append(oreName(item))
        val temperature = temperature(item)
        if (temperature > 0.1f) {
            name.append("\nTemp.:").append(floor(temperature)).append("°C")
        }
        return name.toString()
    }

    override fun maxStackSize(item: ItemStack): Int {
        if (item.data() == 0) {
            return if (temperature(item) == 0f) 16 else 1
        } else {
            return if (temperature(item) == 0f) 4 else 1
        }
    }

    override fun types(): Int {
        return 10
    }

    override fun texture(data: Int): String {
        when (data) {
            0 -> return "VanillaBasics:image/terrain/ore/chunk/Bismuthinite.png"
            1 -> return "VanillaBasics:image/terrain/ore/chunk/Chalcocite.png"
            2 -> return "VanillaBasics:image/terrain/ore/chunk/Cassiterite.png"
            3 -> return "VanillaBasics:image/terrain/ore/chunk/Sphalerite.png"
            4 -> return "VanillaBasics:image/terrain/ore/chunk/Pyrite.png"
            5 -> return "VanillaBasics:image/terrain/ore/chunk/Magnetite.png"
            6 -> return "VanillaBasics:image/terrain/ore/chunk/Silver.png"
            7 -> return "VanillaBasics:image/terrain/ore/chunk/Gold.png"
            8, 9 -> return "VanillaBasics:image/terrain/ore/chunk/IronBloom.png"
            else -> throw IllegalArgumentException("Unknown data: {}" + data)
        }
    }

    fun oreName(item: ItemStack): String {
        when (item.data()) {
            0 -> return "Bismuthinite"
            1 -> return "Chalcocite"
            2 -> return "Cassiterite"
            3 -> return "Sphalerite"
            4 -> return "Pyrite"
            5 -> return "Magnetite"
            6 -> return "Native Silver"
            7 -> return "Native Gold"
            8 -> return "Iron Bloom"
            9 -> return "Worked Iron Bloom"
            else -> throw IllegalArgumentException(
                    "Unknown data: {}" + item.data())
        }
    }

    override fun heat(item: ItemStack,
                      temperature: Float) {
        var currentTemperature = temperature(item)
        if (currentTemperature < 1.0 && temperature < currentTemperature) {
            item.metaData("Vanilla")["Temperature"] = 0.0
        } else {
            currentTemperature += (temperature - currentTemperature) / 400.0f
            item.metaData("Vanilla")["Temperature"] = currentTemperature
            if (currentTemperature >= meltingPoint(item)) {
                val data = item.data()
                if (data == 4 || data == 5) {
                    item.setData(8)
                } else {
                    val metal = metal(item)
                    item.setMaterial(materials.ingot, 1)
                    createIngot(item, metal, currentTemperature)
                }
            }
        }
    }

    override fun cool(item: ItemStack) {
        val currentTemperature = temperature(item)
        if (currentTemperature < 1.0) {
            item.metaData("Vanilla")["Temperature"] = 0.0
        } else {
            item.metaData("Vanilla")["Temperature"] = currentTemperature / 1.002
        }
    }

    override fun cool(item: MobItemServer) {
        val currentTemperature = temperature(item.item())
        if (currentTemperature < 1.0) {
            item.item().metaData("Vanilla")["Temperature"] = 0.0
        } else {
            if (item.isInWater) {
                item.item().metaData(
                        "Vanilla")["Temperature"] = currentTemperature / 4.0
            } else {
                item.item().metaData(
                        "Vanilla")["Temperature"] = currentTemperature / 1.002
            }
        }
    }

    override fun meltingPoint(item: ItemStack): Float {
        when (item.data()) {
            0 -> return 271.0f
            1 -> return 1084.0f
            2 -> return 231.0f
            3 -> return 419.0f
            4 -> return 1538.0f
            5 -> return 1538.0f
            6 -> return 961.0f
            7 -> return 1064.0f
            8 -> return 1538.0f
            9 -> return 1538.0f
            else -> throw IllegalArgumentException(
                    "Unknown data: {}" + item.data())
        }
    }

    override fun temperature(item: ItemStack): Float {
        return item.metaData("Vanilla")["Temperature"]?.toFloat() ?: 0.0f
    }

    fun metal(item: ItemStack): MetalType {
        when (item.data()) {
            0 -> return plugin.metalType("Bismuth")
            1 -> return plugin.metalType("Copper")
            2 -> return plugin.metalType("Tin")
            3 -> return plugin.metalType("Zinc")
            6 -> return plugin.metalType("Silver")
            7 -> return plugin.metalType("Gold")
            9 -> return plugin.metalType("Iron")
            else -> throw IllegalArgumentException(
                    "Unknown data: {}" + item.data())
        }
    }

    override fun identifiers(item: ItemStack): Array<String> {
        return arrayOf("vanilla.basics.item.OreChunk",
                "vanilla.basics.item.OreChunk." + oreName(item))
    }
}
