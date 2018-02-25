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

package org.tobi29.scapes.inventory

import org.tobi29.io.tag.*
import java.util.concurrent.atomic.AtomicReferenceArray
import kotlin.collections.asSequence
import kotlin.collections.set

class Inventory(size: Int) {
    private val items = AtomicReferenceArray<Item?>(size)

    constructor(inventory: Inventory) : this(inventory.items.length()) {
        for (i in 0 until size()) {
            items[i] = inventory.items[i]
        }
    }

    operator fun get(i: Int): Item? = items[i]

    operator fun set(i: Int,
                     value: Item?) = items.set(i, value)

    fun reference(i: Int): () -> Item? = { items[i] }

    fun add(add: Item?): Item? {
        if (add == null) return null
        var current: Item = add
        for (i in 0 until size()) {
            val item = items[i]
            if (!item.isEmpty()) {
                val (stack, remaining) = item.stack(current)
                items[i] = stack
                if (remaining == null) return null
                current = remaining
            }
        }
        for (i in 0 until size()) {
            val (stack, remaining) = items[i].stack(current)
            items[i] = stack
            if (remaining == null) return null
            current = remaining
        }
        return current
    }

    fun canAdd(add: Item?): Item? {
        if (add == null) return null
        var current: Item = add
        for (i in 0 until size()) {
            current = items[i].stack(current).second ?: return null
        }
        return current
    }

    fun canAddAll(add: Item?): Boolean = canAdd(add).isEmpty()

    fun take(take: Item?): Pair<Item?, Item?> {
        if (take == null) return null to take
        var current: Item = take
        var taken: Item? = null
        for (i in 0 until size()) {
            val (left, took, remaining) = items[i].take(current)
            val (stack, spilled) = taken.stack(took)
            if (spilled != null) return taken to current
            items[i] = left
            taken = stack
            current = remaining ?: return taken to null
        }
        return taken to current
    }

    fun canTake(take: Item?): Pair<Item?, Item?> {
        if (take == null) return null to take
        var current: Item = take
        var taken: Item? = null
        for (i in 0 until size()) {
            val (_, took, remaining) = items[i].take(current)
            val (stack, spilled) = taken.stack(took)
            if (spilled != null) return taken to current
            taken = stack
            current = remaining ?: return taken to null
        }
        return taken to current
    }

    fun canTakeAll(take: Item?): Boolean = canTake(take).second.isEmpty()

    fun size(): Int {
        return items.length()
    }

    fun clear() {
        for (i in 0 until size()) {
            items[i] = null
        }
    }

    fun read(registry: (Int) -> ItemType?,
             map: TagMap) {
        map["Items"]?.toList()?.asSequence()?.mapNotNull(
                Tag::toMap)?.forEachIndexed { i, element ->
            items[i] = element.toItem(registry)
        }
    }

    fun write(map: ReadWriteTagMap) {
        map["Items"] = TagList {
            for (i in 0 until size()) {
                add(items[i]?.toTag() ?: TagMap())
            }
        }
    }
}
