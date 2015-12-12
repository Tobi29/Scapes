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

public final class StructureOre {
    public static int genOre(TerrainServer.TerrainMutable terrain, int x, int y,
            int z, BlockType stone, BlockType ore, int sizeX, int sizeY,
            int sizeZ, int chance, Random random) {
        int ores = 0;
        for (int xx = -sizeX; xx <= sizeX; xx++) {
            int xxx = x + xx;
            for (int yy = -sizeY; yy <= sizeY; yy++) {
                int yyy = y + yy;
                for (int zz = -sizeZ; zz <= sizeZ; zz++) {
                    int zzz = z + zz;
                    if (FastMath.sqr((double) xx / sizeX) +
                            FastMath.sqr((double) yy / sizeY) +
                            FastMath.sqr((double) zz / sizeZ) <
                            random.nextDouble() * 0.1 + 0.9) {
                        if (random.nextInt(chance) == 0) {
                            if (terrain.type(xxx, yyy, zzz) == stone) {
                                terrain.type(xxx, yyy, zzz, ore);
                                ores++;
                            }
                        }
                    }
                }
            }
        }
        return ores;
    }
}
