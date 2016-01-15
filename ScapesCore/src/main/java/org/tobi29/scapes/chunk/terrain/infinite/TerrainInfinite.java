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
import java8.util.function.Consumer;
import java8.util.function.Function;
import org.tobi29.scapes.block.AABBElement;
import org.tobi29.scapes.block.BlockType;
import org.tobi29.scapes.chunk.lighting.LightingEngine;
import org.tobi29.scapes.chunk.lighting.LightingEngineThreaded;
import org.tobi29.scapes.chunk.terrain.Terrain;
import org.tobi29.scapes.engine.utils.Pool;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.PointerPane;
import org.tobi29.scapes.engine.utils.task.TaskExecutor;

import java.util.Collection;

public abstract class TerrainInfinite implements Terrain {
    protected static final ThreadLocal<Pool<AABBElement>> AABBS =
            new ThreadLocal<Pool<AABBElement>>() {
                @Override
                protected Pool<AABBElement> initialValue() {
                    return new Pool<>(AABBElement::new);
                }
            };
    protected static final ThreadLocal<Pool<PointerPane>> POINTER_PANES =
            new ThreadLocal<Pool<PointerPane>>() {
                @Override
                protected Pool<PointerPane> initialValue() {
                    return new Pool<>(PointerPane::new);
                }
            };
    protected final int zSize, cxMin, cxMax, cyMin, cyMax;
    protected final BlockType voidBlock;
    protected final TerrainInfiniteChunkManager chunkManager;
    protected final LightingEngine lighting;

    protected TerrainInfinite(int zSize,
            TerrainInfiniteChunkManager chunkManager, TaskExecutor taskExecutor,
            BlockType voidBlock) {
        this.zSize = zSize;
        this.voidBlock = voidBlock;
        int radius = 0x8000000 - 16;
        cxMin = -radius + 1;
        cxMax = radius;
        cyMin = -radius + 1;
        cyMax = radius;
        this.chunkManager = chunkManager;
        lighting = new LightingEngineThreaded(this, taskExecutor);
    }

    public Optional<? extends TerrainInfiniteChunk> chunkNoLoad(int x, int y) {
        return chunkManager.get(x, y);
    }

    public void chunk(int x, int y, Consumer<TerrainInfiniteChunk> consumer) {
        chunk(x, y).ifPresent(consumer::accept);
    }

    public <R> Optional<R> chunkReturn(int x, int y,
            Function<TerrainInfiniteChunk, R> consumer) {
        Optional<? extends TerrainInfiniteChunk> chunk = chunk(x, y);
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
        Optional<? extends TerrainInfiniteChunk> chunk =
                chunkNoLoad(x >> 4, y >> 4);
        return chunk.isPresent() && chunk.get().isLoaded();
    }

    @Override
    public boolean isBlockTicking(int x, int y, int z) {
        Optional<? extends TerrainInfiniteChunk> chunk =
                chunkNoLoad(x >> 4, y >> 4);
        return chunk.isPresent() && chunk.get().isLoaded();
    }

    @Override
    public Pool<AABBElement> collisions(int minX, int minY, int minZ, int maxX,
            int maxY, int maxZ) {
        Pool<AABBElement> aabbs = AABBS.get();
        minZ = FastMath.max(minZ, 0);
        maxZ = FastMath.max(maxZ, zSize);
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                Optional<? extends TerrainInfiniteChunk> chunk =
                        chunkNoLoad(x >> 4, y >> 4);
                if (chunk.isPresent() && chunk.get().isLoaded()) {
                    TerrainInfiniteChunk chunk2 = chunk.get();
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

    public boolean hasChunk(int x, int y) {
        return chunkManager.has(x, y);
    }

    public abstract Optional<? extends TerrainInfiniteChunk> chunk(int x,
            int y);

    public LightingEngine lighting() {
        return lighting;
    }

    public Collection<? extends TerrainInfiniteChunk> loadedChunks() {
        return chunkManager.iterator();
    }
}
