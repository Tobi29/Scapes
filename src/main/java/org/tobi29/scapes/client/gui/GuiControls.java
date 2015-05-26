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

import org.tobi29.scapes.client.ScapesClient;
import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.gui.Gui;
import org.tobi29.scapes.engine.gui.GuiComponentScrollPaneList;
import org.tobi29.scapes.engine.gui.GuiComponentSlider;
import org.tobi29.scapes.engine.gui.GuiComponentText;
import org.tobi29.scapes.engine.input.Controller;
import org.tobi29.scapes.engine.input.ControllerJoystick;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.math.FastMath;

public abstract class GuiControls extends GuiMenu {
    protected final GuiComponentScrollPaneList scrollPane;

    protected GuiControls(GameState state, Gui previous, ScapesClient game) {
        super(state, "Controls", "Save", previous);
        game.setFreezeInputMode(true);
        back.addLeftClick(event -> {
            game.setFreezeInputMode(false);
            game.loadInput();
        });
        scrollPane = new GuiComponentScrollPaneList(16, 80, 368, 350, 40);
        pane.add(scrollPane);
    }

    protected void addText(String text) {
        scrollPane.add(new GuiComponentText(40, 0, 18, text));
    }

    protected void addButton(String name, String id, TagStructure tagStructure,
            Controller controller) {
        GuiComponentControlsButton button =
                new GuiComponentControlsButton(15, 0, 328, 30, 18, name, id,
                        tagStructure, controller);
        scrollPane.add(button);
    }

    protected void addAxis(String name, String id, TagStructure tagStructure,
            ControllerJoystick controller) {
        GuiComponentControlsAxis button =
                new GuiComponentControlsAxis(15, 0, 328, 30, 18, name, id,
                        tagStructure, controller);
        scrollPane.add(button);
    }

    protected void addSlider(String name, String id,
            TagStructure tagStructure) {
        GuiComponentSlider slider =
                new GuiComponentSlider(15, 0, 328, 30, 18, name,
                        reverseSensitivity(tagStructure.getDouble(id)),
                        (text, value) -> text + ": " +
                                FastMath.round(sensitivity(value) * 100.0) +
                                '%');
        slider.addHover(
                event -> tagStructure.setDouble(id, sensitivity(slider.value)));
        scrollPane.add(slider);
    }

    private static double sensitivity(double value) {
        value = value * 2.0 - 1.0;
        return value * value * value * 10.0;
    }

    private static double reverseSensitivity(double value) {
        value *= 0.1;
        if (value >= 0.0) {
            value = FastMath.pow(value, 1.0 / 3.0);
        } else {
            value = -FastMath.pow(-value, 1.0 / 3.0);
        }
        return value * 0.5 + 0.5;
    }
}
