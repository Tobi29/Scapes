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
import org.tobi29.scapes.block.TerrainTextureRegistry
import org.tobi29.scapes.engine.utils.toArray
import org.tobi29.scapes.vanilla.basics.material.StoneType
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial

abstract class BlockOre protected constructor(materials: VanillaMaterial, nameID: String,
                                              stoneRegistry: GameRegistry.Registry<StoneType>) : BlockStone(
        materials, nameID, stoneRegistry) {

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

    override fun texture(data: Int): String {
        return ""
    }

    override fun registerTextures(registry: TerrainTextureRegistry) {
        val ore = "VanillaBasics:image/terrain/ore/block/" +
                oreTexture() + ".png"
        textures = stoneRegistry.values().asSequence().map {
            it?.let {
                return@map registry.registerTexture(
                        it.textureRoot() + "/raw/" + it.texture() + ".png", ore)
            }
        }.toArray()
    }

    protected abstract fun oreTexture(): String
}
