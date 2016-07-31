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

import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.engine.graphics.*;
import org.tobi29.scapes.engine.gui.GuiComponentButtonHeavy;
import org.tobi29.scapes.engine.gui.GuiLayoutData;
import org.tobi29.scapes.engine.gui.GuiRenderer;
import org.tobi29.scapes.engine.utils.math.vector.Vector2;
import org.tobi29.scapes.entity.client.MobPlayerClientMain;

public class GuiComponentHotbarButton extends GuiComponentButtonHeavy {
    private final GuiComponentItem item;
    private final MobPlayerClientMain player;
    private final int slot;
    private Model model;

    public GuiComponentHotbarButton(GuiLayoutData parent, ItemStack item,
            MobPlayerClientMain player, int slot) {
        super(parent);
        this.player = player;
        this.slot = slot;
        this.item =
                addSubHori(0, 0, -1, -1, p -> new GuiComponentItem(p, item));
    }

    public ItemStack item() {
        return item.item();
    }

    public void setItem(ItemStack item) {
        this.item.setItem(item);
    }

    @Override
    public void renderComponent(GL gl, Shader shader, Vector2 size,
            Vector2 pixelSize, double delta) {
        if (player.inventorySelectLeft() == slot) {
            gl.textures().bind("Scapes:image/gui/HotbarLeft", gl);
            model.render(gl, shader);
        } else if (player.inventorySelectRight() == slot) {
            gl.textures().bind("Scapes:image/gui/HotbarRight", gl);
            model.render(gl, shader);
        }
    }

    @Override
    public void updateMesh(GuiRenderer renderer, Vector2 size) {
        super.updateMesh(renderer, size);
        model = VAOUtility.createVCTI(gui.style().engine(),
                new float[]{0.0f, size.floatY() - 32.0f, 0.0f, size.floatY(),
                        size.floatY() - 32.0f, 0.0f, 0.0f, -32.0f, 0.0f,
                        size.floatY(), -32.0f, 0.0f},
                new float[]{1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f,
                        1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f},
                new float[]{0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f},
                new int[]{0, 1, 2, 3, 2, 1}, RenderType.TRIANGLES);
    }
}
