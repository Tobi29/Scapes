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
import org.tobi29.scapes.chunk.terrain.TerrainMutableServer
import org.tobi29.math.Face
import org.tobi29.math.Random
import org.tobi29.utils.toArray
import org.tobi29.scapes.entity.server.MobPlayerServer
import org.tobi29.scapes.inventory.Item
import org.tobi29.scapes.inventory.TypedItem
import org.tobi29.scapes.inventory.kind
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterialType
import org.tobi29.scapes.vanilla.basics.util.dropItems

abstract class BlockOre(type: VanillaMaterialType) : BlockStone(type) {
    override fun destroy(terrain: TerrainMutableServer,
                         x: Int,
                         y: Int,
                         z: Int,
                         data: Int,
                         face: Face,
                         player: MobPlayerServer,
                         item: Item?): Boolean {
        if (!super.destroy(terrain, x, y, z, data, face, player, item)) {
            return false
        }
        if ("Pickaxe" == item.kind<ItemTypeTool>()?.toolType() && !canBeBroken(
                item.kind<ItemTypeTool>()?.toolLevel() ?: 0, data)) {
            player.world.dropItems(drops(item, data), x + face.x, y + face.y,
                    z + face.z)
            terrain.type(x, y, z, materials.stoneRaw)
            return false
        }
        return true
    }

    override fun drops(item: Item?,
                       data: Int): List<Item> {
        return dropsOre(item, data) + listOf(
                ItemStackData(materials.stoneRock, data,
                        Random().nextInt(4) + 8))
    }

    abstract fun dropsOre(item: Item?,
                          data: Int): List<Item>

    override fun resistance(item: Item?,
                            data: Int): Double {
        if ("Pickaxe" == item.kind<ItemTypeTool>()?.toolType() && canBeBroken(
                item.kind<ItemTypeTool>()?.toolLevel() ?: 0, data)) {
            return super.resistance(item, data)
        } else if ("Pickaxe" == item.kind<ItemTypeTool>()?.toolType()) {
            return 12.0
        }
        return -1.0
    }

    override fun registerTextures(registry: TerrainTextureRegistry) {
        val ore = "VanillaBasics:image/terrain/ore/block/" +
                oreTexture() + ".png"
        textures = stoneRegistry.values().asSequence().map {
            it?.let {
                return@map registry.registerTexture(
                        "${it.textureRoot}/raw/${it.texture}.png", ore)
            }
        }.toArray()
    }

    override fun maxStackSize(item: TypedItem<BlockType>) = 4

    protected abstract fun oreTexture(): String
}
