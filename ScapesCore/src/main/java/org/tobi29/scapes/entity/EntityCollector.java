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
package org.tobi29.scapes.entity;

import java8.util.function.Consumer;
import java8.util.function.IntFunction;
import java8.util.stream.Stream;
import org.tobi29.scapes.engine.utils.Streams;
import org.tobi29.scapes.engine.utils.ThreadLocalUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EntityCollector {
    private static final ThreadLocal<Set<Object>> ENTITY_SET =
            ThreadLocalUtil.of(HashSet::new);

    public static <E> Stream<E> entities(
            Consumer<Consumer<Stream<? extends E>>> consumer) {
        return Streams.of(entityList(consumer));
    }

    @SuppressWarnings("unchecked")
    public static <E> List<E> entityList(
            Consumer<Consumer<Stream<? extends E>>> consumer) {
        Set<Object> entities = ENTITY_SET.get();
        consumer.accept(stream -> stream.forEach(entities::add));
        List<E> list = new ArrayList<>(entities.size());
        Streams.forEach(entities, entity -> list.add((E) entity));
        entities.clear();
        return list;
    }

    public static <E> Stream<E> entities(Consumer<Consumer<Stream<E>>> consumer,
            IntFunction<E[]> arraySupplier) {
        return Streams.of(entityList(consumer, arraySupplier));
    }

    @SuppressWarnings("SuspiciousToArrayCall")
    public static <E> E[] entityList(Consumer<Consumer<Stream<E>>> consumer,
            IntFunction<E[]> arraySupplier) {
        Set<Object> entities = ENTITY_SET.get();
        consumer.accept(stream -> stream.forEach(entities::add));
        E[] array = arraySupplier.apply(entities.size());
        entities.toArray(array);
        entities.clear();
        return array;
    }
}
