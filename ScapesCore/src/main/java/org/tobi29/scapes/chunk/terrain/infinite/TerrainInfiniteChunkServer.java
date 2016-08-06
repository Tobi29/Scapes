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
package org.tobi29.scapes.chunk.terrain.infinite;

import java8.util.Optional;
import java8.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.block.*;
import org.tobi29.scapes.chunk.generator.ChunkGenerator;
import org.tobi29.scapes.chunk.generator.GeneratorOutput;
import org.tobi29.scapes.engine.utils.Streams;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.vector.Vector2i;
import org.tobi29.scapes.engine.utils.profiler.Profiler;
import org.tobi29.scapes.entity.server.EntityServer;
import org.tobi29.scapes.entity.server.MobPlayerServer;
import org.tobi29.scapes.packets.PacketBlockChange;
import org.tobi29.scapes.packets.PacketBlockChangeAir;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class TerrainInfiniteChunkServer extends TerrainInfiniteChunk {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(TerrainInfiniteChunkServer.class);
    private final Optional<TerrainInfiniteChunkServer> optional =
            Optional.of(this);
    private final TerrainInfiniteServer terrain;
    private final Map<Integer, EntityServer> entities =
            new ConcurrentHashMap<>();
    private final List<Update> delayedUpdates = new ArrayList<>();
    private long lastAccess = System.currentTimeMillis();

    public TerrainInfiniteChunkServer(Vector2i pos,
            TerrainInfiniteServer terrain, int zSize,
            TagStructure tagStructure) {
        super(pos, terrain, terrain.world(), zSize);
        this.terrain = terrain;
        try (Profiler.C ignored = Profiler.section("Load")) {
            load(tagStructure);
        }
        try (Profiler.C ignored = Profiler.section("HeightMap")) {
            initHeightMap();
        }
    }

    public TerrainInfiniteChunkServer(Vector2i pos,
            TerrainInfiniteServer terrain, int zSize, ChunkGenerator generator,
            GeneratorOutput output) {
        super(pos, terrain, terrain.world(), zSize);
        this.terrain = terrain;
        try (Profiler.C ignored = Profiler.section("Generate")) {
            generate(generator, output);
        }
        try (Profiler.C ignored = Profiler.section("Sunlight")) {
            initSunLight();
        }
        try (Profiler.C ignored = Profiler.section("HeightMap")) {
            initHeightMap();
        }
    }

    protected void accessed() {
        lastAccess = System.currentTimeMillis();
    }

    public long lastAccess() {
        return lastAccess;
    }

    public Optional<TerrainInfiniteChunkServer> optional() {
        return optional;
    }

    public void updateServer(double delta) {
        Streams.forEach(terrain.world().players(), entity -> {
            int x = FastMath.floor(entity.x()) >> 4;
            int y = FastMath.floor(entity.y()) >> 4;
            if (x == pos.intX() && y == pos.intY()) {
                addEntity(entity);
            }
        });
        Streams.forEach(entities.values(), entity -> {
            int x = FastMath.floor(entity.x()) >> 4;
            int y = FastMath.floor(entity.y()) >> 4;
            if (x != pos.intX() || y != pos.intY()) {
                terrain.chunkS(x, y, chunk -> chunk.addEntity(entity));
                removeEntity(entity);
            }
        });
        if (state.id >= State.LOADED.id) {
            synchronized (delayedUpdates) {
                int i = 0;
                while (i < delayedUpdates.size()) {
                    Update update = delayedUpdates.get(i);
                    if (update.isValid()) {
                        if (update.delay(delta) <= 0) {
                            delayedUpdates.remove(i--);
                            if (update.isValidOn(
                                    typeG(update.x(), update.y(), update.z()),
                                    terrain)) {
                                update.run(terrain);
                            }
                        }
                        i++;
                    } else {
                        delayedUpdates.remove(i);
                    }
                }
            }
            Random random = ThreadLocalRandom.current();
            if (random.nextInt(16) == 0) {
                int x = random.nextInt(16);
                int y = random.nextInt(16);
                int z = random.nextInt(zSize);
                typeL(x, y, z).update(terrain, x + posBlock.intX(),
                        y + posBlock.intY(), z);
            }
        }
    }

    public TagStructure dispose() {
        return save(false);
    }

    public void addEntity(EntityServer entity) {
        entities.put(entity.entityID(), entity);
    }

    public boolean removeEntity(EntityServer entity) {
        return entities.remove(entity.entityID()) != null;
    }

    public void addDelayedUpdate(Update update) {
        synchronized (delayedUpdates) {
            delayedUpdates.add(update);
        }
    }

    public boolean hasDelayedUpdate(int x, int y, int z,
            Class<? extends Update> clazz) {
        synchronized (delayedUpdates) {
            for (Update update : delayedUpdates) {
                if (update.x() == x && update.y() == y && update.z() == z) {
                    if (update.isValidOn(
                            typeG(update.x(), update.y(), update.z()),
                            terrain)) {
                        if (update.getClass() == clazz) {
                            return true;
                        }
                    } else {
                        update.markAsInvalid();
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void update(int x, int y, int z, boolean updateTile) {
        if (updateTile) {
            addDelayedUpdate(new UpdateBlockUpdateUpdateTile()
                    .set(x + posBlock.intX(), y + posBlock.intY(), z, 0.0));
        } else {
            addDelayedUpdate(new UpdateBlockUpdate()
                    .set(x + posBlock.intX(), y + posBlock.intY(), z, 0.0));
        }
        if (state.id >= State.SENDABLE.id) {
            BlockType type = typeL(x, y, z);
            if (type == terrain.world().air()) {
                terrain.world()
                        .send(new PacketBlockChangeAir(x + posBlock.intX(),
                                y + posBlock.intY(), z));
            } else {
                terrain.world().send(new PacketBlockChange(x + posBlock.intX(),
                        y + posBlock.intY(), z, type.id(), dataL(x, y, z)));
            }
        }
        if (state.id >= State.LOADED.id) {
            terrain.lighting()
                    .updateLight(x + posBlock.intX(), y + posBlock.intY(), z);
        }
    }

    @Override
    public void updateLight(int x, int y, int z) {
    }

    public void load(TagStructure tagStructure) {
        bID.load(tagStructure.getList("BlockID"));
        bData.load(tagStructure.getList("BlockData"));
        bLight.load(tagStructure.getList("BlockLight"));
        initHeightMap();
        for (TagStructure tag : tagStructure.getList("Entities")) {
            EntityServer entity =
                    EntityServer.make(tag.getInteger("ID"), terrain.world());
            entity.read(tag.getStructure("Data"));
            synchronized (terrain.entityLock) {
                entity.setEntityID(terrain.freeEntityID());
                addEntity(entity);
            }
            long oldTick = tag.getLong("Tick");
            long newTick = terrain.world().tick();
            if (newTick > oldTick) {
                entity.tickSkip(oldTick, newTick);
            }
        }
        for (TagStructure tag : tagStructure.getList("Updates")) {
            int xy = tag.getByte("PosXY");
            if (xy < 0) {
                xy += 256;
            }
            addDelayedUpdate(Update.make(terrain.world().plugins().registry(),
                    (xy & 0xF) + posBlock.intX(), (xy >> 4) + posBlock.intY(),
                    tag.getInteger("PosZ"), tag.getDouble("Delay"),
                    tag.getShort("ID")));
        }
        if (tagStructure.getBoolean("Populated")) {
            state = State.POPULATED;
        }
        metaData = tagStructure.getStructure("MetaData");
    }

    public TagStructure save(boolean packet) {
        bID.compress();
        bData.compress();
        bLight.compress();
        long tick = terrain.world().tick();
        TagStructure tagStructure = new TagStructure();
        tagStructure.setList("BlockID", bID.save());
        tagStructure.setList("BlockData", bData.save());
        tagStructure.setList("BlockLight", bLight.save());
        List<TagStructure> entitiesTag = new ArrayList<>();
        GameRegistry registry = terrain.world().registry();
        Streams.forEach(entities.values(),
                entity -> !(entity instanceof MobPlayerServer) || packet,
                entity -> {
                    TagStructure entityTag = new TagStructure();
                    if (packet) {
                        entityTag.setInteger("EntityID", entity.entityID());
                    } else {
                        entityTag.setLong("Tick", tick);
                    }
                    entityTag.setInteger("ID", entity.id(registry));
                    entityTag.setStructure("Data", entity.write());
                    entitiesTag.add(entityTag);
                });
        tagStructure.setList("Entities", entitiesTag);
        tagStructure.setStructure("MetaData", metaData);
        if (!packet) {
            List<TagStructure> updatesTag = new ArrayList<>();
            synchronized (delayedUpdates) {
                Streams.forEach(delayedUpdates, update -> update
                        .isValidOn(typeG(update.x(), update.y(), update.z()),
                                terrain), update -> {
                    TagStructure updateTag = new TagStructure();
                    updateTag.setShort("ID", update.id(registry));
                    updateTag.setDouble("Delay", update.delay());
                    updateTag.setByte("PosXY",
                            (byte) (update.x() - posBlock.intX() |
                                    update.y() - posBlock.intY() << 4));
                    int xy = updateTag.getByte("PosXY");
                    if (xy < 0) {
                        xy += 256;
                    }
                    updateTag.setInteger("PosZ", update.z());
                    updatesTag.add(updateTag);
                });
            }
            tagStructure.setList("Updates", updatesTag);
            tagStructure
                    .setBoolean("Populated", state.id >= State.POPULATED.id);
        }
        return tagStructure;
    }

    private void generate(ChunkGenerator generator, GeneratorOutput output) {
        generator.seed(posBlock.intX(), posBlock.intY());
        for (int y = 0; y < 16; y++) {
            int yy = posBlock.intY() + y;
            for (int x = 0; x < 16; x++) {
                int xx = posBlock.intX() + x;
                generator.makeLand(xx, yy, 0, zSize, output);
                for (int z = 0; z < zSize; z++) {
                    int type = output.type[z];
                    if (type != 0) {
                        bID.setDataUnsafe(x, y, z, 0, type);
                        int data = output.data[z];
                        if (data != 0) {
                            bData.setDataUnsafe(x, y, z, 0, data);
                        }
                    }
                }
                Streams.forEach(output.updates, this::addDelayedUpdate);
                output.updates.clear();
            }
        }
    }

    public void populate() {
        state = State.POPULATING;
        terrain.queue(handle -> {
            terrain.world().populators()
                    .forEach(pop -> pop.populate(handle, this));
            updateSunLight();
            bID.compress();
            bData.compress();
            bLight.compress();
            state = State.POPULATED;
        });
    }

    public void finish() {
        terrain.queue(handle -> {
            terrain.world().populators().forEach(pop -> pop.load(handle, this));
            state = State.BORDER;
            terrain.updateAdjacent(pos.intX(), pos.intY());
        });
    }

    public boolean isSendable() {
        return state.id >= State.SENDABLE.id;
    }

    public boolean shouldPopulate() {
        return state == State.SHOULD_POPULATE;
    }

    public boolean shouldFinish() {
        return state == State.POPULATED;
    }

    public void updateAdjacent() {
        if (terrain.checkBorder(this, 1)) {
            if (state == State.BORDER) {
                state = State.LOADED;
                terrain.updateAdjacent(pos.intX(), pos.intY());
            } else if (state == State.NEW) {
                state = State.SHOULD_POPULATE;
            }
            if (terrain.checkLoaded(this, 1)) {
                if (state == State.LOADED) {
                    state = State.SENDABLE;
                    terrain.updateAdjacent(pos.intX(), pos.intY());
                }
            } else if (state == State.SENDABLE) {
                state = State.LOADED;
                terrain.updateAdjacent(pos.intX(), pos.intY());
            }
        } else if (state.id >= State.LOADED.id) {
            state = State.BORDER;
            terrain.updateAdjacent(pos.intX(), pos.intY());
        }
    }

    public Stream<EntityServer> entities() {
        return Streams.of(entities.values());
    }

    public Optional<EntityServer> entity(int id) {
        return Optional.ofNullable(entities.get(id));
    }
}
