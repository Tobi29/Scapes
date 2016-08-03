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

package org.tobi29.scapes.vanilla.basics.gui;

import java8.util.Optional;
import org.tobi29.scapes.block.Inventory;
import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.client.gui.GuiComponentItemButton;
import org.tobi29.scapes.client.gui.GuiUtils;
import org.tobi29.scapes.client.gui.desktop.GuiMenu;
import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.graphics.*;
import org.tobi29.scapes.engine.gui.*;
import org.tobi29.scapes.engine.utils.Pair;
import org.tobi29.scapes.engine.utils.Streams;
import org.tobi29.scapes.engine.utils.math.vector.Vector2;
import org.tobi29.scapes.packets.PacketInventoryInteraction;
import org.tobi29.scapes.vanilla.basics.entity.client.MobPlayerClientMainVB;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GuiInventory extends GuiMenu {
    protected final GuiComponentVisiblePane inventoryPane;
    protected final MobPlayerClientMainVB player;
    private String hover, hoverNew, hoverName;
    private List<Pair<Model, Texture>> hoverText = Collections.emptyList();
    private double cursorX = Double.NaN, cursorY = Double.NaN;

    public GuiInventory(String name, MobPlayerClientMainVB player,
            GuiStyle style) {
        super(player.game(), name, style);
        this.player = player;
        inventoryPane =
                pane.add(16, 268, 368, 162, GuiComponentVisiblePane::new);
        int i = 0;
        List<GuiComponent> buttons = new ArrayList<>(10);
        for (int x = 0; x < 10; x++) {
            int xx = x * 35 + 11;
            buttons.add(button(xx, 121, 30, 30, i));
            i++;
        }
        selection(-2, buttons);
        buttons.clear();
        for (int y = 0; y < 3; y++) {
            int yy = y * 35 + 11;
            for (int x = 0; x < 10; x++) {
                int xx = x * 35 + 11;
                buttons.add(button(xx, yy, 30, 30, i));
                i++;
            }
            selection(-1, buttons);
            buttons.clear();
        }

        on(GuiAction.BACK, player::closeGui);
    }

    protected GuiComponentItemButton button(int x, int y, int width, int height,
            int slot) {
        return button(x, y, width, height, "Container", slot);
    }

    protected GuiComponentItemButton button(int x, int y, int width, int height,
            String id, int slot) {
        Inventory inventory = player.inventories().accessUnsafe(id);
        GuiComponentItemButton button = inventoryPane.add(x, y, width, height,
                p -> new GuiComponentItemButton(p, inventory.item(slot)));
        button.on(GuiEvent.CLICK_LEFT, event -> leftClick(id, slot));
        button.on(GuiEvent.CLICK_RIGHT, event -> rightClick(id, slot));
        button.on(GuiEvent.HOVER, event -> setTooltip(inventory.item(slot)));
        return button;
    }

    protected void leftClick(String id, int i) {
        player.connection().send(new PacketInventoryInteraction(player,
                PacketInventoryInteraction.LEFT, id, i));
    }

    protected void rightClick(String id, int i) {
        player.connection().send(new PacketInventoryInteraction(player,
                PacketInventoryInteraction.RIGHT, id, i));
    }

    @Override
    public void updateComponent(ScapesEngine engine, double delta) {
        Optional<GuiCursor> cursor = engine.guiController().cursors().findAny();
        if (cursor.isPresent()) {
            cursorX = cursor.get().guiX();
            cursorY = cursor.get().guiY();
        } else {
            cursorX = Double.NaN;
            cursorY = Double.NaN;
        }
        hover = hoverNew;
        hoverNew = null;
    }

    @Override
    public void renderOverlay(GL gl, Shader shader, Vector2 pixelSize) {
        FontRenderer font = style.font();
        String hover = this.hover;
        if (hover != null) {
            MatrixStack matrixStack = gl.matrixStack();
            if (!hover.equals(hoverName)) {
                updateText(hover, font, pixelSize);
            }
            Matrix matrix = matrixStack.push();
            matrix.translate((float) cursorX, (float) cursorY, 0.0f);
            Streams.forEach(hoverText, mesh -> {
                mesh.b.bind(gl);
                mesh.a.render(gl, shader);
            });
            matrixStack.pop();
        }
        player.inventories().access("Hold", inventory -> GuiUtils
                .items((float) cursorX, (float) cursorY, 30.0f, 30.0f,
                        inventory.item(0), gl, shader, font, pixelSize));
    }

    protected void setTooltip(ItemStack item) {
        setTooltip(item, "");
    }

    protected void setTooltip(ItemStack item, String prefix) {
        hoverNew = prefix + item.material().name(item);
    }

    private void updateText(String text, FontRenderer font, Vector2 pixelSize) {
        hoverName = text;
        GuiRenderBatch batch = new GuiRenderBatch(pixelSize);
        font.render(FontRenderer.to(batch, 1.0f, 1.0f, 1.0f, 1.0f), text,
                12.0f);
        hoverText = batch.finish();
    }
}
