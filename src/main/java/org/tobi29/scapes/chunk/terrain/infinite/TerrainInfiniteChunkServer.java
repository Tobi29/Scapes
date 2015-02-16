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

package org.tobi29.scapes.chunk.terrain.infinite;

import org.tobi29.scapes.block.*;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.vector.Vector2i;
import org.tobi29.scapes.entity.server.EntityServer;
import org.tobi29.scapes.entity.server.MobPlayerServer;
import org.tobi29.scapes.packets.PacketBlockChange;
import org.tobi29.scapes.packets.PacketBlockChangeAir;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class TerrainInfiniteChunkServer extends TerrainInfiniteChunk {
    private final TerrainInfiniteServer terrain;
    private final List<Update> delayedUpdates = new ArrayList<>();

    public TerrainInfiniteChunkServer(Vector2i pos,
            TerrainInfiniteServer terrain, int zSize,
            TagStructure tagStructure) {
        super(pos, terrain, terrain.getWorld(), zSize);
        this.terrain = terrain;
        if (tagStructure == null) {
            terrain.getWorld().getGenerator()
                    .makeLand(pos.intX(), pos.intY(), this, bID, bData);
            initSunLight();
            initHeightMap();
        } else {
            load(tagStructure);
        }
    }

    public void updateServer(double delta) {
        if (state.id >= State.LOADED.id) {
            synchronized (delayedUpdates) {
                int i = 0;
                while (i < delayedUpdates.size()) {
                    Update update = delayedUpdates.get(i);
                    if (update.isValid()) {
                        if (update.delay(delta) <= 0) {
                            delayedUpdates.remove(i--);
                            if (update.isValidOn(getBlockType(
                                    update.getX() - posBlock.intX(),
                                    update.getY() - posBlock.intY(),
                                    update.getZ()), terrain)) {
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
            for (int i = 0; i < zSize / 128; i++) {
                int x = random.nextInt(16);
                int y = random.nextInt(16);
                int z = random.nextInt(zSize);
                getBlockType(x, y, z).update(terrain, x + posBlock.intX(),
                        y + posBlock.intY(), z);
            }
        }
    }

    public void disposeServer() throws IOException {
        List<EntityServer> entities = getEntities();
        for (EntityServer entity : entities) {
            terrain.getWorld().deleteEntity(entity);
        }
        terrain.getTerrainFormat()
                .putChunkTag(pos.intX(), pos.intY(), save(false, entities));
        disposed = true;
    }

    public void addDelayedUpdate(Update update) {
        synchronized (delayedUpdates) {
            delayedUpdates.add(update);
        }
    }

    public boolean hasDelayedUpdate(int x, int y, int z) {
        synchronized (delayedUpdates) {
            for (Update update : delayedUpdates) {
                if (update.getX() == x && update.getY() == y &&
                        update.getZ() == z) {
                    if (update.isValidOn(
                            getBlockType(update.getX() - posBlock.intX(),
                                    update.getY() - posBlock.intY(),
                                    update.getZ()), terrain)) {
                        return true;
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
            BlockType type = getBlockType(x, y, z);
            if (type == terrain.getWorld().getAir()) {
                terrain.getWorld().getConnection()
                        .send(new PacketBlockChangeAir(x + posBlock.intX(),
                                y + posBlock.intY(), z));
            } else {
                terrain.getWorld().getConnection()
                        .send(new PacketBlockChange(x + posBlock.intX(),
                                y + posBlock.intY(), z, type.getID(),
                                getBlockData(x, y, z)));
            }
        }
        if (state.id >= State.LOADED.id) {
            terrain.getLighting()
                    .updateLight(x + posBlock.intX(), y + posBlock.intY(), z);
        }
    }

    public void load(TagStructure tagStructure) {
        bID.load(tagStructure.getList("BlockID"));
        bData.load(tagStructure.getList("BlockData"));
        bLight.load(tagStructure.getList("BlockLight"));
        initHeightMap();
        for (TagStructure tag : tagStructure.getList("Entities")) {
            EntityServer entity =
                    EntityServer.make(tag.getInteger("ID"), terrain.getWorld());
            entity.read(tag.getStructure("Data"));
            terrain.getWorld().addEntity(entity);
            long oldTick = tag.getLong("Tick");
            long newTick = terrain.getWorld().getTick();
            if (newTick > oldTick) {
                entity.tickSkip(oldTick, newTick);
            }
        }
        for (TagStructure tag : tagStructure.getList("Updates")) {
            int xy = tag.getByte("PosXY");
            if (xy < 0) {
                xy += 256;
            }
            addDelayedUpdate(
                    Update.make(terrain.getWorld().getPlugins().getRegistry(),
                            (xy & 0xF) + posBlock.intX(),
                            (xy >> 4) + posBlock.intY(), tag.getInteger("PosZ"),
                            tag.getDouble("Delay"), tag.getShort("ID")));
        }
        if (tagStructure.getBoolean("Populated")) {
            state = State.POPULATED;
        }
        metaData = tagStructure.getStructure("MetaData");
        initHeightMap();
    }

    public TagStructure save(boolean packet) {
        return save(packet, getEntities());
    }

    public TagStructure save(boolean packet, List<EntityServer> entities) {
        bID.compress();
        bData.compress();
        bLight.compress();
        long tick = terrain.getWorld().getTick();
        TagStructure tagStructure = new TagStructure();
        tagStructure.setList("BlockID", bID.save());
        tagStructure.setList("BlockData", bData.save());
        tagStructure.setList("BlockLight", bLight.save());
        List<TagStructure> entitiesTag = new ArrayList<>();
        GameRegistry registry = terrain.getWorld().getRegistry();
        entities.stream()
                .filter(entity -> !(entity instanceof MobPlayerServer) ||
                        packet).forEach(entity -> {
            TagStructure entityTag = new TagStructure();
            if (packet) {
                entityTag.setInteger("EntityID", entity.getEntityID());
            } else {
                entityTag.setLong("Tick", tick);
            }
            entityTag.setInteger("ID", entity.getID(registry));
            entityTag.setStructure("Data", entity.write());
            entitiesTag.add(entityTag);
        });
        tagStructure.setList("Entities", entitiesTag);
        tagStructure.setStructure("MetaData", metaData);
        if (!packet) {
            List<TagStructure> updatesTag = new ArrayList<>();
            synchronized (delayedUpdates) {
                delayedUpdates.stream().filter(update -> update.isValidOn(
                        getBlockType(update.getX() - posBlock.intX(),
                                update.getY() - posBlock.intY(), update.getZ()),
                        terrain)).forEach(update -> {
                    TagStructure updateTag = new TagStructure();
                    updateTag.setShort("ID", update.getID(registry));
                    updateTag.setDouble("Delay", update.getDelay());
                    updateTag.setByte("PosXY",
                            (byte) (update.getX() - posBlock.intX() |
                                    update.getY() - posBlock.intY() << 4));
                    int xy = updateTag.getByte("PosXY");
                    if (xy < 0) {
                        xy += 256;
                    }
                    updateTag.setInteger("PosZ", update.getZ());
                    updatesTag.add(updateTag);
                });
            }
            tagStructure.setList("Updates", updatesTag);
            tagStructure
                    .setBoolean("Populated", state.id >= State.POPULATED.id);
        }
        return tagStructure;
    }

    public void populate() {
        state = State.POPULATING;
        terrain.queueBlockChanges(handle -> {
            terrain.getWorld().getPopulators().forEach(pop -> pop
                    .populate(handle, posBlock.intX(), posBlock.intY(), 16,
                            16));
            updateSunLight();
            terrain.getLighting()
                    .initLight(posBlock.intX(), posBlock.intY(), 16, 16);
            bID.compress();
            bData.compress();
            bLight.compress();
            state = State.POPULATED;
        });
    }

    public void finish() {
        terrain.queueBlockChanges(handle -> {
            terrain.getWorld().getPopulators().forEach(pop -> pop
                    .load(handle, posBlock.intX(), posBlock.intY(), 16, 16));
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
            } else {
                if (state == State.SENDABLE) {
                    state = State.LOADED;
                    terrain.updateAdjacent(pos.intX(), pos.intY());
                }
            }
        } else {
            if (state.id >= State.LOADED.id) {
                state = State.BORDER;
                terrain.updateAdjacent(pos.intX(), pos.intY());
            }
        }
    }

    private List<EntityServer> getEntities() {
        return terrain.getWorld().getEntities().filter(entity ->
                FastMath.floor(entity.getX() / 16.0) == pos.intX() &&
                        FastMath.floor(entity.getY() / 16.0) == pos.intY())
                .collect(Collectors.toList());
    }
}
