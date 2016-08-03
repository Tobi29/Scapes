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

package org.tobi29.scapes.client.connection;

import java8.util.concurrent.ConcurrentMaps;
import java8.util.stream.Stream;
import org.tobi29.scapes.engine.utils.Pair;
import org.tobi29.scapes.engine.utils.Streams;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

public class ConnectionProfiler {
    private final ConcurrentMap<Class<?>, AtomicLong> bytes =
            new ConcurrentHashMap<>();

    public void packet(Object packet, long size) {
        ConcurrentMaps.computeIfAbsent(bytes, packet.getClass(),
                key -> new AtomicLong()).addAndGet(size);
    }

    public Stream<Pair<Class<?>, Long>> entries() {
        return Streams.of(bytes.entrySet())
                .map(entry -> new Pair<>(entry.getKey(),
                        entry.getValue().get()));
    }

    public void clear() {
        bytes.clear();
    }
}
