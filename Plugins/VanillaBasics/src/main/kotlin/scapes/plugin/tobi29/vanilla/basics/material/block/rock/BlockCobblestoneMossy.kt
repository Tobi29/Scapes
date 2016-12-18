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

package scapes.plugin.tobi29.vanilla.basics.material.block.rock

import org.tobi29.scapes.block.GameRegistry
import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.block.TerrainTextureRegistry
import org.tobi29.scapes.engine.utils.toArray
import scapes.plugin.tobi29.vanilla.basics.material.StoneType
import scapes.plugin.tobi29.vanilla.basics.material.VanillaMaterial

class BlockCobblestoneMossy(materials: VanillaMaterial,
                            stoneRegistry: GameRegistry.Registry<StoneType>) : BlockStone(
        materials, "vanilla.basics.block.CobblestoneMossy", stoneRegistry) {

    override fun drops(item: ItemStack,
                       data: Int): List<ItemStack> {
        if ("Pickaxe" == item.material().toolType(item) && canBeBroken(
                item.material().toolLevel(item), data)) {
            return listOf(ItemStack(materials.stoneRock, data, 9))
        }
        return emptyList()
    }

    override fun texture(data: Int): String {
        return ""
    }

    override fun registerTextures(registry: TerrainTextureRegistry) {
        textures = stoneRegistry.values().asSequence().map {
            it?.let {
                return@map registry.registerTexture(
                        it.textureRoot() + "/raw/" + it.texture() + ".png",
                        "VanillaBasics:image/terrain/stone/overlay/Cobble.png",
                        "VanillaBasics:image/terrain/stone/overlay/Mossy.png")
            }
        }.toArray()
    }

    override fun name(item: ItemStack): String {
        return "Mossy " + stoneName(item) + " Cobblestone"
    }
}
