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

package org.tobi29.scapes.client.gui;

import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.gui.GuiComponentButtonHeavy;
import org.tobi29.scapes.engine.gui.GuiComponentText;
import org.tobi29.scapes.engine.gui.GuiEvent;
import org.tobi29.scapes.engine.gui.GuiLayoutData;
import org.tobi29.scapes.engine.input.ControllerJoystick;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;

import java.util.ArrayList;
import java.util.List;

public class GuiComponentControlsAxis extends GuiComponentButtonHeavy {
    private final GuiComponentText text;
    private final String name, id;
    private final TagStructure tagStructure;
    private final ControllerJoystick controller;
    private final List<Integer> blacklist = new ArrayList<>();
    private byte editing;
    private int axis;

    public GuiComponentControlsAxis(GuiLayoutData parent, int textSize,
            String name, String id, TagStructure tagStructure,
            ControllerJoystick controller) {
        super(parent);
        text = addSubHori(4, 0, -1, textSize, p -> new GuiComponentText(p, ""));
        this.name = name;
        this.id = id;
        this.tagStructure = tagStructure;
        this.controller = controller;
        axis = tagStructure.getInteger(id);
        on(GuiEvent.CLICK_LEFT, event -> {
            if (editing == 0) {
                blacklist.clear();
                int axes = controller.axes();
                for (int i = 0; i < axes; i++) {
                    if (controller.axis(i) > 0.5) {
                        blacklist.add(i);
                    }
                }
                editing = 1;
                updateText();
            }
        });
        on(GuiEvent.HOVER_LEAVE, event -> {
            if (editing > 1) {
                editing = 0;
                updateText();
            }
        });
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
        this.text.setText(text.toString());
    }

    @Override
    protected void updateComponent(ScapesEngine engine, double delta) {
        if (editing > 1) {
            int axes = controller.axes();
            for (int i = 0; i < axes; i++) {
                boolean blacklisted = blacklist.contains(i);
                if (controller.axis(i) > 0.5) {
                    if (!blacklisted) {
                        axis = i;
                        tagStructure.setInteger(id, axis);
                        editing = 0;
                        updateText();
                        break;
                    }
                } else if (blacklisted) {
                    blacklist.remove(i);
                }
            }
        } else if (editing > 0) {
            editing = 2;
        }
    }
}
