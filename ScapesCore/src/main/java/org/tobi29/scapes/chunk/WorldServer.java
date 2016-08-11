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
package org.tobi29.scapes.chunk;

import java8.util.Optional;
import java8.util.function.Consumer;
import java8.util.function.Function;
import java8.util.stream.Stream;
import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.chunk.generator.ChunkGenerator;
import org.tobi29.scapes.chunk.generator.ChunkPopulator2D;
import org.tobi29.scapes.chunk.terrain.TerrainServer;
import org.tobi29.scapes.connection.PlayConnection;
import org.tobi29.scapes.engine.utils.MutableInteger;
import org.tobi29.scapes.engine.utils.Streams;
import org.tobi29.scapes.engine.utils.Sync;
import org.tobi29.scapes.engine.utils.io.tag.MultiTag;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;
import org.tobi29.scapes.engine.utils.profiler.Profiler;
import org.tobi29.scapes.engine.utils.task.Joiner;
import org.tobi29.scapes.engine.utils.task.TaskExecutor;
import org.tobi29.scapes.entity.CreatureType;
import org.tobi29.scapes.entity.EntityContainer;
import org.tobi29.scapes.entity.server.*;
import org.tobi29.scapes.packets.PacketClient;
import org.tobi29.scapes.packets.PacketEntityAdd;
import org.tobi29.scapes.packets.PacketEntityDespawn;
import org.tobi29.scapes.packets.PacketSoundEffect;
import org.tobi29.scapes.server.connection.PlayerConnection;
import org.tobi29.scapes.server.connection.ServerConnection;
import org.tobi29.scapes.server.format.WorldFormat;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class WorldServer extends World<EntityServer>
        implements EntityContainer<EntityServer>, MultiTag.ReadAndWrite,
        PlayConnection<PacketClient> {
    private final Collection<EntityListener> entityListeners =
            Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Collection<MobSpawner> spawners =
            Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final String id;
    private final List<ChunkPopulator2D> populators = new ArrayList<>();
    private final TerrainServer terrain;
    private final ServerConnection connection;
    private final Sync sync = new Sync(20, 5000000000L, true, "Server-Update");
    private final Map<String, MobPlayerServer> players =
            new ConcurrentHashMap<>();
    private final EnvironmentServer environment;
    private final ChunkGenerator generator;
    private Joiner joiner;

    public WorldServer(WorldFormat worldFormat, String id, long seed,
            ServerConnection connection, TaskExecutor taskExecutor,
            Function<WorldServer, TerrainServer> terrainSupplier,
            Function<WorldServer, EnvironmentServer> environmentSupplier) {
        super(worldFormat.plugins(), taskExecutor,
                worldFormat.plugins().registry(), seed);
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

    @Override
    public boolean addEntity(EntityServer entity) {
        initEntity(entity);
        return terrain.addEntity(entity);
    }

    @Override
    public boolean removeEntity(EntityServer entity) {
        return terrain.removeEntity(entity);
    }

    @Override
    public boolean hasEntity(EntityServer entity) {
        return entity(entity.uuid()).isPresent() || terrain.hasEntity(entity);
    }

    @Override
    public Optional<EntityServer> entity(UUID uuid) {
        Optional<MobPlayerServer> player = Streams.of(players.values())
                .filter(entity -> entity.uuid().equals(uuid)).findAny();
        if (player.isPresent()) {
            return Optional.of(player.get());
        }
        return terrain.entity(uuid);
    }

    @Override
    public void entities(Consumer<Stream<? extends EntityServer>> consumer) {
        terrain.entities(consumer);
        consumer.accept(Streams.of(players.values()));
    }

    @Override
    public void entities(int x, int y, int z,
            Consumer<Stream<? extends EntityServer>> consumer) {
        terrain.entities(x, y, z, consumer);
        consumer.accept(Streams.of(players.values())
                .filter(entity -> FastMath.floor(entity.x()) == x)
                .filter(entity -> FastMath.floor(entity.y()) == y)
                .filter(entity -> FastMath.floor(entity.z()) == z));
    }

    @Override
    public void entitiesAtLeast(int minX, int minY, int minZ, int maxX,
            int maxY, int maxZ,
            Consumer<Stream<? extends EntityServer>> consumer) {
        terrain.entities(minX, minY, minZ, maxX, maxY, maxZ, consumer);
        consumer.accept(Streams.of(players.values()));
    }

    @Override
    public void entityAdded(EntityServer entity) {
        initEntity(entity);
        send(new PacketEntityAdd(entity, plugins.registry()));
    }

    @Override
    public void entityRemoved(EntityServer entity) {
        send(new PacketEntityDespawn(entity));
    }

    public void addEntityNew(EntityServer entity) {
        initEntity(entity);
        entity.onSpawn();
        terrain.addEntity(entity);
    }

    public void initEntity(EntityServer entity) {
        Streams.forEach(entityListeners, listener -> listener.listen(entity));
    }

    public Collection<MobPlayerServer> players() {
        return players.values();
    }

    public void addPlayer(MobPlayerServer player, boolean isNew) {
        initEntity(player);
        if (isNew) {
            player.onSpawn();
        }
        players.put(player.nickname(), player);
    }

    public synchronized void removePlayer(MobPlayerServer player) {
        players.remove(player.nickname());
        removeEntity(player);
    }

    public int mobs(CreatureType creatureType) {
        // TODO: Optimize: Keep track using add and remove
        MutableInteger count = new MutableInteger(0);
        entities(stream -> count.a +=
                stream.filter(entity -> entity instanceof MobLivingServer)
                        .filter(entity ->
                                ((MobLivingServer) entity).creatureType() ==
                                        creatureType).count());
        return count.a;
    }

    private void update(double delta) {
        synchronized (terrain) {
            try (Profiler.C ignored = Profiler.section("Terrain")) {
                terrain.update(delta, spawners);
            }
            try (Profiler.C ignored = Profiler.section("Entities")) {
                Streams.forEach(players.values(), player -> {
                    player.update(delta);
                    player.updateListeners(delta);
                    player.move(delta);
                    if (player.isDead()) {
                        player.onDeath();
                    }
                });
            }
            try (Profiler.C ignored = Profiler.section("Environment")) {
                environment.tick(delta);
            }
            try (Profiler.C ignored = Profiler.section("Tasks")) {
                taskExecutor.tick();
            }
            tick++;
        }
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
        addEntityNew(entity);
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

    public void addPopulator(ChunkPopulator2D populator) {
        populators.add(populator);
    }

    public ChunkGenerator generator() {
        return generator;
    }

    public Stream<ChunkPopulator2D> populators() {
        return Streams.of(populators);
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

    public void stop(Optional<WorldServer> dropWorld) {
        if (dropWorld.isPresent() && dropWorld.get() != this) {
            WorldServer world = dropWorld.get();
            while (!players.isEmpty()) {
                Streams.forEach(players.values(),
                        player -> player.connection().setWorld(world));
            }
        } else {
            while (!players.isEmpty()) {
                Streams.forEach(players.values(), player -> player.connection()
                        .disconnect("World closed", 5));
            }
        }
        joiner.join();
    }

    public void start() {
        joiner = taskExecutor.runTask(joiner -> {
            thread = Thread.currentThread();
            sync.init();
            while (!joiner.marked()) {
                if (!players.isEmpty()) {
                    try (Profiler.C ignored = Profiler.section("Tick")) {
                        update(0.05);
                    }
                    if (Streams.of(players.values())
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
            thread = null;
        }, "Tick-" + id, TaskExecutor.Priority.MEDIUM);
    }

    @Override
    public void send(PacketClient packet) {
        Streams.forEach(players.values(),
                player -> player.connection().send(packet));
    }

    public void send(PacketClient packet, List<PlayerConnection> exceptions) {
        Streams.of(players.values()).map(MobPlayerServer::connection)
                .filter(player -> !exceptions.contains(player))
                .forEach(player -> player.send(packet));
    }

    @Override
    protected Stream<MobPlayerServer> worldEntities() {
        return Streams.of(players.values());
    }

    public interface EntityListener {
        void listen(EntityServer entity);
    }
}
