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
import org.tobi29.scapes.engine.gui.GuiComponentHeavy;
import org.tobi29.scapes.engine.gui.GuiLayoutData;
import org.tobi29.scapes.engine.opengl.GL;
import org.tobi29.scapes.engine.opengl.shader.Shader;

public class GuiComponentItem extends GuiComponentHeavy {
    private ItemStack item;

    public GuiComponentItem(GuiLayoutData parent, ItemStack item) {
        super(parent);
        this.item = item;
    }

    public ItemStack item() {
        return item;
    }

    public void setItem(ItemStack item) {
        this.item = item;
    }

    @Override
    public void renderComponent(GL gl, Shader shader, double width,
            double height) {
        super.renderComponent(gl, shader, width, height);
        GuiUtils.items(0.0f, 0.0f, (float) width, (float) height, item, gl,
                shader, gui.style().font());
    }
}