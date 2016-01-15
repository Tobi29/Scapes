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

import org.tobi29.scapes.client.gui.GuiComponentItemButton;
import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.gui.*;
import org.tobi29.scapes.engine.utils.Pair;
import org.tobi29.scapes.engine.utils.Streams;
import org.tobi29.scapes.engine.utils.math.vector.Vector2;
import org.tobi29.scapes.vanilla.basics.VanillaBasics;
import org.tobi29.scapes.vanilla.basics.entity.client.MobPlayerClientMainVB;
import org.tobi29.scapes.vanilla.basics.material.CraftingRecipe;
import org.tobi29.scapes.vanilla.basics.material.CraftingRecipeType;
import org.tobi29.scapes.vanilla.basics.packet.PacketCrafting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GuiCrafting extends GuiInventory {
    private final GuiComponentScrollPaneViewport scrollPaneTypes,
            scrollPaneRecipes;
    private final boolean table;
    private List<Element> elements = Collections.emptyList();
    private int type = -1, example;
    private long nextExample;

    public GuiCrafting(boolean table, MobPlayerClientMainVB player,
            GuiStyle style) {
        super("Crafting" + (table ? " Table" : ""), player, style);
        this.table = table;
        scrollPaneTypes = pane.add(16, 80, 90, 180,
                p -> new GuiComponentScrollPane(p, 20)).viewport();
        scrollPaneRecipes = pane.add(116, 80, 268, 180,
                p -> new GuiComponentScrollPane(p, 40)).viewport();
        updateTypes();
        updateRecipes();
    }

    private void updateTypes() {
        scrollPaneTypes.removeAll();
        List<CraftingRecipeType> recipeTypes;
        VanillaBasics plugin = (VanillaBasics) player.connection().plugins()
                .plugin("VanillaBasics");
        recipeTypes = plugin.getCraftingRecipes();
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
                    scrollPaneTypes.addVert(0, 0, -1, 20,
                            p -> new ElementType(p, recipeType, i, true));
                } else {
                    tableOnly.add(new Pair<>(recipeType, id));
                }
            }
            id++;
        }
        if (!tableOnly.isEmpty()) {
            GuiComponentGroup separator = scrollPaneTypes
                    .addVert(0, 0, 90, 30, GuiComponentGroup::new);
            separator.add(5, 9, -1, 12,
                    p -> new GuiComponentText(p, "Table only:"));
            Streams.of(tableOnly).forEach(type -> scrollPaneTypes
                    .addVert(0, 0, -1, 20,
                            p -> new ElementType(p, type.a, type.b, false)));
        }
    }

    @Override
    public void updateComponent(ScapesEngine engine, Vector2 size) {
        super.updateComponent(engine, size);
        if (System.currentTimeMillis() > nextExample) {
            if (example == Integer.MAX_VALUE) {
                example = 0;
            } else {
                example++;
            }
            Streams.of(elements).forEach(Element::examples);
            nextExample = System.currentTimeMillis() + 1000;
        }
    }

    private void updateRecipes() {
        List<Element> elements = new ArrayList<>();
        scrollPaneRecipes.removeAll();
        VanillaBasics plugin = (VanillaBasics) player.connection().plugins()
                .plugin("VanillaBasics");
        CraftingRecipeType recipeType = plugin.getCraftingRecipes().get(type);
        int id = 0;
        for (CraftingRecipe recipe : recipeType.recipes()) {
            int i = id++;
            elements.add(scrollPaneRecipes.addVert(0, 0, 268, 40,
                    p -> new Element(p, recipe, type, i)));
        }
        this.elements = elements;
        nextExample = System.currentTimeMillis() + 1000;
    }

    private class ElementType extends GuiComponentGroupSlab {
        public ElementType(GuiLayoutData parent, CraftingRecipeType recipe,
                int id, boolean enabled) {
            super(parent);
            GuiComponentTextButton label =
                    addHori(5, 2, -1, -1, p -> button(p, 12, recipe.name()));
            if (enabled) {
                label.onClickLeft(event -> {
                    type = id;
                    example = 0;
                    updateRecipes();
                });
            }
        }
    }

    private class Element extends GuiComponentGroupSlab {
        private final List<Runnable> examples = new ArrayList<>();

        public Element(GuiLayoutData parent, CraftingRecipe recipe, int type,
                int id) {
            super(parent);
            GuiComponentItemButton result = addHori(5, 5, 30, 30,
                    p -> new GuiComponentItemButton(p, recipe.result()));
            result.onClickLeft(event -> player.connection()
                    .send(new PacketCrafting(type, id)));
            result.onHover(event -> setTooltip(result.item(), "Result:\n"));
            addHori(5, 5, -1, 16, p -> new GuiComponentFlowText(p, "<="));
            recipe.ingredients().forEach(ingredient -> {
                GuiComponentItemButton b = addHori(5, 5, 25, 25,
                        p -> new GuiComponentItemButton(p,
                                ingredient.example(example)));
                b.onHover(event -> setTooltip(b.item(), "Ingredient:\n"));
                examples.add(() -> b.setItem(ingredient.example(example)));
            });
            if (recipe.requirements().findAny().isPresent()) {
                addHori(5, 5, -1, 16, p -> new GuiComponentFlowText(p, "+"));
            }
            recipe.requirements().forEach(requirement -> {
                GuiComponentItemButton b = addHori(5, 5, 25, 25,
                        p -> new GuiComponentItemButton(p,
                                requirement.example(example)));
                b.onHover(event -> setTooltip(b.item(), "Requirement:\n"));
                examples.add(() -> b.setItem(requirement.example(example)));
            });
        }

        public void examples() {
            Streams.of(examples).forEach(Runnable::run);
        }
    }
}
