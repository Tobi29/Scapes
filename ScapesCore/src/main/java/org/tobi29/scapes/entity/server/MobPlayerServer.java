package org.tobi29.scapes.entity.server;

import java8.util.stream.Stream;
import org.tobi29.scapes.block.AABBElement;
import org.tobi29.scapes.block.Inventory;
import org.tobi29.scapes.block.InventoryContainer;
import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.chunk.WorldServer;
import org.tobi29.scapes.chunk.terrain.TerrainServer;
import org.tobi29.scapes.connection.PlayConnection;
import org.tobi29.scapes.engine.utils.Checksum;
import org.tobi29.scapes.engine.utils.Pool;
import org.tobi29.scapes.engine.utils.Streams;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.math.AABB;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.Frustum;
import org.tobi29.scapes.engine.utils.math.PointerPane;
import org.tobi29.scapes.engine.utils.math.vector.Vector2;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;
import org.tobi29.scapes.entity.CreatureType;
import org.tobi29.scapes.entity.MobPositionHandler;
import org.tobi29.scapes.packets.PacketEntityChange;
import org.tobi29.scapes.packets.PacketOpenGui;
import org.tobi29.scapes.packets.PacketUpdateInventory;
import org.tobi29.scapes.server.connection.PlayerConnection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class MobPlayerServer extends MobLivingEquippedServer
        implements EntityContainerServer {
    protected final MobPositionHandler sendPositionHandler;
    protected final PlayerConnection connection;
    protected final List<MobPlayerServer> viewers = new ArrayList<>();
    protected final String nickname;
    protected final InventoryContainer inventories;
    private final Checksum skin;
    private final Map<String, PunchListener> punchListeners =
            new ConcurrentHashMap<>();
    protected int inventorySelectLeft, inventorySelectRight = 9, healWait;
    protected EntityContainerServer currentContainer;

    protected MobPlayerServer(WorldServer world, Vector3 pos, Vector3 speed,
            AABB aabb, double lives, double maxLives, Frustum viewField,
            Frustum hitField, String nickname, Checksum skin,
            PlayerConnection connection) {
        super(world, pos, speed, aabb, lives, maxLives, viewField, hitField);
        this.nickname = nickname;
        this.skin = skin;
        inventories = new InventoryContainer(
                id -> world.send(new PacketUpdateInventory(this, id)));
        inventories.add("Container", new Inventory(registry, 40));
        inventories.add("Hold", new Inventory(registry, 1));
        this.connection = connection;
        viewers.add(this);
        List<PlayerConnection> exceptions =
                Collections.singletonList(connection);
        sendPositionHandler = new MobPositionHandler(pos,
                packet -> world.send(packet, exceptions), pos2 -> {
        }, speed2 -> {
        }, rot -> {
        }, (ground, slidingWall, inWater, swimming) -> {
        });
        listener((DeathListener) () -> {
            Streams.forEachIterable(
                    inventories.modifyReturn("Container", inventory -> {
                        List<ItemStack> items = new ArrayList<>();
                        for (int i = 0; i < inventory.size(); i++) {
                            inventory.item(i).take().ifPresent(items::add);
                        }
                        return items;
                    }), item -> world.dropItem(item, this.pos.now()));
            inventories
                    .modifyReturn("Hold", inventory -> inventory.item(0).take())
                    .ifPresent(item -> world.dropItem(item, this.pos.now()));
            setSpeed(Vector3d.ZERO);
            setPos(new Vector3d(0.5, 0.5, 1.5).plus(world.spawn()));
            health = maxHealth;
            world.send(new PacketEntityChange(this));
            onSpawn();
        });
    }

    public abstract boolean isActive();

    @Override
    public ItemStack leftWeapon() {
        return inventories.accessReturn("Container",
                inventory -> inventory.item(inventorySelectLeft));
    }

    @Override
    public ItemStack rightWeapon() {
        return inventories.accessReturn("Container",
                inventory -> inventory.item(inventorySelectRight));
    }

    public void setInventorySelectLeft(int select) {
        inventorySelectLeft = select;
    }

    public void setInventorySelectRight(int select) {
        inventorySelectRight = select;
    }

    public String nickname() {
        return nickname;
    }

    public PointerPane selectedBlock(Vector2 direction) {
        return block(6, direction);
    }

    @Override
    public InventoryContainer inventories() {
        return inventories;
    }

    @Override
    public void addViewer(MobPlayerServer player) {
        if (!viewers.contains(player)) {
            viewers.add(player);
        }
    }

    @Override
    public Stream<MobPlayerServer> viewers() {
        return Streams.of(viewers);
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

    public List<MobLivingServer> attackLeft(double strength,
            Vector2 direction) {
        return attack(true, strength, direction);
    }

    public List<MobLivingServer> attackRight(double strength,
            Vector2 direction) {
        return attack(false, strength, direction);
    }

    protected synchronized List<MobLivingServer> attack(boolean side,
            double strength, Vector2 direction) {
        double rotX = rot.doubleX() + direction.doubleY();
        double rotZ = rot.doubleZ() + direction.doubleX();
        double factor = FastMath.cosTable(rotX * FastMath.PI / 180) * 6;
        double lookX = FastMath.cosTable(rotZ * FastMath.PI / 180) * factor;
        double lookY = FastMath.sinTable(rotZ * FastMath.PI / 180) * factor;
        double lookZ = FastMath.sinTable(rotX * FastMath.PI / 180) * 6;
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
        List<MobLivingServer> mobs = new ArrayList<>();
        world.entities(hitField)
                .filter(mob -> mob instanceof MobLivingServer && mob != this)
                .map(mob -> (MobLivingServer) mob).forEach(mob -> {
            mobs.add(mob);
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
        return mobs;
    }

    public boolean hasGui() {
        return currentContainer != null;
    }

    public void openGui(EntityContainerServer gui) {
        if (hasGui()) {
            closeGui();
        }
        currentContainer = gui;
        world.send(new PacketEntityChange((EntityServer) gui));
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

    public TagStructure write(boolean packet) {
        TagStructure tagStructure = super.write();
        tagStructure.setInteger("HealWait", healWait);
        TagStructure inventoryTag = tagStructure.getStructure("Inventory");
        inventories.forEach((id, inventory) -> inventoryTag
                .setStructure(id, inventory.save()));
        if (packet) {
            tagStructure.setString("Nickname", nickname);
            tagStructure.setByteArray("SkinChecksum", skin.array());
        }
        return tagStructure;
    }

    public void onPunch(double strength) {
        Streams.forEach(punchListeners.values(),
                listener -> listener.onPunch(strength));
    }

    public interface PunchListener extends EntityServer.Listener {
        void onPunch(double strength);
    }
}
