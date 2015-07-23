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

import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.math.FastMath;

import java.util.Optional;

public class ItemStack {
    private final GameRegistry registry;
    private int amount;
    private int data;
    private Material material;
    private TagStructure metaData;

    public ItemStack(ItemStack item) {
        this(item.material, item.data, item.amount, item.metaData.copy());
    }

    public ItemStack(GameRegistry registry) {
        this(registry.air(), 0, 0);
    }

    public ItemStack(Material material, int data, int amount,
            TagStructure metaData) {
        registry = material.registry();
        this.material = material;
        this.data = data;
        this.amount = amount;
        this.metaData = metaData;
    }

    public ItemStack(Material material, int data) {
        this(material, data, 1);
    }

    public ItemStack(Material material, int data, int amount) {
        this(material, data, amount, new TagStructure());
    }

    public ItemStack(Material material, int data, TagStructure metaData) {
        this(material, data, 1, metaData);
    }

    public int amount() {
        return amount;
    }

    public ItemStack setAmount(int amount) {
        this.amount = amount;
        if (this.amount <= 0 || material == registry.air()) {
            metaData = new TagStructure();
            this.amount = 0;
            material = registry.air();
        }
        return this;
    }

    public int data() {
        return data;
    }

    public ItemStack setData(int data) {
        this.data = data;
        return this;
    }

    public Material material() {
        return material;
    }

    public ItemStack setMaterial(Material material) {
        if (material == null) {
            material = registry.air();
        }
        this.material = material;
        return this;
    }

    public int canStack(ItemStack add) {
        return canStack(add, FastMath.min(
                FastMath.min(material.maxStackSize(this),
                        add.material.maxStackSize(add)) - amount, add.amount));
    }

    public int canStack(ItemStack add, int amount) {
        if ((add.material != material || add.data != data ||
                amount + this.amount > FastMath.min(material.maxStackSize(this),
                        add.material.maxStackSize(add))) &&
                material != registry.air() && this.amount > 0) {
            return 0;
        }
        return amount;
    }

    public int canTake(ItemStack take) {
        return canTake(take, take.amount);
    }

    public int canTake(ItemStack take, int amount) {
        if (take.material != material ||
                take.data != data || material == registry.air() ||
                this.amount <= 0) {
            return 0;
        }
        return FastMath.min(this.amount, amount);
    }

    public void load(TagStructure tag) {
        material = registry.material(tag.getInteger("Type"))
                .orElse(registry.air());
        data = tag.getShort("Data");
        amount = tag.getInteger("Amount");
        metaData = tag.getStructure("MetaData");
    }

    public TagStructure save() {
        TagStructure tag = new TagStructure();
        tag.setInteger("Type", material.itemID());
        tag.setInteger("Data", data);
        tag.setInteger("Amount", amount);
        tag.setStructure("MetaData", metaData);
        return tag;
    }

    public TagStructure metaData(String category) {
        return metaData.getStructure(category);
    }

    public void setMetaData(String category, TagStructure metaData) {
        this.metaData.setStructure(category, metaData);
    }

    public int stack(ItemStack add) {
        return stack(add, FastMath.min(FastMath.min(material.maxStackSize(this),
                add.material.maxStackSize(add)) - amount, add.amount));
    }

    public int stack(ItemStack add, int amount) {
        if ((add.material != material || add.data != data ||
                amount + this.amount > FastMath.min(material.maxStackSize(this),
                        add.material.maxStackSize(add))) &&
                material != registry.air() && this.amount > 0) {
            return 0;
        }
        material = add.material;
        data = add.data;
        if (this.amount == 0) {
            metaData = add.metaData.copy();
        }
        this.amount += amount;
        return amount;
    }

    public Optional<ItemStack> take(ItemStack take) {
        return take(take, take.amount);
    }

    public Optional<ItemStack> take(ItemStack take, int amount) {
        if (take.material != material ||
                take.data != data || material == registry.air() ||
                this.amount <= 0) {
            return Optional.empty();
        }
        ItemStack give = new ItemStack(this);
        give.setAmount(FastMath.min(this.amount, amount));
        this.amount -= give.amount;
        if (this.amount <= 0) {
            metaData = new TagStructure();
            this.amount = 0;
            material = registry.air();
        }
        return Optional.of(give);
    }

    public Optional<ItemStack> take(int amount) {
        if (material == registry.air() || this.amount <= 0) {
            return Optional.empty();
        }
        ItemStack give = new ItemStack(this);
        give.setAmount(amount);
        this.amount -= amount;
        if (this.amount <= 0 || material == registry.air()) {
            metaData = new TagStructure();
            this.amount = 0;
            material = registry.air();
        }
        return Optional.of(give);
    }

    public boolean isEmpty() {
        return amount <= 0 || material == registry.air();
    }

    public String name() {
        return material.name(this);
    }
}
