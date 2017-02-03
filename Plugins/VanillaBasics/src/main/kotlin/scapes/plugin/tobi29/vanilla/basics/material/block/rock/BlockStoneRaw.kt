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

package scapes.plugin.tobi29.vanilla.basics.material.block.rock

import org.tobi29.scapes.block.GameRegistry
import org.tobi29.scapes.block.ItemStack
import scapes.plugin.tobi29.vanilla.basics.material.StoneType
import scapes.plugin.tobi29.vanilla.basics.material.VanillaMaterial
import java.util.*

class BlockStoneRaw(materials: VanillaMaterial,
                    stoneRegistry: GameRegistry.Registry<StoneType>) : BlockStone(
        materials, "vanilla.basics.block.StoneRaw", stoneRegistry) {

    override fun drops(item: ItemStack,
                       data: Int): List<ItemStack> {
        if ("Pickaxe" == item.material().toolType(item) && canBeBroken(
                item.material().toolLevel(item), data)) {
            return listOf(ItemStack(materials.stoneRock, data,
                    Random().nextInt(4) + 8))
        }
        return emptyList()
    }

    override fun texture(data: Int): String {
        val type = stoneRegistry[data]
        return type.textureRoot() + "/raw/" +
                type.texture() +
                ".png"
    }

    override fun name(item: ItemStack): String {
        return stoneName(item)
    }
}
