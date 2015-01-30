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

import org.tobi29.scapes.client.gui.GuiContainerInventory;
import org.tobi29.scapes.engine.opengl.FontRenderer;
import org.tobi29.scapes.engine.opengl.GraphicsSystem;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.entity.client.MobPlayerClientMain;
import org.tobi29.scapes.vanilla.basics.entity.client.EntityForgeClient;

public class GuiForgeInventory extends GuiContainerInventory {
    private final EntityForgeClient container;
    private FontRenderer.Text vaoTemperatureText;
    private String currentText;

    public GuiForgeInventory(EntityForgeClient container,
            MobPlayerClientMain player) {
        super("Forge", player, container);
        this.container = container;
        addButton(16, 210, 30, 30, 0);
        addButton(56, 210, 30, 30, 1);
        addButton(96, 210, 30, 30, 2);
        addButton(136, 210, 30, 30, 3);
        addButton(16, 80, 30, 30, 4);
        addButton(16, 120, 30, 30, 5);
        addButton(56, 120, 30, 30, 6);
        addButton(96, 120, 30, 30, 7);
        addButton(96, 80, 30, 30, 8);
    }

    @Override
    public void renderOverlay(GraphicsSystem graphics, Shader shader,
            FontRenderer font) {
        super.renderOverlay(graphics, shader, font);
        String text = FastMath.floor(container.getTemperature()) + "°C";
        if (!text.equals(currentText) || vaoTemperatureText == null) {
            currentText = text;
            vaoTemperatureText =
                    font.render(text, 220, 170, 24, 1.0f, 1.0f, 1.0f, 1.0f);
        }
        vaoTemperatureText.render(graphics, shader);
    }
}
