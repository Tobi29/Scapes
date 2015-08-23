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

import org.tobi29.scapes.engine.opengl.FontRenderer;
import org.tobi29.scapes.engine.opengl.GL;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.entity.client.MobPlayerClientMain;
import org.tobi29.scapes.vanilla.basics.entity.client.EntityFurnaceClient;
import org.tobi29.scapes.vanilla.basics.entity.client.MobPlayerClientMainVB;

public class GuiFurnaceInventory extends GuiContainerInventory {
    private final EntityFurnaceClient container;
    private FontRenderer.Text vaoTemperatureText;
    private String currentText;

    public GuiFurnaceInventory(EntityFurnaceClient container,
            MobPlayerClientMainVB player) {
        super("Furnace", player, container);
        this.container = container;
        buttonContainer(16, 210, 30, 30, 0);
        buttonContainer(56, 210, 30, 30, 1);
        buttonContainer(96, 210, 30, 30, 2);
        buttonContainer(136, 210, 30, 30, 3);
        buttonContainer(16, 80, 30, 30, 4);
        buttonContainer(16, 120, 30, 30, 5);
        buttonContainer(56, 120, 30, 30, 6);
        buttonContainer(96, 120, 30, 30, 7);
    }

    @Override
    public void renderOverlay(GL gl, Shader shader, FontRenderer font) {
        super.renderOverlay(gl, shader, font);
        String text = FastMath.floor(container.temperature()) + "°C";
        if (!text.equals(currentText) || vaoTemperatureText == null) {
            currentText = text;
            vaoTemperatureText =
                    font.render(text, 220, 170, 24, 1.0f, 1.0f, 1.0f, 1.0f);
        }
        vaoTemperatureText.render(gl, shader);
    }
}
