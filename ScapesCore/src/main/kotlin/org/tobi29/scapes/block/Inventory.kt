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

package org.tobi29.scapes.block

import java8.util.stream.Collectors
import org.tobi29.scapes.engine.utils.io.tag.TagStructure
import org.tobi29.scapes.engine.utils.io.tag.getListStructure
import org.tobi29.scapes.engine.utils.stream

class Inventory(private val registry: GameRegistry, size: Int) {
    private val items: Array<ItemStack>

    init {
        items = Array(size, { ItemStack(registry) })
    }

    constructor(inventory: Inventory) : this(inventory.registry,
            inventory.items.size) {
        for (i in items.indices) {
            items[i] = ItemStack(inventory.items[i])
        }
    }

    fun add(add: ItemStack): Int {
        for (item in items) {
            if (item.canStack(add) > 0 && item.material() !== registry.air()) {
                add.setAmount(add.amount() - item.stack(add))
                if (add.amount() <= 0) {
                    return add.amount()
                }
            }
        }
        for (item in items) {
            add.setAmount(add.amount() - item.stack(add))
            if (add.amount() <= 0) {
                return add.amount()
            }
        }
        return add.amount()
    }

    fun canAdd(add: ItemStack): Int {
        var hasToStack = add.amount()
        for (item in items) {
            hasToStack -= item.canStack(add)
            if (hasToStack <= 0) {
                return add.amount()
            }
        }
        return add.amount() - hasToStack
    }

    fun canTake(take: ItemStack): Boolean {
        var amount = 0
        for (item in items) {
            amount += item.canTake(take)
        }
        return amount >= take.amount()
    }

    fun item(id: Int): ItemStack {
        return items[id]
    }

    fun size(): Int {
        return items.size
    }

    fun take(take: ItemStack): ItemStack? {
        var give: ItemStack? = null
        var amount = take.amount()
        var i = 0
        while (i < items.size && amount > 0) {
            val give2 = items[i].take(take, amount)
            if (give2 != null) {
                val item = give2
                amount -= item.amount()
                if (give == null) {
                    give = item
                } else {
                    give.stack(item)
                }
            }
            i++
        }
        return give
    }

    fun clear() {
        for (item in items) {
            item.setAmount(0)
        }
    }

    fun load(tag: TagStructure) {
        var i = 0
        tag.getListStructure("Items") { element ->
            items[i++].load(element)
        }
    }

    fun save(): TagStructure {
        val structure = TagStructure()
        structure.setList("Items", stream(*items).map { it.save() }.collect(
                Collectors.toList<Any>()))
        return structure
    }
}
