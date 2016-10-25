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

package org.tobi29.scapes.vanilla.basics.material

import java8.util.stream.Stream
import org.tobi29.scapes.block.GameRegistry
import org.tobi29.scapes.block.Inventory
import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.engine.utils.stream
import java.util.*

class CraftingRecipe(private val ingredients: List<CraftingRecipe.Ingredient>,
                     private val requirements: List<CraftingRecipe.Ingredient>, private val result: ItemStack) {

    fun ingredients(): Stream<Ingredient> {
        return ingredients.stream()
    }

    fun requirements(): Stream<Ingredient> {
        return requirements.stream()
    }

    fun takes(inventory: Inventory): Stream<ItemStack>? {
        var inventory = inventory
        inventory = Inventory(inventory)
        val takes = ArrayList<ItemStack>()
        for (ingredient in ingredients) {
            val take = ingredient.match(inventory)
            if (take != null) {
                takes.add(take)
                inventory.take(take)
            } else {
                return null
            }
        }
        for (requirement in requirements) {
            if (requirement.match(inventory) == null) {
                return null
            }
        }
        return takes.stream()
    }

    fun result(): ItemStack {
        return ItemStack(result)
    }

    fun data(registry: GameRegistry): Int {
        return registry.get<Any>("VanillaBasics", "CraftingRecipe")[this]
    }

    interface Ingredient {
        fun match(inventory: Inventory): ItemStack?

        fun example(i: Int): ItemStack
    }

    class IngredientList(private val variations: List<ItemStack>) : Ingredient {

        constructor(vararg variations: ItemStack) : this(
                Arrays.asList(*variations)) {
        }

        override fun match(inventory: Inventory): ItemStack? {
            for (variant in variations) {
                if (inventory.canTake(variant)) {
                    return variant
                }
            }
            return null
        }

        override fun example(i: Int): ItemStack {
            return variations[i % variations.size]
        }
    }

    companion object {

        operator fun get(registry: GameRegistry,
                         data: Int): CraftingRecipe {
            return registry.get<CraftingRecipe>("VanillaBasics",
                    "CraftingRecipe")[data]
        }
    }
}