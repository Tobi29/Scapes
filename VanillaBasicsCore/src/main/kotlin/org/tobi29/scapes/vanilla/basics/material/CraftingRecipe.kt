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

import org.tobi29.scapes.block.Inventory
import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.block.Registries
import org.tobi29.scapes.engine.utils.readOnly
import java.util.*

class CraftingRecipe(val id: Int,
                     ingredients: List<CraftingRecipe.Ingredient>,
                     requirements: List<CraftingRecipe.Ingredient>,
                     private val result: ItemStack) {
    val ingredients = ingredients.readOnly()
    val requirements = requirements.readOnly()

    fun takes(inventory: Inventory): List<ItemStack>? {
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
        return takes
    }

    fun result(): ItemStack {
        return ItemStack(result)
    }

    interface Ingredient {
        fun match(inventory: Inventory): ItemStack?

        fun example(i: Int): ItemStack
    }

    class IngredientList(private val variations: List<ItemStack>) : Ingredient {

        constructor(vararg variations: ItemStack) : this(
                listOf(*variations))

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
        operator fun get(registry: Registries,
                         data: Int): CraftingRecipe {
            return registry.get<CraftingRecipe>("VanillaBasics",
                    "CraftingRecipe")[data]
        }
    }
}
