/*
 * Copyright 2012-2017 Tobi29
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

import org.tobi29.scapes.block.Registries
import org.tobi29.stdex.readOnly
import org.tobi29.scapes.inventory.Inventory
import org.tobi29.scapes.inventory.Item

class CraftingRecipe(val id: Int,
                     ingredients: List<CraftingRecipe.Ingredient>,
                     requirements: List<CraftingRecipe.Ingredient>,
                     val result: Item?) {
    val ingredients = ingredients.readOnly()
    val requirements = requirements.readOnly()

    fun takes(inventory: Inventory): List<Item>? {
        val inventory = Inventory(inventory)
        val takes = ArrayList<Item>()
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
        return takes
    }

    interface Ingredient {
        fun match(inventory: Inventory): Item?

        fun example(i: Int): Item
    }

    class IngredientList(private val variations: List<Item>) : Ingredient {

        constructor(vararg variations: Item) : this(variations.asList())

        override fun match(inventory: Inventory): Item? {
            for (variant in variations) {
                if (inventory.canTakeAll(variant)) {
                    return variant
                }
            }
            return null
        }

        override fun example(i: Int): Item {
            return variations[i % variations.size]
        }
    }

    class IngredientPredicate(private val predicate: (Item) -> Boolean,
                              val examples: List<Item>) : Ingredient {
        override fun match(inventory: Inventory): Item? {
            for (i in 0 until inventory.size()) {
                val item = inventory[i] ?: continue
                if (predicate(item)) return item
            }
            return null
        }

        override fun example(i: Int): Item {
            return examples[i % examples.size]
        }
    }

    companion object {
        operator fun get(registry: Registries,
                         data: Int): CraftingRecipe {
            return registry.get<CraftingRecipe>("VanillaBasics",
                    "CraftingRecipe")[data]
        }
    }
}
