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

package scapes.plugin.tobi29.vanilla.basics.material.item.tool

import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.entity.server.MobPlayerServer
import scapes.plugin.tobi29.vanilla.basics.material.VanillaMaterial

class ItemFlintPickaxe(materials: VanillaMaterial) : ItemFlintTool(materials,
        "vanilla.basics.item.FlintPickaxe") {

    override fun click(entity: MobPlayerServer,
                       item: ItemStack) {
        if (item.data() == 0) {
            val itemHandle = ItemStack(materials.stick, 0)
            val itemString = ItemStack(materials.string, 0, 2)
            entity.inventories().modify("Container") { inventory ->
                if (inventory.canTake(itemHandle) && inventory.canTake(
                        itemString)) {
                    inventory.take(itemHandle)
                    inventory.take(itemString)
                    item.setData(1)
                }
            }
        }
    }

    override fun type(): String {
        return "Pickaxe"
    }
}
