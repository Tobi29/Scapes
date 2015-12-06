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
import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.gui.*;
import org.tobi29.scapes.engine.utils.Pair;
import org.tobi29.scapes.engine.utils.Streams;
import org.tobi29.scapes.packets.PacketCrafting;
import org.tobi29.scapes.vanilla.basics.entity.client.MobPlayerClientMainVB;

import java.util.ArrayList;
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
        scrollPaneTypes = pane.add(16, 80,
                p -> new GuiComponentScrollPane(p, 90, 180, 20)).viewport();
        scrollPaneRecipes = pane.add(116, 80,
                p -> new GuiComponentScrollPane(p, 268, 180, 40)).viewport();
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
                    int i = id;
                    scrollPaneTypes.addVert(0, 0,
                            p -> new ElementType(p, recipeType, i, true));
                } else {
                    tableOnly.add(new Pair<>(recipeType, id));
                }
            }
            id++;
        }
        if (!tableOnly.isEmpty()) {
            GuiComponentPane separator = scrollPaneTypes
                    .addVert(0, 0, p -> new GuiComponentPane(p, 90, 30));
            separator
                    .add(5, 9, p -> new GuiComponentText(p, 12, "Table only:"));
            Streams.of(tableOnly).forEach(type -> scrollPaneTypes.addVert(0, 0,
                    p -> new ElementType(p, type.a, type.b, false)));
        }
    }

    @Override
    public void updateComponent(ScapesEngine engine) {
        super.updateComponent(engine);
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
            int i = id++;
            scrollPaneRecipes
                    .addVert(0, 0, p -> new Element(p, recipe, type, i));
        }
        nextExample = System.currentTimeMillis() + 1000;
    }

    private class ElementType extends GuiComponentPane {
        public ElementType(GuiLayoutData parent, CraftingRecipeType recipe,
                int id, boolean enabled) {
            super(parent, 80, 20);
            GuiComponentTextButton label =
                    add(5, 2, p -> button(p, 70, 15, 12, recipe.name()));
            if (enabled) {
                label.onClickLeft(event -> {
                    type = id;
                    example = 0;
                    updateRecipes();
                });
            }
        }
    }

    private class Element extends GuiComponentPane {
        public Element(GuiLayoutData parent, CraftingRecipe recipe, int type,
                int id) {
            super(parent, 268, 40);
            GuiComponentItemButton result = addHori(5, 5,
                    p -> new GuiComponentItemButton(p, 30, 30,
                            recipe.result()));
            result.onClickLeft(event -> player.connection()
                    .send(new PacketCrafting(type, id)));
            result.onHover(event -> setTooltip(result.item(), "Result:\n"));
            addHori(5, 5, p -> new GuiComponentText(p, 16, "<="));
            recipe.ingredients().forEach(ingredient -> {
                GuiComponentItemButton b = addHori(5, 5,
                        p -> new GuiComponentItemButton(p, 25, 25,
                                ingredient.example(example)));
                b.onHover(event -> setTooltip(b.item(), "Ingredient:\n"));
            });
            if (recipe.ingredients().findAny().isPresent()) {
                addHori(5, 5, p -> new GuiComponentText(p, 16, "+"));
            }
            recipe.ingredients().forEach(requirement -> {
                GuiComponentItemButton b = addHori(5, 5,
                        p -> new GuiComponentItemButton(p, 25, 25,
                                requirement.example(example)));
                b.onHover(event -> setTooltip(b.item(), "Requirement:\n"));
            });
        }
    }
}
