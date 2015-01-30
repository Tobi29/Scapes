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
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.vector.Vector2i;

public abstract class TerrainInfiniteChunk {
    protected final ChunkData bID;
    protected final ChunkData bData;
    protected final ChunkData bLight;
    protected final Vector2i pos, posBlock;
    protected final TerrainInfinite terrain;
    protected final World world;
    protected final int zSize;
    protected final BlockType[] blocks;
    protected final int[] heightMap;
    protected boolean disposed;
    protected State state = State.NEW;
    protected TagStructure metaData = new TagStructure();

    protected TerrainInfiniteChunk(Vector2i pos, TerrainInfinite terrain,
            World world, int zSize) {
        this.pos = pos;
        posBlock = new Vector2i(pos.intX() << 4, pos.intY() << 4);
        this.terrain = terrain;
        this.world = world;
        this.zSize = zSize;
        blocks = world.getRegistry().getBlocks();
        bData = new ChunkData(1, 1, 6, 3, 3, 3, ChunkArraySection1x16::new);
        bID = new ChunkData(1, 1, 6, 3, 3, 3, ChunkArraySection1x16::new);
        bLight = new ChunkData(0, 0, 5, 4, 4, 4, ChunkArraySection2x4::new);
        heightMap = new int[256];
    }

    public abstract void update(int x, int y, int z, boolean updateTile);

    public void updateSunLight() {
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                boolean flag = true;
                byte sunLight = 15;
                for (int z = heightMap[y << 4 | x]; z >= 0 && flag; z--) {
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
                    if (bLight.getData(x, y, z, 0) == sunLight) {
                        flag = false;
                    } else {
                        bLight.setData(x, y, z, 0, sunLight);
                    }
                }
            }
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

    public TagStructure getMetaData(String category) {
        return metaData.getStructure(category);
    }

    public Vector2i getPos() {
        return pos;
    }

    public int getX() {
        return pos.intX();
    }

    public int getY() {
        return pos.intY();
    }

    public int getZSize() {
        return zSize;
    }

    public boolean isLoaded() {
        return state.id >= State.LOADED.id;
    }

    public boolean isEmpty(int i) {
        i <<= 4;
        return bID.isEmpty(0, 0, i, 15, 15, i + 15);
    }

    public TerrainInfinite getTerrain() {
        return terrain;
    }

    public int getHighestBlockZAt(int x, int y) {
        if (x >= 0 && x < 16 && y >= 0 && y < 16) {
            return heightMap[y << 4 | x] + 1;
        }
        throw new ChunkMissException("Tried to access block " + x + ' ' + y +
                " in chunk " + pos.intX() +
                ' ' + pos.intY());
    }

    public int getHighestTerrainBlockZAt(int x, int y) {
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
        }
        throw new ChunkMissException("Tried to access block " + x + ' ' + y +
                " in chunk " + pos.intX() +
                ' ' + pos.intY());
    }

    public BlockType getBlockType(int x, int y, int z) {
        if (x >= 0 && x < 16 && y >= 0 && y < 16 && z >= 0 &&
                z < zSize) {
            return blocks[bID.getData(x, y, z, 0)];
        }
        throw new ChunkMissException(
                "Tried to access block " + x + ' ' + y + ' ' + z +
                        " in chunk " + pos.intX() +
                        ' ' + pos.intY());
    }

    public int getBlockData(int x, int y, int z) {
        if (x >= 0 && x < 16 && y >= 0 && y < 16 && z >= 0 &&
                z < zSize) {
            return bData.getData(x, y, z, 0);
        }
        throw new ChunkMissException(
                "Tried to access block " + x + ' ' + y + ' ' + z +
                        " in chunk " + pos.intX() +
                        ' ' + pos.intY());
    }

    public int getLight(int x, int y, int z) {
        if (x >= 0 && x < 16 && y >= 0 && y < 16 && z >= 0 &&
                z < zSize) {
            return (int) FastMath.max(bLight.getData(x, y, z, 1),
                    bLight.getData(x, y, z, 0) - world.getEnvironment()
                            .getSunLightReduction(x + posBlock.intX(),
                                    y + posBlock.intY()));
        }
        throw new ChunkMissException(
                "Tried to access block " + x + ' ' + y + ' ' + z +
                        " in chunk " + pos.intX() +
                        ' ' + pos.intY());
    }

    public int getSunLight(int x, int y, int z) {
        if (x >= 0 && x < 16 && y >= 0 && y < 16 && z >= 0 &&
                z < zSize) {
            return bLight.getData(x, y, z, 0);
        }
        throw new ChunkMissException(
                "Tried to access block " + x + ' ' + y + ' ' + z +
                        " in chunk " + pos.intX() +
                        ' ' + pos.intY());
    }

    public int getBlockLight(int x, int y, int z) {
        if (x >= 0 && x < 16 && y >= 0 && y < 16 && z >= 0 &&
                z < zSize) {
            return bLight.getData(x, y, z, 1);
        }
        throw new ChunkMissException(
                "Tried to access block " + x + ' ' + y + ' ' + z +
                        " in chunk " + pos.intX() +
                        ' ' + pos.intY());
    }

    public void setBlockType(int x, int y, int z, BlockType type) {
        if (x >= 0 && x < 16 && y >= 0 && y < 16 && z >= 0 &&
                z < zSize) {
            BlockType oldType = blocks[bID.getData(x, y, z, 0)];
            if (oldType != type) {
                bID.setData(x, y, z, 0, type.getID());
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

    public void setBlockIdAndData(int x, int y, int z, BlockType type,
            int data) {
        if (x >= 0 && x < 16 && y >= 0 && y < 16 && z >= 0 &&
                z < zSize) {
            BlockType oldType = blocks[bID.getData(x, y, z, 0)];
            if (oldType != type || bData.getData(x, y, z, 0) != data) {
                bID.setData(x, y, z, 0, type.getID());
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

    public void setBlockData(int x, int y, int z, int data) {
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

    public void setSunLight(int x, int y, int z, int light) {
        if (x >= 0 && x < 16 && y >= 0 && y < 16 && z >= 0 &&
                z < zSize) {
            if (bLight.getData(x, y, z, 0) != light) {
                bLight.setData(x, y, z, 0, light);
            }
        } else {
            throw new ChunkMissException(
                    "Tried to access block " + x + ' ' + y + ' ' + z +
                            " in chunk " + pos.intX() +
                            ' ' + pos.intY());
        }
    }

    public void setBlockLight(int x, int y, int z, int light) {
        if (x >= 0 && x < 16 && y >= 0 && y < 16 && z >= 0 &&
                z < zSize) {
            if (bLight.getData(x, y, z, 1) != light) {
                bLight.setData(x, y, z, 1, light);
            }
        } else {
            throw new ChunkMissException(
                    "Tried to access block " + x + ' ' + y + ' ' + z +
                            " in chunk " + pos.intX() +
                            ' ' + pos.intY());
        }
    }

    public void setMetaData(String category, TagStructure metaData) {
        this.metaData.setStructure(category, metaData);
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
        if (z > height && type != world.getAir()) {
            heightMap[y << 4 | x] = z;
        } else if (height == z && type == world.getAir()) {
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
}
