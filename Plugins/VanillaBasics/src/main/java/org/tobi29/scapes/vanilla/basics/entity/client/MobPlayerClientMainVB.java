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
package org.tobi29.scapes.vanilla.basics.entity.client;

import org.tobi29.scapes.Scapes;
import org.tobi29.scapes.block.Inventory;
import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.client.Playlist;
import org.tobi29.scapes.client.ScapesClient;
import org.tobi29.scapes.client.gui.GuiChatWrite;
import org.tobi29.scapes.client.gui.GuiPause;
import org.tobi29.scapes.engine.gui.Gui;
import org.tobi29.scapes.engine.input.ControllerDefault;
import org.tobi29.scapes.engine.input.ControllerKey;
import org.tobi29.scapes.engine.opengl.texture.Texture;
import org.tobi29.scapes.engine.utils.math.AABB;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.Frustum;
import org.tobi29.scapes.engine.utils.math.vector.Vector2;
import org.tobi29.scapes.engine.utils.math.vector.Vector2d;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;
import org.tobi29.scapes.entity.CreatureType;
import org.tobi29.scapes.entity.WieldMode;
import org.tobi29.scapes.entity.client.EntityContainerClient;
import org.tobi29.scapes.entity.client.MobClient;
import org.tobi29.scapes.entity.client.MobLivingClient;
import org.tobi29.scapes.entity.client.MobPlayerClientMain;
import org.tobi29.scapes.entity.model.MobLivingModelHuman;
import org.tobi29.scapes.entity.model.MobModel;
import org.tobi29.scapes.packets.PacketInteraction;
import org.tobi29.scapes.packets.PacketItemUse;
import org.tobi29.scapes.vanilla.basics.gui.GuiPlayerInventory;

import java.util.Optional;

public class MobPlayerClientMainVB extends MobPlayerClientMain
        implements EntityContainerClient {
    protected long punchLeft = -1, punchRight = -1;
    protected float chargeLeft, chargeRight;

    public MobPlayerClientMainVB(WorldClient world, Vector3 pos, Vector3 speed,
            double xRot, double zRot, String nickname) {
        super(world, pos, speed, new AABB(-0.4, -0.4, -1, 0.4, 0.4, 0.9), 100,
                100, new Frustum(90, 1, 0.1, 24), new Frustum(50, 1, 0.1, 2),
                nickname);
        rot.setX(xRot);
        rot.setZ(zRot);
    }

    @Override
    public void update(double delta) {
        super.update(delta);
        Controller controller =
                ((ScapesClient) game.engine().game()).inputMode()
                        .playerController();
        if (controller.inventory()) {
            if (!(currentGui instanceof GuiChatWrite)) {
                if (hasGui()) {
                    world.connection().send(new PacketInteraction(
                            PacketInteraction.CLOSE_INVENTORY));
                } else {
                    world.connection().send(new PacketInteraction(
                            PacketInteraction.OPEN_INVENTORY));
                }
            }
        }
        if (controller.chat()) {
            if (!hasGui()) {
                openGui(new GuiChatWrite(game, this));
            }
        }
        if (controller.menu()) {
            if (hasGui()) {
                closeGui();
            } else {
                openGui(new GuiPause(game, this));
            }
        }
        if (currentGui == null) {
            // Inventory
            int previous = inventorySelectLeft;
            int hotbar = controller.hotbarLeft(previous);
            if (hotbar != previous) {
                setInventorySelectLeft(hotbar);
                world.connection().send(new PacketInteraction(
                        PacketInteraction.INVENTORY_SLOT_LEFT,
                        (byte) inventorySelectLeft));
            }
            previous = inventorySelectRight;
            hotbar = controller.hotbarRight(previous);
            if (hotbar != previous) {
                setInventorySelectRight(hotbar);
                world.connection().send(new PacketInteraction(
                        PacketInteraction.INVENTORY_SLOT_RIGHT,
                        (byte) inventorySelectRight));
            }
            // Debug
            if (Scapes.debug) {
                ControllerDefault controllerDefault =
                        game.engine().controller();
                if (controllerDefault.isPressed(ControllerKey.KEY_F5)) {
                    flying = !flying;
                }
                if (flying) {
                    if (controllerDefault.isDown(ControllerKey.KEY_Q)) {
                        speed.plusZ(1.0);
                    }
                    if (controllerDefault.isDown(ControllerKey.KEY_C)) {
                        speed.plusZ(-1.0);
                    }
                }
            }
            // Movement
            if (controller.jump()) {
                if (swimming) {
                    speed.plusZ(1.2);
                } else if (ground) {
                    speed.setZ(5.1);
                    ground = false;
                }
            }
            Vector2 camera = controller.camera(delta);
            rot.setZ((rot.doubleZ() - camera.doubleX()) % 360);
            rot.setX(FastMath.min(89,
                    FastMath.max(-89, rot.doubleX() - camera.doubleY())));
            Vector2 walk = controller.walk();
            double walkSpeed = FastMath.clamp(
                    FastMath.max(FastMath.abs(walk.doubleX()),
                            FastMath.abs(walk.doubleY())), 0.0, 1.0) * 120.0;
            if (!ground && !slidingWall && !inWater && !flying) {
                walkSpeed *= 0.0006;
            } else if (!ground && !inWater && !flying) {
                walkSpeed *= 0.05;
            } else if (inWater) {
                walkSpeed *= 0.2;
            }
            if (walkSpeed > 0.0) {
                double dir = (FastMath.pointDirection(Vector2d.ZERO, walk) +
                        rot.doubleZ() - 90.0) * FastMath.DEG_2_RAD;
                walkSpeed *= delta;
                speed.plusX(FastMath.cosTable(dir) * walkSpeed);
                speed.plusY(FastMath.sinTable(dir) * walkSpeed);
            }
            // Placement
            if (controller.left() && wieldMode() != WieldMode.RIGHT) {
                if (chargeLeft < 0.01f) {
                    if (punchLeft == -1) {
                        punchLeft = System.currentTimeMillis();
                    }
                }
            } else if (punchLeft != -1) {
                updatePosition();
                breakParticles(world.terrain(), 16);
                world.connection().send(new PacketItemUse(true, FastMath.min(
                        (double) (System.currentTimeMillis() - punchLeft) /
                                leftWeapon().material().hitWait(leftWeapon()),
                        0.5) * 2.0));
                punchLeft = -1;
            }
            if (controller.right() && wieldMode() != WieldMode.LEFT) {
                if (chargeRight < 0.01f) {
                    if (punchRight == -1) {
                        punchRight = System.currentTimeMillis();
                    }
                }
            } else if (punchRight != -1) {
                updatePosition();
                breakParticles(world.terrain(), 16);
                world.connection().send(new PacketItemUse(false, FastMath.min(
                        (double) (System.currentTimeMillis() - punchRight) /
                                rightWeapon().material().hitWait(rightWeapon()),
                        0.5) * 2.0));
                punchRight = -1;
            }
            long time = System.currentTimeMillis();
            float swingTime = leftWeapon().material().hitWait(leftWeapon());
            long punch;
            if (punchLeft == -1) {
                punch = 0;
            } else {
                punch = time - punchLeft;
            }
            if (punch > 0) {
                chargeLeft = FastMath.min(punch / swingTime, 0.5f);
            } else {
                if (chargeLeft > 0.0f) {
                    if (chargeLeft >= 0.45f) {
                        chargeLeft = 0.0f;
                    } else {
                        chargeLeft = 0.0f;
                    }
                }
            }
            swingTime = rightWeapon().material().hitWait(rightWeapon());
            if (punchRight == -1) {
                punch = 0;
            } else {
                punch = time - punchRight;
            }
            if (punch > 0) {
                chargeRight = FastMath.min(punch / swingTime, 0.5f);
            } else {
                if (chargeRight > 0.0f) {
                    if (chargeRight >= 0.45f) {
                        chargeRight = 0.0f;
                    } else {
                        chargeRight = 0.0f;
                    }
                }
            }
        }
    }

    @Override
    public void onNotice(MobClient notice) {
        if (notice instanceof MobLivingClient) {
            if (((MobLivingClient) notice).creatureType() ==
                    CreatureType.MONSTER) {
                game.playlist().setMusic(Playlist.Music.BATTLE, this);
            }
        }
    }

    @Override
    public Vector3 viewOffset() {
        return new Vector3d(0.0, 0.0, 0.63);
    }

    @Override
    public ItemStack leftWeapon() {
        return inventoryContainer.item(inventorySelectLeft);
    }

    @Override
    public ItemStack rightWeapon() {
        return inventoryContainer.item(inventorySelectRight);
    }

    @Override
    public WieldMode wieldMode() {
        return inventorySelectLeft == inventorySelectRight ? WieldMode.RIGHT :
                WieldMode.DUAL;
    }

    @Override
    public float leftCharge() {
        return chargeLeft;
    }

    @Override
    public float rightCharge() {
        return chargeRight;
    }

    @Override
    public Optional<Gui> gui(MobPlayerClientMain player) {
        if (player instanceof MobPlayerClientMainVB) {
            return Optional
                    .of(new GuiPlayerInventory((MobPlayerClientMainVB) player));
        }
        return Optional.empty();
    }

    @Override
    public Inventory inventory(String id) {
        return inventories.get(id);
    }

    @Override
    public Optional<MobModel> createModel() {
        Texture texture = world.scene().skinStorage().get(skin);
        return Optional.of(new MobLivingModelHuman(this, texture));
    }
}
