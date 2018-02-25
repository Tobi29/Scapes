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

package org.tobi29.scapes.vanilla.basics.material.item.tool

import org.tobi29.scapes.block.copy
import org.tobi29.scapes.block.data
import org.tobi29.scapes.block.inventories
import org.tobi29.scapes.entity.server.MobPlayerServer
import org.tobi29.scapes.inventory.Item
import org.tobi29.scapes.inventory.ItemStack
import org.tobi29.scapes.inventory.TypedItem
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterialType
import org.tobi29.scapes.vanilla.basics.material.item.VanillaItem

class ItemMetalPickaxe(type: VanillaMaterialType) : ItemMetalTool(type) {
    override fun click(entity: MobPlayerServer,
                       item: TypedItem<VanillaItem>): Item? =
            if (item.data == 0) {
                val itemHandle = ItemStack(type = materials.stick, amount = 1)
                val itemString = ItemStack(type = materials.string, amount = 2)
                val assemble = entity.inventories.modify(
                        "Container") { inventory ->
                    if (inventory.canTakeAll(itemHandle)
                            && inventory.canTakeAll(itemString)) {
                        inventory.take(itemHandle)
                        inventory.take(itemString)
                        true
                    } else false
                }
                if (assemble) item.copy(data = 1) else item
            } else item

    override fun type(): String {
        return "Pickaxe"
    }
}
