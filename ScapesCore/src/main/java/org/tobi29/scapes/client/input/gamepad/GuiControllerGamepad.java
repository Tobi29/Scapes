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

package org.tobi29.scapes.client.input.gamepad;

import java8.util.stream.Stream;
import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.gui.GuiAction;
import org.tobi29.scapes.engine.gui.GuiController;
import org.tobi29.scapes.engine.gui.GuiCursor;
import org.tobi29.scapes.engine.input.ControllerBasic;
import org.tobi29.scapes.engine.input.ControllerJoystick;
import org.tobi29.scapes.engine.input.ControllerKey;
import org.tobi29.scapes.engine.input.ControllerKeyReference;
import org.tobi29.scapes.engine.utils.Pair;
import org.tobi29.scapes.engine.utils.Streams;

public class GuiControllerGamepad extends GuiController {
    private final ControllerJoystick controller;
    private final ControllerKeyReference primaryButton, secondaryButton,
            upButton, downButton, leftButton, rightButton;

    public GuiControllerGamepad(ScapesEngine engine,
            ControllerJoystick controller, ControllerKeyReference primaryButton,
            ControllerKeyReference secondaryButton,
            ControllerKeyReference upButton, ControllerKeyReference downButton,
            ControllerKeyReference leftButton,
            ControllerKeyReference rightButton) {
        super(engine);
        this.controller = controller;
        this.primaryButton = primaryButton;
        this.secondaryButton = secondaryButton;
        this.upButton = upButton;
        this.downButton = downButton;
        this.leftButton = leftButton;
        this.rightButton = rightButton;
    }

    @Override
    public void update(double delta) {
        controller.pressEvents().forEach(event -> {
            switch (event.state()) {
                case PRESS:
                    handlePress(event.key());
                    break;
            }
        });
    }

    @Override
    public void focusTextField(TextFieldData data, boolean multiline) {
        // TODO: Implement keyboard for gamepads
    }

    @Override
    public boolean processTextField(TextFieldData data, boolean multiline) {
        return false; // TODO: Implement keyboard for gamepads
    }

    @Override
    public Stream<GuiCursor> cursors() {
        return Streams.of();
    }

    @Override
    public Stream<Pair<GuiCursor, ControllerBasic.PressEvent>> clicks() {
        return Streams.of();
    }

    @Override
    public boolean captureCursor() {
        return true;
    }

    private void handlePress(ControllerKey key) {
        if (primaryButton.isPressed(key, controller) &&
                engine.guiStack().fireAction(GuiAction.ACTIVATE, engine)) {
            return;
        }
        if (secondaryButton.isPressed(key, controller) &&
                engine.guiStack().fireAction(GuiAction.BACK, engine)) {
            return;
        }
        if (upButton.isPressed(key, controller) &&
                engine.guiStack().fireAction(GuiAction.UP, engine)) {
            return;
        }
        if (downButton.isPressed(key, controller) &&
                engine.guiStack().fireAction(GuiAction.DOWN, engine)) {
            return;
        }
        if (leftButton.isPressed(key, controller) &&
                engine.guiStack().fireAction(GuiAction.LEFT, engine)) {
            return;
        }
        if (rightButton.isPressed(key, controller) &&
                engine.guiStack().fireAction(GuiAction.RIGHT, engine)) {
            return;
        }
        firePress(key);
    }
}
