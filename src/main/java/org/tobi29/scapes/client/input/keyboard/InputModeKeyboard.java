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

package org.tobi29.scapes.client.input.keyboard;

import org.tobi29.scapes.client.ScapesClient;
import org.tobi29.scapes.client.gui.GuiControlsDefault;
import org.tobi29.scapes.client.input.InputMode;
import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.gui.Gui;
import org.tobi29.scapes.engine.gui.GuiController;
import org.tobi29.scapes.engine.gui.GuiControllerDefault;
import org.tobi29.scapes.engine.input.ControllerDefault;
import org.tobi29.scapes.engine.input.ControllerKey;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.vector.Vector2;
import org.tobi29.scapes.engine.utils.math.vector.Vector2d;
import org.tobi29.scapes.entity.client.MobPlayerClientMain;

public class InputModeKeyboard implements InputMode {
    private final ControllerDefault controller;
    private final TagStructure tagStructure;
    private final GuiController guiController;
    private final PlayerController playerController;

    public InputModeKeyboard(ScapesEngine engine, ControllerDefault controller,
            TagStructure tagStructure) {
        this.tagStructure = tagStructure.getStructure("Default");
        defaultConfig(this.tagStructure);
        this.controller = controller;
        TagStructure miscTag = this.tagStructure.getStructure("Misc");
        TagStructure miscScrollTag = miscTag.getStructure("Scroll");
        double scrollSensitivity = miscScrollTag.getDouble("Sensitivity");
        guiController =
                new GuiControllerDefault(engine, controller, scrollSensitivity);
        playerController = new PlayerController();
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
        return controller.isActive();
    }

    @Override
    public Gui createControlsGui(GameState state, Gui prev) {
        return new GuiControlsDefault(state, prev,
                (ScapesClient) state.getEngine().getGame(), tagStructure,
                controller);
    }

    @Override
    public MobPlayerClientMain.Controller getPlayerController() {
        return playerController;
    }

    @Override
    public GuiController getGuiController() {
        return guiController;
    }

    @Override
    public String toString() {
        return "Keyboard + Mouse";
    }

    private class PlayerController implements MobPlayerClientMain.Controller {
        private final ControllerKey walkForward, walkBackward, walkLeft,
                walkRight, walkSprint, jump, inventory, menu, chat, left, right,
                hotbarAdd, hotbarSubtract, hotbarLeft, hotbar0, hotbar1,
                hotbar2, hotbar3, hotbar4, hotbar5, hotbar6, hotbar7, hotbar8,
                hotbar9;
        private final double cameraSensitivity, scrollSensitivity;

        public PlayerController() {
            TagStructure movementTag = tagStructure.getStructure("Movement");
            walkForward =
                    ControllerKey.valueOf(movementTag.getString("Forward"));
            walkBackward =
                    ControllerKey.valueOf(movementTag.getString("Backward"));
            walkLeft = ControllerKey.valueOf(movementTag.getString("Left"));
            walkRight = ControllerKey.valueOf(movementTag.getString("Right"));
            walkSprint = ControllerKey.valueOf(movementTag.getString("Sprint"));
            jump = ControllerKey.valueOf(movementTag.getString("Jump"));

            TagStructure cameraTag = tagStructure.getStructure("Camera");
            cameraSensitivity = cameraTag.getDouble("Sensitivity");

            TagStructure actionTag = tagStructure.getStructure("Action");
            left = ControllerKey.valueOf(actionTag.getString("Left"));
            right = ControllerKey.valueOf(actionTag.getString("Right"));

            TagStructure menuTag = tagStructure.getStructure("Menu");
            inventory = ControllerKey.valueOf(menuTag.getString("Inventory"));
            menu = ControllerKey.valueOf(menuTag.getString("Menu"));
            chat = ControllerKey.valueOf(menuTag.getString("Chat"));

            TagStructure hotbarTag = tagStructure.getStructure("Hotbar");
            hotbarAdd = ControllerKey.valueOf(hotbarTag.getString("Add"));
            hotbarSubtract =
                    ControllerKey.valueOf(hotbarTag.getString("Subtract"));
            hotbarLeft = ControllerKey.valueOf(hotbarTag.getString("Left"));
            hotbar0 = ControllerKey.valueOf(hotbarTag.getString("0"));
            hotbar1 = ControllerKey.valueOf(hotbarTag.getString("1"));
            hotbar2 = ControllerKey.valueOf(hotbarTag.getString("2"));
            hotbar3 = ControllerKey.valueOf(hotbarTag.getString("3"));
            hotbar4 = ControllerKey.valueOf(hotbarTag.getString("4"));
            hotbar5 = ControllerKey.valueOf(hotbarTag.getString("5"));
            hotbar6 = ControllerKey.valueOf(hotbarTag.getString("6"));
            hotbar7 = ControllerKey.valueOf(hotbarTag.getString("7"));
            hotbar8 = ControllerKey.valueOf(hotbarTag.getString("8"));
            hotbar9 = ControllerKey.valueOf(hotbarTag.getString("9"));
            scrollSensitivity = hotbarTag.getDouble("Sensitivity");
        }

        @Override
        public Vector2 getWalk() {
            double x = 0.0, y = 0.0;
            if (controller.isDown(walkForward)) {
                y += 1.0d;
            }
            if (controller.isDown(walkBackward)) {
                y -= 1.0d;
            }
            if (controller.isDown(walkLeft)) {
                x -= 1.0d;
            }
            if (controller.isDown(walkRight)) {
                x += 1.0d;
            }
            if (!controller.isDown(walkSprint)) {
                x *= 0.4;
                y *= 0.4;
            }
            return new Vector2d(x, y);
        }

        @Override
        public Vector2 getCamera(double delta) {
            Vector2 camera = new Vector2d(controller.getDeltaX(),
                    controller.getDeltaY());
            return camera.multiply(cameraSensitivity);
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
                if (controller.isPressed(hotbar0)) {
                    previous = 0;
                } else if (controller.isPressed(hotbar1)) {
                    previous = 1;
                } else if (controller.isPressed(hotbar2)) {
                    previous = 2;
                } else if (controller.isPressed(hotbar3)) {
                    previous = 3;
                } else if (controller.isPressed(hotbar4)) {
                    previous = 4;
                } else if (controller.isPressed(hotbar5)) {
                    previous = 5;
                } else if (controller.isPressed(hotbar6)) {
                    previous = 6;
                } else if (controller.isPressed(hotbar7)) {
                    previous = 7;
                } else if (controller.isPressed(hotbar8)) {
                    previous = 8;
                } else if (controller.isPressed(hotbar9)) {
                    previous = 9;
                }
                if (controller.isPressed(hotbarAdd)) {
                    previous++;
                }
                if (controller.isPressed(hotbarSubtract)) {
                    previous--;
                }
                previous += FastMath.round(
                        controller.getScrollY() * scrollSensitivity);
            }
            return previous;
        }

        @Override
        public int getHotbarRight(int previous) {
            if (!controller.isDown(hotbarLeft)) {
                if (controller.isPressed(hotbar0)) {
                    previous = 0;
                } else if (controller.isPressed(hotbar1)) {
                    previous = 1;
                } else if (controller.isPressed(hotbar2)) {
                    previous = 2;
                } else if (controller.isPressed(hotbar3)) {
                    previous = 3;
                } else if (controller.isPressed(hotbar4)) {
                    previous = 4;
                } else if (controller.isPressed(hotbar5)) {
                    previous = 5;
                } else if (controller.isPressed(hotbar6)) {
                    previous = 6;
                } else if (controller.isPressed(hotbar7)) {
                    previous = 7;
                } else if (controller.isPressed(hotbar8)) {
                    previous = 8;
                } else if (controller.isPressed(hotbar9)) {
                    previous = 9;
                }
                if (controller.isPressed(hotbarAdd)) {
                    previous++;
                }
                if (controller.isPressed(hotbarSubtract)) {
                    previous--;
                }
                previous += FastMath.round(
                        controller.getScrollY() * scrollSensitivity);
            }
            return previous;
        }
    }
}
