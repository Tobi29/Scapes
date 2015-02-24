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

package org.tobi29.scapes.entity.client;

import org.tobi29.scapes.Scapes;
import org.tobi29.scapes.block.AABBElement;
import org.tobi29.scapes.block.BlockType;
import org.tobi29.scapes.block.TerrainTexture;
import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.chunk.terrain.TerrainClient;
import org.tobi29.scapes.client.Playlist;
import org.tobi29.scapes.client.ScapesClient;
import org.tobi29.scapes.client.connection.ClientConnection;
import org.tobi29.scapes.client.gui.GuiChatWrite;
import org.tobi29.scapes.client.gui.GuiPause;
import org.tobi29.scapes.client.states.GameStateGameMP;
import org.tobi29.scapes.engine.gui.Gui;
import org.tobi29.scapes.engine.input.ControllerDefault;
import org.tobi29.scapes.engine.input.ControllerKey;
import org.tobi29.scapes.engine.utils.Pool;
import org.tobi29.scapes.engine.utils.math.AABB;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.PointerPane;
import org.tobi29.scapes.engine.utils.math.vector.Vector2;
import org.tobi29.scapes.engine.utils.math.vector.Vector2d;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;
import org.tobi29.scapes.entity.CreatureType;
import org.tobi29.scapes.entity.particle.ParticleBlock;
import org.tobi29.scapes.entity.particle.ParticleManager;
import org.tobi29.scapes.packets.PacketInteraction;
import org.tobi29.scapes.packets.PacketItemUse;

import java.util.Iterator;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class MobPlayerClientMain extends MobPlayerClient {
    private final GameStateGameMP game;
    private int blockWait;
    private Gui currentGui;
    private long punchLeft = -1, punchRight = -1;
    private boolean flying;
    private int swim;
    private double gravitationMultiplier = 1.0, airFriction = 0.2,
            groundFriction = 1.6, wallFriction = 2.0, waterFriction = 8.0,
            stepHeight = 1.0;
    private float chargeLeft, chargeRight;

    public MobPlayerClientMain(WorldClient world, Vector3 pos, Vector3 speed,
            double xRot, double zRot, String nickname, String skin) {
        super(world, pos, speed, xRot, zRot, nickname, skin);
        game = world.getGame();
    }

    private static Iterator<AABB> getCollisions(Pool<AABBElement> aabbs) {
        return aabbs.stream().filter(AABBElement::isSolid)
                .map(AABBElement::getAABB).iterator();
    }

    private void updateVelocity(double gravitation, double delta) {
        speed.div(1.0 + airFriction * delta);
        if (inWater) {
            speed.div(1.0 + waterFriction * delta);
        } else {
            if (ground) {
                speed.div(1.0 + groundFriction * delta * gravitation);
            }
            if (slidingWall) {
                speed.div(1.0 + wallFriction * delta);
            }
        }
        speed.plusZ(-gravitation * gravitationMultiplier * delta);
    }

    private void move(AABB aabb, Pool<AABBElement> aabbs, double goX,
            double goY, double goZ) {
        boolean ground = false;
        boolean slidingWall = false;
        double lastGoZ = aabb.moveOutZ(getCollisions(aabbs), goZ);
        pos.plusZ(lastGoZ);
        aabb.add(0, 0, lastGoZ);
        if (lastGoZ - goZ > 0) {
            ground = true;
        }
        // Walk
        boolean walking = true;
        while (walking) {
            walking = false;
            if (goX != 0.0d) {
                double lastGoX = aabb.moveOutX(getCollisions(aabbs), goX);
                if (lastGoX != 0.0d) {
                    pos.plusX(lastGoX);
                    aabb.add(lastGoX, 0.0d, 0.0d);
                    goX -= lastGoX;
                    walking = true;
                }
            }
            if (goY != 0.0d) {
                double lastGoY = aabb.moveOutY(getCollisions(aabbs), goY);
                if (lastGoY != 0.0d) {
                    pos.plusY(lastGoY);
                    aabb.add(0.0d, lastGoY, 0.0d);
                    goY -= lastGoY;
                    walking = true;
                }
            }
        }
        // Check collision
        boolean slidingX = goX != 0.0d;
        boolean slidingY = goY != 0.0d;
        if (slidingX || slidingY) {
            if (stepHeight > 0.0d && (this.ground || inWater)) {
                // Step
                // Calculate step height
                AABB aabbStep = new AABB(aabb).add(goX, 0.0d, 0.0d);
                double stepX =
                        aabbStep.moveOutZ(getCollisions(aabbs), stepHeight);
                aabbStep = new AABB(aabb).add(0.0d, goY, 0.0d);
                double stepY =
                        aabbStep.moveOutZ(getCollisions(aabbs), stepHeight);
                double step = FastMath.max(stepX, stepY);
                aabbStep = new AABB(aabb).add(goX, goY, step);
                step += aabbStep.moveOutZ(getCollisions(aabbs), -step);
                // Check step height
                aabbStep.copy(aabb).add(0.0d, 0.0d, step);
                step = aabb.moveOutZ(getCollisions(aabbs), step);
                // Attempt walk at new height
                double lastGoX = aabbStep.moveOutX(getCollisions(aabbs), goX);
                aabbStep.add(lastGoX, 0.0d, 0.0d);
                double lastGoY = aabbStep.moveOutY(getCollisions(aabbs), goY);
                // Check if walk was successful
                if (lastGoX != 0.0d || lastGoY != 0.0d) {
                    pos.plusX(lastGoX);
                    pos.plusY(lastGoY);
                    aabb.copy(aabbStep).add(0.0d, lastGoY, 0.0d);
                    pos.plusZ(step);
                } else {
                    // Collide
                    slidingWall = true;
                    if (slidingX) {
                        speed.setX(0.0);
                    }
                    if (slidingY) {
                        speed.setY(0.0);
                    }
                }
            } else {
                // Collide
                slidingWall = true;
                if (slidingX) {
                    speed.setX(0.0);
                }
                if (slidingY) {
                    speed.setY(0.0);
                }
            }
        }
        this.ground = ground;
        this.slidingWall = slidingWall;
    }

    private void collide(AABB aabb, Pool<AABBElement> aabbs) {
        boolean inWater = false;
        boolean swimming;
        for (AABBElement element : aabbs) {
            if (aabb.overlay(element.aabb)) {
                if (element.collision.isLiquid()) {
                    inWater = true;
                }
            }
        }
        aabb.minZ = FastMath.mix(aabb.minZ, aabb.maxZ, 0.6);
        boolean water = false;
        for (AABBElement element : aabbs) {
            if (aabb.overlay(element.aabb)) {
                if (element.collision.isLiquid()) {
                    water = true;
                }
            }
        }
        if (water) {
            swim++;
            swimming = swim > 1;
        } else {
            swimming = false;
            swim = 0;
        }
        this.inWater = inWater;
        this.swimming = swimming;
    }

    public void updatePosition() {
        positionHandler
                .submitUpdate(entityID, pos.now(), speed.now(), rot.now(),
                        ground, slidingWall, inWater, swimming, true);
    }

    @Override
    public void update(double delta) {
        Controller controller =
                ((ScapesClient) game.getEngine().getGame()).getInputMode()
                        .getPlayerController();
        if (controller.getInventory()) {
            if (!(currentGui instanceof GuiChatWrite)) {
                if (hasGui()) {
                    world.getConnection().send(new PacketInteraction(
                            PacketInteraction.CLOSE_INVENTORY));
                } else {
                    world.getConnection().send(new PacketInteraction(
                            PacketInteraction.OPEN_INVENTORY));
                }
            }
        }
        if (controller.getChat()) {
            if (!hasGui()) {
                openGui(new GuiChatWrite(game, this,
                        world.getScene().getChat()));
            }
        }
        if (controller.getMenu()) {
            if (hasGui()) {
                closeGui();
            } else {
                openGui(new GuiPause(game, this));
            }
        }
        if (currentGui == null) {
            // Inventory
            int previous = inventorySelectLeft;
            int hotbar = controller.getHotbarLeft(previous);
            if (hotbar != previous) {
                setInventorySelectLeft(hotbar);
                world.getConnection().send(new PacketInteraction(
                        PacketInteraction.INVENTORY_SLOT_CHANGE,
                        (byte) inventorySelectLeft));
            }
            previous = inventorySelectRight;
            hotbar = controller.getHotbarRight(previous);
            if (hotbar != previous) {
                setInventorySelectRight(hotbar);
                world.getConnection().send(new PacketInteraction(
                        PacketInteraction.INVENTORY_SLOT_CHANGE,
                        (byte) (inventorySelectRight + 10)));
            }
            // Debug
            if (Scapes.debug) {
                ControllerDefault controllerDefault =
                        game.getEngine().getController();
                if (controllerDefault.isPressed(ControllerKey.KEY_F5)) {
                    flying = !flying;
                }
                if (flying) {
                    if (controllerDefault.isDown(ControllerKey.KEY_Q)) {
                        speed.plusZ(1.0d);
                    }
                    if (controllerDefault.isDown(ControllerKey.KEY_C)) {
                        speed.plusZ(-1.0d);
                    }
                }
            }
            // Movement
            if (controller.getJump()) {
                if (swimming) {
                    speed.plusZ(1.2);
                } else if (ground) {
                    speed.setZ(5.1);
                    ground = false;
                }
            }
            Vector2 camera = controller.getCamera();
            rot.setZ((rot.doubleZ() - camera.doubleX()) % 360);
            rot.setX(FastMath.min(89,
                    FastMath.max(-89, rot.doubleX() - camera.doubleY())));
            Vector2 walk = controller.getWalk();
            double walkSpeed = FastMath.clamp(
                    FastMath.max(FastMath.abs(walk.doubleX()),
                            FastMath.abs(walk.doubleY())), 0.0d, 1.0d) * 120.0;
            if (!ground && !slidingWall && !inWater && !flying) {
                walkSpeed *= 0.0006;
            } else if (!ground && !inWater && !flying) {
                walkSpeed *= 0.05;
            } else if (inWater) {
                walkSpeed *= 0.2;
            }
            if (walkSpeed > 0.0d) {
                double dir = (FastMath.pointDirection(Vector2d.ZERO, walk) +
                        rot.doubleZ() - 90.0d) * FastMath.DEG_2_RAD;
                walkSpeed *= delta;
                speed.plusX(FastMath.cosTable(dir) * walkSpeed);
                speed.plusY(FastMath.sinTable(dir) * walkSpeed);
            }
            // Placement
            if (controller.getLeft()) {
                if (chargeLeft < 0.01f) {
                    if (punchLeft == -1) {
                        punchLeft = System.currentTimeMillis();
                    }
                }
            } else if (punchLeft != -1) {
                updatePosition();
                breakParticles(world.getTerrain(), 16);
                world.getConnection().send(new PacketItemUse(true, FastMath.min(
                        (double) (System.currentTimeMillis() - punchLeft) /
                                getLeftWeapon().getMaterial()
                                        .getHitWait(getLeftWeapon()), 0.5) *
                        2.0d));
                punchLeft = -1;
            }
            if (controller.getRight()) {
                if (chargeRight < 0.01f) {
                    if (punchRight == -1) {
                        punchRight = System.currentTimeMillis();
                    }
                }
            } else if (punchRight != -1) {
                updatePosition();
                breakParticles(world.getTerrain(), 16);
                world.getConnection().send(new PacketItemUse(false,
                        FastMath.min((double) (System.currentTimeMillis() -
                                punchRight) / getRightWeapon().getMaterial()
                                .getHitWait(getRightWeapon()), 0.5) * 2.0d));
                punchRight = -1;
            }
            if (blockWait > 0) {
                blockWait--;
            }
        }
        long time = System.currentTimeMillis();
        float swingTime =
                getLeftWeapon().getMaterial().getHitWait(getLeftWeapon());
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
        swingTime = getRightWeapon().getMaterial().getHitWait(getRightWeapon());
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

    @Override
    public void onNotice(MobClient notice) {
        if (notice instanceof MobLivingClient) {
            if (((MobLivingClient) notice).getCreatureType() ==
                    CreatureType.MONSTER) {
                game.getPlaylist().setMusic(Playlist.Music.BATTLE, this);
            }
        }
    }

    @Override
    public void onDamage(double damage) {
        game.getClient().getWorld().getScene().damageShake(damage);
    }

    @Override
    public void move(double delta) {
        updateVelocity(flying ? 0.0 : world.getGravitation(), delta);
        double goX = FastMath.clamp(speed.doubleX() * delta, -1.0, 1.0);
        double goY = FastMath.clamp(speed.doubleY() * delta, -1.0, 1.0);
        double goZ = FastMath.clamp(speed.doubleZ() * delta, -1.0, 1.0);
        if (flying) {
            pos.plus(new Vector3d(goX, goY, goZ));
        } else {
            AABB aabb = getAABB();
            Pool<AABBElement> aabbs = world.getTerrain().getCollisions(
                    FastMath.floor(aabb.minX + FastMath.min(goX, 0.0d)),
                    FastMath.floor(aabb.minY + FastMath.min(goY, 0.0d)),
                    FastMath.floor(aabb.minZ + FastMath.min(goZ, 0.0d)),
                    FastMath.floor(aabb.maxX + FastMath.max(goX, 0.0d)),
                    FastMath.floor(aabb.maxY + FastMath.max(goY, 0.0d)),
                    FastMath.floor(aabb.maxZ + FastMath.max(goZ, stepHeight)));
            move(aabb, aabbs, goX, goY, goZ);
            if (ground) {
                speed.setZ(speed.doubleZ() / (1.0 + 4.0 * delta));
            }
            headInWater = world.getTerrain()
                    .getBlockType(pos.intX(), pos.intY(),
                            FastMath.floor(pos.doubleZ() + 0.7)).isLiquid();
            collide(aabb, aabbs);
            aabbs.reset();
        }
        positionHandler
                .submitUpdate(entityID, pos.now(), speed.now(), rot.now(),
                        ground, slidingWall, inWater, swimming);
        double lookX = FastMath.cosTable(rot.doubleZ() * FastMath.PI / 180) *
                FastMath.cosTable(rot.doubleX() * FastMath.PI / 180) * 6;
        double lookY = FastMath.sinTable(rot.doubleZ() * FastMath.PI / 180) *
                FastMath.cosTable(rot.doubleX() * FastMath.PI / 180) * 6;
        double lookZ = FastMath.sinTable(rot.doubleX() * FastMath.PI / 180) * 6;
        Vector3 viewOffset = getViewOffset();
        viewField.setView(pos.doubleX() + viewOffset.doubleX(),
                pos.doubleY() + viewOffset.doubleY(),
                pos.doubleZ() + viewOffset.doubleZ(), pos.doubleX() + lookX,
                pos.doubleY() + lookY, pos.doubleZ() + lookZ, 0, 0, 1);
        world.getEntities().filter(entity -> entity instanceof MobClient)
                .forEach(entity -> {
                    MobClient mob = (MobClient) entity;
                    if (viewField.inView(mob.getAABB()) > 0) {
                        if (!world.checkBlocked(pos.intX(), pos.intY(),
                                pos.intZ(), entity.pos.intX(),
                                entity.pos.intY(), entity.pos.intZ())) {
                            onNotice(mob);
                        }
                    }
                });
        footStep -= delta;
        if (footStep <= 0.0) {
            footStep = 0.0;
            if (FastMath.max(FastMath.abs((Vector2) speed.now())) > 0.1) {
                int x = pos.intX(), y = pos.intY(), z =
                        FastMath.floor(pos.doubleZ() - 0.1);
                String footSteepSound = world.getTerrain().getBlockType(x, y, z)
                        .getFootStep(world.getTerrain().getBlockData(x, y, z));
                if (footSteepSound.isEmpty() && ground) {
                    z = FastMath.floor(pos.doubleZ() - 1.4);
                    footSteepSound = world.getTerrain().getBlockType(x, y, z)
                            .getFootStep(
                                    world.getTerrain().getBlockData(x, y, z));
                }
                if (!footSteepSound.isEmpty()) {
                    Random random = ThreadLocalRandom.current();
                    game.getEngine().getSounds().playSound(footSteepSound,
                            0.9f + random.nextFloat() * 0.2f, 1.0f);
                    footStep = 1.0 /
                            FastMath.clamp(FastMath.length(speed.now()), 1.0,
                                    4.0);
                }
            }
        }
        if (invincibleTicks > 0) {
            invincibleTicks--;
        }
    }

    @Override
    public float getLeftCharge() {
        return chargeLeft;
    }

    @Override
    public float getRightCharge() {
        return chargeRight;
    }

    private void breakParticles(TerrainClient terrain, int amount) {
        PointerPane pane = getSelectedBlock();
        if (pane != null) {
            BlockType type = terrain.getBlockType(pane.x, pane.y, pane.z);
            Optional<TerrainTexture> tex =
                    type.getParticleTexture(pane.face, terrain, pane.x, pane.y,
                            pane.z);
            if (tex.isPresent()) {
                TerrainTexture texture = tex.get();
                Vector3 blockPos = new Vector3d(pane.x, pane.y, pane.z);
                ParticleManager particleManager = world.getParticleManager();
                Random random = ThreadLocalRandom.current();
                for (int i = 0; i < amount; i++) {
                    particleManager.add(new ParticleBlock(particleManager,
                            blockPos.plus(new Vector3d(random.nextDouble(),
                                    random.nextDouble(), random.nextDouble())),
                            new Vector3d(-1.0 + random.nextDouble() * 2.0,
                                    -1.0 + random.nextDouble() * 2.0,
                                    random.nextDouble() * 2.0 + 1.0), texture,
                            random.nextFloat() * 360,
                            type.getParticleColorR(pane.face, terrain, pane.x,
                                    pane.y, pane.z),
                            type.getParticleColorG(pane.face, terrain, pane.x,
                                    pane.y, pane.z),
                            type.getParticleColorB(pane.face, terrain, pane.x,
                                    pane.y, pane.z), 1.0f));
                }
            }
        }
    }

    public synchronized void openGui(Gui gui) {
        if (currentGui != null) {
            closeGui();
        }
        game.add(gui);
        currentGui = gui;
        game.getClient().getWorld().getScene().setHudVisible(false);
    }

    public boolean hasGui() {
        return currentGui != null;
    }

    public synchronized void closeGui() {
        if (currentGui != null) {
            game.remove(currentGui);
            currentGui = null;
            game.getClient().getWorld().getScene().setHudVisible(true);
        }
    }

    public GameStateGameMP getGame() {
        return game;
    }

    public ClientConnection getConnection() {
        return game.getClient();
    }

    public interface Controller {
        Vector2 getWalk();

        Vector2 getCamera();

        boolean getLeft();

        boolean getRight();

        boolean getJump();

        boolean getInventory();

        boolean getMenu();

        boolean getChat();

        int getHotbarLeft(int previous);

        int getHotbarRight(int previous);
    }
}
