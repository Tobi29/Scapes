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
package org.tobi29.scapes.client.input.touch;

import org.tobi29.scapes.client.gui.desktop.GuiMessage;
import org.tobi29.scapes.client.input.InputMode;
import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.gui.*;
import org.tobi29.scapes.engine.input.ControllerTouch;
import org.tobi29.scapes.engine.utils.math.vector.MutableVector2;
import org.tobi29.scapes.engine.utils.math.vector.MutableVector2d;
import org.tobi29.scapes.engine.utils.math.vector.Vector2;
import org.tobi29.scapes.engine.utils.math.vector.Vector2d;
import org.tobi29.scapes.entity.client.MobPlayerClientMain;

public class InputModeTouch implements InputMode {
    private final ControllerTouch controller;
    private final GuiController guiController;
    private final PlayerController playerController;
    private final MutableVector2 swipe = new MutableVector2d();
    private boolean walkUp, walkDown, walkLeft, walkRight, inventoryOpen,
            menuOpen;

    public InputModeTouch(ScapesEngine engine, ControllerTouch controller) {
        this.controller = controller;
        guiController = new GuiControllerTouch(engine, controller);
        playerController = new PlayerController();
    }

    @Override
    public boolean poll() {
        inventoryOpen = false;
        menuOpen = false;
        return controller.isActive();
    }

    @Override
    public Gui createControlsGUI(GameState state, Gui prev) {
        return new GuiMessage(state, prev, "Oi!", "No haxing pls >:V",
                prev.style());
    }

    @Override
    public void createInGameGUI(Gui gui) {
        int size = 120, gap = 20;
        GuiComponentGroup pad = gui.add(610, 270,
                p -> new GuiComponentGroup(p, size * 3 + (gap << 1),
                        (size << 1) + gap));
        GuiComponentButton padUp = pad.add(size + gap, 0,
                p -> new GuiComponentButton(p, size, size));
        GuiComponentButton padDown = pad.add(size + gap, size + gap,
                p -> new GuiComponentButton(p, size, size));
        GuiComponentButton padLeft = pad.add(0, size + gap,
                p -> new GuiComponentButton(p, size, size));
        GuiComponentButton padRight = pad.add(size + gap << 1, size + gap,
                p -> new GuiComponentButton(p, size, size));
        GuiComponentButton inventory =
                gui.add(0, 0, p -> new GuiComponentButton(p, 40, 40));
        GuiComponentButton menu =
                gui.add(50, 0, p -> new GuiComponentButton(p, 40, 40));
        GuiComponentPane swipe =
                gui.add(0, 0, p -> new GuiComponentPane(p, 960, 540));

        padUp.onPressLeft(event -> {
            walkUp = true;
        });
        padUp.onDropLeft(event -> {
            walkUp = false;
        });
        padUp.onDragLeft(this::swipe);
        padDown.onPressLeft(event -> {
            walkDown = true;
        });
        padDown.onDropLeft(event -> {
            walkDown = false;
        });
        padDown.onDragLeft(this::swipe);
        padLeft.onPressLeft(event -> {
            walkLeft = true;
        });
        padLeft.onDropLeft(event -> {
            walkLeft = false;
        });
        padLeft.onDragLeft(this::swipe);
        padRight.onPressLeft(event -> {
            walkRight = true;
        });
        padRight.onDropLeft(event -> {
            walkRight = false;
        });
        padRight.onDragLeft(this::swipe);
        inventory.onClickLeft(event -> {
            inventoryOpen = true;
        });
        menu.onClickLeft(event -> {
            menuOpen = true;
        });
        swipe.onDragLeft(this::swipe);
    }

    @Override
    public MobPlayerClientMain.Controller playerController() {
        return playerController;
    }

    @Override
    public GuiController guiController() {
        return guiController;
    }

    private void swipe(GuiComponentEvent event) {
        swipe.plusX(event.relativeX()).plusY(event.relativeY());
    }

    @Override
    public String toString() {
        return "Touchscreen";
    }

    private class PlayerController implements MobPlayerClientMain.Controller {
        @Override
        public Vector2 walk() {
            double x = 0.0, y = 0.0;
            if (walkUp) {
                y += 1.0;
            }
            if (walkDown) {
                y -= 1.0;
            }
            if (walkLeft) {
                x -= 1.0;
            }
            if (walkRight) {
                x += 1.0;
            }
            return new Vector2d(x, y);
        }

        @Override
        public Vector2 camera(double delta) {
            Vector2 camera = swipe.now();
            swipe.set(0.0, 0.0);
            return camera;
        }

        @Override
        public boolean left() {
            return false;
        }

        @Override
        public boolean right() {
            return false;
        }

        @Override
        public boolean jump() {
            return false;
        }

        @Override
        public boolean inventory() {
            return inventoryOpen;
        }

        @Override
        public boolean menu() {
            return menuOpen;
        }

        @Override
        public boolean chat() {
            return false;
        }

        @Override
        public int hotbarLeft(int previous) {
            return 0;
        }

        @Override
        public int hotbarRight(int previous) {
            return 0;
        }
    }
}
