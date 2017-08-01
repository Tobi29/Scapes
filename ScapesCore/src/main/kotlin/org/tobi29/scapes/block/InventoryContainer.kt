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

package org.tobi29.scapes.block

import org.tobi29.scapes.engine.utils.ConcurrentHashMap

class InventoryContainer(private val updateHandler: (String) -> Unit = { }) {
    private val inventories = ConcurrentHashMap<String, Inventory>()

    fun add(id: String,
            inventory: Inventory) {
        inventories.put(id, inventory)
    }

    fun accessUnsafe(id: String): Inventory {
        return inventories[id] ?: throw IllegalArgumentException(
                "Invalid inventory: $id")
    }

    fun <R> access(id: String,
                   consumer: (Inventory) -> R): R {
        inventories[id]?.let {
            return synchronized(it) {
                consumer(it)
            }
        }
        throw IllegalArgumentException("Unknown inventory")
    }

    fun <R> modify(id: String,
                   consumer: (Inventory) -> R): R {
        val output = access(id, consumer)
        update(id)
        return output
    }

    fun forEach(consumer: (Inventory) -> Unit) {
        inventories.entries.forEach { entry ->
            val inventory = entry.value
            synchronized(inventory) {
                consumer(inventory)
            }
        }
    }

    fun forEach(consumer: (String, Inventory) -> Unit) {
        inventories.entries.forEach { entry ->
            val id = entry.key
            val inventory = entry.value
            synchronized(inventory) {
                consumer(id, inventory)
            }
        }
    }

    fun forEachModify(consumer: (Inventory) -> Boolean) {
        inventories.entries.forEach { entry ->
            val id = entry.key
            val inventory = entry.value
            synchronized(inventory) {
                if (consumer(inventory)) {
                    update(id)
                }
            }
        }
    }

    fun forEachModify(consumer: (String, Inventory) -> Boolean) {
        inventories.entries.forEach { entry ->
            val id = entry.key
            val inventory = entry.value
            synchronized(inventory) {
                if (consumer(id, inventory)) {
                    update(id)
                }
            }
        }
    }

    fun update(id: String) {
        updateHandler(id)
    }
}
