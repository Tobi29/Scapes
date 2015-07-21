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

import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.gui.GuiComponentEvent;
import org.tobi29.scapes.engine.gui.GuiComponentTextButton;
import org.tobi29.scapes.engine.input.ControllerJoystick;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;

public class GuiComponentControlsAxis extends GuiComponentTextButton {
    private final String name, id;
    private final TagStructure tagStructure;
    private final ControllerJoystick controller;
    private byte editing;
    private int axis;

    public GuiComponentControlsAxis(int x, int y, int width, int height,
            int textSize, String name, String id, TagStructure tagStructure,
            ControllerJoystick controller) {
        super(x, y, width, height, textSize, "");
        this.name = name;
        this.id = id;
        this.tagStructure = tagStructure;
        this.controller = controller;
        axis = tagStructure.getInteger(id);
        updateText();
    }

    private void updateText() {
        StringBuilder text = new StringBuilder(16);
        if (editing > 0) {
            text.append('<');
        }
        text.append(name);
        text.append(": Axis ");
        text.append(axis);
        if (editing > 0) {
            text.append('>');
        }
        setText(text.toString());
    }

    @Override
    public void update(double mouseX, double mouseY, boolean mouseInside,
            ScapesEngine engine) {
        super.update(mouseX, mouseY, mouseInside, engine);
        if (editing > 1) {
            int axes = controller.axes();
            for (int i = 0; i < axes; i++) {
                if (controller.axis(i) > 0.5) {
                    axis = i;
                    tagStructure.setInteger(id, axis);
                    editing = 0;
                    updateText();
                    break;
                }
            }
        } else if (editing > 0) {
            editing = 2;
        }
    }

    @Override
    public void clickLeft(GuiComponentEvent event, ScapesEngine engine) {
        super.clickLeft(event, engine);
        if (editing == 0) {
            editing = 1;
            updateText();
        }
    }

    @Override
    public void setHover(boolean hover, ScapesEngine engine) {
        super.setHover(hover, engine);
        if (editing > 1 && !hover) {
            editing = 0;
            updateText();
        }
    }
}
