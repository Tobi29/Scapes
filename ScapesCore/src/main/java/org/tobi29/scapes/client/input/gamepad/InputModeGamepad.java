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

import org.tobi29.scapes.client.ScapesClient;
import org.tobi29.scapes.client.gui.desktop.GuiControlsGamepad;
import org.tobi29.scapes.client.input.InputMode;
import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.gui.Gui;
import org.tobi29.scapes.engine.gui.GuiController;
import org.tobi29.scapes.engine.input.ControllerJoystick;
import org.tobi29.scapes.engine.input.ControllerKey;
import org.tobi29.scapes.engine.input.ControllerKeyReference;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.vector.Vector2;
import org.tobi29.scapes.engine.utils.math.vector.Vector2d;
import org.tobi29.scapes.entity.client.MobPlayerClientMain;

public class InputModeGamepad implements InputMode {
    private final ControllerJoystick controller;
    private final TagStructure tagStructure;
    private final GuiController guiController;
    private final PlayerController playerController;

    public InputModeGamepad(ScapesEngine engine, ControllerJoystick controller,
            TagStructure tagStructure) {
        this.controller = controller;
        String id = controller.id();
        this.tagStructure = tagStructure.getStructure(id);
        defaultConfig(this.tagStructure);
        TagStructure guiTag = this.tagStructure.getStructure("GUI");
        ControllerKeyReference primaryButton =
                ControllerKeyReference.valueOf(guiTag.getString("Primary"));
        ControllerKeyReference secondaryButton =
                ControllerKeyReference.valueOf(guiTag.getString("Secondary"));
        TagStructure guiCursorTag = guiTag.getStructure("Cursor");
        int cursorXAxis = guiCursorTag.getInteger("X");
        int cursorYAxis = guiCursorTag.getInteger("Y");
        double cursorSensitivity =
                guiCursorTag.getDouble("Sensitivity") * 1000.0;
        TagStructure guiScrollTag = guiTag.getStructure("Scroll");
        int scrollAxisX = guiScrollTag.getInteger("X");
        int scrollAxisY = guiScrollTag.getInteger("Y");
        double scrollSensitivity =
                guiScrollTag.getDouble("Sensitivity") * 100.0;
        guiController =
                new GuiControllerGamepad(engine, controller, primaryButton,
                        secondaryButton, cursorXAxis, cursorYAxis, scrollAxisX,
                        scrollAxisY, cursorSensitivity, scrollSensitivity);
        playerController = new PlayerController();
    }

    private static void defaultConfig(TagStructure tagStructure) {
        TagStructure movementTag = tagStructure.getStructure("Movement");
        check("X", 0, movementTag);
        check("Y", 1, movementTag);
        check("Jump", ControllerKey.BUTTON_0, movementTag);

        TagStructure cameraTag = tagStructure.getStructure("Camera");
        check("X", 3, cameraTag);
        check("Y", 4, cameraTag);
        check("Sensitivity", 1.0, cameraTag);

        TagStructure guiTag = tagStructure.getStructure("GUI");
        check("Primary", ControllerKey.BUTTON_0, guiTag);
        check("Secondary", ControllerKey.BUTTON_1, guiTag);

        TagStructure guiCursorTag = guiTag.getStructure("Cursor");
        check("X", 0, guiCursorTag);
        check("Y", 1, guiCursorTag);
        check("Sensitivity", 1.0, guiCursorTag);

        TagStructure guiScrollTag = guiTag.getStructure("Scroll");
        check("X", 3, guiScrollTag);
        check("Y", 4, guiScrollTag);
        check("Sensitivity", 1.0, guiScrollTag);

        TagStructure actionTag = tagStructure.getStructure("Action");
        check("Left", ControllerKey.AXIS_2, actionTag);
        check("Right", ControllerKey.AXIS_5, actionTag);

        TagStructure menuTag = tagStructure.getStructure("Menu");
        check("Inventory", ControllerKey.BUTTON_7, menuTag);
        check("Menu", ControllerKey.BUTTON_10, menuTag);
        check("Chat", ControllerKey.BUTTON_6, menuTag);

        TagStructure hotbarTag = tagStructure.getStructure("Hotbar");
        check("Add", ControllerKey.BUTTON_5, hotbarTag);
        check("Subtract", ControllerKey.BUTTON_4, hotbarTag);
        check("Left", ControllerKey.BUTTON_2, hotbarTag);
    }

    private static void check(String id, ControllerKey def,
            TagStructure tagStructure) {
        if (!tagStructure.has(id)) {
            tagStructure.setString(id, def.toString());
        }
    }

    private static void check(String id, double def,
            TagStructure tagStructure) {
        if (!tagStructure.has(id)) {
            tagStructure.setDouble(id, def);
        }
    }

    private static void check(String id, int def, TagStructure tagStructure) {
        if (!tagStructure.has(id)) {
            tagStructure.setInteger(id, def);
        }
    }

    @Override
    public String toString() {
        return controller.name();
    }

    @Override
    public boolean poll() {
        controller.poll();
        return controller.isActive();
    }

    @Override
    public Gui createControlsGUI(GameState state, Gui prev) {
        return new GuiControlsGamepad(state, prev,
                (ScapesClient) state.engine().game(), tagStructure, controller,
                prev.style());
    }

    @Override
    public MobPlayerClientMain.Controller playerController() {
        return playerController;
    }

    @Override
    public GuiController guiController() {
        return guiController;
    }

    private class PlayerController implements MobPlayerClientMain.Controller {
        private final int axisWalkX, axisWalkY, axisCameraX, axisCameraY;
        private final ControllerKeyReference jump, inventory, menu, chat, left,
                right, hotbarAdd, hotbarSubtract, hotbarLeft;
        private final double cameraSensitivity;

        public PlayerController() {
            TagStructure movementTag = tagStructure.getStructure("Movement");
            axisWalkX = movementTag.getInteger("X");
            axisWalkY = movementTag.getInteger("Y");
            jump = ControllerKeyReference
                    .valueOf(movementTag.getString("Jump"));

            TagStructure cameraTag = tagStructure.getStructure("Camera");
            axisCameraX = cameraTag.getInteger("X");
            axisCameraY = cameraTag.getInteger("Y");
            cameraSensitivity = cameraTag.getDouble("Sensitivity") * 400.0;

            TagStructure menuTag = tagStructure.getStructure("Menu");
            inventory = ControllerKeyReference
                    .valueOf(menuTag.getString("Inventory"));
            menu = ControllerKeyReference.valueOf(menuTag.getString("Menu"));
            chat = ControllerKeyReference.valueOf(menuTag.getString("Chat"));

            TagStructure actionTag = tagStructure.getStructure("Action");
            left = ControllerKeyReference.valueOf(actionTag.getString("Left"));
            right = ControllerKeyReference
                    .valueOf(actionTag.getString("Right"));

            TagStructure hotbarTag = tagStructure.getStructure("Hotbar");
            hotbarAdd =
                    ControllerKeyReference.valueOf(hotbarTag.getString("Add"));
            hotbarSubtract = ControllerKeyReference
                    .valueOf(hotbarTag.getString("Subtract"));
            hotbarLeft =
                    ControllerKeyReference.valueOf(hotbarTag.getString("Left"));
        }

        @Override
        public Vector2 walk() {
            return new Vector2d(controller.axis(axisWalkX),
                    -controller.axis(axisWalkY));
        }

        @Override
        public Vector2 camera(double delta) {
            double x = controller.axis(axisCameraX);
            double y = controller.axis(axisCameraY);
            double cx = FastMath.sqrNoAbs(x);
            double cy = FastMath.sqrNoAbs(y);
            x = FastMath.mix(x, cx, 0.5);
            y = FastMath.mix(y, cy, 0.5);
            return new Vector2d(x, y).multiply(cameraSensitivity * delta);
        }

        @Override
        public boolean left() {
            return left.isDown(controller);
        }

        @Override
        public boolean right() {
            return right.isDown(controller);
        }

        @Override
        public boolean jump() {
            return jump.isDown(controller);
        }

        @Override
        public boolean inventory() {
            return inventory.isPressed(controller);
        }

        @Override
        public boolean menu() {
            return menu.isPressed(controller);
        }

        @Override
        public boolean chat() {
            return chat.isPressed(controller);
        }

        @Override
        public int hotbarLeft(int previous) {
            if (hotbarLeft.isDown(controller)) {
                if (hotbarAdd.isPressed(controller)) {
                    previous++;
                }
                if (hotbarSubtract.isPressed(controller)) {
                    previous--;
                }
            }
            previous %= 10;
            if (previous < 0) {
                previous += 10;
            }
            return previous;
        }

        @Override
        public int hotbarRight(int previous) {
            if (!hotbarLeft.isDown(controller)) {
                if (hotbarAdd.isPressed(controller)) {
                    previous++;
                }
                if (hotbarSubtract.isPressed(controller)) {
                    previous--;
                }
            }
            previous %= 10;
            if (previous < 0) {
                previous += 10;
            }
            return previous;
        }
    }
}