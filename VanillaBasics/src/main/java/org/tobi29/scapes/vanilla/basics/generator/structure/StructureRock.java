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

import java.util.Random;

public final class StructureRock {
    public static void genOreRock(TerrainServer.TerrainMutable terrain, int x,
            int y, int z, BlockType stone, BlockType ore, short data,
            int oreChance, double size, Random random) {
        int ceilSize = (int) FastMath.ceil(size);
        for (int xx = -ceilSize; xx <= ceilSize; xx++) {
            for (int yy = -ceilSize; yy <= ceilSize; yy++) {
                for (int zz = -ceilSize; zz <= ceilSize; zz++) {
                    if (xx * xx + yy * yy + zz * zz <=
                            size * size - random.nextDouble() * 3) {
                        BlockType type;
                        if (random.nextInt(oreChance) == 0) {
                            type = ore;
                        } else {
                            type = stone;
                        }
                        terrain.setBlockTypeAndData(x + xx, y + yy, z + zz,
                                type, data);
                    }
                }
            }
        }
    }
}
