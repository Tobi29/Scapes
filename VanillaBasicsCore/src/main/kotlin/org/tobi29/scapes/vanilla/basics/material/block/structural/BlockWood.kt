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

import org.tobi29.scapes.block.BlockType
import org.tobi29.scapes.block.ItemStackData
import org.tobi29.scapes.block.ItemTypeTool
import org.tobi29.scapes.block.toolType
import org.tobi29.scapes.inventory.Item
import org.tobi29.scapes.inventory.ItemStack
import org.tobi29.scapes.inventory.TypedItem
import org.tobi29.scapes.inventory.kind
import org.tobi29.scapes.vanilla.basics.material.ItemTypeFuelI
import org.tobi29.scapes.vanilla.basics.material.TreeType
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterialType
import org.tobi29.scapes.vanilla.basics.material.block.BlockSimpleDataTextured

class BlockWood(type: VanillaMaterialType) : BlockSimpleDataTextured(
        type),
        ItemTypeFuelI<BlockType> {
    private val treeRegistry = plugins.registry.get<TreeType>("VanillaBasics",
            "TreeType")

    override fun resistance(item: Item?,
                            data: Int): Double {
        return (if ("Axe" == item.kind<ItemTypeTool>()?.toolType() || "Saw" == item.kind<ItemTypeTool>()?.toolType())
            2
        else
            -1).toDouble()
    }

    override fun drops(item: Item?,
                       data: Int): List<Item> {
        if ("Saw" == item.kind<ItemTypeTool>()?.toolType()) {
            return listOf(ItemStack(materials.stick, 8))
        }
        return listOf(ItemStackData(this, data))
    }

    override fun footStepSound(data: Int): String {
        return "VanillaBasics:sound/footsteps/Wood.ogg"
    }

    override fun breakSound(item: Item?,
                            data: Int): String {
        return if ("Axe" == item.kind<ItemTypeTool>()?.toolType())
            "VanillaBasics:sound/blocks/Axe.ogg"
        else
            "VanillaBasics:sound/blocks/Saw.ogg"
    }

    override fun types(): Int {
        return treeRegistry.values().size
    }

    override fun texture(data: Int): String {
        val type = treeRegistry[data]
        return "${type.texture}/Planks.png"
    }

    override fun name(item: TypedItem<BlockType>): String {
        return materials.log.name(item) + " Planks"
    }

    override fun maxStackSize(item: TypedItem<BlockType>) = 16

    override fun fuelTemperature(item: TypedItem<BlockType>) = 0.1

    override fun fuelTime(item: TypedItem<BlockType>) = 40.0

    override fun fuelTier(item: TypedItem<BlockType>) = 5
}
