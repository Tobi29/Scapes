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

package org.tobi29.scapes.entity.server;

import org.tobi29.scapes.block.AABBElement;
import org.tobi29.scapes.block.Inventory;
import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.chunk.WorldServer;
import org.tobi29.scapes.chunk.terrain.TerrainServer;
import org.tobi29.scapes.connection.PlayConnection;
import org.tobi29.scapes.engine.utils.Pool;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.math.AABB;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.Frustum;
import org.tobi29.scapes.engine.utils.math.PointerPane;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;
import org.tobi29.scapes.entity.CreatureType;
import org.tobi29.scapes.entity.MobPositionHandler;
import org.tobi29.scapes.packets.PacketEntityChange;
import org.tobi29.scapes.packets.PacketOpenGui;
import org.tobi29.scapes.packets.PacketUpdateInventory;
import org.tobi29.scapes.server.connection.PlayerConnection;
import org.tobi29.scapes.server.connection.ServerConnection;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

public class MobPlayerServer extends MobLivingEquippedServer
        implements EntityContainerServer {
    protected final MobPositionHandler sendPositionHandler;
    protected final PlayerConnection connection;
    protected final ServerConnection serverConnection;
    protected final List<MobPlayerServer> viewers = new ArrayList<>();
    protected final String nickname;
    protected final Inventory inventory;
    private final byte[] skin;
    private final Map<String, PunchListener> punchListeners =
            new ConcurrentHashMap<>();
    protected int inventorySelectLeft, inventorySelectRight = 9, healWait;
    private EntityContainerServer currentContainer;

    public MobPlayerServer(WorldServer world, Vector3 pos, Vector3 speed,
            double xRot, double zRot, String nickname, byte[] skin,
            PlayerConnection connection) {
        super(world, pos, speed, new AABB(-0.4, -0.4, -1, 0.4, 0.4, 0.9), 100,
                100, new Frustum(90, 1, 0.1, 24), new Frustum(50, 1, 0.1, 2));
        rot.setX(xRot);
        rot.setZ(zRot);
        this.nickname = nickname;
        this.skin = skin;
        inventory = new Inventory(registry, 44);
        this.connection = connection;
        serverConnection = connection.getServer();
        viewers.add(this);
        List<PlayerConnection> exceptions =
                Collections.singletonList(connection);
        sendPositionHandler = new MobPositionHandler(pos,
                packet -> serverConnection.send(packet, exceptions), pos2 -> {
        }, speed2 -> {
        }, rot -> {
        }, (ground, slidingWall, inWater, swimming) -> {
        });
        listener((DeathListener) () -> {
            for (int i = 0; i < inventory.getSize(); i++) {
                world.dropItem(inventory.getItem(i), this.pos.now());
                inventory.setItem(i, new ItemStack(registry));
            }
            setSpeed(Vector3d.ZERO);
            setPos(new Vector3d(0.5, 0.5, 1.5).plus(world.getSpawn()));
            lives = maxLives;
            world.getConnection().send(new PacketEntityChange(this));
            onSpawn();
        });
    }

    @Override
    public ItemStack getLeftWeapon() {
        return inventory.getItem(inventorySelectLeft);
    }

    @Override
    public ItemStack getRightWeapon() {
        return inventory.getItem(inventorySelectRight);
    }

    public void setInventorySelectLeft(int select) {
        int c = 1;
        if (select < inventorySelectLeft) {
            c = -1;
        }
        select %= 10;
        if (select < 0) {
            select += 10;
        }
        if (select == inventorySelectRight) {
            select += c;
        }
        select %= 10;
        if (select < 0) {
            select += 10;
        }
        inventorySelectLeft = select;
    }

    public void setInventorySelectRight(int select) {
        int c = 1;
        if (select < inventorySelectRight) {
            c = -1;
        }
        select %= 10;
        if (select < 0) {
            select += 10;
        }
        if (select == inventorySelectLeft) {
            select += c;
        }
        select %= 10;
        if (select < 0) {
            select += 10;
        }
        inventorySelectRight = select;
    }

    public String getNickname() {
        return nickname;
    }

    public PointerPane getSelectedBlock() {
        return getBlock(6);
    }

    public TagStructure write(boolean packet) {
        TagStructure tagStructure = super.write();
        tagStructure.setInteger("HealWait", healWait);
        tagStructure.setStructure("Inventory", inventory.save());
        if (packet) {
            tagStructure.setString("Nickname", nickname);
            tagStructure.setByteArray("SkinChecksum", skin);
        }
        return tagStructure;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public void addViewer(MobPlayerServer player) {
        if (!viewers.contains(player)) {
            viewers.add(player);
        }
    }

    @Override
    public Stream<MobPlayerServer> getViewers() {
        return viewers.stream();
    }

    @Override
    public void removeViewer(MobPlayerServer player) {
        viewers.remove(player);
    }

    @Override
    public void update(double delta) {
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
        world.getEntities().filter(entity -> entity instanceof MobServer)
                .forEach(entity -> {
                    MobServer mob = (MobServer) entity;
                    if (viewField.inView(mob.getAABB()) > 0) {
                        if (!world.checkBlocked(pos.intX(), pos.intY(),
                                pos.intZ(), mob.pos.intX(), mob.pos.intY(),
                                mob.pos.intZ())) {
                            onNotice(mob);
                        }
                    }
                });
        if (pos.doubleZ() < -100.0) {
            damage(-pos.doubleZ() - 100.0);
        }
        if (lives < 10.0) {
            Random random = ThreadLocalRandom.current();
            if (random.nextInt(40) == 0) {
                push(random.nextDouble() * 2 - 1, 0, 0);
            }
            if (random.nextInt(40) == 0) {
                push(0, random.nextDouble() * 2 - 1, 0);
            }
            if (random.nextInt(20) == 0) {
                setXRot(getXRot() + random.nextDouble() * 60 - 30);
            }
            if (random.nextInt(20) == 0) {
                setZRot(getZRot() + random.nextDouble() * 60 - 30);
            }
        }
    }

    @Override
    protected MobPositionHandler createPositionHandler(
            PlayConnection connection) {
        return new MobPositionHandler(pos.now(), packet -> {
        }, super::setPos, super::setSpeed, super::setRot,
                (ground, slidingWall, inWater, swimming) -> {
                    if (ground != this.ground) {
                        this.ground = ground;
                        if (speed.doubleZ() > 0.0 && !inWater) {
                            onJump();
                        }
                    }
                    this.slidingWall = slidingWall;
                    this.inWater = inWater;
                    this.swimming = swimming;
                });
    }

    @Override
    public synchronized void setSpeed(Vector3 speed) {
        super.setSpeed(speed);
        if (sendPositionHandler != null) {
            sendPositionHandler.sendSpeed(entityID, this.speed.now(), true,
                    serverConnection::send);
        }
    }

    @Override
    public synchronized void setRot(Vector3 rot) {
        super.setRot(rot);
        if (sendPositionHandler != null) {
            sendPositionHandler.sendRotation(entityID, this.rot.now(), true,
                    serverConnection::send);
        }
    }

    @Override
    public synchronized void setXSpeed(double x) {
        super.setXSpeed(x);
        if (sendPositionHandler != null) {
            sendPositionHandler.sendSpeed(entityID, speed.now(), true,
                    serverConnection::send);
        }
    }

    @Override
    public synchronized void setXRot(double xRot) {
        super.setXRot(xRot);
        if (sendPositionHandler != null) {
            sendPositionHandler.sendRotation(entityID, rot.now(), true,
                    serverConnection::send);
        }
    }

    @Override
    public synchronized void setYSpeed(double y) {
        super.setYSpeed(y);
        if (sendPositionHandler != null) {
            sendPositionHandler.sendSpeed(entityID, speed.now(), true,
                    serverConnection::send);
        }
    }

    @Override
    public synchronized void setYRot(double yRot) {
        super.setYRot(yRot);
        if (sendPositionHandler != null) {
            sendPositionHandler.sendRotation(entityID, rot.now(), true,
                    serverConnection::send);
        }
    }

    @Override
    public synchronized void setZSpeed(double z) {
        super.setZSpeed(z);
        if (sendPositionHandler != null) {
            sendPositionHandler.sendSpeed(entityID, speed.now(), true,
                    serverConnection::send);
        }
    }

    @Override
    public synchronized void setZRot(double zRot) {
        super.setZRot(zRot);
        if (sendPositionHandler != null) {
            sendPositionHandler.sendRotation(entityID, rot.now(), true,
                    serverConnection::send);
        }
    }

    @Override
    public synchronized void push(double x, double y, double z) {
        super.push(x, y, z);
        if (sendPositionHandler != null) {
            sendPositionHandler.sendSpeed(entityID, speed.now(), true,
                    serverConnection::send);
        }
    }

    @Override
    public synchronized void setPos(Vector3 value) {
        super.setPos(value);
        if (sendPositionHandler != null) {
            sendPositionHandler
                    .submitUpdate(entityID, value, speed.now(), rot.now(),
                            ground, slidingWall, inWater, swimming, true,
                            serverConnection::send);
        }
    }

    @Override
    public synchronized void setX(double x) {
        super.setX(x);
        if (sendPositionHandler != null) {
            sendPositionHandler
                    .submitUpdate(entityID, pos.now(), speed.now(), rot.now(),
                            ground, slidingWall, inWater, swimming, true,
                            serverConnection::send);
        }
    }

    @Override
    public synchronized void setY(double y) {
        super.setY(y);
        if (sendPositionHandler != null) {
            sendPositionHandler
                    .submitUpdate(entityID, pos.now(), speed.now(), rot.now(),
                            ground, slidingWall, inWater, swimming, true,
                            serverConnection::send);
        }
    }

    @Override
    public synchronized void setZ(double z) {
        super.setZ(z);
        if (sendPositionHandler != null) {
            sendPositionHandler
                    .submitUpdate(entityID, pos.now(), speed.now(), rot.now(),
                            ground, slidingWall, inWater, swimming, true,
                            serverConnection::send);
        }
    }

    public PlayerConnection getConnection() {
        return connection;
    }

    public List<MobServer> attackLeft(double strength) {
        return attack(true, strength);
    }

    public List<MobServer> attackRight(double strength) {
        return attack(false, strength);
    }

    protected synchronized List<MobServer> attack(boolean side,
            double strength) {
        double lookX = FastMath.cosTable(rot.doubleZ() * FastMath.PI / 180) *
                FastMath.cosTable(rot.doubleX() * FastMath.PI / 180) * 6;
        double lookY = FastMath.sinTable(rot.doubleZ() * FastMath.PI / 180) *
                FastMath.cosTable(rot.doubleX() * FastMath.PI / 180) * 6;
        double lookZ = FastMath.sinTable(rot.doubleX() * FastMath.PI / 180) * 6;
        Vector3 viewOffset = getViewOffset();
        hitField.setView(pos.doubleX() + viewOffset.doubleX(),
                pos.doubleY() + viewOffset.doubleY(),
                pos.doubleZ() + viewOffset.doubleZ(), pos.doubleX() + lookX,
                pos.doubleY() + lookY, pos.doubleZ() + lookZ, 0, 0, 1);
        double range;
        if (side) {
            range = getLeftWeapon().getMaterial().getHitRange(getLeftWeapon());
        } else {
            range = getRightWeapon().getMaterial()
                    .getHitRange(getRightWeapon());
        }
        hitField.setPerspective(100 / range, 1, 0.1, range);
        List<MobServer> entities =
                world.getEntities(Collections.singletonList((MobServer) this),
                        hitField);
        entities.stream().filter(entity -> entity instanceof MobLivingServer &&
                entity != this).forEach(entity -> {
            if (side) {
                ((MobLivingServer) entity).damage(getLeftWeapon().getMaterial()
                        .click(this, getLeftWeapon(), entity) * strength);
            } else {
                ((MobLivingServer) entity).damage(getRightWeapon().getMaterial()
                        .click(this, getRightWeapon(), entity) * strength);
            }
            ((MobLivingServer) entity).onNotice(this);
            entity.push(
                    FastMath.cosTable(rot.doubleZ() * FastMath.DEG_2_RAD) * 0.3,
                    FastMath.sinTable(rot.doubleZ() * FastMath.DEG_2_RAD) * 0.3,
                    0.1);
        });
        return entities;
    }

    public boolean hasGui() {
        return currentContainer != null;
    }

    public void openGui(EntityContainerServer gui) {
        if (hasGui()) {
            closeGui();
        }
        currentContainer = gui;
        world.getConnection().send(new PacketUpdateInventory(gui));
        world.getConnection().send(new PacketUpdateInventory(this));
        gui.addViewer(this);
        connection.send(new PacketOpenGui(gui));
    }

    public void closeGui() {
        if (currentContainer != null) {
            currentContainer.removeViewer(this);
            currentContainer = null;
        }
    }

    @Override
    public boolean canMoveHere(TerrainServer terrain, int x, int y, int z) {
        return false;
    }

    @Override
    public CreatureType getCreatureType() {
        return CreatureType.CREATURE;
    }

    @Override
    public TagStructure write() {
        return write(true);
    }

    @Override
    public void read(TagStructure tagStructure) {
        super.read(tagStructure);
        healWait = tagStructure.getInteger("HealWait");
        inventory.load(tagStructure.getStructure("Inventory"));
    }

    @Override
    public void move(double delta) {
        AABB aabb = getAABB();
        Pool<AABBElement> aabbs = world.getTerrain()
                .getCollisions(FastMath.floor(aabb.minX),
                        FastMath.floor(aabb.minY), FastMath.floor(aabb.minZ),
                        FastMath.floor(aabb.maxX), FastMath.floor(aabb.maxY),
                        FastMath.floor(aabb.maxZ));
        collide(aabb, aabbs, delta);
        aabbs.reset();
        sendPositionHandler
                .submitUpdate(entityID, pos.now(), speed.now(), rot.now(),
                        ground, slidingWall, inWater, swimming);
        headInWater = world.getTerrain().type(pos.intX(), pos.intY(),
                FastMath.floor(pos.doubleZ() + 0.7)).isLiquid();
        if (invincibleTicks > 0.0) {
            invincibleTicks = FastMath.max(invincibleTicks - delta, 0.0);
        }
    }

    @Override
    public Vector3 getViewOffset() {
        return new Vector3d(0.0, 0.0, 0.63);
    }

    @Override
    public void listener(String id, EntityServer.Listener listener) {
        super.listener(id, listener);
        if (listener instanceof PunchListener) {
            punchListeners.put(id, (PunchListener) listener);
        }
    }

    public void onPunch(double strength) {
        punchListeners.values().forEach(listener -> listener.onPunch(strength));
    }

    @FunctionalInterface
    public interface PunchListener extends EntityServer.Listener {
        void onPunch(double strength);
    }
}
