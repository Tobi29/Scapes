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

import org.tobi29.scapes.block.Inventory;
import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.client.gui.GuiComponentItemButton;
import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.gui.*;
import org.tobi29.scapes.engine.opengl.FontRenderer;
import org.tobi29.scapes.engine.opengl.GL;
import org.tobi29.scapes.engine.opengl.matrix.Matrix;
import org.tobi29.scapes.engine.opengl.matrix.MatrixStack;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.packets.PacketInventoryInteraction;
import org.tobi29.scapes.vanilla.basics.entity.client.MobPlayerClientMainVB;

import java.util.Objects;

public class GuiInventory extends Gui {
    protected final GuiComponentVisiblePane pane, inventoryPane;
    protected final MobPlayerClientMainVB player;
    private String hover, renderHover, currentHover;
    private FontRenderer.Text vaoText;
    private String currentName;
    private double cursorX, cursorY;
    private FontRenderer font;

    public GuiInventory(String name, MobPlayerClientMainVB player) {
        super(GuiAlignment.CENTER);
        this.player = player;
        pane = new GuiComponentVisiblePane(this, 200, 0, 400, 512);
        new GuiComponentText(pane, 16, 16, 32, name);
        new GuiComponentSeparator(pane, 24, 64, 352, 2);
        inventoryPane = new GuiComponentVisiblePane(pane, 16, 268, 368, 162);
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
        new GuiComponentSeparator(pane, 24, 448, 352, 2);
    }

    protected void button(int x, int y, int width, int height, int slot) {
        button(x, y, width, height, "Container", slot);
    }

    protected void button(int x, int y, int width, int height, String id,
            int slot) {
        Inventory inventory = player.inventory(id);
        GuiComponentItemButton button =
                new GuiComponentItemButton(inventoryPane, x, y, width, height,
                        inventory.item(slot));
        button.addLeftClick(event -> leftClick(id, slot));
        button.addRightClick(event -> rightClick(id, slot));
        button.addHover(event -> setTooltip(inventory.item(slot)));
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
    public void renderOverlay(GL gl, Shader shader, FontRenderer font) {
        if (renderHover != null) {
            MatrixStack matrixStack = gl.matrixStack();
            if (!renderHover.equals(currentName) || this.font != font) {
                this.font = font;
                updateText(renderHover);
            }
            Matrix matrix = matrixStack.push();
            matrix.translate((float) cursorX, (float) cursorY, 0.0f);
            vaoText.render(gl, shader);
            matrixStack.pop();
        }
        org.tobi29.scapes.client.gui.GuiUtils
                .renderItem((float) cursorX, (float) cursorY, 30.0f, 30.0f,
                        player.inventory("Hold").item(0), gl, shader, font);
    }

    @Override
    public void updateComponent() {
        if (!Objects.equals(hover, currentHover)) {
            renderHover = hover;
            currentHover = hover;
        }
        hover = null;
    }

    @Override
    public void update(double mouseX, double mouseY, boolean mouseInside,
            ScapesEngine engine) {
        super.update(mouseX, mouseY, mouseInside, engine);
        cursorX = alignedX(mouseX, engine);
        cursorY = mouseY;
    }

    protected void setTooltip(ItemStack item) {
        setTooltip(item, "");
    }

    protected void setTooltip(ItemStack item, String prefix) {
        hover = prefix + item.material().name(item);
    }

    private void updateText(String text) {
        currentName = text;
        vaoText = font.render(text, 0, 0, 12, 1.0f, 1.0f, 1.0f, 1);
    }
}
