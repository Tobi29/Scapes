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
import org.tobi29.math.threadLocalRandom
import org.tobi29.utils.toArray
import org.tobi29.scapes.inventory.Item
import org.tobi29.scapes.inventory.TypedItem
import org.tobi29.scapes.inventory.kind
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterialType

class BlockStoneRaw(type: VanillaMaterialType) : BlockStone(type) {
    override fun drops(item: Item?,
                       data: Int): List<Item> {
        if ("Pickaxe" == item.kind<ItemTypeTool>()?.toolType() && canBeBroken(
                item.kind<ItemTypeTool>()?.toolLevel() ?: 0, data)) {
            return listOf(ItemStackData(materials.stoneRock, data,
                    threadLocalRandom().nextInt(4) + 8))
        }
        return emptyList()
    }

    override fun registerTextures(registry: TerrainTextureRegistry) {
        textures = (0 until types()).asSequence().map {
            val type = stoneRegistry[it]
            val texture = "${type.textureRoot}/raw/${type.texture}.png"
            registry.registerTexture(texture)
        }.toArray()
    }

    override fun name(item: TypedItem<BlockType>): String {
        return stoneName(item)
    }
}
