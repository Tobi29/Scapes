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
package org.tobi29.scapes.client.connection

import java8.util.concurrent.ConcurrentMaps
import java8.util.stream.Stream
import org.tobi29.scapes.engine.utils.stream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class ConnectionProfiler {
    private val bytes = ConcurrentHashMap<Class<*>, AtomicLong>()

    fun packet(packet: Any,
               size: Long) {
        ConcurrentMaps.computeIfAbsent(bytes, packet.javaClass
        ) { key -> AtomicLong() }.addAndGet(size)
    }

    fun entries(): Stream<Pair<Class<*>, Long>> {
        return bytes.entries.stream().map { entry ->
            Pair(entry.key, entry.value.get())
        }
    }

    fun clear() {
        bytes.clear()
    }
}
