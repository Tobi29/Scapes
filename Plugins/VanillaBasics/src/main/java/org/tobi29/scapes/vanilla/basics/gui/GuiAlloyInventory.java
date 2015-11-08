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
import org.tobi29.scapes.vanilla.basics.entity.client.EntityAlloyClient;
import org.tobi29.scapes.vanilla.basics.entity.client.MobPlayerClientMainVB;

import java.util.Map;

public class GuiAlloyInventory extends GuiContainerInventory {
    private final EntityAlloyClient container;
    private FontRenderer.Text infoText;

    public GuiAlloyInventory(EntityAlloyClient container,
            MobPlayerClientMainVB player, GuiStyle style) {
        super("Alloy Mold", player, container, style);
        this.container = container;
        buttonContainer(16, 120, 30, 30, 0);
        buttonContainer(16, 160, 30, 30, 1);
        updateInfoText();
    }

    @Override
    public void renderOverlay(GL gl, Shader shader) {
        super.renderOverlay(gl, shader);
        updateInfoText();
        infoText.render(gl, shader);
    }

    private void updateInfoText() {
        FontRenderer font = style.font();
        StringBuilder textBuilder = new StringBuilder(64);
        textBuilder.append("Metal: ");
        String result = container.result();
        if (!result.isEmpty()) {
            textBuilder.append(result);
        } else {
            textBuilder.append("Unknown");
        }
        double size = 0.0f;
        for (Double amount : container.metals().values()) {
            size += amount;
        }
        for (Map.Entry<String, Double> entry : container.metals().entrySet()) {
            textBuilder.append('\n').append(entry.getKey()).append(" - ")
                    .append(FastMath.round(entry.getValue() / size * 100.0f))
                    .append('%');
        }
        String text = textBuilder.toString();
        if (!text.equals(infoText.text())) {
            infoText = font.render(text, 220, 170, 24, 1.0f, 1.0f, 1.0f, 1.0f);
        }
    }
}
