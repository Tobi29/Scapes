/*
 * Copyright 2012-2015 Tobi29
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

import java8.util.Optional;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;

import java.util.ArrayList;
import java.util.List;

public class Inventory {
    protected final GameRegistry registry;
    protected final ItemStack[] items;

    public Inventory(GameRegistry registry, int size) {
        this.registry = registry;
        items = new ItemStack[size];
        for (int i = 0; i < items.length; i++) {
            items[i] = new ItemStack(registry);
        }
    }

    public int add(ItemStack add) {
        if (add == null) {
            throw new IllegalArgumentException("Item cannot be null!");
        }
        for (ItemStack item : items) {
            if (item.canStack(add) > 0 && item.material() != registry.air()) {
                add.setAmount(add.amount() - item.stack(add));
                if (add.amount() <= 0) {
                    return add.amount();
                }
            }
        }
        for (ItemStack item : items) {
            add.setAmount(add.amount() - item.stack(add));
            if (add.amount() <= 0) {
                return add.amount();
            }
        }
        return add.amount();
    }

    public int canAdd(ItemStack add) {
        if (add == null) {
            throw new IllegalArgumentException("Item cannot be null!");
        }
        int hasToStack = add.amount();
        for (ItemStack item : items) {
            hasToStack -= item.canStack(add);
            if (hasToStack <= 0) {
                return add.amount();
            }
        }
        return add.amount() - hasToStack;
    }

    public boolean canTake(ItemStack take) {
        if (take == null) {
            throw new IllegalArgumentException("Item cannot be null!");
        }
        int amount = 0;
        for (ItemStack item : items) {
            amount += item.canTake(take);
        }
        return amount >= take.amount();
    }

    public ItemStack item(int id) {
        return items[id];
    }

    public int size() {
        return items.length;
    }

    public ItemStack take(ItemStack take) {
        if (take == null) {
            throw new IllegalArgumentException("Item cannot be null!");
        }
        ItemStack give = null;
        int amount = take.amount();
        for (int i = 0; i < items.length && amount > 0; i++) {
            Optional<ItemStack> give2 = items[i].take(take, amount);
            if (give2.isPresent()) {
                ItemStack item = give2.get();
                amount -= item.amount();
                if (give == null) {
                    give = item;
                } else {
                    give.stack(item);
                }
            }
        }
        return give;
    }

    public void clear() {
        for (ItemStack item : items) {
            item.setAmount(0);
        }
    }

    public void load(TagStructure tag) {
        List<TagStructure> list = tag.getList("Items");
        for (int i = 0; i < list.size(); i++) {
            items[i].load(list.get(i));
        }
    }

    public TagStructure save() {
        TagStructure tag = new TagStructure();
        List<TagStructure> list = new ArrayList<>();
        for (int i = 0; i < items.length; i++) {
            list.add(i, items[i].save());
        }
        tag.setList("Items", list);
        return tag;
    }
}
