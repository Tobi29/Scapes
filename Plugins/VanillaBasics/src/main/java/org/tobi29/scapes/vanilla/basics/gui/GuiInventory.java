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
import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.client.gui.GuiComponentItemButton;
import org.tobi29.scapes.client.gui.GuiUtils;
import org.tobi29.scapes.client.gui.desktop.GuiMenu;
import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.gui.GuiComponentVisiblePane;
import org.tobi29.scapes.engine.gui.GuiCursor;
import org.tobi29.scapes.engine.gui.GuiStyle;
import org.tobi29.scapes.engine.opengl.FontRenderer;
import org.tobi29.scapes.engine.opengl.GL;
import org.tobi29.scapes.engine.opengl.matrix.Matrix;
import org.tobi29.scapes.engine.opengl.matrix.MatrixStack;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.engine.utils.math.vector.Vector2;
import org.tobi29.scapes.packets.PacketInventoryInteraction;
import org.tobi29.scapes.vanilla.basics.entity.client.MobPlayerClientMainVB;

public class GuiInventory extends GuiMenu {
    protected final GuiComponentVisiblePane inventoryPane;
    protected final MobPlayerClientMainVB player;
    private String hover, hoverNew;
    private FontRenderer.Text hoverName = FontRenderer.EMPTY_TEXT;
    private double cursorX = Double.NaN, cursorY = Double.NaN;

    public GuiInventory(String name, MobPlayerClientMainVB player,
            GuiStyle style) {
        super(player.game(), name, style);
        this.player = player;
        inventoryPane =
                pane.add(16, 268, 368, 162, GuiComponentVisiblePane::new);
        int i = 0;
        for (int x = 0; x < 10; x++) {
            int xx = x * 35 + 11;
            button(xx, 121, 30, 30, i);
            i++;
        }
        for (int y = 0; y < 3; y++) {
            int yy = y * 35 + 11;
            for (int x = 0; x < 10; x++) {
                int xx = x * 35 + 11;
                button(xx, yy, 30, 30, i);
                i++;
            }
        }

        back.onClickLeft(event -> player.closeGui());
    }

    protected void button(int x, int y, int width, int height, int slot) {
        button(x, y, width, height, "Container", slot);
    }

    protected void button(int x, int y, int width, int height, String id,
            int slot) {
        player.inventories().access(id, inventory -> {
            GuiComponentItemButton button = inventoryPane
                    .add(x, y, width, height, p -> new GuiComponentItemButton(p,
                            inventory.item(slot)));
            button.onClickLeft(event -> leftClick(id, slot));
            button.onClickRight(event -> rightClick(id, slot));
            button.onHover(event -> setTooltip(inventory.item(slot)));
        });
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
    public void renderOverlay(GL gl, Shader shader) {
        FontRenderer font = style.font();
        String hover = this.hover;
        if (hover != null) {
            MatrixStack matrixStack = gl.matrixStack();
            if (!hover.equals(hoverName.text())) {
                updateText(hover, font);
            }
            Matrix matrix = matrixStack.push();
            matrix.translate((float) cursorX, (float) cursorY, 0.0f);
            hoverName.render(gl, shader);
            matrixStack.pop();
        }
        player.inventories().access("Hold", inventory -> GuiUtils
                .renderItem((float) cursorX, (float) cursorY, 30.0f, 30.0f,
                        inventory.item(0), gl, shader, font));
    }

    @Override
    public void updateComponent(ScapesEngine engine, double delta,
            Vector2 size) {
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

    protected void setTooltip(ItemStack item) {
        setTooltip(item, "");
    }

    protected void setTooltip(ItemStack item, String prefix) {
        hoverNew = prefix + item.material().name(item);
    }

    private void updateText(String text, FontRenderer font) {
        hoverName = font.render(text, 0, 0, 12, 1.0f, 1.0f, 1.0f, 1);
    }
}
