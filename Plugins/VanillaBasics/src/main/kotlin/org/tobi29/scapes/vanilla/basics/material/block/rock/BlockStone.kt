/*
 * Copyright 2012-2016 Tobi29
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

import org.tobi29.scapes.block.GameRegistry
import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.engine.utils.math.round
import org.tobi29.scapes.vanilla.basics.material.StoneType
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial
import org.tobi29.scapes.vanilla.basics.material.block.BlockSimpleData

abstract class BlockStone protected constructor(materials: VanillaMaterial, nameID: String,
                                                protected val stoneRegistry: GameRegistry.Registry<StoneType>) : BlockSimpleData(
        materials, nameID) {

    override fun types(): Int {
        return stoneRegistry.values().size
    }

    override fun resistance(item: ItemStack,
                            data: Int): Double {
        return if ("Pickaxe" == item.material().toolType(item) && canBeBroken(
                item.material().toolLevel(item), data))
            8.0 * stoneRegistry[data].resistance()
        else
            -1.0
    }

    override fun footStepSound(data: Int): String {
        return "VanillaBasics:sound/footsteps/Stone.ogg"
    }

    override fun breakSound(item: ItemStack,
                            data: Int): String {
        return "VanillaBasics:sound/blocks/Stone.ogg"
    }

    fun canBeBroken(toolLevel: Int,
                    data: Int): Boolean {
        return round(
                stoneRegistry[data].resistance()) * 10 <= toolLevel
    }

    override fun maxStackSize(item: ItemStack): Int {
        return 16
    }

    protected fun stoneName(item: ItemStack): String {
        return stoneRegistry[item.data()].name()
    }
}