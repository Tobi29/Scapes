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

import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.launch
import org.tobi29.coroutines.Timer
import org.tobi29.coroutines.loopUntilCancel
import org.tobi29.scapes.client.gui.GuiComponentItem
import org.tobi29.scapes.engine.gui.*
import org.tobi29.scapes.inventory.Item
import org.tobi29.scapes.inventory.ItemTypeNamed
import org.tobi29.scapes.inventory.kind
import org.tobi29.scapes.inventory.name
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
    private var updateJob: Job? = null

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

    override fun init() = updateVisible()

    override fun updateVisible() {
        synchronized(this) {
            dispose()
            if (!isVisible) return@synchronized
            updateJob = launch(engine.taskExecutor) {
                Timer().apply { init() }.loopUntilCancel(Timer.toDiff(1.0)) {
                    if (example == Int.MAX_VALUE) {
                        example = 0
                    } else {
                        example++
                    }
                }
            }
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
            updateVisible()
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
        init {
            val result = addHori(5.0, 5.0, 30.0, 30.0) {
                it.selectable = true
                GuiComponentResultButton(it, recipe.result)
            }
            addHori(5.0, 5.0, -1.0, 16.0) { GuiComponentFlowText(it, "<=") }
            recipe.ingredients.forEach { ingredient ->
                addHori(5.0, 5.0, 25.0, 25.0) {
                    GuiComponentIngredientButton(it) {
                        ingredient.example(example)
                    }
                }
            }
            if (recipe.requirements.isNotEmpty()) {
                addHori(5.0, 5.0, -1.0, 16.0) { GuiComponentFlowText(it, "+") }
            }
            recipe.requirements.forEach { requirement ->
                addHori(5.0, 5.0, 25.0, 25.0) {
                    GuiComponentIngredientButton(it) {
                        requirement.example(example)
                    }
                }
            }

            result.on(GuiEvent.CLICK_LEFT) {
                player.connection().send(
                        PacketCrafting(player.registry, recipe.id))
            }
        }
    }
}

sealed private class GuiComponentCraftingButton(
        parent: GuiLayoutData,
        item: () -> Item?
) : GuiComponentButton(parent) {
    val item = addSubHori(0.0, 0.0, -1.0, -1.0) {
        GuiComponentItem(it, item)
    }
}

private class GuiComponentIngredientButton(
        parent: GuiLayoutData,
        item: () -> Item?
) : GuiComponentCraftingButton(parent, item) {
    override fun tooltip(p: GuiContainerRow): (() -> Unit)? {
        val text = p.addVert(15.0, 15.0, -1.0, 16.0) {
            GuiComponentText(it, "")
        }
        return {
            text.text = item.item()?.let { item ->
                "Ingredient:\n${item.kind<ItemTypeNamed>()?.name ?: ""}"
            } ?: ""
        }
    }
}

private class GuiComponentRequirementButton(
        parent: GuiLayoutData,
        item: () -> Item?
) : GuiComponentCraftingButton(parent, item) {
    override fun tooltip(p: GuiContainerRow): (() -> Unit)? {
        val text = p.addVert(15.0, 15.0, -1.0, 16.0) {
            GuiComponentText(it, "")
        }
        return {
            text.text = item.item()?.let { item ->
                "Requirement:\n${item.kind<ItemTypeNamed>()?.name ?: ""}"
            } ?: ""
        }
    }
}

private class GuiComponentResultButton(
        parent: GuiLayoutData,
        item: Item?
) : GuiComponentCraftingButton(parent, { item }) {
    override fun tooltip(p: GuiContainerRow): (() -> Unit)? {
        val text = p.addVert(15.0, 15.0, -1.0, 16.0) {
            GuiComponentText(it, "")
        }
        return {
            text.text = item.item()?.let { item ->
                "Result:\n${item.kind<ItemTypeNamed>()?.name ?: ""}"
            } ?: ""
        }
    }
}
