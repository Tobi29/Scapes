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

package org.tobi29.scapes.vanilla.basics.material.block.rock

import org.tobi29.scapes.block.*
import org.tobi29.scapes.inventory.Item
import org.tobi29.scapes.inventory.TypedItem
import org.tobi29.scapes.inventory.kind
import org.tobi29.scapes.vanilla.basics.generator.StoneType
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterialType
import org.tobi29.scapes.vanilla.basics.material.block.BlockSimpleData
import kotlin.math.roundToInt

abstract class BlockStone(type: VanillaMaterialType) : BlockSimpleData(type) {
    protected val stoneRegistry = plugins.registry.get<StoneType>(
            "VanillaBasics", "StoneType")

    override fun types(): Int {
        return stoneRegistry.values().size
    }

    override fun resistance(item: Item?,
                            data: Int): Double {
        val tool = item.kind<ItemTypeTool>()
        return if ("Pickaxe" == tool?.toolType() && canBeBroken(
                tool.toolLevel(), data))
            8.0 * stoneRegistry[data].resistance
        else
            -1.0
    }

    override fun footStepSound(data: Int): String {
        return "VanillaBasics:sound/footsteps/Stone.ogg"
    }

    override fun breakSound(item: Item?,
                            data: Int): String {
        return "VanillaBasics:sound/blocks/Stone.ogg"
    }

    fun canBeBroken(toolLevel: Int,
                    data: Int): Boolean {
        return (stoneRegistry[data].resistance).roundToInt() * 10 <= toolLevel
    }

    override fun maxStackSize(item: TypedItem<BlockType>): Int {
        return 16
    }

    protected fun stoneName(item: TypedItem<BlockType>): String {
        return stoneRegistry[item.data].name
    }
}
