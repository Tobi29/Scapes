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

package org.tobi29.scapes.vanilla.basics.material;

import java8.util.Optional;
import java8.util.stream.Stream;
import org.tobi29.scapes.block.GameRegistry;
import org.tobi29.scapes.block.Inventory;
import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.engine.utils.Streams;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CraftingRecipe {
    private final List<Ingredient> ingredients, requirements;
    private final ItemStack result;

    public CraftingRecipe(List<Ingredient> ingredients,
            List<Ingredient> requirements, ItemStack result) {
        this.ingredients = ingredients;
        this.requirements = requirements;
        this.result = result;
    }

    public static CraftingRecipe get(GameRegistry registry, int data) {
        return registry.<CraftingRecipe>get("VanillaBasics", "CraftingRecipe")
                .get(data);
    }

    public Stream<Ingredient> ingredients() {
        return Streams.of(ingredients);
    }

    public Stream<Ingredient> requirements() {
        return Streams.of(requirements);
    }

    public Optional<Stream<ItemStack>> takes(Inventory inventory) {
        inventory = new Inventory(inventory);
        List<ItemStack> takes = new ArrayList<>();
        for (Ingredient ingredient : ingredients) {
            Optional<ItemStack> take = ingredient.match(inventory);
            if (take.isPresent()) {
                takes.add(take.get());
                inventory.take(take.get());
            } else {
                return Optional.empty();
            }
        }
        for (Ingredient requirement : requirements) {
            Optional<ItemStack> take = requirement.match(inventory);
            if (!take.isPresent()) {
                return Optional.empty();
            }
        }
        return Optional.of(Streams.of(takes));
    }

    public ItemStack result() {
        return new ItemStack(result);
    }

    public int data(GameRegistry registry) {
        return registry.get("VanillaBasics", "CraftingRecipe").get(this);
    }

    public interface Ingredient {
        Optional<ItemStack> match(Inventory inventory);

        ItemStack example(int i);
    }

    public static class IngredientList implements Ingredient {
        private final List<ItemStack> variations;

        public IngredientList(ItemStack... variations) {
            this(Arrays.asList(variations));
        }

        public IngredientList(List<ItemStack> variations) {
            this.variations = variations;
        }

        @Override
        public Optional<ItemStack> match(Inventory inventory) {
            for (ItemStack variant : variations) {
                if (inventory.canTake(variant)) {
                    return Optional.of(variant);
                }
            }
            return Optional.empty();
        }

        @Override
        public ItemStack example(int i) {
            return variations.get(i % variations.size());
        }
    }
}
