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

public abstract class TerrainInfinite implements Terrain {
    protected static final ThreadLocal<Pool<AABBElement>> AABBS =
            ThreadLocal.withInitial(() -> new Pool<>(AABBElement::new));
    protected static final ThreadLocal<Pool<PointerPane>> POINTER_PANES =
            ThreadLocal.withInitial(() -> new Pool<>(PointerPane::new));
    protected final int zSize, cxMin, cxMax, cyMin, cyMax;
    protected final BlockType voidBlock;
    protected volatile boolean dispose;
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

    public TerrainInfiniteChunk getChunkNoLoad(int x, int y) {
        return chunkManager.get(x, y);
    }

    @Override
    public void setSunLight(int x, int y, int z, int light) {
        if (z < 0 || z >= zSize) {
            return;
        }
        TerrainInfiniteChunk chunk =
                getChunk(FastMath.floor(x / 16.0d), FastMath.floor(y / 16.0d));
        if (chunk == null) {
            return;
        }
        chunk.setSunLight(x - (chunk.getX() << 4), y - (chunk.getY() << 4), z,
                light);
    }

    @Override
    public void setBlockLight(int x, int y, int z, int light) {
        if (z < 0 || z >= zSize) {
            return;
        }
        TerrainInfiniteChunk chunk =
                getChunk(FastMath.floor(x / 16.0d), FastMath.floor(y / 16.0d));
        if (chunk == null) {
            return;
        }
        chunk.setBlockLight(x - (chunk.getX() << 4), y - (chunk.getY() << 4), z,
                light);
    }

    @Override
    public BlockType getBlockType(int x, int y, int z) {
        if (z < 0 || z >= zSize) {
            return voidBlock;
        }
        TerrainInfiniteChunk chunk =
                getChunk(FastMath.floor(x / 16.0d), FastMath.floor(y / 16.0d));
        if (chunk == null) {
            return voidBlock;
        }
        return chunk
                .getBlockType(x - (chunk.getX() << 4), y - (chunk.getY() << 4),
                        z);
    }

    @Override
    public int getBlockData(int x, int y, int z) {
        if (z < 0 || z >= zSize) {
            return 0;
        }
        TerrainInfiniteChunk chunk =
                getChunk(FastMath.floor(x / 16.0d), FastMath.floor(y / 16.0d));
        if (chunk == null) {
            return (short) 0;
        }
        return chunk
                .getBlockData(x - (chunk.getX() << 4), y - (chunk.getY() << 4),
                        z);
    }

    @Override
    public int getLight(int x, int y, int z) {
        if (z < 0 || z >= zSize) {
            return 0;
        }
        TerrainInfiniteChunk chunk =
                getChunk(FastMath.floor(x / 16.0d), FastMath.floor(y / 16.0d));
        if (chunk == null) {
            return (byte) 0;
        }
        return chunk
                .getLight(x - (chunk.getX() << 4), y - (chunk.getY() << 4), z);
    }

    @Override
    public int getSunLight(int x, int y, int z) {
        if (z < 0 || z >= zSize) {
            return 0;
        }
        TerrainInfiniteChunk chunk =
                getChunk(FastMath.floor(x / 16.0d), FastMath.floor(y / 16.0d));
        if (chunk == null) {
            return (byte) 0;
        }
        return chunk
                .getSunLight(x - (chunk.getX() << 4), y - (chunk.getY() << 4),
                        z);
    }

    @Override
    public int getBlockLight(int x, int y, int z) {
        if (z < 0 || z >= zSize) {
            return 0;
        }
        TerrainInfiniteChunk chunk =
                getChunk(FastMath.floor(x / 16.0d), FastMath.floor(y / 16.0d));
        if (chunk == null) {
            return (byte) 0;
        }
        return chunk
                .getBlockLight(x - (chunk.getX() << 4), y - (chunk.getY() << 4),
                        z);
    }

    @Override
    public int getHighestBlockZAt(int x, int y) {
        TerrainInfiniteChunk chunk =
                getChunk(FastMath.floor(x / 16.0d), FastMath.floor(y / 16.0d));
        if (chunk == null) {
            return 1;
        }
        return chunk.getHighestBlockZAt(x - (chunk.getX() << 4),
                y - (chunk.getY() << 4));
    }

    @Override
    public int getHighestTerrainBlockZAt(int x, int y) {
        TerrainInfiniteChunk chunk =
                getChunk(FastMath.floor(x / 16.0d), FastMath.floor(y / 16.0d));
        if (chunk == null) {
            return 1;
        }
        return chunk.getHighestTerrainBlockZAt(x - (chunk.getX() << 4),
                y - (chunk.getY() << 4));
    }

    @Override
    public boolean isBlockAvailable(int x, int y, int z) {
        if (z < 0 || z >= zSize) {
            return false;
        }
        TerrainInfiniteChunk chunk = getChunkNoLoad(FastMath.floor(x / 16.0d),
                FastMath.floor(y / 16.0d));
        return chunk != null;
    }

    @Override
    public boolean isBlockLoaded(int x, int y, int z) {
        if (z < 0 || z >= zSize) {
            return false;
        }
        TerrainInfiniteChunk chunk = getChunkNoLoad(FastMath.floor(x / 16.0d),
                FastMath.floor(y / 16.0d));
        return chunk != null && chunk.isLoaded();
    }

    @Override
    public boolean isBlockTicking(int x, int y, int z) {
        TerrainInfiniteChunk chunk = getChunkNoLoad(FastMath.floor(x / 16.0d),
                FastMath.floor(y / 16.0d));
        return chunk != null && chunk.isLoaded();
    }

    @Override
    public Pool<AABBElement> getCollisions(int minX, int minY, int minZ,
            int maxX, int maxY, int maxZ) {
        Pool<AABBElement> aabbs = AABBS.get();
        minZ = FastMath.max(minZ, 0);
        maxZ = FastMath.max(maxZ, zSize);
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                TerrainInfiniteChunk chunk =
                        getChunkNoLoad(FastMath.floor(x / 16.0d),
                                FastMath.floor(y / 16.0d));
                if (chunk != null && chunk.isLoaded()) {
                    for (int z = minZ; z <= maxZ; z++) {
                        if (z >= 0 && z < zSize) {
                            chunk.getBlockType(x - (chunk.getX() << 4),
                                    y - (chunk.getY() << 4), z)
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
                        TerrainInfiniteChunk chunk =
                                getChunk(FastMath.floor(xxx / 16.0d),
                                        FastMath.floor(yyy / 16.0d));
                        if (chunk != null) {
                            chunk.getBlockType(xxx - (chunk.getX() << 4),
                                    yyy - (chunk.getY() << 4), zzz)
                                    .addPointerCollision(chunk.getBlockData(
                                                    xxx - (chunk.getX() << 4),
                                                    yyy - (chunk.getY() << 4),
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

    public TerrainInfiniteChunk getChunk(int x, int y) {
        TerrainInfiniteChunk chunk = chunkManager.get(x, y);
        if (chunk == null) {
            return addChunk(x, y);
        }
        return chunk;
    }

    public abstract TerrainInfiniteChunk addChunk(int x, int y);

    public LightingEngine getLighting() {
        return lighting;
    }

    public Collection<? extends TerrainInfiniteChunk> getLoadedChunks() {
        return chunkManager.getIterator();
    }
}
