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

import org.tobi29.scapes.engine.gui.GuiComponentText;
import org.tobi29.scapes.engine.gui.GuiStyle;
import org.tobi29.scapes.engine.opengl.GL;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.vanilla.basics.VanillaBasics;
import org.tobi29.scapes.vanilla.basics.entity.client.EntityAlloyClient;
import org.tobi29.scapes.vanilla.basics.entity.client.MobPlayerClientMainVB;
import org.tobi29.scapes.vanilla.basics.util.MetalUtil;

public class GuiAlloyInventory extends GuiContainerInventory {
    private final EntityAlloyClient container;
    private final GuiComponentText infoText;

    public GuiAlloyInventory(EntityAlloyClient container,
            MobPlayerClientMainVB player, GuiStyle style) {
        super("Alloy Mold", player, container, style);
        this.container = container;
        selection(buttonContainer(16, 120, 30, 30, 0));
        selection(buttonContainer(16, 160, 30, 30, 1));
        infoText =
                pane.add(60, 80, -1, 24, p -> new GuiComponentText(p, ""));
        updateInfoText();
    }

    @Override
    public void renderOverlay(GL gl, Shader shader) {
        super.renderOverlay(gl, shader);
        updateInfoText();
    }

    private void updateInfoText() {
        StringBuilder text = new StringBuilder(64);
        MetalUtil.Alloy alloy = container.alloy();
        VanillaBasics plugin = (VanillaBasics) container.world().plugins()
                .plugin("VanillaBasics");
        if (alloy.metals().findAny().isPresent()) {
            text.append("Metal: ").append(alloy.type(plugin).name());
            alloy.metals().forEach(
                    entry -> text.append('\n').append(entry.a.name())
                            .append(" - ")
                            .append(FastMath.round(entry.b * 100.0) / 100.0));
        } else {
            text.append("Insert molten metal on top\nslot, extract below.");
        }
        infoText.setText(text.toString());
    }
}
