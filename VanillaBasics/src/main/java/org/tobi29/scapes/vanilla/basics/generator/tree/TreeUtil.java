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

package org.tobi29.scapes.vanilla.basics.generator.tree;

import org.tobi29.scapes.block.BlockType;
import org.tobi29.scapes.chunk.terrain.TerrainServer;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;

import java.util.Random;

public final class TreeUtil {
    public static void makeBranch(TerrainServer.TerrainMutable terrain,
            Vector3 start, Vector3 end, BlockType type, short data) {
        double distance = FastMath.pointDistance(start, end);
        if (distance > 0.0) {
            Vector3 delta = end.minus(start);
            double step = 1.0 / distance;
            for (double i = 0; i <= 1; i += step) {
                Vector3 block = start.plus(delta.multiply(i));
                changeBlock(terrain, block.intX(), block.intY(), block.intZ(),
                        type, data);
            }
        }
    }

    public static void changeBlock(TerrainServer.TerrainMutable terrain, int x,
            int y, int z, BlockType type, short data) {
        if (terrain.type(x, y, z).isReplaceable(terrain, x, y, z) ||
                terrain.type(x, y, z).isTransparent(terrain, x, y, z)) {
            terrain.typeData(x, y, z, type, data);
        }
    }

    public static void makeLeaves(TerrainServer.TerrainMutable terrain, int x,
            int y, int z, BlockType type, short data, int size) {
        for (int xx = -size; xx <= size; xx++) {
            for (int yy = -size; yy <= size; yy++) {
                for (int zz = -size; zz <= size; zz++) {
                    if (xx * xx + yy * yy + zz * zz <= size) {
                        changeBlock(terrain, x + xx, y + yy, z + zz, type,
                                data);
                    }
                }
            }
        }
    }

    public static void makeWillowLeaves(TerrainServer.TerrainMutable terrain,
            int x, int y, int z, BlockType type, short data, int size,
            int vineLength, int vineLengthRandom, int vineChance,
            Random random) {
        for (int xx = -size; xx <= size; xx++) {
            for (int yy = -size; yy <= size; yy++) {
                for (int zz = -size; zz <= size; zz++) {
                    if (xx * xx + yy * yy + zz * zz <= size) {
                        changeBlock(terrain, x + xx, y + yy, z + zz, type,
                                data);
                    }
                }
                if (random.nextInt(vineChance) == 0) {
                    if (xx * xx + yy * yy <= size) {
                        int length =
                                vineLength + random.nextInt(vineLengthRandom);
                        for (int zz = 0; zz <= length; zz++) {
                            changeBlock(terrain, x + xx, y + yy, z - zz, type,
                                    data);
                        }
                    }
                }
            }
        }
    }

    public static void makeLayer(TerrainServer.TerrainMutable terrain, int x,
            int y, int z, BlockType type, short data, int size) {
        int sizeSqr = size * size;
        for (int yy = -size; yy <= size; yy++) {
            int yyy = y + yy;
            for (int xx = -size; xx <= size; xx++) {
                int xxx = x + xx;
                if (xx * xx + yy * yy <= sizeSqr) {
                    changeBlock(terrain, xxx, yyy, z, type, data);
                }
            }
        }
    }

    public static void makeRandomLayer(TerrainServer.TerrainMutable terrain,
            int x, int y, int z, BlockType type, short data, int size,
            int randomSize, Random random) {
        int sizeSqr = size * size;
        randomSize *= size;
        randomSize++;
        for (int yy = -size; yy <= size; yy++) {
            int yyy = y + yy;
            for (int xx = -size; xx <= size; xx++) {
                int xxx = x + xx;
                if (xx * xx + yy * yy <= sizeSqr - random.nextInt(randomSize)) {
                    changeBlock(terrain, xxx, yyy, z, type, data);
                }
            }
        }
    }

    public static void makePalmLeaves(TerrainServer.TerrainMutable terrain,
            int x, int y, int z, BlockType type, short data, BlockType logType,
            short logData, int length, int height, int dirX, int dirY) {
        int xx = x, yy = y;
        for (double i = 0; i < length; i++) {
            int h = FastMath.round(
                    FastMath.sinTable(i / length * FastMath.PI) * height);
            changeBlock(terrain, xx, yy, z + h, logType, logData);
            xx += dirX;
            yy += dirY;
        }
        xx = x;
        yy = y;
        for (double i = 0; i < length; i++) {
            int h = FastMath.round(
                    FastMath.sinTable(i / length * FastMath.PI) * height);
            changeBlock(terrain, xx, yy, z + h + 1, type, data);
            changeBlock(terrain, xx - 1, yy, z + h, type, data);
            changeBlock(terrain, xx + 1, yy, z + h, type, data);
            changeBlock(terrain, xx, yy - 1, z + h, type, data);
            changeBlock(terrain, xx, yy + 1, z + h, type, data);
            xx += dirX;
            yy += dirY;
        }
    }

    public static void fillGround(TerrainServer.TerrainMutable terrain, int x,
            int y, int z, BlockType type, short data, int maxDepth) {
        for (int i = 0; i < maxDepth; i++) {
            if (terrain.type(x, y, z - i).isReplaceable(terrain, x, y, z - i)) {
                terrain.typeData(x, y, z - i, type, data);
            } else {
                return;
            }
        }
    }
}
