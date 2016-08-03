/*
 * Copyright 2012-2016 Tobi29
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

package org.tobi29.scapes.client.gui.desktop;

import org.tobi29.scapes.client.ScapesClient;
import org.tobi29.scapes.client.gui.GuiComponentControlsAxis;
import org.tobi29.scapes.client.gui.GuiComponentControlsButton;
import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.gui.*;
import org.tobi29.scapes.engine.input.ControllerBasic;
import org.tobi29.scapes.engine.input.ControllerJoystick;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.math.FastMath;

public abstract class GuiControls extends GuiMenu {
    protected final GuiComponentScrollPaneViewport scrollPane;

    protected GuiControls(GameState state, Gui previous, ScapesClient game,
            GuiStyle style) {
        super(state, "Controls", "Save", style);
        game.setFreezeInputMode(true);
        // This intentionally disable the back action to allow binding ESC
        back.on(GuiEvent.CLICK_LEFT, (event, engine) -> {
            game.setFreezeInputMode(false);
            game.loadInput();
            state.engine().guiStack().swap(this, previous);
        });
        scrollPane = pane.add(16, 80, 368, 390,
                p -> new GuiComponentScrollPane(p, 40)).viewport();
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

    protected void addText(String text) {
        scrollPane.addVert(40, 16, 40, 5, -1, 18,
                p -> new GuiComponentText(p, text));
    }

    protected void addButton(String name, String id, TagStructure tagStructure,
            ControllerBasic controller) {
        GuiComponentControlsButton button = row(scrollPane,
                p -> new GuiComponentControlsButton(p, 18, name, id,
                        tagStructure, controller));
        selection(button);
    }

    protected void addAxis(String name, String id, TagStructure tagStructure,
            ControllerJoystick controller) {
        GuiComponentControlsAxis axis = row(scrollPane,
                p -> new GuiComponentControlsAxis(p, 18, name, id, tagStructure,
                        controller));
        selection(axis);
    }

    protected void addSlider(String name, String id,
            TagStructure tagStructure) {
        GuiComponentSlider slider = row(scrollPane, p -> slider(p, name,
                reverseSensitivity(tagStructure.getDouble(id)),
                (text, value) -> text + ": " +
                        FastMath.round(sensitivity(value) * 100.0) +
                        '%'));
        selection(slider);
        slider.on(GuiEvent.CHANGE, event -> tagStructure
                .setDouble(id, sensitivity(slider.value())));
    }
}
