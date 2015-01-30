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

package org.tobi29.scapes.client.gui;

import org.tobi29.scapes.block.CraftingRecipe;
import org.tobi29.scapes.block.CraftingRecipeType;
import org.tobi29.scapes.engine.gui.GuiComponentPane;
import org.tobi29.scapes.engine.gui.GuiComponentScrollPaneList;
import org.tobi29.scapes.engine.gui.GuiComponentText;
import org.tobi29.scapes.engine.gui.GuiComponentTextButton;
import org.tobi29.scapes.entity.client.MobPlayerClientMain;
import org.tobi29.scapes.packets.PacketCrafting;

import java.util.List;

public class GuiCrafting extends GuiInventory {
    private final GuiComponentScrollPaneList scrollPaneTypes, scrollPaneRecipes;
    private final boolean table;
    private int type;

    public GuiCrafting(boolean table, MobPlayerClientMain player) {
        super("Crafting" + (table ? " Table" : ""), player);
        this.table = table;
        scrollPaneTypes = new GuiComponentScrollPaneList(16, 80, 90, 180, 40);
        scrollPaneRecipes =
                new GuiComponentScrollPaneList(106, 80, 278, 180, 40);
        pane.add(scrollPaneTypes);
        pane.add(scrollPaneRecipes);
        updateTypes();
        updateRecipes();
    }

    private void updateTypes() {
        scrollPaneTypes.removeAll();
        List<CraftingRecipeType> recipeTypes;
        recipeTypes = player.getConnection().getPlugins().getRegistry()
                .getCraftingRecipes(table);
        int id = 0;
        for (CraftingRecipeType recipeType : recipeTypes) {
            if (recipeType.availableFor(player)) {
                ElementType type = new ElementType(recipeType, id);
                scrollPaneTypes.add(type);
            }
            id++;
        }
    }

    private void updateRecipes() {
        scrollPaneRecipes.removeAll();
        CraftingRecipeType recipeType =
                player.getConnection().getPlugins().getRegistry()
                        .getCraftingRecipes(table).get(type);
        int id = 0;
        for (CraftingRecipe recipe : recipeType.getRecipes()) {
            Element element = new Element(recipe, type, id++);
            scrollPaneRecipes.add(element);
        }
    }

    private class ElementType extends GuiComponentPane {
        public ElementType(CraftingRecipeType recipe, int id) {
            super(0, 0, 378, 40);
            GuiComponentTextButton label =
                    new GuiComponentTextButton(0, 5, 80, 30, 18,
                            recipe.getName());
            label.addLeftClick(event -> {
                type = id;
                updateRecipes();
            });
            add(label);
        }
    }

    private class Element extends GuiComponentPane {
        public Element(CraftingRecipe recipe, int type, int id) {
            super(0, 0, 378, 40);
            GuiComponentItemButton result =
                    new GuiComponentItemButton(15, 5, 30, 30,
                            recipe.getResult());
            result.addLeftClick(event -> player.getConnection()
                    .send(new PacketCrafting(type, id, table)));
            result.addHover(event -> setTooltip(result.getItem(), "Result:\n"));
            add(result);
            add(new GuiComponentText(47, 12, 16, "<="));
            int x = 70;
            List<CraftingRecipe.Ingredient> ingredients =
                    recipe.getIngredients();
            List<CraftingRecipe.Ingredient> requirements =
                    recipe.getRequirements();
            if (!ingredients.isEmpty()) {
                for (CraftingRecipe.Ingredient ingredient : recipe
                        .getIngredients()) {
                    GuiComponentItemButton b =
                            new GuiComponentItemButton(x, 7, 25, 25,
                                    ingredient.example(0));
                    b.addHover(
                            event -> setTooltip(b.getItem(), "Ingredient:\n"));
                    add(b);
                    x += 35;
                }
                if (!requirements.isEmpty()) {
                    add(new GuiComponentText(x + 2, 12, 16, "+"));
                    x += 20;
                }
            }
            for (CraftingRecipe.Ingredient requirement : recipe
                    .getRequirements()) {
                GuiComponentItemButton b =
                        new GuiComponentItemButton(x, 7, 25, 25,
                                requirement.example(0));
                b.addHover(event -> setTooltip(b.getItem(), "Requirement:\n"));
                add(b);
                x += 35;
            }
        }
    }
}
