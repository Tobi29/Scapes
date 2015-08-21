package org.tobi29.scapes.entity.server;

import org.tobi29.scapes.block.AABBElement;
import org.tobi29.scapes.block.Inventory;
import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.chunk.WorldServer;
import org.tobi29.scapes.chunk.terrain.TerrainServer;
import org.tobi29.scapes.engine.server.PlayConnection;
import org.tobi29.scapes.engine.utils.Pair;
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
import org.tobi29.scapes.server.connection.PlayerConnection;
import org.tobi29.scapes.server.connection.ServerConnection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public abstract class MobPlayerServer extends MobLivingEquippedServer
        implements EntityContainerServer {
    protected final MobPositionHandler sendPositionHandler;
    protected final PlayerConnection connection;
    protected final ServerConnection serverConnection;
    protected final List<MobPlayerServer> viewers = new ArrayList<>();
    protected final String nickname;
    protected final Inventory inventoryContainer, inventoryHold;
    protected final Map<String, Inventory> inventories =
            new ConcurrentHashMap<>();
    private final byte[] skin;
    private final Map<String, PunchListener> punchListeners =
            new ConcurrentHashMap<>();
    protected int inventorySelectLeft, inventorySelectRight = 9, healWait;
    protected EntityContainerServer currentContainer;

    protected MobPlayerServer(WorldServer world, Vector3 pos, Vector3 speed,
            AABB aabb, double lives, double maxLives, Frustum viewField,
            Frustum hitField, String nickname, byte[] skin,
            PlayerConnection connection) {
        super(world, pos, speed, aabb, lives, maxLives, viewField, hitField);
        this.nickname = nickname;
        this.skin = skin;
        inventoryContainer = new Inventory(registry, 40);
        inventoryHold = new Inventory(registry, 1);
        inventories.put("Container", inventoryContainer);
        inventories.put("Hold", inventoryHold);
        this.connection = connection;
        serverConnection = connection.server();
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
            for (int i = 0; i < inventoryContainer.size(); i++) {
                inventoryContainer.item(i).take().ifPresent(
                        item -> world.dropItem(item, this.pos.now()));
            }
            inventoryHold.item(0).take()
                    .ifPresent(item -> world.dropItem(item, this.pos.now()));
            setSpeed(Vector3d.ZERO);
            setPos(new Vector3d(0.5, 0.5, 1.5).plus(world.spawn()));
            health = maxHealth;
            world.connection().send(new PacketEntityChange(this));
            onSpawn();
        });
    }

    @Override
    public ItemStack leftWeapon() {
        return inventoryContainer.item(inventorySelectLeft);
    }

    @Override
    public ItemStack rightWeapon() {
        return inventoryContainer.item(inventorySelectRight);
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

    public String nickname() {
        return nickname;
    }

    public PointerPane selectedBlock() {
        return block(6);
    }

    @Override
    public Inventory inventory(String id) {
        return inventories.get(id);
    }

    @Override
    public Stream<Pair<String, Inventory>> inventories() {
        return inventories.entrySet().stream()
                .map(entry -> new Pair<>(entry.getKey(), entry.getValue()));
    }

    @Override
    public void addViewer(MobPlayerServer player) {
        if (!viewers.contains(player)) {
            viewers.add(player);
        }
    }

    @Override
    public Stream<MobPlayerServer> viewers() {
        return viewers.stream();
    }

    @Override
    public void removeViewer(MobPlayerServer player) {
        viewers.remove(player);
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
        positionHandler
                .sendSpeed(entityID, this.speed.now(), true, connection::send);
    }

    @Override
    public synchronized void setRot(Vector3 rot) {
        super.setRot(rot);
        positionHandler
                .sendRotation(entityID, this.rot.now(), true, connection::send);
    }

    @Override
    public synchronized void push(double x, double y, double z) {
        super.push(x, y, z);
        positionHandler
                .sendSpeed(entityID, speed.now(), true, connection::send);
    }

    @Override
    public synchronized void setPos(Vector3 pos) {
        super.setPos(pos);
        positionHandler
                .sendPos(entityID, this.pos.now(), true, connection::send);
    }

    public PlayerConnection connection() {
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
        Vector3 viewOffset = viewOffset();
        hitField.setView(pos.doubleX() + viewOffset.doubleX(),
                pos.doubleY() + viewOffset.doubleY(),
                pos.doubleZ() + viewOffset.doubleZ(), pos.doubleX() + lookX,
                pos.doubleY() + lookY, pos.doubleZ() + lookZ, 0, 0, 1);
        double range;
        if (side) {
            range = leftWeapon().material().hitRange(leftWeapon());
        } else {
            range = rightWeapon().material().hitRange(rightWeapon());
        }
        hitField.setPerspective(100 / range, 1, 0.1, range);
        List<MobServer> entities =
                world.entities(Collections.singletonList((MobServer) this),
                        hitField);
        entities.stream().filter(mob -> mob instanceof MobLivingServer)
                .map(mob -> (MobLivingServer) mob).forEach(mob -> {
            if (side) {
                mob.damage(
                        leftWeapon().material().click(this, leftWeapon(), mob) *
                                strength);
            } else {
                mob.damage(rightWeapon().material()
                        .click(this, rightWeapon(), mob) * strength);
            }
            mob.onNotice(this);
            double rad = rot.doubleZ() * FastMath.DEG_2_RAD;
            mob.push(FastMath.cosTable(rad) * 10.0,
                    FastMath.sinTable(rad) * 10.0, 2.0);
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
        world.connection().send(new PacketEntityChange((EntityServer) gui));
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
    public CreatureType creatureType() {
        return CreatureType.CREATURE;
    }

    @Override
    public TagStructure write() {
        return write(true);
    }

    public TagStructure write(boolean packet) {
        TagStructure tagStructure = super.write();
        tagStructure.setInteger("HealWait", healWait);
        TagStructure inventoryTag = tagStructure.getStructure("Inventory");
        inventories.forEach((id, inventory) -> inventoryTag
                .setStructure(id, inventory.save()));
        if (packet) {
            tagStructure.setString("Nickname", nickname);
            tagStructure.setByteArray("SkinChecksum", skin);
        }
        return tagStructure;
    }

    @Override
    public void read(TagStructure tagStructure) {
        super.read(tagStructure);
        healWait = tagStructure.getInteger("HealWait");
        TagStructure inventoryTag = tagStructure.getStructure("Inventory");
        inventories.forEach((id, inventory) -> inventory
                .load(inventoryTag.getStructure(id)));
    }

    @Override
    public void move(double delta) {
        AABB aabb = aabb();
        Pool<AABBElement> aabbs = world.getTerrain()
                .collisions(FastMath.floor(aabb.minX),
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
