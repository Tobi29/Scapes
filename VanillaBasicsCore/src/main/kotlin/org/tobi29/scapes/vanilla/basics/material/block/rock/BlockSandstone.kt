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

import org.tobi29.scapes.block.BlockType
import org.tobi29.scapes.block.ItemTypeTool
import org.tobi29.scapes.block.toolType
import org.tobi29.scapes.inventory.Item
import org.tobi29.scapes.inventory.TypedItem
import org.tobi29.scapes.inventory.kind
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterialType
import org.tobi29.scapes.vanilla.basics.material.block.BlockSimpleDataTextured

class BlockSandstone(type: VanillaMaterialType) : BlockSimpleDataTextured(type) {
    override fun resistance(item: Item?,
                            data: Int): Double {
        return (if ("Pickaxe" == item.kind<ItemTypeTool>()?.toolType()) 4 else -1).toDouble()
    }

    override fun footStepSound(data: Int): String {
        return "VanillaBasics:sound/footsteps/Stone.ogg"
    }

    override fun breakSound(item: Item?,
                            data: Int): String {
        return "VanillaBasics:sound/blocks/Stone.ogg"
    }

    override fun name(item: TypedItem<BlockType>): String {
        return "Sandstone"
    }

    override fun maxStackSize(item: TypedItem<BlockType>): Int {
        return 16
    }

    override fun types(): Int {
        return TEXTURES.size
    }

    override fun texture(data: Int): String {
        return TEXTURES[data]
    }

    companion object {
        private val TEXTURES = arrayOf(
                "VanillaBasics:image/terrain/stone/sandstone/1.png",
                "VanillaBasics:image/terrain/stone/sandstone/2.png",
                "VanillaBasics:image/terrain/stone/sandstone/3.png",
                "VanillaBasics:image/terrain/stone/sandstone/4.png",
                "VanillaBasics:image/terrain/stone/sandstone/5.png",
                "VanillaBasics:image/terrain/stone/sandstone/6.png")
    }
}
