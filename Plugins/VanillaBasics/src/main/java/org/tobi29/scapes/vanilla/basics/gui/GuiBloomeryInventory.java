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

import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.gui.GuiComponentText;
import org.tobi29.scapes.engine.gui.GuiStyle;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.vanilla.basics.entity.client.EntityBloomeryClient;
import org.tobi29.scapes.vanilla.basics.entity.client.MobPlayerClientMainVB;

public class GuiBloomeryInventory extends GuiContainerInventory {
    private final EntityBloomeryClient container;
    private final GuiComponentText temperatureText, bellowsText;

    public GuiBloomeryInventory(EntityBloomeryClient container,
            MobPlayerClientMainVB player, GuiStyle style) {
        super("Bloomery", player, container, style);
        this.container = container;
        selection(buttonContainer(16, 210, 30, 30, 0));
        selection(buttonContainer(56, 210, 30, 30, 1));
        selection(buttonContainer(96, 210, 30, 30, 2));
        selection(buttonContainer(136, 210, 30, 30, 3));
        selection(buttonContainer(16, 80, 30, 30, 4));
        selection(buttonContainer(16, 120, 30, 30, 5));
        selection(buttonContainer(56, 120, 30, 30, 6));
        selection(buttonContainer(96, 120, 30, 30, 7));
        selection(buttonContainer(136, 120, 30, 30, 8));
        selection(buttonContainer(176, 120, 30, 30, 9));
        selection(buttonContainer(216, 120, 30, 30, 10));
        selection(buttonContainer(256, 120, 30, 30, 11));
        selection(buttonContainer(296, 120, 30, 30, 12));
        selection(buttonContainer(336, 120, 30, 30, 13));
        temperatureText =
                pane.add(220, 170, -1, 24, p -> new GuiComponentText(p, ""));
        bellowsText = pane.add(300, 170, -1, 24,
                p -> new GuiComponentText(p, "No bellows attached!"));
        updateTemperatureText();
    }

    @Override
    public void updateComponent(ScapesEngine engine, double delta) {
        super.updateComponent(engine, delta);
        updateTemperatureText();
        bellowsText.setVisible(!container.hasBellows());
    }

    private void updateTemperatureText() {
        String text = FastMath.floor(container.temperature()) + "°C";
        temperatureText.setText(text);
    }
}
