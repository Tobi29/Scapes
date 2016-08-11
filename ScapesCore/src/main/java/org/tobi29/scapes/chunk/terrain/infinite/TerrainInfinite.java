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
import java8.util.function.Consumer;
import java8.util.function.Function;
import java8.util.stream.Stream;
import org.tobi29.scapes.block.AABBElement;
import org.tobi29.scapes.block.BlockType;
import org.tobi29.scapes.chunk.lighting.LightingEngine;
import org.tobi29.scapes.chunk.lighting.LightingEngineThreaded;
import org.tobi29.scapes.chunk.terrain.TerrainEntity;
import org.tobi29.scapes.engine.utils.Pool;
import org.tobi29.scapes.engine.utils.Streams;
import org.tobi29.scapes.engine.utils.ThreadLocalUtil;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.PointerPane;
import org.tobi29.scapes.engine.utils.task.TaskExecutor;
import org.tobi29.scapes.entity.Entity;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class TerrainInfinite<E extends Entity>
        implements TerrainEntity<E> {
    protected static final ThreadLocal<Pool<AABBElement>> AABBS =
            ThreadLocalUtil.of(() -> new Pool<>(AABBElement::new));
    protected static final ThreadLocal<Pool<PointerPane>> POINTER_PANES =
            ThreadLocalUtil.of(() -> new Pool<>(PointerPane::new));
    protected final Map<UUID, E> entityMap = new ConcurrentHashMap<>();
    protected final int zSize, cxMin, cxMax, cyMin, cyMax;
    protected final BlockType voidBlock;
    protected final TerrainInfiniteChunkManager<E> chunkManager;
    protected final LightingEngine lighting;

    protected TerrainInfinite(int zSize,
            TerrainInfiniteChunkManager<E> chunkManager,
            TaskExecutor taskExecutor, BlockType voidBlock) {
        this.zSize = zSize;
        this.chunkManager = chunkManager;
        this.voidBlock = voidBlock;
        int radius = 0x8000000 - 16;
        cxMin = -radius + 1;
        cxMax = radius;
        cyMin = -radius + 1;
        cyMax = radius;
        lighting = new LightingEngineThreaded(this, taskExecutor);
    }

    public boolean chunkS2(int x, int y,
            Consumer<TerrainInfiniteChunk<E>> consumer) {
        Optional<? extends TerrainInfiniteChunk<E>> chunk = chunk(x, y);
        if (chunk.isPresent()) {
            consumer.accept(chunk.get());
            return true;
        }
        return false;
    }

    public <R> Optional<R> chunkReturnS2(int x, int y,
            Function<TerrainInfiniteChunk<E>, R> consumer) {
        Optional<? extends TerrainInfiniteChunk<E>> chunk = chunk(x, y);
        if (chunk.isPresent()) {
            return Optional.of(consumer.apply(chunk.get()));
        }
        return Optional.empty();
    }

    public Optional<? extends TerrainInfiniteChunk<E>> chunkNoLoad(int x,
            int y) {
        return chunkManager.get(x, y);
    }

    public boolean chunk(int x, int y,
            Consumer<TerrainInfiniteChunk<E>> consumer) {
        Optional<? extends TerrainInfiniteChunk<E>> chunk = chunk(x, y);
        if (chunk.isPresent()) {
            consumer.accept(chunk.get());
            return true;
        }
        return false;
    }

    public <R> Optional<R> chunkReturn(int x, int y,
            Function<TerrainInfiniteChunk<E>, R> consumer) {
        Optional<? extends TerrainInfiniteChunk<E>> chunk = chunk(x, y);
        if (chunk.isPresent()) {
            return Optional.of(consumer.apply(chunk.get()));
        }
        return Optional.empty();
    }

    @Override
    public void sunLight(int x, int y, int z, int light) {
        if (z < 0 || z >= zSize) {
            return;
        }
        chunk(x >> 4, y >> 4, chunk -> chunk.sunLightG(x, y, z, light));
    }

    @Override
    public void blockLight(int x, int y, int z, int light) {
        if (z < 0 || z >= zSize) {
            return;
        }
        chunk(x >> 4, y >> 4, chunk -> chunk.blockLightG(x, y, z, light));
    }

    @Override
    public BlockType type(int x, int y, int z) {
        if (z < 0 || z >= zSize) {
            return voidBlock;
        }
        return chunkReturn(x >> 4, y >> 4, chunk -> chunk.typeG(x, y, z))
                .orElse(voidBlock);
    }

    @Override
    public int data(int x, int y, int z) {
        if (z < 0 || z >= zSize) {
            return 0;
        }
        return chunkReturn(x >> 4, y >> 4, chunk -> chunk.dataG(x, y, z))
                .orElse(0);
    }

    @Override
    public int light(int x, int y, int z) {
        if (z < 0 || z >= zSize) {
            return 0;
        }
        return chunkReturn(x >> 4, y >> 4, chunk -> chunk.lightG(x, y, z))
                .orElse(0);
    }

    @Override
    public int sunLight(int x, int y, int z) {
        if (z < 0 || z >= zSize) {
            return 0;
        }
        return chunkReturn(x >> 4, y >> 4, chunk -> chunk.sunLightG(x, y, z))
                .orElse(0);
    }

    @Override
    public int blockLight(int x, int y, int z) {
        if (z < 0 || z >= zSize) {
            return 0;
        }
        return chunkReturn(x >> 4, y >> 4, chunk -> chunk.blockLightG(x, y, z))
                .orElse(0);
    }

    @Override
    public int highestBlockZAt(int x, int y) {
        return chunkReturn(x >> 4, y >> 4,
                chunk -> chunk.highestBlockZAtG(x, y)).orElse(1);
    }

    @Override
    public int highestTerrainBlockZAt(int x, int y) {
        return chunkReturn(x >> 4, y >> 4,
                chunk -> chunk.highestTerrainBlockZAtG(x, y)).orElse(1);
    }

    @Override
    public boolean isBlockLoaded(int x, int y, int z) {
        if (z < 0 || z >= zSize) {
            return false;
        }
        Optional<? extends TerrainInfiniteChunk<E>> chunk =
                chunkNoLoad(x >> 4, y >> 4);
        return chunk.isPresent() && chunk.get().isLoaded();
    }

    @Override
    public boolean isBlockTicking(int x, int y, int z) {
        Optional<? extends TerrainInfiniteChunk<E>> chunk =
                chunkNoLoad(x >> 4, y >> 4);
        return chunk.isPresent() && chunk.get().isLoaded();
    }

    @Override
    public Pool<AABBElement> collisions(int minX, int minY, int minZ, int maxX,
            int maxY, int maxZ) {
        Pool<AABBElement> aabbs = AABBS.get();
        aabbs.reset();
        minZ = FastMath.clamp(minZ, 0, zSize);
        maxZ = FastMath.clamp(maxZ, 0, zSize);
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                Optional<? extends TerrainInfiniteChunk<E>> chunk =
                        chunkNoLoad(x >> 4, y >> 4);
                if (chunk.isPresent() && chunk.get().isLoaded()) {
                    TerrainInfiniteChunk<E> chunk2 = chunk.get();
                    for (int z = minZ; z <= maxZ; z++) {
                        if (z >= 0 && z < zSize) {
                            chunk2.typeG(x, y, z)
                                    .addCollision(aabbs, this, x, y, z);
                        }
                    }
                } else {
                    aabbs.push().set(x, y, minZ, x + 1, y + 1, maxZ + 1,
                            BlockType.STANDARD_COLLISION);
                }
            }
        }
        return aabbs;
    }

    @Override
    public Pool<PointerPane> pointerPanes(int x, int y, int z, int range) {
        Pool<PointerPane> pointerPanes = POINTER_PANES.get();
        for (int xx = -range; xx <= range; xx++) {
            int xxx = x + xx;
            for (int yy = -range; yy <= range; yy++) {
                int yyy = y + yy;
                for (int zz = -range; zz <= range; zz++) {
                    int zzz = z + zz;
                    if (zzz >= 0 && zzz < zSize) {
                        chunk(xxx >> 4, yyy >> 4,
                                chunk -> chunk.typeG(xxx, yyy, zzz)
                                        .addPointerCollision(
                                                chunk.dataG(xxx, yyy, zzz),
                                                pointerPanes, xxx, yyy, zzz));
                    }
                }
            }
        }
        return pointerPanes;
    }

    @Override
    public boolean removeEntity(E entity) {
        int x = FastMath.floor(entity.x()) >> 4;
        int y = FastMath.floor(entity.y()) >> 4;
        if (chunkReturnS2(x, y, chunk -> chunk.removeEntity(entity))
                .orElse(false)) {
            return true;
        }
        for (TerrainInfiniteChunk<E> chunk : chunkManager.iterator()) {
            if (chunk.removeEntity(entity)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasEntity(E entity) {
        return entityMap.containsValue(entity);
    }

    @Override
    public Optional<E> entity(UUID uuid) {
        return Optional.ofNullable(entityMap.get(uuid));
    }

    @Override
    public void entities(Consumer<Stream<? extends E>> consumer) {
        consumer.accept(Streams.of(entityMap.values()));
    }

    @Override
    public void entities(int x, int y, int z,
            Consumer<Stream<? extends E>> consumer) {
        chunkS2(x >> 4, y >> 4, chunk -> consumer.accept(chunk.entities()
                .filter(entity -> FastMath.floor(entity.x()) == x)
                .filter(entity -> FastMath.floor(entity.y()) == y)
                .filter(entity -> FastMath.floor(entity.z()) == z)));
    }

    @Override
    public void entitiesAtLeast(int minX, int minY, int minZ, int maxX,
            int maxY, int maxZ, Consumer<Stream<? extends E>> consumer) {
        int minCX = minX >> 4;
        int minCY = minY >> 4;
        int maxCX = maxX >> 4;
        int maxCY = maxY >> 4;
        for (int yy = minCY; yy <= maxCY; yy++) {
            for (int xx = minCX; xx <= maxCX; xx++) {
                chunkNoLoad(xx, yy)
                        .ifPresent(chunk -> consumer.accept(chunk.entities()));
            }
        }
    }

    @Override
    public void entityAdded(E entity) {
        E removed = entityMap.put(entity.uuid(), entity);
        if (removed != null) {
            throw new IllegalStateException(
                    "Duplicate entity: " + removed.uuid());
        }
    }

    @Override
    public void entityRemoved(E entity) {
        entityMap.remove(entity.uuid());
    }

    public boolean hasChunk(int x, int y) {
        return chunkManager.has(x, y);
    }

    public abstract Optional<? extends TerrainInfiniteChunk<E>> chunk(int x,
            int y);

    public LightingEngine lighting() {
        return lighting;
    }

    public Collection<? extends TerrainInfiniteChunk<E>> loadedChunks() {
        return chunkManager.iterator();
    }
}
