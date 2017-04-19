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
import org.tobi29.scapes.engine.utils.assert
import org.tobi29.scapes.engine.utils.readOnly
import org.tobi29.scapes.engine.utils.tag.*

open class Registries(private val idStorage: MutableTagMap) {
    private val registries = ConcurrentHashMap<Pair<String, String>, Registry<*>>()
    private var lockedTypes = false
    private var locked = false

    fun registryTypes(consumer: (RegistryAdder) -> Unit) {
        if (lockedTypes) {
            throw IllegalStateException("Early initializing already ended")
        }
        val registry = RegistryAdder()
        consumer(registry)
        lockedTypes = true
    }

    fun writeIDStorage() = idStorage.toTag()

    fun lock() {
        locked = true
    }

    operator fun <E : Any> get(module: String,
                               type: String): Registry<E> {
        if (!lockedTypes) {
            throw IllegalStateException("Early initializing not finished")
        }
        @Suppress("UNCHECKED_CAST")
        return registries[Pair(module, type)] as Registry<E>
    }

    inner class Registry<E : Any>(private val module: String,
                                  private val type: String,
                                  private val idSupplier: ReadWriteTagMutableMap.(String, Int?) -> Int) {
        private val objectsByStr = ConcurrentHashMap<String, E>()
        private val ids = ConcurrentHashMap<E, Int>()
        private val valuesMut = ArrayList<E?>()
        val values = valuesMut.readOnly()

        operator fun <T : E> invoke(name: String,
                                    block: (Int) -> T) = reg(name, block)

        operator fun <T : E> invoke(name: String,
                                    id: Int? = null,
                                    block: (Int) -> T) = reg(name, id, block)

        @Synchronized fun <T : E> reg(name: String,
                                      block: (Int) -> T) = reg(name, null,
                block)

        @Synchronized fun <T : E> reg(name: String,
                                      id: Int? = null,
                                      block: (Int) -> T): T {
            if (locked) {
                throw IllegalStateException("Initializing already ended")
            }
            val i = idSupplier(idStorage.mapMut(module).mapMut(type), name, id)
            assert { id == null || id == i }
            val element = block(i)
            objectsByStr.put(name, element)
            ids.put(element, i)
            while (valuesMut.size <= i) {
                valuesMut.add(null)
            }
            valuesMut[i] = element
            return element
        }

        fun values(): List<E?> {
            return valuesMut
        }

        operator fun get(id: Int) =
                (if (id >= valuesMut.size) {
                    null
                } else {
                    valuesMut[id]
                }) ?: throw IllegalArgumentException("Invalid id")

        operator fun get(id: String) = objectsByStr[id] ?: throw IllegalArgumentException(
                "Invalid id")

        operator fun get(instance: E): Int {
            return ids[instance] ?: throw IllegalArgumentException("$instance")
        }
    }

    inner class RegistryAdder {
        fun add(module: String,
                type: String,
                min: Int,
                max: Int) = add(module, type) { name, id ->
            idFromRange(min..max, name, id)
        }

        @Synchronized fun add(module: String,
                              type: String,
                              idSupplier: ReadWriteTagMutableMap.(String, Int?) -> Int) {
            if (lockedTypes) {
                throw IllegalStateException("Early initializing already ended")
            }
            val pair = Pair(module, type)
            var registry: Registry<*>? = registries[pair]
            if (registry == null) {
                registry = Registry<Any>(module, type, idSupplier)
                registries.put(pair, registry)
            }
        }
    }
}

fun ReadWriteTagMutableMap.idFromRange(range: IntRange,
                                       name: String,
                                       id: Int?): Int =
        getID(name, (id?.let { id..id } ?: range).asSequence())

fun ReadWriteTagMutableMap.getID(name: String,
                                 range: Sequence<Int>): Int {
    this[name]?.toInt()?.let { return it }
    val i = range.filter {
        !containsValue(it.toTag())
    }.firstOrNull() ?: throw IllegalStateException(
            "Overflowed IDs for: $name")
    this[name] = i
    return i
}
