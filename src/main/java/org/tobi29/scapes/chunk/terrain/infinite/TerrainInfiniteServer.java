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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.block.BlockType;
import org.tobi29.scapes.block.Update;
import org.tobi29.scapes.chunk.MobSpawner;
import org.tobi29.scapes.chunk.WorldServer;
import org.tobi29.scapes.chunk.terrain.TerrainServer;
import org.tobi29.scapes.engine.utils.SleepUtil;
import org.tobi29.scapes.engine.utils.io.filesystem.Directory;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.vector.Vector2;
import org.tobi29.scapes.engine.utils.math.vector.Vector2d;
import org.tobi29.scapes.engine.utils.math.vector.Vector2i;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;
import org.tobi29.scapes.engine.utils.task.Joiner;
import org.tobi29.scapes.engine.utils.task.TaskExecutor;
import org.tobi29.scapes.entity.server.EntityServer;
import org.tobi29.scapes.entity.server.MobPlayerServer;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;

public class TerrainInfiniteServer extends TerrainInfinite
        implements TerrainServer.TerrainMutable {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(TerrainInfiniteServer.class);
    private final WorldServer world;
    private final TerrainInfiniteFormat format;
    private final Queue<BlockChanges> blockChanges =
            new ConcurrentLinkedQueue<>();
    private final Queue<TerrainInfiniteChunkServer> chunkUnloadQueue =
            new ConcurrentLinkedQueue<>();
    private final TerrainInfiniteChunkManagerServer chunkManager;
    private final Joiner joiner;

    public TerrainInfiniteServer(WorldServer world, int zSize,
            Directory directory, TaskExecutor taskExecutor,
            BlockType voidBlock) {
        super(zSize, new TerrainInfiniteChunkManagerServer(), taskExecutor,
                voidBlock);
        this.world = world;
        format = new TerrainInfiniteFormat(directory, this);
        chunkManager = (TerrainInfiniteChunkManagerServer) super.chunkManager;
        joiner = world.getTaskExecutor().runTask(joiner -> {
            List<Vector2> requiredChunks = new ArrayList<>();
            List<Vector2> loadingChunks = new ArrayList<>();
            while (!joiner.marked()) {
                Collection<MobPlayerServer> players = world.getPlayers();
                if (players.isEmpty()) {
                    chunkManager.getIterator().forEach(this::removeChunk);
                    SleepUtil.sleep(100);
                } else {
                    for (MobPlayerServer player : players) {
                        int xx = FastMath.floor(player.getX() / 16.0d);
                        int yy = FastMath.floor(player.getY() / 16.0d);
                        int loadingRadius =
                                (player.getConnection().getLoadingRadius() >>
                                        4) + 3;
                        int loadingRadiusSqr = loadingRadius * loadingRadius;
                        for (int x = -loadingRadius; x <= loadingRadius; x++) {
                            int xxx = x + xx;
                            for (int y = -loadingRadius; y <= loadingRadius;
                                    y++) {
                                int yyy = y + yy;
                                if (xxx >= cxMin && xxx <= cxMax &&
                                        yyy >= cyMin &&
                                        yyy <= cyMax) {
                                    if (x * x + y * y < loadingRadiusSqr) {
                                        requiredChunks.add(new Vector2i(x + xx,
                                                y + yy));
                                    }
                                }
                            }
                        }
                    }
                    for (MobPlayerServer player : players) {
                        Vector2 loadArea = new Vector2d(player.getX() / 16.0d,
                                player.getY() / 16.0d);
                        requiredChunks.stream()
                                .filter(pos -> !hasChunk(pos.intX(),
                                        pos.intY()))
                                .forEach(loadingChunks::add);
                        if (loadingChunks.size() > 64) {
                            loadingChunks.stream().sorted((pos1, pos2) -> {
                                double distance1 =
                                        FastMath.pointDistanceSqr(loadArea,
                                                pos1);
                                double distance2 =
                                        FastMath.pointDistanceSqr(loadArea,
                                                pos2);
                                return distance1 == distance2 ? 0 :
                                        distance1 > distance2 ? 1 : -1;
                            }).limit(32).forEach(
                                    pos -> addChunk(pos.intX(), pos.intY()));
                        } else {
                            loadingChunks.stream().limit(32).forEach(
                                    pos -> addChunk(pos.intX(), pos.intY()));
                        }
                        Collection<TerrainInfiniteChunkServer> chunks =
                                chunkManager.getIterator();
                        chunks.stream()
                                .filter(TerrainInfiniteChunkServer::shouldPopulate)
                                .limit(32)
                                .forEach(TerrainInfiniteChunkServer::populate);
                        chunks.stream()
                                .filter(TerrainInfiniteChunkServer::shouldFinish)
                                .forEach(TerrainInfiniteChunkServer::finish);
                        chunks.stream().filter(chunk -> !requiredChunks
                                .contains(chunk.getPos()))
                                .forEach(chunkUnloadQueue::add);
                        chunks.forEach(
                                TerrainInfiniteChunkServer::updateAdjacent);
                        if (loadingChunks.isEmpty()) {
                            SleepUtil.sleep(100);
                        }
                        loadingChunks.clear();
                    }
                    requiredChunks.clear();
                }
            }
        }, "Chunk-Loading");
    }

    public TerrainInfiniteFormat getTerrainFormat() {
        return format;
    }

    @Override
    public TerrainInfiniteChunkServer getChunkNoLoad(int x, int y) {
        return chunkManager.get(x, y);
    }

    @Override
    public void dispose() {
        joiner.join();
        super.dispose();
        chunkManager.getIterator().forEach(this::removeChunk);
        format.dispose();
    }

    @Override
    public TerrainInfiniteChunkServer getChunk(int x, int y) {
        TerrainInfiniteChunkServer chunk = chunkManager.get(x, y);
        if (chunk == null) {
            return addChunk(x, y);
        }
        return chunk;
    }

    @Override
    public TerrainInfiniteChunkServer addChunk(int x, int y) {
        if (x < cxMin || x > cxMax || y < cyMin || y > cyMax) {
            return null;
        }
        TerrainInfiniteChunkServer chunk;
        synchronized (chunkManager) {
            chunk = chunkManager.get(x, y);
            if (chunk == null) {
                TagStructure tagStructure = null;
                try {
                    tagStructure = format.getChunkTag(x, y);
                } catch (IOException e) {
                    LOGGER.error("Failed to load chunk:", e);
                }
                chunk = new TerrainInfiniteChunkServer(new Vector2i(x, y), this,
                        zSize, tagStructure);
                chunkManager.add(chunk);
            }
        }
        updateAdjacent(chunk.getX(), chunk.getY());
        return chunk;
    }

    @Override
    public WorldServer getWorld() {
        return world;
    }

    @Override
    public void queueBlockChanges(BlockChanges blockChanges) {
        this.blockChanges.add(blockChanges);
    }

    @Override
    public void update(double delta, Collection<MobSpawner> spawners) {
        chunkManager.getIterator().forEach(chunk -> chunk.updateServer(delta));
        Random random = ThreadLocalRandom.current();
        for (MobSpawner spawner : spawners) {
            if (world.getMobs(spawner.getCreatureType()) <
                    chunkManager.getAmount() * spawner.getMobsPerChunk()) {
                for (TerrainInfiniteChunkServer chunk : chunkManager
                        .getIterator()) {
                    if (random.nextInt(spawner.getChunkChance()) == 0 &&
                            chunk.isLoaded()) {
                        for (int i = 0; i < spawner.getSpawnAttempts(); i++) {
                            int x = random.nextInt(16);
                            int y = random.nextInt(16);
                            int z = random.nextInt(chunk.getZSize());
                            int xx = x + (chunk.getX() << 4);
                            int yy = y + (chunk.getY() << 4);
                            if (spawner.getCreatureType().getDespawn()) {
                                MobPlayerServer player = world.getNearestPlayer(
                                        new Vector3d(xx, yy, z));
                                if (player == null) {
                                    continue;
                                } else {
                                    double distance =
                                            FastMath.sqr(player.getX() - xx) +
                                                    FastMath.sqr(player.getY() -
                                                            yy) +
                                                    FastMath.sqr(
                                                            player.getZ() - z);
                                    if (distance > 9216.0d ||
                                            distance < 256.0d) {
                                        continue;
                                    }
                                }
                            }
                            if (spawner.canSpawn(this, xx, yy, z)) {
                                EntityServer entity =
                                        spawner.spawn(this, xx, yy, z);
                                entity.onSpawn();
                                world.addEntity(entity);
                            }
                        }
                    }
                }
            }
        }
        while (!blockChanges.isEmpty()) {
            blockChanges.poll().run(this);
        }
        while (!chunkUnloadQueue.isEmpty()) {
            removeChunk(chunkUnloadQueue.poll());
        }
    }

    @Override
    public void addDelayedUpdate(Update update) {
        TerrainInfiniteChunkServer chunk =
                getChunk(FastMath.floor(update.getX() / 16.0),
                        FastMath.floor(update.getY() / 16.0));
        if (chunk == null) {
            return;
        }
        chunk.addDelayedUpdate(update);
    }

    @Override
    public boolean hasDelayedUpdate(int x, int y, int z) {
        TerrainInfiniteChunkServer chunk =
                getChunk(FastMath.floor(x / 16.0d), FastMath.floor(y / 16.0d));
        return chunk != null && chunk.hasDelayedUpdate(x, y, z);
    }

    @Override
    public boolean isBlockSendable(MobPlayerServer player, int x, int y, int z,
            boolean chunkContent) {
        if (z < 0 || z >= zSize) {
            return false;
        }
        int loadingRadius = player.getConnection().getLoadingRadius() >> 4;
        x = FastMath.floor(x / 16.0d);
        y = FastMath.floor(y / 16.0d);
        int x2 = FastMath.floor(player.getX() / 16.0d);
        int y2 = FastMath.floor(player.getY() / 16.0d);
        TerrainInfiniteChunkServer chunk = getChunkNoLoad(x, y);
        int dis = FastMath.abs(x - x2);
        dis = FastMath.max(dis, FastMath.abs(y - y2));
        if (chunk != null) {
            if (dis > loadingRadius || !chunk.isSendable() && chunkContent) {
                return false;
            }
        } else {
            if (dis > loadingRadius) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void setBlockType(int x, int y, int z, BlockType type) {
        TerrainInfiniteChunkServer chunk =
                getChunk(FastMath.floor((double) x / 16),
                        FastMath.floor((double) y / 16));
        if (chunk == null) {
            return;
        }
        chunk.setBlockType(x - (chunk.getX() << 4), y - (chunk.getY() << 4), z,
                type);
    }

    @Override
    public void setBlockData(int x, int y, int z, int data) {
        TerrainInfiniteChunkServer chunk =
                getChunk(FastMath.floor((double) x / 16),
                        FastMath.floor((double) y / 16));
        if (chunk == null) {
            return;
        }
        chunk.setBlockData(x - (chunk.getX() << 4), y - (chunk.getY() << 4), z,
                data);
    }

    @Override
    public void setBlockTypeAndData(int x, int y, int z, BlockType block,
            int data) {
        TerrainInfiniteChunkServer chunk =
                getChunk(FastMath.floor((double) x / 16),
                        FastMath.floor((double) y / 16));
        if (chunk == null) {
            return;
        }
        chunk.setBlockIdAndData(x - (chunk.getX() << 4),
                y - (chunk.getY() << 4), z, block, data);
    }

    public void updateAdjacent(int x, int y) {
        for (int xx = -1; xx <= 1; xx++) {
            int xxx = xx + x;
            for (int yy = -1; yy <= 1; yy++) {
                if (xx != 0 || yy != 0) {
                    TerrainInfiniteChunkServer chunk =
                            chunkManager.get(xxx, yy + y);
                    if (chunk != null) {
                        chunk.updateAdjacent();
                    }
                }
            }
        }
    }

    public boolean checkBorder(TerrainInfiniteChunkServer chunk, int radius) {
        for (int x = -radius; x <= radius; x++) {
            int xx = chunk.getX() + x;
            for (int y = -radius; y <= radius; y++) {
                int yy = chunk.getY() + y;
                if (xx >= cxMin && xx <= cxMax && yy >= cyMin && yy <= cyMax) {
                    if (getChunkNoLoad(xx, yy) == null) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public boolean checkLoaded(TerrainInfiniteChunkServer chunk, int radius) {
        for (int x = -radius; x <= radius; x++) {
            int xx = chunk.getX() + x;
            for (int y = -radius; y <= radius; y++) {
                int yy = chunk.getY() + y;
                if (xx >= cxMin && xx <= cxMax && yy >= cyMin && yy <= cyMax) {
                    TerrainInfiniteChunkServer check = getChunkNoLoad(xx, yy);
                    if (check == null) {
                        return false;
                    }
                    if (!check.isLoaded()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private void removeChunk(TerrainInfiniteChunkServer chunk) {
        int x = chunk.getX();
        int y = chunk.getY();
        chunkManager.remove(x, y);
        try {
            chunk.disposeServer();
        } catch (IOException e) {
            LOGGER.error("Failed to save chunk:", e);
        }
        updateAdjacent(chunk.getX(), chunk.getY());
        while (chunkUnloadQueue.remove(chunk)) {
        }
    }
}
