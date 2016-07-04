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

import java8.util.Optional;
import org.tobi29.scapes.client.gui.GuiComponentItemButton;
import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.gui.*;
import org.tobi29.scapes.engine.utils.Streams;
import org.tobi29.scapes.vanilla.basics.VanillaBasics;
import org.tobi29.scapes.vanilla.basics.entity.client.MobPlayerClientMainVB;
import org.tobi29.scapes.vanilla.basics.material.CraftingRecipe;
import org.tobi29.scapes.vanilla.basics.material.CraftingRecipeType;
import org.tobi29.scapes.vanilla.basics.packet.PacketCrafting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class GuiCrafting extends GuiInventory {
    private final GuiComponentScrollPaneViewport scrollPaneTypes,
            scrollPaneRecipes;
    private final boolean table;
    private List<Element> elements = Collections.emptyList();
    private Optional<CraftingRecipeType> currentType = Optional.empty();
    private int example;
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
        currentType = Optional.empty();
        scrollPaneTypes.removeAll();
        VanillaBasics plugin = (VanillaBasics) player.connection().plugins()
                .plugin("VanillaBasics");
        List<CraftingRecipeType> tableOnly = new ArrayList<>();
        Iterator<CraftingRecipeType> iterator =
                plugin.craftingRecipes().iterator();
        while (iterator.hasNext()) {
            CraftingRecipeType recipeType = iterator.next();
            if (recipeType.availableFor(player)) {
                boolean enabled = table || !recipeType.table();
                if (enabled) {
                    if (!currentType.isPresent()) {
                        currentType = Optional.of(recipeType);
                    }
                    scrollPaneTypes.addVert(0, 0, -1, 20,
                            p -> new ElementType(p, recipeType, true));
                } else {
                    tableOnly.add(recipeType);
                }
            }
        }
        if (!tableOnly.isEmpty()) {
            GuiComponentGroup separator = scrollPaneTypes
                    .addVert(0, 0, 90, 30, GuiComponentGroup::new);
            separator.add(5, 9, -1, 12,
                    p -> new GuiComponentText(p, "Table only:"));
            Streams.forEach(tableOnly, type -> scrollPaneTypes
                    .addVert(0, 0, -1, 20,
                            p -> new ElementType(p, type, false)));
        }
    }

    @Override
    public void updateComponent(ScapesEngine engine, double delta) {
        super.updateComponent(engine, delta);
        if (System.currentTimeMillis() > nextExample) {
            if (example == Integer.MAX_VALUE) {
                example = 0;
            } else {
                example++;
            }
            Streams.forEach(elements, Element::examples);
            nextExample = System.currentTimeMillis() + 1000;
        }
    }

    private void updateRecipes() {
        List<Element> elements = new ArrayList<>();
        scrollPaneRecipes.removeAll();
        currentType.ifPresent(recipeType -> {
            recipeType.recipes().forEach(recipe -> elements
                    .add(scrollPaneRecipes.addVert(0, 0, 268, 40,
                            p -> new Element(p, recipe))));
            this.elements = elements;
            nextExample = System.currentTimeMillis() + 1000;
        });
    }

    private class ElementType extends GuiComponentGroupSlab {
        public ElementType(GuiLayoutData parent, CraftingRecipeType recipeType,
                boolean enabled) {
            super(parent);
            GuiComponentTextButton label = addHori(5, 2, -1, -1,
                    p -> button(p, 12, recipeType.name()));
            if (enabled) {
                selection(1, label);

                label.on(GuiEvent.CLICK_LEFT, event -> {
                    currentType = Optional.of(recipeType);
                    example = 0;
                    updateRecipes();
                });
            }
        }
    }

    private class Element extends GuiComponentGroupSlab {
        private final List<Runnable> examples = new ArrayList<>();

        public Element(GuiLayoutData parent, CraftingRecipe recipe) {
            super(parent);
            GuiComponentItemButton result = addHori(5, 5, 30, 30,
                    p -> new GuiComponentItemButton(p, recipe.result()));
            addHori(5, 5, -1, 16, p -> new GuiComponentFlowText(p, "<="));
            recipe.ingredients().forEach(ingredient -> {
                GuiComponentItemButton b = addHori(5, 5, 25, 25,
                        p -> new GuiComponentItemButton(p,
                                ingredient.example(example)));
                b.on(GuiEvent.HOVER,
                        event -> setTooltip(b.item(), "Ingredient:\n"));
                examples.add(() -> b.setItem(ingredient.example(example)));
            });
            if (recipe.requirements().findAny().isPresent()) {
                addHori(5, 5, -1, 16, p -> new GuiComponentFlowText(p, "+"));
            }
            recipe.requirements().forEach(requirement -> {
                GuiComponentItemButton b = addHori(5, 5, 25, 25,
                        p -> new GuiComponentItemButton(p,
                                requirement.example(example)));
                b.on(GuiEvent.HOVER,
                        event -> setTooltip(b.item(), "Requirement:\n"));
                examples.add(() -> b.setItem(requirement.example(example)));
            });

            selection(result);

            result.on(GuiEvent.CLICK_LEFT, event -> player.connection()
                    .send(new PacketCrafting(recipe.data(
                            player.connection().plugins().registry()))));
            result.on(GuiEvent.HOVER,
                    event -> setTooltip(result.item(), "Result:\n"));
        }

        public void examples() {
            Streams.forEach(examples, Runnable::run);
        }
    }
}
