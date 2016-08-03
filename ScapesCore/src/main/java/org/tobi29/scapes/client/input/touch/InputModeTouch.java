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

package org.tobi29.scapes.client.input.touch;

import java8.util.Optional;
import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.client.gui.desktop.GuiMessage;
import org.tobi29.scapes.client.gui.desktop.GuiPause;
import org.tobi29.scapes.client.input.InputMode;
import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.gui.*;
import org.tobi29.scapes.engine.input.ControllerTouch;
import org.tobi29.scapes.engine.utils.graphics.Cam;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.matrix.Matrix4f;
import org.tobi29.scapes.engine.utils.math.vector.*;
import org.tobi29.scapes.entity.client.MobPlayerClientMain;
import org.tobi29.scapes.packets.PacketInteraction;

public class InputModeTouch implements InputMode {
    private final ControllerTouch controller;
    private final GuiController guiController;
    private final MutableVector2 swipe = new MutableVector2d(), direction =
            new MutableVector2d();
    private final Matrix4f matrix1 = new Matrix4f(), matrix2 = new Matrix4f();
    private Optional<Vector2> swipeStart;
    private boolean walkUp, walkDown, walkLeft, walkRight, leftHand, rightHand;
    private long lastTouch;

    public InputModeTouch(ScapesEngine engine, ControllerTouch controller) {
        this.controller = controller;
        guiController = new GuiControllerTouch(engine, controller);
    }

    @Override
    public boolean poll() {
        controller.poll();
        return controller.isActive();
    }

    @Override
    public Gui createControlsGUI(GameState state, Gui prev) {
        return new GuiMessage(state, prev, "Oi!", "No haxing pls >:V",
                prev.style());
    }

    @Override
    public void createInGameGUI(Gui gui, WorldClient world) {
        int size = 100, gap = 20;
        GuiComponentGroup pad =
                gui.add(610, 310, size * 3 + (gap << 1), (size << 1) + gap,
                        GuiComponentGroup::new);
        GuiComponentButton padUp =
                pad.add(size + gap, 0, size, size, GuiComponentButton::new);
        GuiComponentButton padDown = pad.add(size + gap, size + gap, size, size,
                GuiComponentButton::new);
        GuiComponentButton padLeft =
                pad.add(0, size + gap, size, size, GuiComponentButton::new);
        GuiComponentButton padRight =
                pad.add(size + gap << 1, size + gap, size, size,
                        GuiComponentButton::new);
        GuiComponentButton inventory =
                gui.add(0, 0, 40, 40, GuiComponentButton::new);
        GuiComponentButton menu =
                gui.add(50, 0, 40, 40, GuiComponentButton::new);
        GuiComponentPane swipe = gui.add(0, 0, -1, -1, GuiComponentPane::new);

        padUp.on(GuiEvent.PRESS_LEFT, event -> walkUp = true);
        padUp.on(GuiEvent.DROP_LEFT, event -> walkUp = false);
        padUp.on(GuiEvent.DRAG_LEFT, this::swipe);
        padDown.on(GuiEvent.PRESS_LEFT, event -> walkDown = true);
        padDown.on(GuiEvent.DROP_LEFT, event -> walkDown = false);
        padDown.on(GuiEvent.DRAG_LEFT, this::swipe);
        padLeft.on(GuiEvent.PRESS_LEFT, event -> walkLeft = true);
        padLeft.on(GuiEvent.DROP_LEFT, event -> walkLeft = false);
        padLeft.on(GuiEvent.DRAG_LEFT, this::swipe);
        padRight.on(GuiEvent.PRESS_LEFT, event -> walkRight = true);
        padRight.on(GuiEvent.DROP_LEFT, event -> walkRight = false);
        padRight.on(GuiEvent.DRAG_LEFT, this::swipe);
        inventory.on(GuiEvent.CLICK_LEFT, event -> {
            if (!world.player().closeGui()) {
                world.send(new PacketInteraction(
                        PacketInteraction.OPEN_INVENTORY));
            }
        });
        menu.on(GuiEvent.CLICK_LEFT, event -> {
            if (!world.player().closeGui()) {
                world.player().openGui(
                        new GuiPause(world.game(), world.player(),
                                world.game().engine().guiStyle()));
            }
        });
        swipe.on(GuiEvent.DRAG_LEFT, this::swipe);
        swipe.on(GuiEvent.PRESS_LEFT, event -> {
            swipeStart = Optional.of(new Vector2d(event.x(), event.y()));
            if (rightHand) {
                rightHand = false;
            } else if (System.currentTimeMillis() - lastTouch < 250) {
                rightHand = true;
            }
            lastTouch = System.currentTimeMillis();
        });
        swipe.on(GuiEvent.DRAG_LEFT, event -> {
            double x = event.x() / 480.0 - 1.0;
            double y = 1.0 - event.y() / 270.0;
            Cam cam = world.scene().cam();
            matrix1.identity();
            matrix1.perspective(cam.fov, 1920.0f / 1080.0f, cam.near, cam.far);
            matrix1.rotate(-cam.tilt, 0.0f, 0.0f, 1.0f);
            matrix1.rotate(-cam.pitch - 90.0f, 1.0f, 0.0f, 0.0f);
            matrix1.rotate(-cam.yaw + 90.0f, 0.0f, 0.0f, 1.0f);
            matrix1.invert(matrix1, matrix2);
            Vector3 pos = matrix2.multiply(new Vector3d(x, y, 1.0));
            x = FastMath.pointDirection(0.0, 0.0, pos.doubleX(), pos.doubleY());
            y = FastMath.pointDirection(0.0, 0.0,
                    FastMath.pointDistance(0.0, 0.0, pos.doubleX(),
                            pos.doubleY()), pos.doubleZ());
            x = FastMath.angleDiff(cam.yaw, x);
            y -= cam.pitch;
            direction.set(x, y);
            if (swipeStart.isPresent()) {
                if (!leftHand && !rightHand &&
                        System.currentTimeMillis() - lastTouch >= 250) {
                    leftHand = true;
                }
                if (FastMath.pointDistance(swipeStart.get(),
                        new Vector2d(event.x(), event.y())) > 10.0) {
                    swipeStart = Optional.empty();
                }
            }
        });
        swipe.on(GuiEvent.DROP_LEFT, event -> {
            leftHand = false;
            rightHand = false;
            swipeStart = Optional.empty();
        });
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

    private void swipe(GuiComponentEvent event) {
        swipe.plusX(event.relativeX()).plusY(event.relativeY());
    }

    @Override
    public String toString() {
        return "Touchscreen";
    }

    private class PlayerController implements MobPlayerClientMain.Controller {
        public PlayerController(MobPlayerClientMain player) {
        }

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
        public Vector2 hitDirection() {
            return direction.now();
        }

        @Override
        public boolean left() {
            return leftHand;
        }

        @Override
        public boolean right() {
            return rightHand;
        }

        @Override
        public boolean jump() {
            return false;
        }

        @Override
        public int hotbarLeft(int previous) {
            return 0;
        }

        @Override
        public int hotbarRight(int previous) {
            return 9;
        }
    }
}
