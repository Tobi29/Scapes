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
import java.util.Optional;

public abstract class TerrainInfinite implements Terrain {
    protected static final ThreadLocal<Pool<AABBElement>> AABBS =
            ThreadLocal.withInitial(() -> new Pool<>(AABBElement::new));
    protected static final ThreadLocal<Pool<PointerPane>> POINTER_PANES =
            ThreadLocal.withInitial(() -> new Pool<>(PointerPane::new));
    protected final int zSize, cxMin, cxMax, cyMin, cyMax;
    protected final BlockType voidBlock;
    protected final TerrainInfiniteChunkManager chunkManager;
    protected final LightingEngine lighting;
    protected volatile boolean dispose;

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

    public Optional<? extends TerrainInfiniteChunk> getChunkNoLoad(int x,
            int y) {
        return chunkManager.get(x, y);
    }

    @Override
    public void setSunLight(int x, int y, int z, int light) {
        if (z < 0 || z >= zSize) {
            return;
        }
        Optional<? extends TerrainInfiniteChunk> chunk =
                getChunk(FastMath.floor(x / 16.0d), FastMath.floor(y / 16.0d));
        if (chunk.isPresent()) {
            TerrainInfiniteChunk chunk2 = chunk.get();
            chunk2.setSunLight(x - (chunk2.getX() << 4),
                    y - (chunk2.getY() << 4), z, light);
        }
    }

    @Override
    public void setBlockLight(int x, int y, int z, int light) {
        if (z < 0 || z >= zSize) {
            return;
        }
        Optional<? extends TerrainInfiniteChunk> chunk =
                getChunk(FastMath.floor(x / 16.0d), FastMath.floor(y / 16.0d));
        if (chunk.isPresent()) {
            TerrainInfiniteChunk chunk2 = chunk.get();
            chunk2.setBlockLight(x - (chunk2.getX() << 4),
                    y - (chunk2.getY() << 4), z, light);
        }
    }

    @Override
    public BlockType getBlockType(int x, int y, int z) {
        if (z < 0 || z >= zSize) {
            return voidBlock;
        }
        Optional<? extends TerrainInfiniteChunk> chunk =
                getChunk(FastMath.floor(x / 16.0d), FastMath.floor(y / 16.0d));
        if (chunk.isPresent()) {
            TerrainInfiniteChunk chunk2 = chunk.get();
            return chunk2.getBlockType(x - (chunk2.getX() << 4),
                    y - (chunk2.getY() << 4), z);
        }
        return voidBlock;
    }

    @Override
    public int getBlockData(int x, int y, int z) {
        if (z < 0 || z >= zSize) {
            return 0;
        }
        Optional<? extends TerrainInfiniteChunk> chunk =
                getChunk(FastMath.floor(x / 16.0d), FastMath.floor(y / 16.0d));
        if (chunk.isPresent()) {
            TerrainInfiniteChunk chunk2 = chunk.get();
            return chunk2.getBlockData(x - (chunk2.getX() << 4),
                    y - (chunk2.getY() << 4), z);
        }
        return (short) 0;
    }

    @Override
    public int getLight(int x, int y, int z) {
        if (z < 0 || z >= zSize) {
            return 0;
        }
        Optional<? extends TerrainInfiniteChunk> chunk =
                getChunk(FastMath.floor(x / 16.0d), FastMath.floor(y / 16.0d));
        if (chunk.isPresent()) {
            TerrainInfiniteChunk chunk2 = chunk.get();
            return chunk2.getLight(x - (chunk2.getX() << 4),
                    y - (chunk2.getY() << 4), z);
        }
        return (byte) 0;
    }

    @Override
    public int getSunLight(int x, int y, int z) {
        if (z < 0 || z >= zSize) {
            return 0;
        }
        Optional<? extends TerrainInfiniteChunk> chunk =
                getChunk(FastMath.floor(x / 16.0d), FastMath.floor(y / 16.0d));
        if (chunk.isPresent()) {
            TerrainInfiniteChunk chunk2 = chunk.get();
            return chunk2.getSunLight(x - (chunk2.getX() << 4),
                    y - (chunk2.getY() << 4), z);
        }
        return (byte) 0;
    }

    @Override
    public int getBlockLight(int x, int y, int z) {
        if (z < 0 || z >= zSize) {
            return 0;
        }
        Optional<? extends TerrainInfiniteChunk> chunk =
                getChunk(FastMath.floor(x / 16.0d), FastMath.floor(y / 16.0d));
        if (chunk.isPresent()) {
            TerrainInfiniteChunk chunk2 = chunk.get();
            return chunk2.getBlockLight(x - (chunk2.getX() << 4),
                    y - (chunk2.getY() << 4), z);
        }
        return (byte) 0;
    }

    @Override
    public int getHighestBlockZAt(int x, int y) {
        Optional<? extends TerrainInfiniteChunk> chunk =
                getChunk(FastMath.floor(x / 16.0d), FastMath.floor(y / 16.0d));
        if (chunk.isPresent()) {
            TerrainInfiniteChunk chunk2 = chunk.get();
            return chunk2.getHighestBlockZAt(x - (chunk2.getX() << 4),
                    y - (chunk2.getY() << 4));
        }
        return 1;
    }

    @Override
    public int getHighestTerrainBlockZAt(int x, int y) {
        Optional<? extends TerrainInfiniteChunk> chunk =
                getChunk(FastMath.floor(x / 16.0d), FastMath.floor(y / 16.0d));
        if (chunk.isPresent()) {
            TerrainInfiniteChunk chunk2 = chunk.get();
            return chunk2.getHighestTerrainBlockZAt(x - (chunk2.getX() << 4),
                    y - (chunk2.getY() << 4));
        }
        return 1;
    }

    @Override
    public boolean isBlockAvailable(int x, int y, int z) {
        return z >= 0 && z < zSize &&
                hasChunk(FastMath.floor(x / 16.0d), FastMath.floor(y / 16.0d));
    }

    @Override
    public boolean isBlockLoaded(int x, int y, int z) {
        if (z < 0 || z >= zSize) {
            return false;
        }
        Optional<? extends TerrainInfiniteChunk> chunk =
                getChunkNoLoad(FastMath.floor(x / 16.0d),
                        FastMath.floor(y / 16.0d));
        return chunk.isPresent() && chunk.get().isLoaded();
    }

    @Override
    public boolean isBlockTicking(int x, int y, int z) {
        Optional<? extends TerrainInfiniteChunk> chunk =
                getChunkNoLoad(FastMath.floor(x / 16.0d),
                        FastMath.floor(y / 16.0d));
        return chunk.isPresent() && chunk.get().isLoaded();
    }

    @Override
    public Pool<AABBElement> getCollisions(int minX, int minY, int minZ,
            int maxX, int maxY, int maxZ) {
        Pool<AABBElement> aabbs = AABBS.get();
        minZ = FastMath.max(minZ, 0);
        maxZ = FastMath.max(maxZ, zSize);
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                Optional<? extends TerrainInfiniteChunk> chunk =
                        getChunkNoLoad(FastMath.floor(x / 16.0d),
                                FastMath.floor(y / 16.0d));
                if (chunk.isPresent() && chunk.get().isLoaded()) {
                    TerrainInfiniteChunk chunk2 = chunk.get();
                    for (int z = minZ; z <= maxZ; z++) {
                        if (z >= 0 && z < zSize) {
                            chunk2.getBlockType(x - (chunk2.getX() << 4),
                                    y - (chunk2.getY() << 4), z)
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
    public Pool<PointerPane> getPointerPanes(int x, int y, int z, int range) {
        Pool<PointerPane> pointerPanes = POINTER_PANES.get();
        for (int xx = -range; xx <= range; xx++) {
            int xxx = x + xx;
            for (int yy = -range; yy <= range; yy++) {
                int yyy = y + yy;
                for (int zz = -range; zz <= range; zz++) {
                    int zzz = z + zz;
                    if (zzz >= 0 && zzz < zSize) {
                        Optional<? extends TerrainInfiniteChunk> chunk =
                                getChunk(FastMath.floor(xxx / 16.0d),
                                        FastMath.floor(yyy / 16.0d));
                        if (chunk.isPresent()) {
                            TerrainInfiniteChunk chunk2 = chunk.get();
                            chunk2.getBlockType(xxx - (chunk2.getX() << 4),
                                    yyy - (chunk2.getY() << 4), zzz)
                                    .addPointerCollision(chunk2.getBlockData(
                                                    xxx - (chunk2.getX() << 4),
                                                    yyy - (chunk2.getY() << 4),
                                                    zzz), pointerPanes, xxx,
                                            yyy, zzz);
                        }
                    }
                }
            }
        }
        return pointerPanes;
    }

    @Override
    public void dispose() {
        dispose = true;
        lighting.dispose();
    }

    public boolean hasChunk(int x, int y) {
        return chunkManager.has(x, y);
    }

    public abstract Optional<? extends TerrainInfiniteChunk> getChunk(int x,
            int y);

    public abstract Optional<? extends TerrainInfiniteChunk> addChunk(int x,
            int y);

    public LightingEngine getLighting() {
        return lighting;
    }

    public Collection<? extends TerrainInfiniteChunk> getLoadedChunks() {
        return chunkManager.getIterator();
    }
}
