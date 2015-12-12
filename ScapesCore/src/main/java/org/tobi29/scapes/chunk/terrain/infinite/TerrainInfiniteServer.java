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

import java8.util.Optional;
import java8.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.block.BlockType;
import org.tobi29.scapes.block.Update;
import org.tobi29.scapes.chunk.MobSpawner;
import org.tobi29.scapes.chunk.WorldServer;
import org.tobi29.scapes.chunk.generator.GeneratorOutput;
import org.tobi29.scapes.chunk.terrain.TerrainServer;
import org.tobi29.scapes.engine.utils.Pair;
import org.tobi29.scapes.engine.utils.SleepUtil;
import org.tobi29.scapes.engine.utils.Streams;
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
import org.tobi29.scapes.server.format.TerrainInfiniteFormat;

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
    private final GeneratorOutput generatorOutput;
    private final Joiner joiner;

    public TerrainInfiniteServer(WorldServer world, int zSize,
            TerrainInfiniteFormat format, TaskExecutor taskExecutor,
            BlockType voidBlock) {
        super(zSize, new TerrainInfiniteChunkManagerServer(), taskExecutor,
                voidBlock);
        this.world = world;
        this.format = format;
        chunkManager = (TerrainInfiniteChunkManagerServer) super.chunkManager;
        generatorOutput = new GeneratorOutput(zSize);
        Joiner loadJoiner = world.taskExecutor().runTask(joiner -> {
            List<Vector2i> requiredChunks = new ArrayList<>();
            List<Vector2i> loadingChunks = new ArrayList<>();
            while (!joiner.marked()) {
                Collection<MobPlayerServer> players = world.players();
                if (players.isEmpty()) {
                    Streams.of(chunkManager.iterator())
                            .forEach(chunkUnloadQueue::add);
                    SleepUtil.sleep(100);
                } else {
                    for (MobPlayerServer player : players) {
                        int xx = FastMath.floor(player.x() / 16.0);
                        int yy = FastMath.floor(player.y() / 16.0);
                        int loadingRadius =
                                (player.connection().loadingRadius() >> 4) + 3;
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
                        Vector2 loadArea = new Vector2d(player.x() / 16.0,
                                player.y() / 16.0);
                        Streams.of(requiredChunks)
                                .filter(pos -> !hasChunk(pos.intX(),
                                        pos.intY()))
                                .forEach(loadingChunks::add);
                        List<Vector2i> newChunks;
                        if (loadingChunks.size() > 64) {
                            newChunks = Streams.of(loadingChunks)
                                    .sorted((pos1, pos2) -> {
                                        double distance1 =
                                                FastMath.pointDistanceSqr(
                                                        loadArea, pos1);
                                        double distance2 =
                                                FastMath.pointDistanceSqr(
                                                        loadArea, pos2);
                                        return distance1 == distance2 ? 0 :
                                                distance1 > distance2 ? 1 : -1;
                                    }).limit(32).collect(Collectors.toList());
                        } else {
                            newChunks = Streams.of(loadingChunks).limit(32)
                                    .collect(Collectors.toList());
                        }
                        addChunks(newChunks);
                        Collection<TerrainInfiniteChunkServer> chunks =
                                chunkManager.iterator();
                        Streams.of(chunks)
                                .filter(TerrainInfiniteChunkServer::shouldPopulate)
                                .limit(32)
                                .forEach(TerrainInfiniteChunkServer::populate);
                        Streams.of(chunks)
                                .filter(TerrainInfiniteChunkServer::shouldFinish)
                                .forEach(TerrainInfiniteChunkServer::finish);
                        Streams.of(chunks).filter(chunk -> !requiredChunks
                                .contains(chunk.pos()))
                                .forEach(chunkUnloadQueue::add);
                        Streams.of(chunks).forEach(
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
        Joiner updateJoiner = world.taskExecutor().runTask(joiner -> {
            while (!joiner.marked()) {
                boolean idle = true;
                while (!blockChanges.isEmpty()) {
                    blockChanges.poll().run(this);
                    idle = false;
                }
                idle |= removeChunks();
                if (idle) {
                    SleepUtil.sleep(10);
                }
            }
        }, "Chunk-Updating");
        joiner = new Joiner(loadJoiner, updateJoiner);
    }

    @Override
    public int sunLightReduction(int x, int y) {
        return (int) world.environment().sunLightReduction(x, y);
    }

    @Override
    public Optional<TerrainInfiniteChunkServer> chunkNoLoad(int x, int y) {
        return chunkManager.get(x, y);
    }

    @Override
    public Optional<TerrainInfiniteChunkServer> chunk(int x, int y) {
        Optional<TerrainInfiniteChunkServer> chunk = chunkManager.get(x, y);
        if (chunk.isPresent()) {
            return chunk;
        }
        return addChunk(x, y);
    }

    public Optional<TerrainInfiniteChunkServer> addChunk(int x, int y) {
        return addChunks(Collections.singletonList(new Vector2i(x, y))).get(0);
    }

    public List<Optional<TerrainInfiniteChunkServer>> addChunks(
            List<Vector2i> positions) {
        List<Optional<TerrainInfiniteChunkServer>> chunks =
                new ArrayList<>(positions.size());
        List<Optional<TagStructure>> tagStructures =
                format.chunkTags(positions);
        for (int i = 0; i < positions.size(); i++) {
            Vector2i pos = positions.get(i);
            int x = pos.intX();
            int y = pos.intY();
            if (x < cxMin || x > cxMax || y < cyMin || y > cyMax) {
                chunks.add(Optional.empty());
                continue;
            }
            Optional<TerrainInfiniteChunkServer> chunk;
            synchronized (chunkManager) {
                chunk = chunkManager.get(x, y);
                if (!chunk.isPresent()) {
                    Optional<TagStructure> tagStructure = tagStructures.get(i);
                    TerrainInfiniteChunkServer chunk2 =
                            new TerrainInfiniteChunkServer(new Vector2i(x, y),
                                    this, zSize, tagStructure,
                                    world.generator(), generatorOutput);
                    chunkManager.add(chunk2);
                    updateAdjacent(chunk2.x(), chunk2.y());
                    chunk = chunk2.optional();
                }
            }
            chunks.add(chunk);
        }
        return chunks;
    }

    @Override
    public WorldServer world() {
        return world;
    }

    @Override
    public void update(double delta, Collection<MobSpawner> spawners) {
        synchronized (chunkUnloadQueue) {
            Streams.of(chunkManager.iterator())
                    .forEach(chunk -> chunk.updateServer(delta));
            Random random = ThreadLocalRandom.current();
            for (MobSpawner spawner : spawners) {
                if (world.mobs(spawner.creatureType()) <
                        chunkManager.chunks() * spawner.mobsPerChunk()) {
                    for (TerrainInfiniteChunkServer chunk : chunkManager
                            .iterator()) {
                        if (random.nextInt(spawner.chunkChance()) == 0 &&
                                chunk.isLoaded()) {
                            for (int i = 0; i < spawner.spawnAttempts(); i++) {
                                int x = random.nextInt(16);
                                int y = random.nextInt(16);
                                int z = random.nextInt(chunk.zSize());
                                int xx = x + chunk.blockX();
                                int yy = y + chunk.blockY();
                                if (spawner.creatureType().doesDespawn()) {
                                    MobPlayerServer player =
                                            world.nearestPlayer(
                                                    new Vector3d(xx, yy, z));
                                    if (player == null) {
                                        continue;
                                    } else {
                                        double distance =
                                                FastMath.sqr(player.x() - xx) +
                                                        FastMath.sqr(
                                                                player.y() -
                                                                        yy) +
                                                        FastMath.sqr(
                                                                player.z() - z);
                                        if (distance > 9216.0 ||
                                                distance < 256.0) {
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
        }
    }

    @Override
    public void queue(BlockChanges blockChanges) {
        this.blockChanges.add(blockChanges);
    }

    @Override
    public void addDelayedUpdate(Update update) {
        Optional<TerrainInfiniteChunkServer> chunk =
                chunk(update.x() >> 4, update.y() >> 4);
        if (chunk.isPresent()) {
            chunk.get().addDelayedUpdate(update);
        }
    }

    @Override
    public boolean hasDelayedUpdate(int x, int y, int z) {
        if (z < 0 || z >= zSize) {
            return false;
        }
        Optional<TerrainInfiniteChunkServer> chunk = chunk(x >> 4, y >> 4);
        return chunk.isPresent() && chunk.get().hasDelayedUpdate(x, y, z);
    }

    @Override
    public boolean isBlockSendable(MobPlayerServer player, int x, int y, int z,
            boolean chunkContent) {
        if (z < 0 || z >= zSize) {
            return false;
        }
        int loadingRadius = player.connection().loadingRadius() >> 4;
        x = x >> 4;
        y = y >> 4;
        int x2 = FastMath.floor(player.x() / 16.0);
        int y2 = FastMath.floor(player.y() / 16.0);
        Optional<TerrainInfiniteChunkServer> chunk = chunkNoLoad(x, y);
        int dis = FastMath.abs(x - x2);
        dis = FastMath.max(dis, FastMath.abs(y - y2));
        if (chunk.isPresent()) {
            if (dis > loadingRadius ||
                    !chunk.get().isSendable() && chunkContent) {
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
    public void dispose() {
        joiner.join();
        lighting.dispose();
        Streams.of(chunkManager.iterator()).forEach(chunkUnloadQueue::add);
        removeChunks();
        format.dispose();
    }

    @Override
    public void type(int x, int y, int z, BlockType type) {
        if (z < 0 || z >= zSize) {
            return;
        }
        Optional<TerrainInfiniteChunkServer> chunk = chunk(x >> 4, y >> 4);
        if (!chunk.isPresent()) {
            return;
        }
        TerrainInfiniteChunk chunk2 = chunk.get();
        chunk2.blockTypeG(x, y, z, type);
    }

    @Override
    public void data(int x, int y, int z, int data) {
        if (z < 0 || z >= zSize) {
            return;
        }
        Optional<TerrainInfiniteChunkServer> chunk = chunk(x >> 4, y >> 4);
        if (!chunk.isPresent()) {
            return;
        }
        TerrainInfiniteChunk chunk2 = chunk.get();
        chunk2.dataG(x, y, z, data);
    }

    @Override
    public void typeData(int x, int y, int z, BlockType block, int data) {
        if (z < 0 || z >= zSize) {
            return;
        }
        Optional<TerrainInfiniteChunkServer> chunk = chunk(x >> 4, y >> 4);
        if (!chunk.isPresent()) {
            return;
        }
        TerrainInfiniteChunk chunk2 = chunk.get();
        chunk2.typeDataG(x, y, z, block, data);
    }

    public void updateAdjacent(int x, int y) {
        for (int xx = -1; xx <= 1; xx++) {
            int xxx = xx + x;
            for (int yy = -1; yy <= 1; yy++) {
                if (xx != 0 || yy != 0) {
                    chunkManager.get(xxx, yy + y).ifPresent(
                            TerrainInfiniteChunkServer::updateAdjacent);
                }
            }
        }
    }

    public boolean checkBorder(TerrainInfiniteChunkServer chunk, int radius) {
        for (int x = -radius; x <= radius; x++) {
            int xx = chunk.x() + x;
            for (int y = -radius; y <= radius; y++) {
                int yy = chunk.y() + y;
                if (xx >= cxMin && xx <= cxMax && yy >= cyMin && yy <= cyMax) {
                    if (!chunkNoLoad(xx, yy).isPresent()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public boolean checkLoaded(TerrainInfiniteChunkServer chunk, int radius) {
        for (int x = -radius; x <= radius; x++) {
            int xx = chunk.x() + x;
            for (int y = -radius; y <= radius; y++) {
                int yy = chunk.y() + y;
                if (xx >= cxMin && xx <= cxMax && yy >= cyMin && yy <= cyMax) {
                    Optional<TerrainInfiniteChunkServer> check =
                            chunkNoLoad(xx, yy);
                    if (!check.isPresent() || !check.get().isLoaded()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean removeChunks() {
        List<Pair<Vector2i, TagStructure>> chunks = new ArrayList<>();
        while (!chunkUnloadQueue.isEmpty()) {
            synchronized (chunkUnloadQueue) {
                removeChunk(chunkUnloadQueue.poll()).ifPresent(chunks::add);
            }
        }
        if (chunks.isEmpty()) {
            return true;
        }
        try {
            format.putChunkTags(chunks);
        } catch (IOException e) {
            LOGGER.error("Failed to store chunks:", e);
        }
        return false;
    }

    private Optional<Pair<Vector2i, TagStructure>> removeChunk(
            TerrainInfiniteChunkServer chunk) {
        int x = chunk.x();
        int y = chunk.y();
        chunkManager.remove(x, y);
        TagStructure tagStructure = chunk.dispose();
        while (chunkUnloadQueue.remove(chunk)) {
        }
        updateAdjacent(chunk.x(), chunk.y());
        return Optional.of(new Pair<>(new Vector2i(x, y), tagStructure));
    }
}
