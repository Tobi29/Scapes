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

import java8.util.Optional;
import java8.util.stream.Stream;
import org.tobi29.scapes.engine.Container;
import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.gui.GuiComponent;
import org.tobi29.scapes.engine.gui.GuiComponentEvent;
import org.tobi29.scapes.engine.gui.GuiController;
import org.tobi29.scapes.engine.gui.GuiCursor;
import org.tobi29.scapes.engine.input.ControllerBasic;
import org.tobi29.scapes.engine.input.ControllerJoystick;
import org.tobi29.scapes.engine.input.ControllerKey;
import org.tobi29.scapes.engine.input.ControllerKeyReference;
import org.tobi29.scapes.engine.utils.Pair;
import org.tobi29.scapes.engine.utils.Streams;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.vector.Vector2d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GuiControllerGamepad implements GuiController {
    private final ScapesEngine engine;
    private final ControllerJoystick controller;
    private final ControllerKeyReference primaryButton, secondaryButton;
    private final int axisCursorX, axisCursorY, axisScrollX, axisScrollY;
    private final double cursorSensitivity, scrollSensitivity;
    private final GuiCursor cursor = new GuiCursor(true);
    private List<Pair<GuiCursor, ControllerBasic.PressEvent>> clicks =
            Collections.emptyList();
    private double cursorX, cursorY;
    private boolean cursorCentered, cursorLeft, cursorRight;
    private Optional<GuiComponent> draggingLeft = Optional.empty(),
            draggingRight = Optional.empty();
    private double dragLeftX, dragLeftY, dragRightX, dragRightY;

    public GuiControllerGamepad(ScapesEngine engine,
            ControllerJoystick controller, ControllerKeyReference primaryButton,
            ControllerKeyReference secondaryButton, int axisCursorX,
            int axisCursorY, int axisScrollX, int axisScrollY,
            double cursorSensitivity, double scrollSensitivity) {
        this.engine = engine;
        this.controller = controller;
        this.primaryButton = primaryButton;
        this.secondaryButton = secondaryButton;
        this.axisCursorX = axisCursorX;
        this.axisCursorY = axisCursorY;
        this.axisScrollX = axisScrollX;
        this.axisScrollY = axisScrollY;
        this.cursorSensitivity = cursorSensitivity;
        this.scrollSensitivity = scrollSensitivity;
    }

    @Override
    public void update(double delta) {
        Container container = engine.container();
        double width = container.containerWidth();
        double height = container.containerHeight();
        double ratio = 540.0 / height;
        if (!cursorCentered) {
            cursorCentered = true;
            cursorX = width / 2.0;
            cursorY = height / 2.0;
        }
        cursorX += controller.axis(axisCursorX) * cursorSensitivity * delta;
        cursorY += controller.axis(axisCursorY) * cursorSensitivity * delta;
        cursorX = FastMath.clamp(cursorX, 0.0, width);
        cursorY = FastMath.clamp(cursorY, 0.0, height);
        double guiCursorX = cursorX * ratio;
        double guiCursorY = cursorY * ratio;
        cursor.set(new Vector2d(cursorX, cursorY),
                new Vector2d(guiCursorX, guiCursorY));
        if (draggingLeft.isPresent()) {
            GuiComponent component = draggingLeft.get();
            double relativeX = cursor.guiX() - dragLeftX;
            double relativeY = cursor.guiY() - dragLeftY;
            dragLeftX = cursor.guiX();
            dragLeftY = cursor.guiY();
            component.gui().sendNewEvent(
                    new GuiComponentEvent(guiCursorX, guiCursorY, relativeX,
                            relativeY), component, component::dragLeft, engine);
        }
        if (draggingRight.isPresent()) {
            GuiComponent component = draggingRight.get();
            double relativeX = cursor.guiX() - dragRightX;
            double relativeY = cursor.guiY() - dragRightY;
            dragRightX = cursor.guiX();
            dragRightY = cursor.guiY();
            component.gui().sendNewEvent(
                    new GuiComponentEvent(guiCursorX, guiCursorY, relativeX,
                            relativeY), component, component::dragRight,
                    engine);
        }
        List<Pair<GuiCursor, ControllerBasic.PressEvent>> clicks =
                new ArrayList<>();
        if (primaryButton.isDown(controller)) {
            if (!cursorLeft) {
                clicks.add(new Pair<>(cursor,
                        new ControllerBasic.PressEvent(ControllerKey.BUTTON_0,
                                ControllerBasic.PressState.PRESS)));
                cursorLeft = true;
            }
        } else if (cursorLeft) {
            clicks.add(new Pair<>(cursor,
                    new ControllerBasic.PressEvent(ControllerKey.BUTTON_0,
                            ControllerBasic.PressState.RELEASE)));
            cursorLeft = false;
        }
        if (secondaryButton.isDown(controller)) {
            if (!cursorRight) {
                clicks.add(new Pair<>(cursor,
                        new ControllerBasic.PressEvent(ControllerKey.BUTTON_1,
                                ControllerBasic.PressState.PRESS)));
                cursorRight = true;
            }
        } else if (cursorRight) {
            clicks.add(new Pair<>(cursor,
                    new ControllerBasic.PressEvent(ControllerKey.BUTTON_1,
                            ControllerBasic.PressState.RELEASE)));
            cursorRight = false;
        }
        this.clicks = clicks;
        double scrollX =
                controller.axis(axisScrollX) * scrollSensitivity * delta;
        double scrollY =
                controller.axis(axisScrollY) * scrollSensitivity * delta;
        if (scrollX != 0.0 || scrollY != 0.0) {
            engine.guiStack().fireRecursiveEvent(
                    new GuiComponentEvent(guiCursorX, guiCursorY, scrollX,
                            scrollY, false), GuiComponent::scroll, engine);
        }
        GuiComponentEvent componentEvent =
                new GuiComponentEvent(guiCursorX, guiCursorY);
        engine.guiStack()
                .fireEvent(componentEvent, GuiComponent::hover, engine);
        clicks().forEach(event -> {
            switch (event.b.state()) {
                case PRESS:
                    handlePress(event.b.key(), componentEvent);
                    break;
                case RELEASE:
                    handleRelease(event.b.key(), componentEvent);
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
        return Streams.of(cursor);
    }

    @Override
    public Stream<Pair<GuiCursor, ControllerBasic.PressEvent>> clicks() {
        return Streams.of(clicks);
    }

    @Override
    public boolean captureCursor() {
        return true;
    }

    private void handlePress(ControllerKey key, GuiComponentEvent event) {
        switch (key) {
            case BUTTON_0:
                draggingLeft = engine.guiStack()
                        .fireEvent(event, GuiComponent::pressLeft, engine);
                dragLeftX = cursor.guiX();
                dragLeftY = cursor.guiY();
                engine.guiStack()
                        .fireEvent(event, GuiComponent::clickLeft, engine);
                break;
            case BUTTON_1:
                draggingLeft = engine.guiStack()
                        .fireEvent(event, GuiComponent::pressRight, engine);
                dragRightX = cursor.guiX();
                dragRightY = cursor.guiY();
                engine.guiStack()
                        .fireEvent(event, GuiComponent::clickRight, engine);
                break;
        }
    }

    private void handleRelease(ControllerKey key, GuiComponentEvent event) {
        switch (key) {
            case BUTTON_0:
                if (draggingLeft.isPresent()) {
                    GuiComponent component = draggingLeft.get();
                    component.gui()
                            .sendNewEvent(event, component, component::dropLeft,
                                    engine);
                    draggingLeft = Optional.empty();
                }
                break;
            case BUTTON_1:
                if (draggingRight.isPresent()) {
                    GuiComponent component = draggingRight.get();
                    component.gui().sendNewEvent(event, component,
                            component::dropRight, engine);
                    draggingRight = Optional.empty();
                }
                break;
        }
    }
}
