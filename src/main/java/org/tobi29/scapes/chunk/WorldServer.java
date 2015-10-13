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
package org.tobi29.scapes.chunk;

import org.tobi29.scapes.block.BlockExplosive;
import org.tobi29.scapes.block.BlockType;
import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.chunk.generator.ChunkGenerator;
import org.tobi29.scapes.chunk.generator.ChunkPopulator;
import org.tobi29.scapes.chunk.terrain.TerrainServer;
import org.tobi29.scapes.engine.utils.Sync;
import org.tobi29.scapes.engine.utils.io.tag.MultiTag;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.math.AABB;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.Frustum;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;
import org.tobi29.scapes.engine.utils.task.Joiner;
import org.tobi29.scapes.engine.utils.task.TaskExecutor;
import org.tobi29.scapes.entity.CreatureType;
import org.tobi29.scapes.entity.server.*;
import org.tobi29.scapes.packets.Packet;
import org.tobi29.scapes.packets.PacketEntityAdd;
import org.tobi29.scapes.packets.PacketEntityDespawn;
import org.tobi29.scapes.packets.PacketSoundEffect;
import org.tobi29.scapes.server.connection.PlayerConnection;
import org.tobi29.scapes.server.connection.ServerConnection;
import org.tobi29.scapes.server.format.WorldFormat;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WorldServer extends World implements MultiTag.ReadAndWrite {
    private final Map<Integer, EntityServer> entities =
            new ConcurrentHashMap<>();
    private final Collection<EntityListener> entityListeners =
            Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Collection<MobSpawner> spawners =
            Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final String id;
    private final List<ChunkPopulator> populators = new ArrayList<>();
    private final TerrainServer terrain;
    private final ServerConnection connection;
    private final Sync sync = new Sync(20, 5000000000L, true, "Server-Update");
    private final Map<String, MobPlayerServer> players =
            new ConcurrentHashMap<>();
    private final EnvironmentServer environment;
    private final ChunkGenerator generator;
    private Joiner joiner;

    public WorldServer(WorldFormat worldFormat, String id,
            ServerConnection connection, TaskExecutor taskExecutor,
            Function<WorldServer, TerrainServer> terrainSupplier,
            Function<WorldServer, EnvironmentServer> environmentSupplier) {
        super(worldFormat.plugins(), taskExecutor,
                worldFormat.plugins().registry());
        seed = worldFormat.seed();
        this.id = id;
        this.connection = connection;
        environment = environmentSupplier.apply(this);
        terrain = terrainSupplier.apply(this);
        generator = environment.generator();
        populators.add(environment.populator());
        worldFormat.plugins().plugins()
                .forEach(plugin -> plugin.worldInit(this));
    }

    public ServerConnection connection() {
        return connection;
    }

    @Override
    public void read(TagStructure tagStructure) {
        tick = tagStructure.getLong("Tick");
        environment.load(tagStructure.getStructure("Environment"));
    }

    @Override
    public TagStructure write() {
        TagStructure tagStructure = new TagStructure();
        tagStructure.setLong("Tick", tick);
        tagStructure.setStructure("Environment", environment.save());
        return tagStructure;
    }

    public void calculateSpawn() {
        spawn = environment.calculateSpawn(terrain);
    }

    public List<MobServer> entities(List<MobServer> exceptions,
            Frustum hitField) {
        return entities.values().stream()
                .filter(entity -> entity instanceof MobServer)
                .map(entity -> (MobServer) entity)
                .filter(mob -> !exceptions.contains(mob) &&
                        hitField.inView(mob.aabb()) > 0)
                .collect(Collectors.toList());
    }

    public void addEntity(EntityServer add) {
        synchronized (entities) {
            addEntity(add, freeEntityID());
        }
    }

    public void addEntity(EntityServer add, int id) {
        entityListeners.forEach(listener -> listener.listen(add));
        add.setEntityID(id);
        entities.put(id, add);
        if (add.id(plugins.registry()) >= 0) {
            send(new PacketEntityAdd(add, plugins.registry()));
        }
    }

    public void deleteEntity(EntityServer del) {
        if (del != null) {
            entities.remove(del.entityID());
            send(new PacketEntityDespawn(del));
        }
    }

    protected int freeEntityID() {
        Random random = ThreadLocalRandom.current();
        int out = 0;
        while (out == 0) {
            int i = random.nextInt(Integer.MAX_VALUE);
            if (!entities.containsKey(i)) {
                out = i;
            }
        }
        return out;
    }

    public List<MobServer> entities(AABB aabb) {
        return entities.values().stream()
                .filter(entity -> entity instanceof MobServer)
                .map(entity -> (MobServer) entity)
                .filter(mob -> aabb.inside(mob.pos()))
                .collect(Collectors.toList());
    }

    public List<EntityServer> entities(Vector3 pos, double rangeSqr) {
        return entities.values().stream().filter(entity ->
                FastMath.pointDistanceSqr(pos, entity.pos()) <= rangeSqr)
                .collect(Collectors.toList());
    }

    public List<EntityServer> entities(int x, int y, int z) {
        return entities.values().stream()
                .filter(entity -> FastMath.floor(entity.x()) == x &&
                        FastMath.floor(entity.y()) == y &&
                        FastMath.floor(entity.z()) == z)
                .collect(Collectors.toList());
    }

    public EntityServer entity(int i) {
        return entities.get(i);
    }

    public Stream<EntityServer> entities() {
        return entities.values().stream();
    }

    public int mobs(CreatureType creatureType) {
        int i = 0;
        for (EntityServer entity : entities.values()) {
            if (entity instanceof MobLivingServer) {
                if (((MobLivingServer) entity).creatureType() == creatureType) {
                    i++;
                }
            }
        }
        return i;
    }

    private void update(double delta) {
        terrain.update(delta, spawners);
        entities.values().stream().forEach(entity -> {
            entity.update(delta);
            if (entity instanceof MobServer) {
                ((MobServer) entity).move(delta);
            }
            if (entity instanceof MobLivingServer) {
                if (((MobLivingServer) entity).isDead()) {
                    ((MobLivingServer) entity).onDeath();
                    if (!(entity instanceof MobPlayerServer)) {
                        deleteEntity(entity);
                    }
                } else {
                    if (((MobLivingServer) entity).creatureType()
                            .doesDespawn()) {
                        MobPlayerServer player = nearestPlayer(entity.pos());
                        if (player != null) {
                            if (FastMath.pointDistanceSqr(entity.pos(),
                                    player.pos()) > 16384.0) {
                                deleteEntity(entity);
                            }
                        } else {
                            deleteEntity(entity);
                        }
                    }
                }
            }
        });
        environment.tick(delta);
        taskExecutor.tick();
        tick++;
    }

    public List<MobServer> damageEntities(List<MobServer> exceptions,
            Frustum hitField, double damage) {
        List<MobServer> entities = entities(exceptions, hitField);
        entities.stream().filter(entity -> entity instanceof MobLivingServer)
                .forEach(entity -> ((MobLivingServer) entity).damage(damage));
        return entities;
    }

    public void dropItems(List<ItemStack> items, int x, int y, int z) {
        dropItems(items, x, y, z, 600.0);
    }

    public void dropItems(List<ItemStack> items, int x, int y, int z,
            double despawntime) {
        Vector3 pos = new Vector3d(x + 0.5, y + 0.5, z + 0.5);
        for (ItemStack item : items) {
            dropItem(item, pos, despawntime);
        }
    }

    public void dropItem(ItemStack item, int x, int y, int z) {
        dropItem(item, x, y, z, 600.0);
    }

    public void dropItem(ItemStack item, int x, int y, int z,
            double despawntime) {
        dropItem(item, new Vector3d(x + 0.5, y + 0.5, z + 0.5), despawntime);
    }

    public void dropItem(ItemStack item, Vector3 pos) {
        dropItem(item, pos, 600.0);
    }

    public void dropItem(ItemStack item, Vector3 pos, double despawntime) {
        Random random = ThreadLocalRandom.current();
        EntityServer entity = new MobItemServer(this, pos,
                new Vector3d(-2.0 + random.nextDouble() * 4.0,
                        -2.0 + random.nextDouble() * 4.0,
                        random.nextDouble() * 1.0 + 0.5), item, despawntime);
        entity.onSpawn();
        addEntity(entity);
    }

    public boolean checkBlocked(int x1, int y1, int z1, int x2, int y2,
            int z2) {
        int dx = x2 - x1;
        int dy = y2 - y1;
        int dz = z2 - z1;
        double step = 1 / FastMath.sqrt(dx * dx + dy * dy + dz * dz);
        for (double i = 0; i <= 1; i += step) {
            int x = FastMath.floor(x1 + dx * i);
            int y = FastMath.floor(y1 + dy * i);
            int z = FastMath.floor(z1 + dz * i);
            if (!terrain.type(x, y, z).isTransparent(terrain, x, y, z)) {
                return true;
            }
        }
        return false;
    }

    public void explosionEntities(double x, double y, double z, double radius,
            double push, double damage) {
        entities.values().stream().filter(entity -> entity instanceof MobServer)
                .forEach(entity -> {
                    Vector3 relative =
                            entity.pos().minus(new Vector3d(x, y, z));
                    double s = radius - FastMath.length(relative);
                    if (s > 0) {
                        double p = s * push;
                        Vector3 force =
                                FastMath.normalizeSafe(relative).multiply(p);
                        ((MobServer) entity)
                                .push(force.doubleX(), force.doubleY(),
                                        force.doubleZ());
                        if (entity instanceof MobLivingServer) {
                            ((MobLivingServer) entity).damage(s * damage);
                        }
                    }
                });
    }

    public void explosionBlockPush(double x, double y, double z, double size,
            double dropChance, double blockChance, double push, double damage) {
        terrain.queue(handler -> {
            Random random = ThreadLocalRandom.current();
            double step = 360.0 / FastMath.TWO_PI / size;
            for (double pitch = 90.0; pitch >= -90.0; pitch -= step) {
                double cosYaw = FastMath.cosTable(pitch * FastMath.DEG_2_RAD);
                double stepYawForPitch = FastMath.abs(step / cosYaw);
                double deltaZ = FastMath.sinTable(pitch * FastMath.DEG_2_RAD);
                for (double yaw = 0.0; yaw < 360.0; yaw += stepYawForPitch) {
                    double deltaX =
                            FastMath.cosTable(yaw * FastMath.DEG_2_RAD) *
                                    cosYaw;
                    double deltaY =
                            FastMath.sinTable(yaw * FastMath.DEG_2_RAD) *
                                    cosYaw;
                    for (double distance = 0; distance < size; distance++) {
                        int xxx = FastMath.floor(x + deltaX * distance);
                        int yyy = FastMath.floor(y + deltaY * distance);
                        int zzz = FastMath.floor(z + deltaZ * distance);
                        BlockType type = terrain.type(xxx, yyy, zzz);
                        if (type != air) {
                            if (type instanceof BlockExplosive) {
                                ((BlockExplosive) type)
                                        .igniteByExplosion(terrain, xxx, yyy,
                                                zzz);
                            } else {
                                if (random.nextDouble() < dropChance) {
                                    dropItems(
                                            type.drops(new ItemStack(registry),
                                                    terrain.data(xxx, yyy,
                                                            zzz)), xxx, yyy,
                                            zzz);
                                } else if (
                                        type.isSolid(terrain, xxx, yyy, zzz) &&
                                                !type.isTransparent(terrain,
                                                        xxx, yyy, zzz) &&
                                                random.nextDouble() <
                                                        blockChance) {
                                    int data = terrain.data(xxx, yyy, zzz);
                                    EntityServer entity =
                                            new MobFlyingBlockServer(this,
                                                    new Vector3d(xxx + 0.5,
                                                            yyy + 0.5,
                                                            zzz + 0.5),
                                                    new Vector3d(
                                                            random.nextDouble() *
                                                                    0.1 - 0.05,
                                                            random.nextDouble() *
                                                                    0.1 - 0.05,
                                                            random.nextDouble() *
                                                                    1 + 2),
                                                    type, data);
                                    entity.onSpawn();
                                    addEntity(entity);
                                }
                            }
                            handler.typeData(xxx, yyy, zzz, air, 0);
                        }
                    }
                }
            }
            explosionEntities(x, y, z, size, push, damage);
        });
    }

    public MobPlayerServer nearestPlayer(Vector3 pos) {
        MobPlayerServer player = null;
        double distance = -1;
        for (MobPlayerServer playerCheck : players.values()) {
            double distanceCheck =
                    FastMath.pointDistanceSqr(playerCheck.pos(), pos);
            if (distanceCheck < distance || distance == -1) {
                player = playerCheck;
                distance = distanceCheck;
            }
        }
        return player;
    }

    public Collection<MobPlayerServer> players() {
        return players.values();
    }

    public void addPlayer(MobPlayerServer player) {
        players.put(player.nickname(), player);
    }

    public synchronized void removePlayer(MobPlayerServer player) {
        players.remove(player.nickname(), player);
    }

    public void addSpawner(MobSpawner spawner) {
        synchronized (spawners) {
            spawners.add(spawner);
        }
    }

    public EnvironmentServer environment() {
        return environment;
    }

    public String id() {
        return id;
    }

    public void addPopulator(ChunkPopulator populator) {
        populators.add(populator);
    }

    public ChunkGenerator generator() {
        return generator;
    }

    public Stream<ChunkPopulator> populators() {
        return populators.stream();
    }

    public TerrainServer getTerrain() {
        return terrain;
    }

    public void dispose() {
        terrain.dispose();
    }

    public void playSound(String audio, EntityServer entity) {
        playSound(audio, entity, 1.0f, 1.0f);
    }

    public void playSound(String audio, EntityServer entity, float range) {
        playSound(audio, entity, 1.0f, 1.0f, range);
    }

    public void playSound(String audio, EntityServer entity, float pitch,
            float gain) {
        playSound(audio, entity, pitch, gain, 16.0f);
    }

    public void playSound(String audio, EntityServer entity, float pitch,
            float gain, float range) {
        if (entity instanceof MobServer) {
            playSound(audio, entity.pos(), ((MobServer) entity).speed(), pitch,
                    gain, range);
        } else {
            playSound(audio, entity.pos(), Vector3d.ZERO, pitch, gain, range);
        }
    }

    public void playSound(String name, Vector3 position, Vector3 velocity) {
        playSound(name, position, velocity, 1.0f, 1.0f);
    }

    public void playSound(String audio, Vector3 position, Vector3 velocity,
            float range) {
        playSound(audio, position, velocity, 1.0f, 1.0f, range);
    }

    public void playSound(String audio, Vector3 position, Vector3 velocity,
            float pitch, float gain) {
        playSound(audio, position, velocity, pitch, gain, 16.0f);
    }

    public void playSound(String audio, Vector3 position, Vector3 velocity,
            float pitch, float gain, float range) {
        if (audio != null) {
            if (!audio.isEmpty()) {
                send(new PacketSoundEffect(audio, position, velocity, pitch,
                        gain, range));
            }
        }
    }

    public void entityListener(EntityListener listener) {
        entityListeners.add(listener);
    }

    public Stream<EntityListener> entityListeners() {
        return entityListeners.stream();
    }

    public void stop() {
        joiner.join();
    }

    public void start() {
        joiner = taskExecutor.runTask(joiner -> {
            sync.init();
            while (!joiner.marked()) {
                if (!players.isEmpty()) {
                    update(0.05);
                    if (players.values().stream()
                            .filter(MobPlayerServer::isActive).findAny()
                            .isPresent()) {
                        sync.cap();
                    } else {
                        sync.tick();
                    }
                } else {
                    sync.cap();
                }
            }
        }, "Tick-" + id, TaskExecutor.Priority.MEDIUM);
    }

    @Override
    public void send(Packet packet) {
        players.values().forEach(player -> player.connection().send(packet));
    }

    public void send(Packet packet, List<PlayerConnection> exceptions) {
        players.values().stream().map(MobPlayerServer::connection)
                .filter(player -> !exceptions.contains(player))
                .forEach(player -> player.send(packet));
    }

    @FunctionalInterface
    public interface EntityListener {
        void listen(EntityServer entity);
    }
}
