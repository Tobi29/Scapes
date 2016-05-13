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

import java8.util.Optional;
import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.gui.GuiComponentHoverEvent;
import org.tobi29.scapes.engine.gui.GuiComponentTextButton;
import org.tobi29.scapes.engine.gui.GuiLayoutData;
import org.tobi29.scapes.engine.input.ControllerBasic;
import org.tobi29.scapes.engine.input.ControllerKey;
import org.tobi29.scapes.engine.input.ControllerKeyReference;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;

import java.util.ArrayList;
import java.util.List;

public class GuiComponentControlsButton extends GuiComponentTextButton {
    private final String name, id;
    private final TagStructure tagStructure;
    private final ControllerBasic controller;
    private final List<ControllerKey> keys = new ArrayList<>();
    private byte editing;
    private ControllerKeyReference key;

    public GuiComponentControlsButton(GuiLayoutData parent, int textSize,
            String name, String id, TagStructure tagStructure,
            ControllerBasic controller) {
        super(parent, textSize, "");
        this.name = name;
        this.id = id;
        this.tagStructure = tagStructure;
        this.controller = controller;
        key = ControllerKeyReference.valueOf(tagStructure.getString(id));
        onClickLeft(event -> {
            if (editing == 0) {
                editing = 1;
                updateText();
            }
        });
        onHover(event -> {
            if (event.state() == GuiComponentHoverEvent.State.LEAVE &&
                    editing > 1) {
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
    protected void updateComponent(ScapesEngine engine, double delta) {
        if (editing > 1) {
            controller.pressEvents().filter(event -> event.state() ==
                    ControllerBasic.PressState.PRESS)
                    .map(ControllerBasic.PressEvent::key).forEach(keys::add);
            Optional<ControllerBasic.PressEvent> keyEvent2 =
                    controller.pressEvents().filter(event -> event.state() ==
                            ControllerBasic.PressState.RELEASE).findAny();
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
}
