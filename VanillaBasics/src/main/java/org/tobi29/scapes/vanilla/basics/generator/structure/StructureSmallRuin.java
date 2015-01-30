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

package org.tobi29.scapes.vanilla.basics.generator.structure;

import org.tobi29.scapes.block.BlockType;
import org.tobi29.scapes.chunk.terrain.TerrainServer;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;

import java.util.Random;

public final class StructureSmallRuin {
    public static void placeRandomRuin(TerrainServer.TerrainMutable terrain,
            int x, int y, int z, VanillaMaterial materials, short stoneType,
            Random random) {
        if (terrain.getBlockType(x, y, z) != materials.air &&
                random.nextInt(10) != 0) {
            return;
        }
        int type = random.nextInt(2);
        switch (type) {
            case 0:
                placeRuinType1(terrain, x, y,
                        new BlockType[]{materials.cobblestone,
                                materials.cobblestoneCracked,
                                materials.cobblestoneMossy},
                        materials.stoneTotem, stoneType, random);
                break;
            case 1:
                placeRuinType2(terrain, x, y, z,
                        new BlockType[]{materials.cobblestone,
                                materials.cobblestoneCracked,
                                materials.cobblestoneMossy}, materials.wood,
                        stoneType, random);
                break;
        }
    }

    public static void placeRuinType1(TerrainServer.TerrainMutable terrain,
            int x, int y, BlockType[] pillar, BlockType top, short stoneType,
            Random random) {
        double size = random.nextDouble() * 10 + 6;
        int pillars = random.nextInt(12) + 6, xx, yy;
        double d;
        for (int i = 0; i < pillars; i++) {
            d = (double) i / pillars * FastMath.TWO_PI;
            xx = x + FastMath.floor(FastMath.cosTable(d) * size);
            yy = y + FastMath.floor(FastMath.sinTable(d) * size);
            placePillar(terrain, xx, yy,
                    terrain.getHighestTerrainBlockZAt(xx, yy),
                    new BlockType[]{pillar[random.nextInt(pillar.length)],
                            pillar[random.nextInt(pillar.length)],
                            pillar[random.nextInt(pillar.length)], top},
                    stoneType, stoneType, stoneType, stoneType);
        }
    }

    public static void placePillar(TerrainServer.TerrainMutable terrain, int x,
            int y, int z, BlockType[] type, short... data) {
        for (int i = 0; i < type.length; i++) {
            terrain.setBlockTypeAndData(x, y, z + i, type[i], data[i]);
        }
    }

    public static void placeRuinType2(TerrainServer.TerrainMutable terrain,
            int x, int y, int z, BlockType[] walls, BlockType floor,
            short stoneType, Random random) {
        int sizeX = random.nextInt(6) + 4;
        int sizeY = random.nextInt(6) + 4;
        int minHeight = random.nextInt(12);
        int maxHeight = random.nextInt(4) + 3;
        int xxx, yyy;
        short woodType = (short) random.nextInt(2);
        for (int xx = -sizeX; xx <= sizeX; xx++) {
            xxx = x + xx;
            for (int yy = -sizeY; yy <= sizeY; yy++) {
                yyy = y + yy;
                if (FastMath.abs(xx) == sizeX || FastMath.abs(yy) == sizeY) {
                    placePillar(terrain, xxx, yyy, z,
                            walls[random.nextInt(walls.length)], stoneType,
                            random.nextInt(maxHeight) + minHeight);
                    fillGround(terrain, xxx, yyy, z - 1,
                            walls[random.nextInt(walls.length)], stoneType,
                            random.nextInt(9) + 1);
                } else {
                    terrain.setBlockTypeAndData(xxx, yyy, z - 1, floor,
                            woodType);
                    fillGround(terrain, xxx, yyy, z - 2,
                            walls[random.nextInt(walls.length)], stoneType,
                            random.nextInt(9));
                }
            }
        }
    }

    public static void placePillar(TerrainServer.TerrainMutable terrain, int x,
            int y, int z, BlockType type, short data, int height) {
        for (int i = 0; i < height; i++) {
            terrain.setBlockTypeAndData(x, y, z + i, type, data);
        }
    }

    public static void fillGround(TerrainServer.TerrainMutable terrain, int x,
            int y, int z, BlockType type, short data, int maxDepth) {
        for (int i = 0; i < maxDepth; i++) {
            if (terrain.getBlockType(x, y, z - i)
                    .isReplaceable(terrain, x, y, z - i)) {
                terrain.setBlockTypeAndData(x, y, z - i, type, data);
            } else {
                return;
            }
        }
    }
}
