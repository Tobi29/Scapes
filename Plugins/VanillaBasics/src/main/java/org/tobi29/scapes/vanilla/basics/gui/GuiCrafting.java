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
package org.tobi29.scapes.vanilla.basics.gui;

import org.tobi29.scapes.block.CraftingRecipe;
import org.tobi29.scapes.block.CraftingRecipeType;
import org.tobi29.scapes.client.gui.GuiComponentItemButton;
import org.tobi29.scapes.engine.gui.*;
import org.tobi29.scapes.engine.utils.Pair;
import org.tobi29.scapes.packets.PacketCrafting;
import org.tobi29.scapes.vanilla.basics.entity.client.MobPlayerClientMainVB;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GuiCrafting extends GuiInventory {
    private final GuiComponentScrollPaneViewport scrollPaneTypes,
            scrollPaneRecipes;
    private final boolean table;
    private int type = -1, example;
    private long nextExample;

    public GuiCrafting(boolean table, MobPlayerClientMainVB player,
            GuiStyle style) {
        super("Crafting" + (table ? " Table" : ""), player, style);
        this.table = table;
        scrollPaneTypes =
                new GuiComponentScrollPaneList(pane, 16, 80, 90, 180, 20)
                        .viewport();
        scrollPaneRecipes =
                new GuiComponentScrollPaneList(pane, 116, 80, 268, 180, 40)
                        .viewport();
        updateTypes();
        updateRecipes();
    }

    private void updateTypes() {
        scrollPaneTypes.removeAll();
        List<CraftingRecipeType> recipeTypes;
        recipeTypes =
                player.connection().plugins().registry().getCraftingRecipes();
        int id = 0;
        List<Pair<CraftingRecipeType, Integer>> tableOnly = new ArrayList<>();
        for (CraftingRecipeType recipeType : recipeTypes) {
            if (recipeType.availableFor(player)) {
                boolean enabled = table || !recipeType.table();
                if (enabled) {
                    if (type < 0) {
                        type = id;
                    }
                    new ElementType(scrollPaneTypes, recipeType, id, true);
                } else {
                    tableOnly.add(new Pair<>(recipeType, id));
                }
            }
            id++;
        }
        if (!tableOnly.isEmpty()) {
            GuiComponentPane separator =
                    new GuiComponentPane(scrollPaneTypes, 0, 0, 90, 30);
            new GuiComponentText(separator, 5, 9, 12, "Table only:");
            tableOnly.forEach(
                    type -> new ElementType(scrollPaneTypes, type.a, type.b,
                            false));
        }
    }

    @Override
    public void updateComponent() {
        super.updateComponent();
        if (System.currentTimeMillis() > nextExample) {
            if (example == Integer.MAX_VALUE) {
                example = 0;
            } else {
                example++;
            }
            updateRecipes();
        }
    }

    private void updateRecipes() {
        scrollPaneRecipes.removeAll();
        CraftingRecipeType recipeType =
                player.connection().plugins().registry().getCraftingRecipes()
                        .get(type);
        int id = 0;
        for (CraftingRecipe recipe : recipeType.recipes()) {
            new Element(scrollPaneRecipes, recipe, type, id++);
        }
        nextExample = System.currentTimeMillis() + 1000;
    }

    private class ElementType extends GuiComponentPane {
        public ElementType(GuiComponent parent, CraftingRecipeType recipe,
                int id, boolean enabled) {
            super(parent, 0, 0, 80, 40);
            GuiComponentTextButton label =
                    new GuiComponentTextButton(this, 5, 5, 70, 15, 12,
                            recipe.name());
            if (enabled) {
                label.addLeftClick(event -> {
                    type = id;
                    example = 0;
                    updateRecipes();
                });
            }
        }
    }

    private class Element extends GuiComponentPane {
        public Element(GuiComponent parent, CraftingRecipe recipe, int type,
                int id) {
            super(parent, 0, 0, 268, 40);
            GuiComponentItemButton result =
                    new GuiComponentItemButton(this, 15, 5, 30, 30,
                            recipe.result());
            result.addLeftClick(event -> player.connection()
                    .send(new PacketCrafting(type, id)));
            result.addHover(event -> setTooltip(result.item(), "Result:\n"));
            new GuiComponentText(this, 47, 12, 16, "<=");
            int x = 70;
            Iterator<CraftingRecipe.Ingredient> ingredients =
                    recipe.ingredients().iterator();
            Iterator<CraftingRecipe.Ingredient> requirements =
                    recipe.requirements().iterator();
            if (ingredients.hasNext()) {
                while (ingredients.hasNext()) {
                    CraftingRecipe.Ingredient ingredient = ingredients.next();
                    GuiComponentItemButton b =
                            new GuiComponentItemButton(this, x, 7, 25, 25,
                                    ingredient.example(example));
                    b.addHover(event -> setTooltip(b.item(), "Ingredient:\n"));
                    x += 35;
                }
                if (requirements.hasNext()) {
                    new GuiComponentText(this, x + 2, 12, 16, "+");
                    x += 20;
                }
            }
            while (requirements.hasNext()) {
                CraftingRecipe.Ingredient requirement = requirements.next();
                GuiComponentItemButton b =
                        new GuiComponentItemButton(this, x, 7, 25, 25,
                                requirement.example(example));
                b.addHover(event -> setTooltip(b.item(), "Requirement:\n"));
                x += 35;
            }
        }
    }
}
