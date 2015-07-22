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
import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.chunk.terrain.TerrainClient;
import org.tobi29.scapes.chunk.terrain.TerrainRenderer;
import org.tobi29.scapes.engine.opengl.GL;
import org.tobi29.scapes.engine.utils.Pool;
import org.tobi29.scapes.engine.utils.math.PointerPane;
import org.tobi29.scapes.engine.utils.math.vector.Vector2;
import org.tobi29.scapes.packets.PacketBlockChange;

import java.util.Optional;

public class TerrainInfiniteSection implements TerrainClient {
    private final TerrainInfiniteClient terrain;
    private final int zSize;
    private final BlockType air;
    @SuppressWarnings("unchecked")
    private final Optional<? extends TerrainInfiniteChunk>[] chunks =
            new Optional[9];
    private int x, y;

    public TerrainInfiniteSection(TerrainInfiniteClient terrain, int zSize,
            BlockType air) {
        this.terrain = terrain;
        this.zSize = zSize;
        this.air = air;
    }

    public void init(Vector2 pos) {
        x = pos.intX() - 1;
        y = pos.intY() - 1;
        chunks[0] = terrain.chunk(x, y);
        chunks[1] = terrain.chunk(x + 1, y);
        chunks[2] = terrain.chunk(x + 2, y);
        chunks[3] = terrain.chunk(x, y + 1);
        chunks[4] = terrain.chunk(x + 1, y + 1);
        chunks[5] = terrain.chunk(x + 2, y + 1);
        chunks[6] = terrain.chunk(x, y + 2);
        chunks[7] = terrain.chunk(x + 1, y + 2);
        chunks[8] = terrain.chunk(x + 2, y + 2);
    }

    @Override
    public void sunLight(int x, int y, int z, int light) {
        if (z < 0 || z >= zSize) {
            return;
        }
        Optional<? extends TerrainInfiniteChunk> chunk = get(x, y);
        if (chunk.isPresent()) {
            chunk.get().
                    sunLightG(x, y, z, light);
        } else {
            terrain.sunLight(x, y, z, light);
        }
    }

    @Override
    public void blockLight(int x, int y, int z, int light) {
        if (z < 0 || z >= zSize) {
            return;
        }
        Optional<? extends TerrainInfiniteChunk> chunk = get(x, y);
        if (chunk.isPresent()) {
            chunk.get().
                    blockLightG(x, y, z, light);
        } else {
            terrain.blockLight(x, y, z, light);
        }
    }

    @Override
    public BlockType type(int x, int y, int z) {
        if (z < 0 || z >= zSize) {
            return air;
        }
        Optional<? extends TerrainInfiniteChunk> chunk = get(x, y);
        if (chunk.isPresent()) {
            return chunk.get().typeG(x, y, z);
        }
        return terrain.type(x, y, z);
    }

    @Override
    public int data(int x, int y, int z) {
        if (z < 0 || z >= zSize) {
            return 0;
        }
        Optional<? extends TerrainInfiniteChunk> chunk = get(x, y);
        if (chunk.isPresent()) {
            return chunk.get().dataG(x, y, z);
        }
        return terrain.data(x, y, z);
    }

    @Override
    public int light(int x, int y, int z) {
        if (z < 0 || z >= zSize) {
            return 0;
        }
        Optional<? extends TerrainInfiniteChunk> chunk = get(x, y);
        if (chunk.isPresent()) {
            return chunk.get().lightG(x, y, z);
        }
        return terrain.light(x, y, z);
    }

    @Override
    public int sunLight(int x, int y, int z) {
        if (z < 0 || z >= zSize) {
            return 0;
        }
        Optional<? extends TerrainInfiniteChunk> chunk = get(x, y);
        if (chunk.isPresent()) {
            return chunk.get().sunLightG(x, y, z);
        }
        return terrain.sunLight(x, y, z);
    }

    @Override
    public int blockLight(int x, int y, int z) {
        if (z < 0 || z >= zSize) {
            return 0;
        }
        Optional<? extends TerrainInfiniteChunk> chunk = get(x, y);
        if (chunk.isPresent()) {
            return chunk.get().blockLightG(x, y, z);
        }
        return terrain.blockLight(x, y, z);
    }

    @Override
    public int highestBlockZAt(int x, int y) {
        Optional<? extends TerrainInfiniteChunk> chunk = get(x, y);
        if (chunk.isPresent()) {
            TerrainInfiniteChunk chunk2 = chunk.get();
            return chunk2
                    .highestBlockZAt(x - chunk2.blockX(), y - chunk2.blockY());
        }
        return terrain.highestBlockZAt(x, y);
    }

    @Override
    public int highestTerrainBlockZAt(int x, int y) {
        Optional<? extends TerrainInfiniteChunk> chunk = get(x, y);
        if (chunk.isPresent()) {
            TerrainInfiniteChunk chunk2 = chunk.get();
            return chunk2.highestTerrainBlockZAt(x - chunk2.blockX(),
                    y - chunk2.blockY());
        }
        return terrain.highestTerrainBlockZAt(x, y);
    }

    @Override
    public boolean isBlockLoaded(int x, int y, int z) {
        return terrain.isBlockLoaded(x, y, z);
    }

    @Override
    public boolean isBlockTicking(int x, int y, int z) {
        return terrain.isBlockTicking(x, y, z);
    }

    @Override
    public Pool<AABBElement> collisions(int minX, int minY, int minZ, int maxX,
            int maxY, int maxZ) {
        return terrain.collisions(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Override
    public Pool<PointerPane> pointerPanes(int x, int y, int z, int range) {
        return terrain.pointerPanes(x, y, z, range);
    }

    private Optional<? extends TerrainInfiniteChunk> get(int x, int y) {
        x = (x >> 4) - this.x;
        y = (y >> 4) - this.y;
        if (x < 0 || x >= 3 || y < 0 || y >= 3) {
            return Optional.empty();
        }
        return chunks[y * 3 + x];
    }

    @Override
    public WorldClient world() {
        return terrain.world();
    }

    @Override
    public TerrainRenderer renderer() {
        return terrain.renderer();
    }

    @Override
    public void update(double delta) {
        terrain.update(delta);
    }

    @Override
    public void toggleStaticRenderDistance() {
        terrain.toggleStaticRenderDistance();
    }

    @Override
    public void reloadGeometry() {
        terrain.reloadGeometry();
    }

    @Override
    public void process(PacketBlockChange packet) {
        terrain.process(packet);
    }

    @Override
    public void dispose(GL gl) {
        throw new UnsupportedOperationException("Terrain not disposable");
    }
}
