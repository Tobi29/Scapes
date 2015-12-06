package org.tobi29.scapes.client.connection;

import java8.util.Maps;
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
        Maps.computeIfAbsentConcurrent(bytes, packet.getClass(),
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
