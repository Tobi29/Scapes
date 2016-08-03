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

package org.tobi29.scapes.block;

import java8.util.function.*;
import org.tobi29.scapes.engine.utils.Streams;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InventoryContainer {
    private final Map<String, Inventory> inventories =
            new ConcurrentHashMap<>();
    private final Consumer<String> updateHandler;

    public InventoryContainer() {
        this(id -> {
        });
    }

    public InventoryContainer(Consumer<String> updateHandler) {
        this.updateHandler = updateHandler;
    }

    public void add(String id, Inventory inventory) {
        inventories.put(id, inventory);
    }

    public Inventory accessUnsafe(String id) {
        return inventories.get(id);
    }

    @SuppressWarnings("ReturnOfNull")
    public void access(String id, Consumer<Inventory> consumer) {
        accessReturn(id, inventory -> {
            consumer.accept(inventory);
            return null;
        });
    }

    public <R> R accessReturn(String id, Function<Inventory, R> consumer) {
        Inventory inventory = inventories.get(id);
        synchronized (inventory) {
            return consumer.apply(inventory);
        }
    }

    public void modify(String id, Consumer<Inventory> consumer) {
        access(id, consumer);
        update(id);
    }

    public <R> R modifyReturn(String id, Function<Inventory, R> consumer) {
        R output = accessReturn(id, consumer);
        update(id);
        return output;
    }

    public void forEach(Consumer<Inventory> consumer) {
        Streams.forEach(inventories.entrySet(), entry -> {
            Inventory inventory = entry.getValue();
            synchronized (inventory) {
                consumer.accept(inventory);
            }
        });
    }

    public void forEach(BiConsumer<String, Inventory> consumer) {
        Streams.forEach(inventories.entrySet(), entry -> {
            String id = entry.getKey();
            Inventory inventory = entry.getValue();
            synchronized (inventory) {
                consumer.accept(id, inventory);
            }
        });
    }

    public void forEachModify(Predicate<Inventory> consumer) {
        Streams.forEach(inventories.entrySet(), entry -> {
            String id = entry.getKey();
            Inventory inventory = entry.getValue();
            synchronized (inventory) {
                if (consumer.test(inventory)) {
                    update(id);
                }
            }
        });
    }

    public void forEachModify(BiPredicate<String, Inventory> consumer) {
        Streams.forEach(inventories.entrySet(), entry -> {
            String id = entry.getKey();
            Inventory inventory = entry.getValue();
            synchronized (inventory) {
                if (consumer.test(id, inventory)) {
                    update(id);
                }
            }
        });
    }

    public void update(String id) {
        updateHandler.accept(id);
    }
}
