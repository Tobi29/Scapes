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
package org.tobi29.scapes.client.input.gamepad;

import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.gui.GuiController;
import org.tobi29.scapes.engine.input.ControllerJoystick;
import org.tobi29.scapes.engine.input.ControllerKeyReference;
import org.tobi29.scapes.engine.opengl.Container;
import org.tobi29.scapes.engine.utils.math.FastMath;

public class GuiControllerGamepad implements GuiController {
    private final ScapesEngine engine;
    private final ControllerJoystick controller;
    private final ControllerKeyReference primaryButton, secondaryButton;
    private final int axisCursorX, axisCursorY, axisScroll;
    private final double cursorSensitivity, scrollSensitivity;
    private double cursorX, cursorY, scroll, guiCursorX, guiCursorY;
    private boolean cursorCentered;

    public GuiControllerGamepad(ScapesEngine engine,
            ControllerJoystick controller, ControllerKeyReference primaryButton,
            ControllerKeyReference secondaryButton, int axisCursorX,
            int axisCursorY, int axisScroll, double cursorSensitivity,
            double scrollSensitivity) {
        this.engine = engine;
        this.controller = controller;
        this.primaryButton = primaryButton;
        this.secondaryButton = secondaryButton;
        this.axisCursorX = axisCursorX;
        this.axisCursorY = axisCursorY;
        this.axisScroll = axisScroll;
        this.cursorSensitivity = cursorSensitivity;
        this.scrollSensitivity = scrollSensitivity;
    }

    @Override
    public void update(double delta) {
        Container container = engine.container();
        double width = container.containerWidth();
        double height = container.containerHeight();
        if (!cursorCentered) {
            cursorCentered = true;
            cursorX = width / 2.0;
            cursorY = height / 2.0;
        }
        cursorX += controller.axis(axisCursorX) * cursorSensitivity * delta;
        cursorY += controller.axis(axisCursorY) * cursorSensitivity * delta;
        scroll = controller.axis(axisScroll) * scrollSensitivity * delta;
        cursorX = FastMath.clamp(cursorX, 0.0, width);
        cursorY = FastMath.clamp(cursorY, 0.0, height);
        guiCursorX = cursorX / width * 800.0;
        guiCursorY = cursorY / height * 512.0;
    }

    @Override
    public boolean processTextField(TextFieldData data, boolean multiline) {
        return false; // TODO: Implement keyboard for gamepads
    }

    @Override
    public double cursorX() {
        return cursorX;
    }

    @Override
    public double cursorY() {
        return cursorY;
    }

    @Override
    public double guiCursorX() {
        return guiCursorX;
    }

    @Override
    public double guiCursorY() {
        return guiCursorY;
    }

    @Override
    public boolean isSoftwareMouse() {
        return true;
    }

    @Override
    public boolean leftClick() {
        return primaryButton.isPressed(controller);
    }

    @Override
    public boolean rightClick() {
        return secondaryButton.isPressed(controller);
    }

    @Override
    public boolean leftDrag() {
        return primaryButton.isDown(controller);
    }

    @Override
    public boolean rightDrag() {
        return secondaryButton.isDown(controller);
    }

    @Override
    public double scroll() {
        return scroll;
    }
}
