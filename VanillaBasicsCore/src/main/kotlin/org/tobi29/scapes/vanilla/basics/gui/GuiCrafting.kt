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

package org.tobi29.scapes.vanilla.basics.gui

import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.client.gui.GuiComponentItem
import org.tobi29.scapes.engine.gui.*
import org.tobi29.scapes.vanilla.basics.VanillaBasics
import org.tobi29.scapes.vanilla.basics.entity.client.MobPlayerClientMainVB
import org.tobi29.scapes.vanilla.basics.material.CraftingRecipe
import org.tobi29.scapes.vanilla.basics.material.CraftingRecipeType
import org.tobi29.scapes.vanilla.basics.packet.PacketCrafting

class GuiCrafting(private val table: Boolean,
                  player: MobPlayerClientMainVB,
                  style: GuiStyle) : GuiInventory(
        "Crafting" + if (table) " Table" else "", player, style) {
    private val scrollPaneTypes: GuiComponentScrollPaneViewport
    private val scrollPaneRecipes: GuiComponentScrollPaneViewport
    private var elements = emptyList<Element>()
    private var currentType: CraftingRecipeType? = null
    private var example = 0
    private var nextExample = 0L

    init {
        val columns = topPane.addVert(11.0, 0.0, -1.0, -1.0,
                ::GuiComponentGroupSlab)
        scrollPaneTypes = columns.addHori(5.0, 5.0, -0.3, -1.0) {
            GuiComponentScrollPane(it, 20)
        }.viewport
        scrollPaneRecipes = columns.addHori(5.0, 5.0, -1.0, -1.0) {
            GuiComponentScrollPane(it, 40)
        }.viewport
        updateTypes()
        updateRecipes()
    }

    private fun updateTypes() {
        currentType = null
        scrollPaneTypes.removeAll()
        val plugin = player.connection().plugins.plugin(
                "VanillaBasics") as VanillaBasics
        val tableOnly = ArrayList<CraftingRecipeType>()
        plugin.craftingRecipes.forEach { recipeType ->
            if (recipeType.availableFor(player)) {
                val enabled = table || !recipeType.table()
                if (enabled) {
                    if (currentType == null) {
                        currentType = recipeType
                    }
                    scrollPaneTypes.addVert(0.0, 0.0, -1.0, 20.0
                    ) { ElementType(it, recipeType, true) }
                } else {
                    tableOnly.add(recipeType)
                }
            }
        }
        if (!tableOnly.isEmpty()) {
            val separator = scrollPaneTypes.addVert(0.0, 0.0, -1.0, 30.0,
                    ::GuiComponentGroup)
            separator.add(5.0, 9.0, -1.0, 12.0) {
                GuiComponentText(it, "Table only:")
            }
            tableOnly.forEach { type ->
                scrollPaneTypes.addVert(0.0, 0.0, -1.0, 20.0) {
                    ElementType(it, type, false)
                }
            }
        }
    }

    override fun updateComponent(delta: Double) {
        super.updateComponent(delta)
        if (System.currentTimeMillis() > nextExample) {
            if (example == Int.MAX_VALUE) {
                example = 0
            } else {
                example++
            }
            elements.forEach { it.examples() }
            nextExample = System.currentTimeMillis() + 1000
        }
    }

    private fun updateRecipes() {
        val elements = ArrayList<Element>()
        scrollPaneRecipes.removeAll()
        currentType?.let { recipeType ->
            recipeType.recipes.forEach { recipe ->
                elements.add(scrollPaneRecipes.addVert(0.0, 0.0, -1.0, 40.0) {
                    Element(it, recipe)
                })
            }
            this.elements = elements
            nextExample = System.currentTimeMillis() + 1000
        }
    }

    private inner class ElementType(parent: GuiLayoutData,
                                    recipeType: CraftingRecipeType,
                                    enabled: Boolean) : GuiComponentGroupSlab(
            parent) {
        init {
            val label = addHori(5.0, 2.0, -1.0, -1.0) {
                button(it, 12, recipeType.name())
            }
            if (enabled) {
                label.on(GuiEvent.CLICK_LEFT) {
                    currentType = recipeType
                    example = 0
                    updateRecipes()
                }
            }
        }
    }

    private inner class Element(parent: GuiLayoutData,
                                recipe: CraftingRecipe) : GuiComponentGroupSlab(
            parent) {
        private val examples = ArrayList<() -> Unit>()

        init {
            val result = addHori(5.0, 5.0, 30.0, 30.0) {
                it.selectable = true
                GuiComponentResultButton(it, recipe.result())
            }
            addHori(5.0, 5.0, -1.0, 16.0) { GuiComponentFlowText(it, "<=") }
            recipe.ingredients.forEach { ingredient ->
                val b = addHori(5.0, 5.0, 25.0, 25.0) {
                    GuiComponentIngredientButton(it,
                            ingredient.example(example))
                }
                examples.add { b.item.setItem(ingredient.example(example)) }
            }
            if (recipe.requirements.isNotEmpty()) {
                addHori(5.0, 5.0, -1.0, 16.0) { GuiComponentFlowText(it, "+") }
            }
            recipe.requirements.forEach { requirement ->
                val b = addHori(5.0, 5.0, 25.0, 25.0) {
                    GuiComponentRequirementButton(it,
                            requirement.example(example))
                }
                examples.add { b.item.setItem(requirement.example(example)) }
            }

            result.on(GuiEvent.CLICK_LEFT) {
                player.connection().send(
                        PacketCrafting(player.registry, recipe.id))
            }
        }

        fun examples() {
            examples.forEach { it() }
        }
    }
}

sealed private class GuiComponentCraftingButton(
        parent: GuiLayoutData,
        item: ItemStack
) : GuiComponentButton(parent) {
    val item = addSubHori(0.0, 0.0, -1.0, -1.0) {
        GuiComponentItem(it, item)
    }
}

private class GuiComponentIngredientButton(
        parent: GuiLayoutData,
        item: ItemStack
) : GuiComponentCraftingButton(parent, item) {
    override fun tooltip(p: GuiContainerRow): (() -> Unit)? {
        val text = p.addVert(15.0, 15.0, -1.0, 16.0) {
            GuiComponentText(it, "")
        }
        return {
            text.text = "Ingredient:\n${item.item().name()}"
        }
    }
}

private class GuiComponentRequirementButton(
        parent: GuiLayoutData,
        item: ItemStack
) : GuiComponentCraftingButton(parent, item) {
    override fun tooltip(p: GuiContainerRow): (() -> Unit)? {
        val text = p.addVert(15.0, 15.0, -1.0, 16.0) {
            GuiComponentText(it, "")
        }
        return {
            text.text = "Requirement:\n${item.item().name()}"
        }
    }
}

private class GuiComponentResultButton(
        parent: GuiLayoutData,
        item: ItemStack
) : GuiComponentCraftingButton(parent, item) {
    override fun tooltip(p: GuiContainerRow): (() -> Unit)? {
        val text = p.addVert(15.0, 15.0, -1.0, 16.0) {
            GuiComponentText(it, "")
        }
        return {
            text.text = "Result:\n${item.item().name()}"
        }
    }
}
