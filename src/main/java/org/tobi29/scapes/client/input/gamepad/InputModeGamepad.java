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

import org.tobi29.scapes.client.gui.GuiControlsGamepad;
import org.tobi29.scapes.client.input.InputMode;
import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.gui.Gui;
import org.tobi29.scapes.engine.gui.GuiController;
import org.tobi29.scapes.engine.input.ControllerJoystick;
import org.tobi29.scapes.engine.input.ControllerKey;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
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
        String id = controller.getID();
        this.tagStructure = tagStructure.getStructure(id);
        defaultConfig(this.tagStructure);
        TagStructure guiTag = this.tagStructure.getStructure("GUI");
        ControllerKey primaryButton =
                ControllerKey.valueOf(guiTag.getString("Primary"));
        ControllerKey secondaryButton =
                ControllerKey.valueOf(guiTag.getString("Secondary"));
        TagStructure guiCursorTag = guiTag.getStructure("Cursor");
        int cursorXAxis = guiCursorTag.getInteger("X");
        int cursorYAxis = guiCursorTag.getInteger("Y");
        double cursorSensitivity =
                guiCursorTag.getDouble("Sensitivity") * 1000.0;
        TagStructure guiScrollTag = guiTag.getStructure("Scroll");
        int scrollAxis = guiScrollTag.getInteger("Y");
        double scrollSensitivity =
                guiScrollTag.getDouble("Sensitivity") * 100.0;
        guiController =
                new GuiControllerGamepad(engine, controller, primaryButton,
                        secondaryButton, cursorXAxis, cursorYAxis, scrollAxis,
                        cursorSensitivity, scrollSensitivity);
        playerController = new PlayerController();
    }

    private static void defaultConfig(TagStructure tagStructure) {
        TagStructure movementTag = tagStructure.getStructure("Movement");
        check("X", 0, movementTag);
        check("Y", 1, movementTag);
        check("Jump", ControllerKey.BUTTON_1, movementTag);

        TagStructure cameraTag = tagStructure.getStructure("Camera");
        check("X", 2, cameraTag);
        check("Y", 3, cameraTag);
        check("Sensitivity", 1.0, cameraTag);

        TagStructure guiTag = tagStructure.getStructure("GUI");
        check("Primary", ControllerKey.BUTTON_0, guiTag);
        check("Secondary", ControllerKey.BUTTON_3, guiTag);

        TagStructure guiCursorTag = guiTag.getStructure("Cursor");
        check("X", 0, guiCursorTag);
        check("Y", 1, guiCursorTag);
        check("Sensitivity", 1.0, guiCursorTag);

        TagStructure guiScrollTag = guiTag.getStructure("Scroll");
        check("Y", 3, guiScrollTag);
        check("Sensitivity", 1.0, guiScrollTag);

        TagStructure actionTag = tagStructure.getStructure("Action");
        check("Left", ControllerKey.BUTTON_6, actionTag);
        check("Right", ControllerKey.BUTTON_7, actionTag);

        TagStructure menuTag = tagStructure.getStructure("Menu");
        check("Inventory", ControllerKey.BUTTON_9, menuTag);
        check("Menu", ControllerKey.BUTTON_8, menuTag);
        check("Chat", ControllerKey.BUTTON_10, menuTag);

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
        return controller.getName();
    }

    @Override
    public boolean poll() {
        controller.poll();
        return controller.isActive();
    }

    @Override
    public Gui createControlsGui(GameState state, Gui prev) {
        return new GuiControlsGamepad(state, prev, tagStructure, controller);
    }

    @Override
    public MobPlayerClientMain.Controller getPlayerController() {
        return playerController;
    }

    @Override
    public GuiController getGuiController() {
        return guiController;
    }

    private class PlayerController implements MobPlayerClientMain.Controller {
        private final int axisWalkX, axisWalkY, axisCameraX, axisCameraY;
        private final ControllerKey jump, inventory, menu, chat, left, right,
                hotbarAdd, hotbarSubtract, hotbarLeft;
        private final double cameraSensitivity;

        public PlayerController() {
            TagStructure movementTag = tagStructure.getStructure("Movement");
            axisWalkX = movementTag.getInteger("X");
            axisWalkY = movementTag.getInteger("Y");
            jump = ControllerKey.valueOf(movementTag.getString("Jump"));

            TagStructure cameraTag = tagStructure.getStructure("Camera");
            axisCameraX = cameraTag.getInteger("X");
            axisCameraY = cameraTag.getInteger("Y");
            cameraSensitivity = cameraTag.getDouble("Sensitivity") * 10.0;

            TagStructure menuTag = tagStructure.getStructure("Menu");
            inventory = ControllerKey.valueOf(menuTag.getString("Inventory"));
            menu = ControllerKey.valueOf(menuTag.getString("Menu"));
            chat = ControllerKey.valueOf(menuTag.getString("Chat"));

            TagStructure actionTag = tagStructure.getStructure("Action");
            left = ControllerKey.valueOf(actionTag.getString("Left"));
            right = ControllerKey.valueOf(actionTag.getString("Right"));

            TagStructure hotbarTag = tagStructure.getStructure("Hotbar");
            hotbarAdd = ControllerKey.valueOf(hotbarTag.getString("Add"));
            hotbarSubtract =
                    ControllerKey.valueOf(hotbarTag.getString("Subtract"));
            hotbarLeft = ControllerKey.valueOf(hotbarTag.getString("Left"));
        }

        @Override
        public Vector2 getWalk() {
            return new Vector2d(controller.getAxis(axisWalkX),
                    -controller.getAxis(axisWalkY));
        }

        @Override
        public Vector2 getCamera() {
            return new Vector2d(controller.getAxis(axisCameraX),
                    controller.getAxis(axisCameraY))
                    .multiply(cameraSensitivity);
        }

        @Override
        public boolean getLeft() {
            return controller.isDown(left);
        }

        @Override
        public boolean getRight() {
            return controller.isDown(right);
        }

        @Override
        public boolean getJump() {
            return controller.isDown(jump);
        }

        @Override
        public boolean getInventory() {
            return controller.isPressed(inventory);
        }

        @Override
        public boolean getMenu() {
            return controller.isPressed(menu);
        }

        @Override
        public boolean getChat() {
            return controller.isPressed(chat);
        }

        @Override
        public int getHotbarLeft(int previous) {
            if (controller.isDown(hotbarLeft)) {
                if (controller.isPressed(hotbarAdd)) {
                    previous++;
                }
                if (controller.isPressed(hotbarSubtract)) {
                    previous--;
                }
            }
            return previous;
        }

        @Override
        public int getHotbarRight(int previous) {
            if (!controller.isDown(hotbarLeft)) {
                if (controller.isPressed(hotbarAdd)) {
                    previous++;
                }
                if (controller.isPressed(hotbarSubtract)) {
                    previous--;
                }
            }
            return previous;
        }
    }
}
