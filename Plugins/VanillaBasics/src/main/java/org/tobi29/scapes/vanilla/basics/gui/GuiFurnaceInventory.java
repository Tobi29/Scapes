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

import org.tobi29.scapes.engine.gui.GuiStyle;
import org.tobi29.scapes.engine.opengl.FontRenderer;
import org.tobi29.scapes.engine.opengl.GL;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.vanilla.basics.entity.client.EntityFurnaceClient;
import org.tobi29.scapes.vanilla.basics.entity.client.MobPlayerClientMainVB;

public class GuiFurnaceInventory extends GuiContainerInventory {
    private final EntityFurnaceClient container;
    private FontRenderer.Text temperatureText = FontRenderer.EMPTY_TEXT;

    public GuiFurnaceInventory(EntityFurnaceClient container,
            MobPlayerClientMainVB player, GuiStyle style) {
        super("Furnace", player, container, style);
        this.container = container;
        buttonContainer(16, 210, 30, 30, 0);
        buttonContainer(56, 210, 30, 30, 1);
        buttonContainer(96, 210, 30, 30, 2);
        buttonContainer(136, 210, 30, 30, 3);
        buttonContainer(16, 80, 30, 30, 4);
        buttonContainer(16, 120, 30, 30, 5);
        buttonContainer(56, 120, 30, 30, 6);
        buttonContainer(96, 120, 30, 30, 7);
        updateTemperatureText();
    }

    @Override
    public void renderOverlay(GL gl, Shader shader) {
        super.renderOverlay(gl, shader);
        updateTemperatureText();
        temperatureText.render(gl, shader);
    }

    private void updateTemperatureText() {
        FontRenderer font = style.font();
        String text = FastMath.floor(container.temperature()) + "°C";
        if (!text.equals(temperatureText.text())) {
            temperatureText =
                    font.render(text, 220, 170, 24, 1.0f, 1.0f, 1.0f, 1.0f);
        }
    }
}
