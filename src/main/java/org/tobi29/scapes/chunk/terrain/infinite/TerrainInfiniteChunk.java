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

import org.tobi29.scapes.block.BlockType;
import org.tobi29.scapes.chunk.World;
import org.tobi29.scapes.chunk.data.ChunkArraySection1x16;
import org.tobi29.scapes.chunk.data.ChunkArraySection2x4;
import org.tobi29.scapes.chunk.data.ChunkData;
import org.tobi29.scapes.engine.utils.Pair;
import org.tobi29.scapes.engine.utils.Pool;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.vector.Vector2i;

public abstract class TerrainInfiniteChunk {
    private static final ThreadLocal<Pair<Pool<LightSpread>, Pool<LightSpread>>>
            SPREAD_POOLS = ThreadLocal.withInitial(
            () -> new Pair<>(new Pool<>(LightSpread::new),
                    new Pool<>(LightSpread::new)));
    protected final ChunkData bID;
    protected final ChunkData bData;
    protected final ChunkData bLight;
    protected final Vector2i pos, posBlock;
    protected final TerrainInfinite terrain;
    protected final World world;
    protected final int zSize;
    protected final BlockType[] blocks;
    protected final int[] heightMap;
    protected State state = State.NEW;
    protected TagStructure metaData = new TagStructure();

    protected TerrainInfiniteChunk(Vector2i pos, TerrainInfinite terrain,
            World world, int zSize) {
        this.pos = pos;
        posBlock = new Vector2i(pos.intX() << 4, pos.intY() << 4);
        this.terrain = terrain;
        this.world = world;
        this.zSize = zSize;
        blocks = world.registry().blocks();
        bData = new ChunkData(0, 0, 5, 4, 4, 4, ChunkArraySection1x16::new);
        bID = new ChunkData(0, 0, 5, 4, 4, 4, ChunkArraySection1x16::new);
        bLight = new ChunkData(0, 0, 5, 4, 4, 4, ChunkArraySection2x4::new);
        heightMap = new int[256];
    }

    public abstract void update(int x, int y, int z, boolean updateTile);

    public abstract void updateLight(int x, int y, int z);

    public void updateSunLight() {
        Pair<Pool<LightSpread>, Pool<LightSpread>> spreadPools =
                SPREAD_POOLS.get();
        Pool<LightSpread> spreads = spreadPools.a;
        Pool<LightSpread> newSpreads = spreadPools.b;
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                byte sunLight = 15;
                int spread;
                if (x > 0 && x < 15 && y > 0 && y < 15) {
                    spread = heightMap[y << 4 | x - 1];
                    spread = FastMath.max(heightMap[y << 4 | x + 1], spread);
                    spread = FastMath.max(heightMap[y - 1 << 4 | x], spread);
                    spread = FastMath.max(heightMap[y + 1 << 4 | x], spread);
                    spread--;
                } else {
                    spread = -1;
                }
                int light = heightMap[y << 4 | x];
                for (int z = FastMath.max(light, spread); z >= 0; z--) {
                    if (z < light) {
                        int id = bID.getData(x, y, z, 0);
                        if (id != 0) {
                            BlockType type = blocks[id];
                            if (type.isSolid(terrain, x + posBlock.intX(),
                                    y + posBlock.intX(), z) ||
                                    !type.isTransparent(terrain,
                                            x + posBlock.intX(),
                                            y + posBlock.intX(), z)) {
                                sunLight = FastMath.clamp((byte) (sunLight +
                                                type.lightTrough(terrain,
                                                        x + posBlock.intX(),
                                                        y + posBlock.intX(),
                                                        z)), (byte) 0,
                                        (byte) 15);
                            }
                        }
                        bLight.setData(x, y, z, 0, sunLight);
                    }
                    if (z < spread && sunLight > 0) {
                        spreads.push().set(x - 1, y, z, sunLight);
                        spreads.push().set(x + 1, y, z, sunLight);
                        spreads.push().set(x, y - 1, z, sunLight);
                        spreads.push().set(x, y + 1, z, sunLight);
                    }
                }
            }
        }
        while (!spreads.isEmpty()) {
            for (LightSpread s : spreads) {
                if (s.x >= 0 && s.x < 16 && s.y >= 0 && s.y < 16 && s.z >= 0 &&
                        s.z < zSize) {
                    s.l += blocks[bID.getData(s.x, s.y, s.z, 0)]
                            .lightTrough(terrain, s.x + posBlock.intX(),
                                    s.y + posBlock.intY(), s.z);
                    s.l = FastMath.clamp(s.l, (byte) 0, (byte) 15);
                    if (s.l > bLight.getData(s.x, s.y, s.z, 0)) {
                        bLight.setData(s.x, s.y, s.z, 0, s.l);
                        newSpreads.push().set(s.x - 1, s.y, s.z, s.l);
                        newSpreads.push().set(s.x + 1, s.y, s.z, s.l);
                        newSpreads.push().set(s.x, s.y - 1, s.z, s.l);
                        newSpreads.push().set(s.x, s.y + 1, s.z, s.l);
                        newSpreads.push().set(s.x, s.y, s.z - 1, s.l);
                        newSpreads.push().set(s.x, s.y, s.z + 1, s.l);
                    }
                }
            }
            Pool<LightSpread> swapUpdates = spreads;
            swapUpdates.reset();
            spreads = newSpreads;
            newSpreads = swapUpdates;
        }
    }

    public void initSunLight() {
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                byte sunLight = 15;
                for (int z = zSize - 1; z >= 0 && sunLight > 0; z--) {
                    int id = bID.getData(x, y, z, 0);
                    if (id != 0) {
                        BlockType type = blocks[id];
                        if (type.isSolid(terrain, x + posBlock.intX(),
                                y + posBlock.intX(), z) ||
                                !type.isTransparent(terrain,
                                        x + posBlock.intX(),
                                        y + posBlock.intX(), z)) {
                            sunLight = FastMath.clamp((byte) (sunLight +
                                            type.lightTrough(terrain,
                                                    x + posBlock.intX(),
                                                    y + posBlock.intX(), z)),
                                    (byte) 0, (byte) 15);
                        }
                    }
                    bLight.setData(x, y, z, 0, sunLight);
                }
            }
        }
    }

    public TagStructure metaData(String category) {
        return metaData.getStructure(category);
    }

    public Vector2i pos() {
        return pos;
    }

    public int x() {
        return pos.intX();
    }

    public int y() {
        return pos.intY();
    }

    public int blockX() {
        return posBlock.intX();
    }

    public int blockY() {
        return posBlock.intY();
    }

    public int zSize() {
        return zSize;
    }

    public boolean isLoaded() {
        return state.id >= State.LOADED.id;
    }

    public boolean isEmpty(int i) {
        i <<= 4;
        return bID.isEmpty(0, 0, i, 15, 15, i + 15);
    }

    public TerrainInfinite terrain() {
        return terrain;
    }

    public int highestBlockZAt(int x, int y) {
        if (x >= 0 && x < 16 && y >= 0 && y < 16) {
            return heightMap[y << 4 | x] + 1;
        }
        throw new ChunkMissException("Tried to access block " + x + ' ' + y +
                " in chunk " + pos.intX() +
                ' ' + pos.intY());
    }

    public int highestTerrainBlockZAt(int x, int y) {
        if (x >= 0 && x < 16 && y >= 0 && y < 16) {
            for (int z = heightMap[y << 4 | x]; z >= 0; z--) {
                int id = bID.getData(x, y, z, 0);
                if (id != 0) {
                    BlockType type = blocks[id];
                    if (type.isSolid(terrain, x + posBlock.intX(),
                            y + posBlock.intX(), z) &&
                            !type.isTransparent(terrain, x + posBlock.intX(),
                                    y + posBlock.intX(), z)) {
                        return z + 1;
                    }
                }
            }
            return 0;
        }
        throw new ChunkMissException("Tried to access block " + x + ' ' + y +
                " in chunk " + pos.intX() +
                ' ' + pos.intY());
    }

    public BlockType typeG(int x, int y, int z) {
        x -= posBlock.intX();
        y -= posBlock.intY();
        return typeL(x, y, z);
    }

    public BlockType typeL(int x, int y, int z) {
        if (x >= 0 && x < 16 && y >= 0 && y < 16 && z >= 0 &&
                z < zSize) {
            return blocks[bID.getData(x, y, z, 0)];
        }
        throw new ChunkMissException(
                "Tried to access block " + x + ' ' + y + ' ' + z +
                        " in chunk " + pos.intX() +
                        ' ' + pos.intY());
    }

    public int dataG(int x, int y, int z) {
        x -= posBlock.intX();
        y -= posBlock.intY();
        return dataL(x, y, z);
    }

    public int dataL(int x, int y, int z) {
        if (x >= 0 && x < 16 && y >= 0 && y < 16 && z >= 0 &&
                z < zSize) {
            return bData.getData(x, y, z, 0);
        }
        throw new ChunkMissException(
                "Tried to access block " + x + ' ' + y + ' ' + z +
                        " in chunk " + pos.intX() +
                        ' ' + pos.intY());
    }

    public int lightG(int x, int y, int z) {
        x -= posBlock.intX();
        y -= posBlock.intY();
        return lightL(x, y, z);
    }

    public int lightL(int x, int y, int z) {
        if (x >= 0 && x < 16 && y >= 0 && y < 16 && z >= 0 &&
                z < zSize) {
            return FastMath.max(bLight.getData(x, y, z, 1),
                    bLight.getData(x, y, z, 0) -
                            terrain.sunLightReduction(x + posBlock.intX(),
                                    y + posBlock.intY()));
        }
        throw new ChunkMissException(
                "Tried to access block " + x + ' ' + y + ' ' + z +
                        " in chunk " + pos.intX() +
                        ' ' + pos.intY());
    }

    public int sunLightG(int x, int y, int z) {
        x -= posBlock.intX();
        y -= posBlock.intY();
        return sunLightL(x, y, z);
    }

    public int sunLightL(int x, int y, int z) {
        if (x >= 0 && x < 16 && y >= 0 && y < 16 && z >= 0 &&
                z < zSize) {
            return bLight.getData(x, y, z, 0);
        }
        throw new ChunkMissException(
                "Tried to access block " + x + ' ' + y + ' ' + z +
                        " in chunk " + pos.intX() +
                        ' ' + pos.intY());
    }

    public int blockLightG(int x, int y, int z) {
        x -= posBlock.intX();
        y -= posBlock.intY();
        return blockLightL(x, y, z);
    }

    public int blockLightL(int x, int y, int z) {
        if (x >= 0 && x < 16 && y >= 0 && y < 16 && z >= 0 &&
                z < zSize) {
            return bLight.getData(x, y, z, 1);
        }
        throw new ChunkMissException(
                "Tried to access block " + x + ' ' + y + ' ' + z +
                        " in chunk " + pos.intX() +
                        ' ' + pos.intY());
    }

    public void blockTypeG(int x, int y, int z, BlockType type) {
        x -= posBlock.intX();
        y -= posBlock.intY();
        blockTypeL(x, y, z, type);
    }

    public void blockTypeL(int x, int y, int z, BlockType type) {
        if (x >= 0 && x < 16 && y >= 0 && y < 16 && z >= 0 &&
                z < zSize) {
            BlockType oldType = blocks[bID.getData(x, y, z, 0)];
            if (oldType != type) {
                bID.setData(x, y, z, 0, type.id());
                updateHeightMap(x, y, z, type);
                update(x, y, z, oldType.causesTileUpdate());
            }
        } else {
            throw new ChunkMissException(
                    "Tried to access block " + x + ' ' + y + ' ' + z +
                            " in chunk " + pos.intX() +
                            ' ' + pos.intY());
        }
    }

    public void typeDataG(int x, int y, int z, BlockType type, int data) {
        x -= posBlock.intX();
        y -= posBlock.intY();
        typeDataL(x, y, z, type, data);
    }

    public void typeDataL(int x, int y, int z, BlockType type, int data) {
        if (x >= 0 && x < 16 && y >= 0 && y < 16 && z >= 0 &&
                z < zSize) {
            BlockType oldType = blocks[bID.getData(x, y, z, 0)];
            if (oldType != type || bData.getData(x, y, z, 0) != data) {
                bID.setData(x, y, z, 0, type.id());
                bData.setData(x, y, z, 0, data);
                updateHeightMap(x, y, z, type);
                update(x, y, z, oldType.causesTileUpdate());
            }
        } else {
            throw new ChunkMissException(
                    "Tried to access block " + x + ' ' + y + ' ' + z +
                            " in chunk " + pos.intX() +
                            ' ' + pos.intY());
        }
    }

    public void dataG(int x, int y, int z, int data) {
        x -= posBlock.intX();
        y -= posBlock.intY();
        dataL(x, y, z, data);
    }

    public void dataL(int x, int y, int z, int data) {
        if (x >= 0 && x < 16 && y >= 0 && y < 16 && z >= 0 &&
                z < zSize) {
            if (bData.getData(x, y, z, 0) != data) {
                bData.setData(x, y, z, 0, data);
                BlockType oldType = blocks[bID.getData(x, y, z, 0)];
                update(x, y, z, oldType.causesTileUpdate());
            }
        } else {
            throw new ChunkMissException(
                    "Tried to access block " + x + ' ' + y + ' ' + z +
                            " in chunk " + pos.intX() +
                            ' ' + pos.intY());
        }
    }

    public void sunLightG(int x, int y, int z, int light) {
        x -= posBlock.intX();
        y -= posBlock.intY();
        sunLightL(x, y, z, light);
    }

    public void sunLightL(int x, int y, int z, int light) {
        if (x >= 0 && x < 16 && y >= 0 && y < 16 && z >= 0 &&
                z < zSize) {
            bLight.setData(x, y, z, 0, light);
            updateLight(x, y, z);
        } else {
            throw new ChunkMissException(
                    "Tried to access block " + x + ' ' + y + ' ' + z +
                            " in chunk " + pos.intX() +
                            ' ' + pos.intY());
        }
    }

    public void blockLightG(int x, int y, int z, int light) {
        x -= posBlock.intX();
        y -= posBlock.intY();
        blockLightL(x, y, z, light);
    }

    public void blockLightL(int x, int y, int z, int light) {
        if (x >= 0 && x < 16 && y >= 0 && y < 16 && z >= 0 &&
                z < zSize) {
            bLight.setData(x, y, z, 1, light);
            updateLight(x, y, z);
        } else {
            throw new ChunkMissException(
                    "Tried to access block " + x + ' ' + y + ' ' + z +
                            " in chunk " + pos.intX() +
                            ' ' + pos.intY());
        }
    }

    protected void initHeightMap() {
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = zSize - 1; z > 0; z--) {
                    if (bID.getData(x, y, z, 0) != 0) {
                        heightMap[y << 4 | x] = z;
                        break;
                    }
                }
            }
        }
    }

    protected void updateHeightMap(int x, int y, int z, BlockType type) {
        int height = heightMap[y << 4 | x];
        if (z > height && type != world.air()) {
            heightMap[y << 4 | x] = z;
        } else if (height == z && type == world.air()) {
            int zzz = 0;
            for (int zz = height; zz >= 0; zz--) {
                if (bID.getData(x, y, zz, 0) != 0) {
                    zzz = zz;
                    break;
                }
            }
            heightMap[y << 4 | x] = zzz;
        }
    }

    protected enum State {
        NEW(0),
        SHOULD_POPULATE(1),
        POPULATING(2),
        POPULATED(3),
        BORDER(4),
        LOADED(5),
        SENDABLE(6);
        final byte id;

        State(int id)

        {
            this.id = (byte) id;
        }
    }

    private static class LightSpread {
        private int x, y, z, l;

        private void set(int x, int y, int z, int l) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.l = l;
        }
    }
}
