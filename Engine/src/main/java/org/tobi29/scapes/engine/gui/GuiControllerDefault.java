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

package org.tobi29.scapes.engine.gui;

import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.input.Controller;
import org.tobi29.scapes.engine.input.ControllerDefault;
import org.tobi29.scapes.engine.input.ControllerKey;
import org.tobi29.scapes.engine.opengl.GraphicsSystem;
import org.tobi29.scapes.engine.utils.MutableSingle;

import java.util.regex.Pattern;

public class GuiControllerDefault implements GuiController {
    private static final Pattern REPLACE = Pattern.compile("\n");
    private final ScapesEngine engine;
    private final ControllerDefault controller;
    private final double scrollSensitivity;
    private double cursorX, cursorY, scroll, guiCursorX, guiCursorY;

    public GuiControllerDefault(ScapesEngine engine,
            ControllerDefault controller) {
        this(engine, controller, 1.0);
    }

    public GuiControllerDefault(ScapesEngine engine,
            ControllerDefault controller, double scrollSensitivity) {
        this.engine = engine;
        this.controller = controller;
        this.scrollSensitivity = scrollSensitivity;
    }

    @Override
    public void update(double delta) {
        cursorX = controller.getX();
        cursorY = controller.getY();
        scroll = controller.getScrollY() * scrollSensitivity;
        GraphicsSystem graphics = engine.getGraphics();
        double width = graphics.getContainerWidth();
        double height = graphics.getContainerHeight();
        guiCursorX = cursorX / width * 800.0d;
        guiCursorY = cursorY / height * 512.0d;
    }

    @Override
    public boolean processTextField(TextFieldData data, boolean multiline) {
        MutableSingle<Boolean> changed = new MutableSingle<>(false);
        boolean shift = controller.isDown(ControllerKey.KEY_LEFT_SHIFT) ||
                controller.isDown(ControllerKey.KEY_RIGHT_SHIFT);
        if (controller.isModifierDown()) {
            controller.getPressEvents().filter(event -> event.getState() !=
                    Controller.PressState.RELEASE).forEach(event -> {
                switch (event.getKey()) {
                    case KEY_A:
                        data.selectAll();
                        break;
                    case KEY_C:
                        data.copy().ifPresent(controller::clipboardCopy);
                        break;
                    case KEY_X:
                        data.cut().ifPresent(controller::clipboardCopy);
                        break;
                    case KEY_V:
                        String paste = controller.clipboardPaste();
                        if (paste != null) {
                            if (!multiline) {
                                paste = REPLACE.matcher(paste).replaceAll("");
                            }
                            data.paste(paste);
                        }
                        break;
                }
                changed.a = true;
            });
        } else {
            controller.getKeyTypeEvents().forEach(event -> {
                char character = event.getCharacter();
                if (!Character.isISOControl(character)) {
                    data.insert(character);
                    changed.a = true;
                }
            });
            controller.getPressEvents().filter(event -> event.getState() !=
                    Controller.PressState.RELEASE).forEach(event -> {
                switch (event.getKey()) {
                    case KEY_LEFT:
                        data.left(shift);
                        break;
                    case KEY_RIGHT:
                        data.right(shift);
                        break;
                    case KEY_ENTER:
                        if (multiline) {
                            data.insert('\n');
                        }
                        break;
                    case KEY_BACKSPACE:
                        if (data.selectionStart >= 0) {
                            data.deleteSelection();
                        } else {
                            if (data.cursor > 0) {
                                data.text.deleteCharAt(data.cursor - 1);
                                data.cursor--;
                            }
                        }
                        break;
                    case KEY_DELETE:
                        if (data.selectionStart >= 0) {
                            data.deleteSelection();
                        } else {
                            if (data.cursor < data.text.length()) {
                                data.text.deleteCharAt(data.cursor);
                            }
                        }
                        break;
                }
                changed.a = true;
            });
        }
        if (changed.a) {
            if (data.selectionStart == data.selectionEnd) {
                data.selectionStart = -1;
            }
            return true;
        }
        return false;
    }

    @Override
    public double getCursorX() {
        return cursorX;
    }

    @Override
    public double getCursorY() {
        return cursorY;
    }

    @Override
    public double getGuiCursorX() {
        return guiCursorX;
    }

    @Override
    public double getGuiCursorY() {
        return guiCursorY;
    }

    @Override
    public boolean isSoftwareMouse() {
        return false;
    }

    @Override
    public boolean getLeftClick() {
        return controller.isPressed(ControllerKey.BUTTON_LEFT);
    }

    @Override
    public boolean getRightClick() {
        return controller.isPressed(ControllerKey.BUTTON_RIGHT);
    }

    @Override
    public boolean getLeftDrag() {
        return controller.isDown(ControllerKey.BUTTON_LEFT);
    }

    @Override
    public boolean getRightDrag() {
        return controller.isDown(ControllerKey.BUTTON_RIGHT);
    }

    @Override
    public double getScroll() {
        return scroll;
    }
}
