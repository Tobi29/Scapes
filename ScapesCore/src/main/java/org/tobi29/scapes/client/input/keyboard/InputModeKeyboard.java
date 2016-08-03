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

package org.tobi29.scapes.client.input.keyboard;

import java8.util.Optional;
import org.tobi29.scapes.client.ScapesClient;
import org.tobi29.scapes.client.gui.GuiChatWrite;
import org.tobi29.scapes.client.gui.desktop.GuiControlsDefault;
import org.tobi29.scapes.client.gui.desktop.GuiPause;
import org.tobi29.scapes.client.input.InputMode;
import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.gui.Gui;
import org.tobi29.scapes.engine.gui.GuiController;
import org.tobi29.scapes.engine.gui.GuiControllerMouse;
import org.tobi29.scapes.engine.input.ControllerDefault;
import org.tobi29.scapes.engine.input.ControllerKey;
import org.tobi29.scapes.engine.input.ControllerKeyReference;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.math.vector.Vector2;
import org.tobi29.scapes.engine.utils.math.vector.Vector2d;
import org.tobi29.scapes.entity.client.MobPlayerClientMain;
import org.tobi29.scapes.packets.PacketInteraction;

public class InputModeKeyboard implements InputMode {
    private final ControllerDefault controller;
    private final TagStructure tagStructure;
    private final GuiControllerMouse guiController;

    public InputModeKeyboard(ScapesEngine engine, ControllerDefault controller,
            TagStructure tagStructure) {
        this.tagStructure = tagStructure.getStructure("Default");
        defaultConfig(this.tagStructure);
        this.controller = controller;
        TagStructure miscTag = this.tagStructure.getStructure("Misc");
        TagStructure miscScrollTag = miscTag.getStructure("Scroll");
        double scrollSensitivity = miscScrollTag.getDouble("Sensitivity");
        guiController =
                new GuiControllerMouse(engine, controller, scrollSensitivity);
    }

    private static void defaultConfig(TagStructure tagStructure) {
        TagStructure movementTag = tagStructure.getStructure("Movement");
        check("Forward", ControllerKey.KEY_W, movementTag);
        check("Backward", ControllerKey.KEY_S, movementTag);
        check("Left", ControllerKey.KEY_A, movementTag);
        check("Right", ControllerKey.KEY_D, movementTag);
        check("Sprint", ControllerKey.KEY_LEFT_SHIFT, movementTag);
        check("Jump", ControllerKey.KEY_SPACE, movementTag);

        TagStructure cameraTag = tagStructure.getStructure("Camera");
        check("Sensitivity", 0.6, cameraTag);

        TagStructure actionTag = tagStructure.getStructure("Action");
        check("Left", ControllerKey.BUTTON_LEFT, actionTag);
        check("Right", ControllerKey.BUTTON_RIGHT, actionTag);

        TagStructure menuTag = tagStructure.getStructure("Menu");
        check("Inventory", ControllerKey.KEY_E, menuTag);
        check("Chat", ControllerKey.KEY_R, menuTag);
        check("Menu", ControllerKey.KEY_ESCAPE, menuTag);

        TagStructure hotbarTag = tagStructure.getStructure("Hotbar");
        check("Add", ControllerKey.SCROLL_DOWN, hotbarTag);
        check("Subtract", ControllerKey.SCROLL_UP, hotbarTag);
        check("Left", ControllerKey.KEY_LEFT_CONTROL, hotbarTag);
        check("Both", ControllerKey.KEY_LEFT_ALT, hotbarTag);
        check("0", ControllerKey.KEY_1, hotbarTag);
        check("1", ControllerKey.KEY_2, hotbarTag);
        check("2", ControllerKey.KEY_3, hotbarTag);
        check("3", ControllerKey.KEY_4, hotbarTag);
        check("4", ControllerKey.KEY_5, hotbarTag);
        check("5", ControllerKey.KEY_6, hotbarTag);
        check("6", ControllerKey.KEY_7, hotbarTag);
        check("7", ControllerKey.KEY_8, hotbarTag);
        check("8", ControllerKey.KEY_9, hotbarTag);
        check("9", ControllerKey.KEY_0, hotbarTag);

        TagStructure miscTag = tagStructure.getStructure("Misc");

        TagStructure miscScrollTag = miscTag.getStructure("Scroll");
        miscScrollTag.setDouble("Sensitivity", 1.0);
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

    @Override
    public boolean poll() {
        controller.poll();
        return controller.isActive();
    }

    @Override
    public Gui createControlsGUI(GameState state, Gui prev) {
        return new GuiControlsDefault(state, prev,
                (ScapesClient) state.engine().game(), tagStructure, controller,
                prev.style());
    }

    @Override
    public MobPlayerClientMain.Controller playerController(
            MobPlayerClientMain player) {
        return new PlayerController(player);
    }

    @Override
    public GuiController guiController() {
        return guiController;
    }

    @Override
    public String toString() {
        return "Keyboard + Mouse";
    }

    private class PlayerController implements MobPlayerClientMain.Controller {
        private final ControllerKeyReference walkForward, walkBackward,
                walkLeft, walkRight, walkSprint, jump, inventory, menu, chat,
                left, right, hotbarAdd, hotbarSubtract, hotbarLeft, hotbarBoth,
                hotbar0, hotbar1, hotbar2, hotbar3, hotbar4, hotbar5, hotbar6,
                hotbar7, hotbar8, hotbar9;
        private final double cameraSensitivity, scrollSensitivity;

        public PlayerController(MobPlayerClientMain player) {
            TagStructure movementTag = tagStructure.getStructure("Movement");
            walkForward = ControllerKeyReference
                    .valueOf(movementTag.getString("Forward"));
            walkBackward = ControllerKeyReference
                    .valueOf(movementTag.getString("Backward"));
            walkLeft = ControllerKeyReference
                    .valueOf(movementTag.getString("Left"));
            walkRight = ControllerKeyReference
                    .valueOf(movementTag.getString("Right"));
            walkSprint = ControllerKeyReference
                    .valueOf(movementTag.getString("Sprint"));
            jump = ControllerKeyReference
                    .valueOf(movementTag.getString("Jump"));

            TagStructure cameraTag = tagStructure.getStructure("Camera");
            cameraSensitivity = cameraTag.getDouble("Sensitivity");

            TagStructure actionTag = tagStructure.getStructure("Action");
            left = ControllerKeyReference.valueOf(actionTag.getString("Left"));
            right = ControllerKeyReference
                    .valueOf(actionTag.getString("Right"));

            TagStructure menuTag = tagStructure.getStructure("Menu");
            inventory = ControllerKeyReference
                    .valueOf(menuTag.getString("Inventory"));
            menu = ControllerKeyReference.valueOf(menuTag.getString("Menu"));
            chat = ControllerKeyReference.valueOf(menuTag.getString("Chat"));

            TagStructure hotbarTag = tagStructure.getStructure("Hotbar");
            hotbarAdd =
                    ControllerKeyReference.valueOf(hotbarTag.getString("Add"));
            hotbarSubtract = ControllerKeyReference
                    .valueOf(hotbarTag.getString("Subtract"));
            hotbarLeft =
                    ControllerKeyReference.valueOf(hotbarTag.getString("Left"));
            hotbarBoth =
                    ControllerKeyReference.valueOf(hotbarTag.getString("Both"));
            hotbar0 = ControllerKeyReference.valueOf(hotbarTag.getString("0"));
            hotbar1 = ControllerKeyReference.valueOf(hotbarTag.getString("1"));
            hotbar2 = ControllerKeyReference.valueOf(hotbarTag.getString("2"));
            hotbar3 = ControllerKeyReference.valueOf(hotbarTag.getString("3"));
            hotbar4 = ControllerKeyReference.valueOf(hotbarTag.getString("4"));
            hotbar5 = ControllerKeyReference.valueOf(hotbarTag.getString("5"));
            hotbar6 = ControllerKeyReference.valueOf(hotbarTag.getString("6"));
            hotbar7 = ControllerKeyReference.valueOf(hotbarTag.getString("7"));
            hotbar8 = ControllerKeyReference.valueOf(hotbarTag.getString("8"));
            hotbar9 = ControllerKeyReference.valueOf(hotbarTag.getString("9"));
            scrollSensitivity = hotbarTag.getDouble("Sensitivity");

            guiController.onPress(player, key -> {
                if (!player.currentGui()
                        .filter(gui -> gui instanceof GuiChatWrite)
                        .isPresent() && menu.isPressed(key, controller)) {
                    if (!player.closeGui()) {
                        player.openGui(new GuiPause(player.game(), player,
                                player.game().engine().guiStyle()));
                    }
                    return true;
                }
                if (!player.currentGui()
                        .filter(gui -> gui instanceof GuiChatWrite)
                        .isPresent() && inventory.isPressed(key, controller)) {
                    if (!player.closeGui()) {
                        player.world().send(new PacketInteraction(
                                PacketInteraction.OPEN_INVENTORY));
                    }
                    return true;
                }
                if (!player.currentGui()
                        .filter(gui -> gui instanceof GuiChatWrite)
                        .isPresent() && chat.isPressed(key, controller)) {
                    if (!player.hasGui()) {
                        player.openGui(new GuiChatWrite(player.game(), player,
                                player.game().engine().guiStyle()));
                        return true;
                    }
                }
                return false;
            });
        }

        @Override
        public Vector2 walk() {
            double x = 0.0, y = 0.0;
            if (walkForward.isDown(controller)) {
                y += 1.0;
            }
            if (walkBackward.isDown(controller)) {
                y -= 1.0;
            }
            if (walkLeft.isDown(controller)) {
                x -= 1.0;
            }
            if (walkRight.isDown(controller)) {
                x += 1.0;
            }
            if (!walkSprint.isDown(controller)) {
                x *= 0.4;
                y *= 0.4;
            }
            return new Vector2d(x, y);
        }

        @Override
        public Vector2 camera(double delta) {
            Vector2 camera =
                    new Vector2d(controller.deltaX(), controller.deltaY());
            return camera.multiply(cameraSensitivity);
        }

        @Override
        public Vector2 hitDirection() {
            return Vector2d.ZERO;
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
        public int hotbarLeft(int previous) {
            if (hotbarLeft.isDown(controller) ||
                    hotbarBoth.isDown(controller)) {
                if (hotbarAdd.isPressed(controller)) {
                    previous++;
                }
                if (hotbarSubtract.isPressed(controller)) {
                    previous--;
                }
                Optional<ControllerKeyReference> hotbarCheck =
                        ControllerKeyReference
                                .isDown(controller, hotbar0, hotbar1, hotbar2,
                                        hotbar3, hotbar4, hotbar5, hotbar6,
                                        hotbar7, hotbar8, hotbar9);
                if (hotbarCheck.isPresent()) {
                    ControllerKeyReference hotbar = hotbarCheck.get();
                    if (hotbar == hotbar0) {
                        previous = 0;
                    } else if (hotbar == hotbar1) {
                        previous = 1;
                    } else if (hotbar == hotbar2) {
                        previous = 2;
                    } else if (hotbar == hotbar3) {
                        previous = 3;
                    } else if (hotbar == hotbar4) {
                        previous = 4;
                    } else if (hotbar == hotbar5) {
                        previous = 5;
                    } else if (hotbar == hotbar6) {
                        previous = 6;
                    } else if (hotbar == hotbar7) {
                        previous = 7;
                    } else if (hotbar == hotbar8) {
                        previous = 8;
                    } else if (hotbar == hotbar9) {
                        previous = 9;
                    }
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
                Optional<ControllerKeyReference> hotbarCheck =
                        ControllerKeyReference
                                .isDown(controller, hotbar0, hotbar1, hotbar2,
                                        hotbar3, hotbar4, hotbar5, hotbar6,
                                        hotbar7, hotbar8, hotbar9);
                if (hotbarCheck.isPresent()) {
                    ControllerKeyReference hotbar = hotbarCheck.get();
                    if (hotbar == hotbar0) {
                        previous = 0;
                    } else if (hotbar == hotbar1) {
                        previous = 1;
                    } else if (hotbar == hotbar2) {
                        previous = 2;
                    } else if (hotbar == hotbar3) {
                        previous = 3;
                    } else if (hotbar == hotbar4) {
                        previous = 4;
                    } else if (hotbar == hotbar5) {
                        previous = 5;
                    } else if (hotbar == hotbar6) {
                        previous = 6;
                    } else if (hotbar == hotbar7) {
                        previous = 7;
                    } else if (hotbar == hotbar8) {
                        previous = 8;
                    } else if (hotbar == hotbar9) {
                        previous = 9;
                    }
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
