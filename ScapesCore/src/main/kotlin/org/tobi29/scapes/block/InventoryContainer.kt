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

import org.tobi29.utils.ComponentType
import org.tobi29.scapes.entity.ComponentEventListenersType
import org.tobi29.scapes.entity.ComponentMapSerializable
import org.tobi29.scapes.entity.Entity
import org.tobi29.scapes.entity.fireEvent
import org.tobi29.scapes.inventory.Inventory
import org.tobi29.scapes.inventory.ItemType
import org.tobi29.scapes.plugins.Plugins
import org.tobi29.io.tag.ReadWriteTagMap
import org.tobi29.io.tag.TagMap
import org.tobi29.io.tag.toMap
import org.tobi29.stdex.ArrayDeque
import org.tobi29.stdex.ConcurrentHashMap
import org.tobi29.stdex.ThreadLocal
import org.tobi29.stdex.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantLock
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set
import kotlin.concurrent.withLock

class InventoryContainer(
        private val holder: Entity
) : ComponentMapSerializable {
    override val id = "Core:InventoryContainer"

    private val inventories = ConcurrentHashMap<String, Inventory>()

    private val holderId = HOLDER_ID.getAndIncrement()

    private val lock = ReentrantLock()

    fun add(id: String,
            size: Int) {
        lock.withLock {
            inventories.put(id, Inventory(size))
        }
    }

    fun <R> access(block: (Map<String, Inventory>) -> R): R {
        val locks = HELD_LOCKS.get()
        locks.forEach { it.lock.unlock() }
        locks.push(this)
        locks.sortedBy { it.holderId }.forEach { it.lock.lock() }
        try {
            return block(inventories)
        } finally {
            lock.unlock()
            locks.pop()
        }
    }

    fun accessUnsafe(id: String): Inventory =
            access {
                inventories[id] ?: throw IllegalArgumentException(
                        "Invalid inventory: $id")
            }

    fun <R> access(id: String,
                   consumer: (Inventory) -> R): R =
            access {
                consumer(inventories[id]
                        ?: throw IllegalArgumentException("Unknown inventory"))

            }

    fun <R> modify(id: String,
                   consumer: (Inventory) -> R): R =
            access {
                val output = access(id, consumer)
                update(id)
                output
            }

    fun forEach(consumer: (Inventory) -> Unit) {
        access {
            inventories.entries.forEach { entry ->
                val inventory = entry.value
                consumer(inventory)
            }
        }
    }

    fun forEach(consumer: (String, Inventory) -> Unit) {
        access {
            inventories.entries.forEach { entry ->
                val id = entry.key
                val inventory = entry.value
                consumer(id, inventory)
            }
        }
    }

    fun forEachModify(consumer: (Inventory) -> Boolean) {
        access {
            inventories.entries.forEach { entry ->
                val id = entry.key
                val inventory = entry.value
                if (consumer(inventory)) {
                    update(id)
                }
            }
        }
    }

    fun forEachModify(consumer: (String, Inventory) -> Boolean) {
        access {
            inventories.entries.forEach { entry ->
                val id = entry.key
                val inventory = entry.value
                if (consumer(id, inventory)) {
                    update(id)
                }
            }
        }
    }

    fun update(id: String) {
        access { holder[ON_UPDATE].fireEvent(id) }
    }

    override fun write(map: ReadWriteTagMap) {
        lock.withLock {
            inventories.forEach { (id, inventory) ->
                map[id] = TagMap { inventory.write(this) }
            }
        }
    }

    override fun read(map: TagMap) {
        lock.withLock {
            inventories.forEach { (id, inventory) ->
                map[id]?.toMap()?.let {
                    inventory.read(holder.world.plugins, it)
                }
            }
        }
    }

    companion object {
        val COMPONENT = ComponentType.of<Entity, InventoryContainer, Any> {
            InventoryContainer(it)
        }

        val ON_UPDATE = ComponentEventListenersType<Entity, String>()
    }
}

inline val Entity.inventories: InventoryContainer
    get() = this[InventoryContainer.COMPONENT]

fun Inventory.read(plugins: Plugins,
                   map: TagMap) =
        read({ plugins.registry.get<ItemType>("Core", "ItemType")[it] }, map)

private val HOLDER_ID = AtomicLong(Long.MIN_VALUE)

private val HELD_LOCKS = ThreadLocal { ArrayDeque<InventoryContainer>() }
