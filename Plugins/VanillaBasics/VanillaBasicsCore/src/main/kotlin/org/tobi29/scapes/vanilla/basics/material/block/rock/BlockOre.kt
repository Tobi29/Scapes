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

import org.tobi29.scapes.block.GameRegistry
import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.block.TerrainTextureRegistry
import org.tobi29.scapes.chunk.terrain.TerrainServer
import org.tobi29.scapes.engine.utils.math.Face
import org.tobi29.scapes.engine.utils.toArray
import org.tobi29.scapes.entity.server.MobPlayerServer
import org.tobi29.scapes.vanilla.basics.material.StoneType
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial
import java.util.*

abstract class BlockOre protected constructor(materials: VanillaMaterial,
                                              nameID: String,
                                              stoneRegistry: GameRegistry.Registry<StoneType>) : BlockStone(
        materials, nameID, stoneRegistry) {
    override fun destroy(terrain: TerrainServer.TerrainMutable,
                         x: Int,
                         y: Int,
                         z: Int,
                         data: Int,
                         face: Face,
                         player: MobPlayerServer,
                         item: ItemStack): Boolean {
        if ("Pickaxe" == item.material().toolType(item) && !canBeBroken(
                item.material().toolLevel(item), data)) {
            drops(item, data).forEach {
                terrain.world.dropItem(it, x + face.x, y + face.y, z + face.z)
            }
            terrain.type(x, y, z, materials.stoneRaw)
            return false
        }
        return true
    }

    override fun drops(item: ItemStack,
                       data: Int): List<ItemStack> {
        return dropsOre(item, data) + listOf(
                ItemStack(materials.stoneRock, data, Random().nextInt(4) + 8))
    }

    abstract fun dropsOre(item: ItemStack,
                          data: Int): List<ItemStack>

    override fun resistance(item: ItemStack,
                            data: Int): Double {
        if ("Pickaxe" == item.material().toolType(item) && canBeBroken(
                item.material().toolLevel(item), data)) {
            return super.resistance(item, data)
        } else if ("Pickaxe" == item.material().toolType(item)) {
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

    override fun maxStackSize(item: ItemStack) = 4

    protected abstract fun oreTexture(): String
}
