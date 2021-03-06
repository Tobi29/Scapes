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
package org.tobi29.scapes.vanilla.basics.material.item

import org.tobi29.scapes.block.copy
import org.tobi29.scapes.block.data
import org.tobi29.stdex.math.floorToInt
import org.tobi29.scapes.inventory.Item
import org.tobi29.scapes.inventory.TypedItem
import org.tobi29.scapes.vanilla.basics.material.ItemDefaultHeatableI
import org.tobi29.scapes.vanilla.basics.material.ItemResearchI
import org.tobi29.scapes.vanilla.basics.material.MetalType
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterialType
import org.tobi29.scapes.vanilla.basics.util.Alloy
import org.tobi29.scapes.vanilla.basics.util.toIngot

class ItemOreChunk(
        type: VanillaMaterialType
) : ItemSimpleData(type),
        ItemDefaultHeatableI<VanillaItem>,
        ItemResearchI<VanillaItem> {
    override fun name(item: TypedItem<VanillaItem>): String {
        val name = StringBuilder(50)
        name.append(oreName(item))
        val temperature = temperature(item)
        name.append("\nTemp.:").append(temperature.floorToInt()).append("°C")
        return name.toString()
    }

    override fun maxStackSize(item: TypedItem<VanillaItem>): Int {
        return 4
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

    fun oreName(item: TypedItem<VanillaItem>): String {
        when (item.data) {
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
                    "Unknown data: {}" + item.data)
        }
    }

    fun meltingPoint(item: TypedItem<VanillaItem>): Float {
        when (item.data) {
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
                    "Unknown data: {}" + item.data)
        }
    }

    override fun heatTransferFactor(item: TypedItem<VanillaItem>) = 0.001

    override fun temperatureUpdated(item: TypedItem<VanillaItem>): Item? {
        val temperature = temperature(item)
        if (temperature >= meltingPoint(item)) {
            val data = item.data
            if (data == 4 || data == 5) {
                return item.copy(data = 8)
            } else {
                val metal = metal(item)
                if (metal != null) {
                    return Alloy(mapOf(metal to 1.0)).toIngot(plugin)
                }
            }
        }
        return item
    }

    fun metal(item: TypedItem<VanillaItem>): MetalType? = when (item.data) {
        0 -> plugin.metalType("Bismuth")
        1 -> plugin.metalType("Copper")
        2 -> plugin.metalType("Tin")
        3 -> plugin.metalType("Zinc")
        6 -> plugin.metalType("Silver")
        7 -> plugin.metalType("Gold")
        9 -> plugin.metalType("Iron")
        else -> null
    }

    override fun identifiers(item: TypedItem<VanillaItem>): Array<String> {
        return arrayOf("vanilla.basics.item.OreChunk",
                "vanilla.basics.item.OreChunk." + oreName(item))
    }
}
