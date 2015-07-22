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

import org.tobi29.scapes.block.Inventory;
import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.gui.*;
import org.tobi29.scapes.engine.opengl.FontRenderer;
import org.tobi29.scapes.engine.opengl.GL;
import org.tobi29.scapes.engine.opengl.matrix.Matrix;
import org.tobi29.scapes.engine.opengl.matrix.MatrixStack;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.entity.client.MobPlayerClientMain;
import org.tobi29.scapes.packets.PacketInventoryInteraction;

public class GuiInventory extends Gui {
    protected final GuiComponentVisiblePane pane;
    protected final MobPlayerClientMain player;
    private String hover, renderHover;
    private FontRenderer.Text vaoText;
    private String currentName;
    private double cursorX, cursorY;
    private FontRenderer font;

    public GuiInventory(String name, MobPlayerClientMain player) {
        super(GuiAlignment.CENTER);
        this.player = player;
        Inventory inventory = player.inventory();
        pane = new GuiComponentVisiblePane(200, 0, 400, 512);
        GuiComponentVisiblePane inventoryPane =
                new GuiComponentVisiblePane(16, 268, 368, 162);
        int i = 0;
        for (int y = -1; y < 3; y++) {
            int yy = y * 35 + 11;
            if (y == -1) {
                yy = 121;
            }
            for (int x = 0; x < 10; x++) {
                int xx = x * 35 + 11;
                int id = i;
                GuiComponentItemButton item =
                        new GuiComponentItemButton(xx, yy, 30, 30,
                                inventory.item(i));
                item.addLeftClick(event -> leftClick(id));
                item.addRightClick(event -> rightClick(id));
                item.addHover(event -> setTooltip(inventory.item(id)));
                inventoryPane.add(item);
                i++;
            }
        }
        pane.add(new GuiComponentText(16, 16, 32, name));
        pane.add(new GuiComponentSeparator(24, 64, 352, 2));
        pane.add(inventoryPane);
        pane.add(new GuiComponentSeparator(24, 448, 352, 2));
        add(pane);
    }

    protected void leftClick(int i) {
        player.connection().send(new PacketInventoryInteraction(player,
                PacketInventoryInteraction.LEFT, i));
    }

    protected void rightClick(int i) {
        player.connection().send(new PacketInventoryInteraction(player,
                PacketInventoryInteraction.RIGHT, i));
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
        player.inventory().hold().ifPresent(hold -> GuiUtils
                .renderItem((float) cursorX, (float) cursorY, 30.0f, 30.0f,
                        hold, gl, shader, font));
    }

    @Override
    public void updateComponent() {
        renderHover = hover;
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
