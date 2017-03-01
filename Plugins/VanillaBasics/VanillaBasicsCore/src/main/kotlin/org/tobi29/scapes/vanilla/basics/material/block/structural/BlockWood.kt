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

package org.tobi29.scapes.vanilla.basics.material.block.structural

import org.tobi29.scapes.block.GameRegistry
import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.vanilla.basics.material.ItemFuel
import org.tobi29.scapes.vanilla.basics.material.TreeType
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial
import org.tobi29.scapes.vanilla.basics.material.block.BlockSimpleDataTextured

class BlockWood(materials: VanillaMaterial,
                private val treeRegistry: GameRegistry.Registry<TreeType>) : BlockSimpleDataTextured(
        materials, "vanilla.basics.block.Wood"), ItemFuel {

    override fun resistance(item: ItemStack,
                            data: Int): Double {
        return (if ("Axe" == item.material().toolType(
                item) || "Saw" == item.material().toolType(item))
            2
        else
            -1).toDouble()
    }

    override fun drops(item: ItemStack,
                       data: Int): List<ItemStack> {
        if ("Saw" == item.material().toolType(item)) {
            return listOf(ItemStack(materials.stick, 0.toShort().toInt(), 8))
        }
        return listOf(ItemStack(this, data))
    }

    override fun footStepSound(data: Int): String {
        return "VanillaBasics:sound/footsteps/Wood.ogg"
    }

    override fun breakSound(item: ItemStack,
                            data: Int): String {
        return if ("Axe" == item.material().toolType(item))
            "VanillaBasics:sound/blocks/Axe.ogg"
        else
            "VanillaBasics:sound/blocks/Saw.ogg"
    }

    override fun types(): Int {
        return treeRegistry.values().size
    }

    override fun texture(data: Int): String {
        val type = treeRegistry[data]
        return type.texture() + "/Planks.png"
    }

    override fun name(item: ItemStack): String {
        return materials.log.name(item) + " Planks"
    }

    override fun maxStackSize(item: ItemStack) = 16

    override fun fuelTemperature(item: ItemStack) = 0.1

    override fun fuelTime(item: ItemStack) = 40.0

    override fun fuelTier(item: ItemStack) = 5
}
