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
import org.tobi29.scapes.chunk.generator.ChunkGeneratorInfinite;
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
import org.tobi29.scapes.packets.PacketEntityAdd;
import org.tobi29.scapes.packets.PacketEntityDespawn;
import org.tobi29.scapes.packets.PacketSoundEffect;
import org.tobi29.scapes.server.connection.ServerConnection;
import org.tobi29.scapes.server.format.WorldFormat;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WorldServer extends World implements MultiTag.ReadAndWrite {
    private final Map<Integer, EntityServer> entities =
            new ConcurrentHashMap<>();
    private final Collection<EntityListener> entityListeners =
            Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Collection<MobSpawner> spawners =
            Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final WorldFormat worldFormat;
    private final String name, id;
    private final List<ChunkPopulator> pop = new ArrayList<>();
    private final TerrainServer terrain;
    private final ServerConnection connection;
    private final Sync sync = new Sync(20, 5000000000L, true, "Server-Update");
    private final Map<String, MobPlayerServer> players =
            new ConcurrentHashMap<>();
    private Joiner joiner;
    private ChunkGeneratorInfinite gen;

    public WorldServer(WorldFormat worldFormat, String name, String id,
            ServerConnection connection, TaskExecutor taskExecutor,
            TerrainSupplier terrainSupplier) {
        super(connection, worldFormat.getPlugins(), taskExecutor,
                worldFormat.getPlugins().getRegistry());
        seed = worldFormat.getSeed();
        this.name = name;
        this.id = id;
        this.worldFormat = worldFormat;
        this.connection = connection;
        terrain = terrainSupplier.get(this);
    }

    @Override
    public ServerConnection getConnection() {
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

    public void init(WorldEnvironment environment) {
        this.environment = environment;
        gen = environment.getGenerator();
        pop.add(environment.getPopulator());
        worldFormat.getPlugins().getPlugins()
                .forEach(plugin -> plugin.worldInit(this));
    }

    public void calculateSpawn() {
        spawn = environment.calculateSpawn(terrain);
    }

    public List<MobServer> getEntities(List<MobServer> exceptions,
            Frustum hitField) {
        return entities.values().stream()
                .filter(entity -> entity instanceof MobServer)
                .map(entity -> (MobServer) entity)
                .filter(mob -> !exceptions.contains(mob) &&
                        hitField.inView(mob.getAABB()) > 0)
                .collect(Collectors.toList());
    }

    public void addEntity(EntityServer add) {
        synchronized (entities) {
            addEntity(add, getFreeEntityID());
        }
    }

    public void addEntity(EntityServer add, int id) {
        entityListeners.forEach(listener -> listener.listen(add));
        add.setEntityID(id);
        entities.put(id, add);
        if (add.getID(plugins.getRegistry()) >= 0) {
            connection.send(new PacketEntityAdd(add, plugins.getRegistry()));
        }
    }

    public void deleteEntity(EntityServer del) {
        if (del != null) {
            entities.remove(del.getEntityID());
            connection.send(new PacketEntityDespawn(del));
        }
    }

    protected int getFreeEntityID() {
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

    public List<MobServer> getEntities(AABB aabb) {
        return entities.values().stream()
                .filter(entity -> entity instanceof MobServer)
                .map(entity -> (MobServer) entity)
                .filter(mob -> aabb.inside(mob.getPos()))
                .collect(Collectors.toList());
    }

    public List<EntityServer> getEntities(Vector3 pos, double rangeSqr) {
        return entities.values().stream().filter(entity ->
                FastMath.pointDistanceSqr(pos, entity.getPos()) <= rangeSqr)
                .collect(Collectors.toList());
    }

    public List<EntityServer> getEntities(int x, int y, int z) {
        return entities.values().stream()
                .filter(entity -> FastMath.floor(entity.getX()) == x &&
                        FastMath.floor(entity.getY()) == y &&
                        FastMath.floor(entity.getZ()) == z)
                .collect(Collectors.toList());
    }

    public EntityServer getEntity(int i) {
        return entities.get(i);
    }

    public Stream<EntityServer> getEntities() {
        return entities.values().stream();
    }

    public int getMobs(CreatureType creatureType) {
        int i = 0;
        for (EntityServer entity : entities.values()) {
            if (entity instanceof MobLivingServer) {
                if (((MobLivingServer) entity).getCreatureType() ==
                        creatureType) {
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
                    if (((MobLivingServer) entity).getCreatureType()
                            .getDespawn()) {
                        MobPlayerServer player =
                                getNearestPlayer(entity.getPos());
                        if (player != null) {
                            if (FastMath.pointDistanceSqr(entity.getPos(),
                                    player.getPos()) > 16384.0d) {
                                deleteEntity(entity);
                            }
                        } else {
                            deleteEntity(entity);
                        }
                    }
                }
            }
        });
        worldFormat.getPlugins().getPlugins()
                .forEach(plugin -> plugin.worldTick(this));
        environment.tick(delta);
        taskExecutor.tick();
        tick++;
    }

    public List<MobServer> damageEntities(List<MobServer> exceptions,
            Frustum hitField, double damage) {
        List<MobServer> entities = getEntities(exceptions, hitField);
        entities.stream().filter(entity -> entity instanceof MobLivingServer)
                .forEach(entity -> ((MobLivingServer) entity).damage(damage));
        return entities;
    }

    public void dropItems(List<ItemStack> items, int x, int y, int z,
            double despawntime) {
        Vector3 pos = new Vector3d(x + 0.5, y + 0.5, z + 0.5);
        for (ItemStack item : items) {
            dropItem(item, pos, despawntime);
        }
    }

    public void dropItem(ItemStack item, Vector3 pos, double despawntime) {
        Random random = ThreadLocalRandom.current();
        EntityServer entity = new MobItemServer(this, pos,
                new Vector3d(-2.0d + random.nextDouble() * 4.0d,
                        -2.0d + random.nextDouble() * 4.0d,
                        random.nextDouble() * 1.0d + 0.5), item, despawntime);
        entity.onSpawn();
        addEntity(entity);
    }

    public void dropItem(ItemStack item, int x, int y, int z) {
        dropItem(item, new Vector3d(x + 0.5, y + 0.5, z + 0.5), 600.0);
    }

    public void dropItem(ItemStack item, int x, int y, int z,
            double despawntime) {
        dropItem(item, new Vector3d(x + 0.5, y + 0.5, z + 0.5), despawntime);
    }

    public void dropItems(List<ItemStack> items, int x, int y, int z) {
        Vector3 pos = new Vector3d(x + 0.5, y + 0.5, z + 0.5);
        for (ItemStack item : items) {
            dropItem(item, pos);
        }
    }

    public void dropItem(ItemStack item, Vector3 pos) {
        dropItem(item, pos, 600.0);
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
            if (!terrain.getBlockType(x, y, z)
                    .isTransparent(terrain, x, y, z)) {
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
                            entity.getPos().minus(new Vector3d(x, y, z));
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
        terrain.queueBlockChanges(handler -> {
            Random random = ThreadLocalRandom.current();
            double step = 360.0d / FastMath.TWO_PI / size;
            for (double pitch = 90.0d; pitch >= -90.0d; pitch -= step) {
                double cosYaw = FastMath.cosTable(pitch * FastMath.DEG_2_RAD);
                double stepYawForPitch = FastMath.abs(step / cosYaw);
                double deltaZ = FastMath.sinTable(pitch * FastMath.DEG_2_RAD);
                for (double yaw = 0.0d; yaw < 360.0d; yaw += stepYawForPitch) {
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
                        BlockType type = terrain.getBlockType(xxx, yyy, zzz);
                        if (type != air) {
                            if (type instanceof BlockExplosive) {
                                ((BlockExplosive) type)
                                        .igniteByExplosion(terrain, xxx, yyy,
                                                zzz);
                            } else {
                                if (random.nextDouble() < dropChance) {
                                    dropItems(type.getDrops(
                                                    new ItemStack(registry),
                                                    terrain.getBlockData(xxx,
                                                            yyy, zzz)), xxx,
                                            yyy, zzz);
                                } else if (
                                        type.isSolid(terrain, xxx, yyy, zzz) &&
                                                !type.isTransparent(terrain,
                                                        xxx, yyy, zzz) &&
                                                random.nextDouble() <
                                                        blockChance) {
                                    int data =
                                            terrain.getBlockData(xxx, yyy, zzz);
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
                            handler.setBlockTypeAndData(xxx, yyy, zzz, air, 0);
                        }
                    }
                }
            }
            explosionEntities(x, y, z, size, push, damage);
        });
    }

    public MobPlayerServer getNearestPlayer(Vector3 pos) {
        MobPlayerServer player = null;
        double distance = -1;
        for (MobPlayerServer playerCheck : players.values()) {
            double distanceCheck =
                    FastMath.pointDistanceSqr(playerCheck.getPos(), pos);
            if (distanceCheck < distance || distance == -1) {
                player = playerCheck;
                distance = distanceCheck;
            }
        }
        return player;
    }

    public Collection<MobPlayerServer> getPlayers() {
        return players.values();
    }

    public void addPlayer(MobPlayerServer player) {
        players.put(player.getNickname(), player);
    }

    public synchronized void removePlayer(MobPlayerServer player) {
        players.remove(player.getNickname(), player);
    }

    public void addSpawner(MobSpawner spawner) {
        synchronized (spawners) {
            spawners.add(spawner);
        }
    }

    public String getName() {
        return name;
    }

    public Sync getSync() {
        return sync;
    }

    public void addPopulator(ChunkPopulator pop) {
        this.pop.add(pop);
    }

    public ChunkGeneratorInfinite getGenerator() {
        return gen;
    }

    public Stream<ChunkPopulator> getPopulators() {
        return pop.stream();
    }

    public TerrainServer getTerrain() {
        return terrain;
    }

    public void dispose() {
        terrain.dispose();
    }

    public void playSound(String audio, EntityServer entity) {
        playSound(audio, entity, 16.0f);
    }

    public void playSound(String audio, EntityServer entity, float range) {
        playSound(audio, entity, 1.0f, 1.0f, range);
    }

    public void playSound(String audio, EntityServer entity, float pitch,
            float gain, float range) {
        if (entity instanceof MobServer) {
            playSound(audio, entity.getPos(), ((MobServer) entity).getSpeed(),
                    pitch, gain, range);
        } else {
            playSound(audio, entity.getPos(), Vector3d.ZERO, pitch, gain,
                    range);
        }
    }

    public void playSound(String audio, Vector3 position, Vector3 velocity,
            float range) {
        playSound(audio, position, velocity, 1.0f, 1.0f, range);
    }

    public void playSound(String name, Vector3 position, Vector3 velocity) {
        playSound(name, position, velocity, 16.0f);
    }

    public void playSound(String audio, Vector3 position, Vector3 velocity,
            float pitch, float gain, float range) {
        if (audio != null) {
            if (!audio.isEmpty()) {
                connection.send(new PacketSoundEffect(audio, position, velocity,
                        pitch, gain, range));
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
                update(0.05);
                sync.capTPS();
            }
        }, Thread.NORM_PRIORITY, "Tick-" + id);
    }

    @FunctionalInterface
    public interface EntityListener {
        void listen(EntityServer entity);
    }

    @FunctionalInterface
    public interface TerrainSupplier {
        TerrainServer get(WorldServer world);
    }
}
