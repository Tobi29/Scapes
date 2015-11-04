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
import org.tobi29.scapes.engine.gui.GuiLayoutData;
import org.tobi29.scapes.engine.input.Controller;
import org.tobi29.scapes.engine.input.ControllerKey;
import org.tobi29.scapes.engine.input.ControllerKeyReference;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GuiComponentControlsButton extends GuiComponentTextButton {
    private final String name, id;
    private final TagStructure tagStructure;
    private final Controller controller;
    private byte editing;
    private ControllerKeyReference key;
    private List<ControllerKey> keys = new ArrayList<>();

    public GuiComponentControlsButton(GuiLayoutData parent, int width,
            int height, int textSize, String name, String id,
            TagStructure tagStructure, Controller controller) {
        super(parent, width, height, textSize, "");
        this.name = name;
        this.id = id;
        this.tagStructure = tagStructure;
        this.controller = controller;
        key = ControllerKeyReference.valueOf(tagStructure.getString(id));
        updateText();
    }

    private void updateText() {
        StringBuilder text = new StringBuilder(16);
        if (editing > 0) {
            text.append('<');
            text.append(name);
            text.append(": ");
            if (!keys.isEmpty()) {
                text.append(new ControllerKeyReference(keys).humanName());
            } else {
                text.append("...");
            }
            text.append('>');
        } else {
            text.append(name);
            text.append(": ");
            text.append(key.humanName());
        }
        setText(text.toString());
    }

    @Override
    public void update(double mouseX, double mouseY, boolean mouseInside,
            ScapesEngine engine) {
        super.update(mouseX, mouseY, mouseInside, engine);
        if (editing > 1) {
            controller.pressEvents().filter(event -> event.state() ==
                    Controller.PressState.PRESS).map(Controller.PressEvent::key)
                    .forEach(keys::add);
            Optional<Controller.PressEvent> keyEvent2 = controller.pressEvents()
                    .filter(event -> event.state() ==
                            Controller.PressState.RELEASE).findAny();
            if (keyEvent2.isPresent() && !keys.isEmpty()) {
                key = new ControllerKeyReference(keys);
                tagStructure.setString(id, key.toString());
                editing = 0;
                keys.clear();
            }
            updateText();
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
