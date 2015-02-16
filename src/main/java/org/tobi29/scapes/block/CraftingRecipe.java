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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CraftingRecipe {
    private final List<Ingredient> ingredients, requirements;
    private final ItemStack result;

    public CraftingRecipe(ItemStack result, Ingredient... ingredients) {
        this(Arrays.asList(ingredients), result);
    }

    public CraftingRecipe(List<Ingredient> ingredients, ItemStack result) {
        this(ingredients, Collections.emptyList(), result);
    }

    public CraftingRecipe(List<Ingredient> ingredients,
            List<Ingredient> requirements, ItemStack result) {
        this.ingredients = ingredients;
        this.requirements = requirements;
        this.result = result;
    }

    public List<Ingredient> getIngredients() {
        return ingredients;
    }

    public List<Ingredient> getRequirements() {
        return requirements;
    }

    public List<ItemStack> getTakes(Inventory inventory) {
        List<ItemStack> takes = new ArrayList<>();
        for (Ingredient ingredient : ingredients) {
            ItemStack take = ingredient.match(inventory);
            if (take == null) {
                return null;
            } else {
                takes.add(take);
            }
        }
        for (Ingredient requirement : requirements) {
            ItemStack take = requirement.match(inventory);
            if (take == null) {
                return null;
            }
        }
        return takes;
    }

    public ItemStack getResult() {
        return new ItemStack(result);
    }

    public interface Ingredient {
        ItemStack match(Inventory inventory);

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
        public ItemStack match(Inventory inventory) {
            for (ItemStack variant : variations) {
                if (inventory.canTake(variant)) {
                    return variant;
                }
            }
            return null;
        }

        @Override
        public ItemStack example(int i) {
            return variations.get(i % variations.size());
        }
    }
}
