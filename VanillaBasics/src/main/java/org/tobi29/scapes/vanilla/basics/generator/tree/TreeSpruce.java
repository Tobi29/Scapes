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

import org.tobi29.scapes.chunk.terrain.TerrainServer;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;

import java.util.Random;

public final class TreeSpruce implements Tree {
    public static final TreeSpruce INSTANCE =
            new TreeSpruce();

    @Override
    public void gen(TerrainServer.TerrainMutable terrain, int x, int y, int z,
            VanillaMaterial materials, Random random) {
        if (terrain.type(x, y, z - 1) != materials.grass) {
            return;
        }
        if (terrain.type(x, y, z) != materials.air) {
            return;
        }
        int size = random.nextInt(4) + 12;
        float leavesSize = 2.0f;
        TreeUtil.makeRandomLayer(terrain, x, y, z + size, materials.leaves,
                (short) 2, 1, 1, random);
        for (int zz = size - 1; zz >= 0; zz--) {
            TreeUtil.changeBlock(terrain, x, y, z + zz, materials.log,
                    (short) 2);
            if (zz > 5) {
                leavesSize += 0.25f;
            } else {
                leavesSize--;
            }
            if (leavesSize > 0) {
                if (zz % 2 == 0) {
                    TreeUtil.makeRandomLayer(terrain, x, y, z + zz,
                            materials.leaves, (short) 2, (int) leavesSize, 1,
                            random);
                } else {
                    TreeUtil.makeRandomLayer(terrain, x, y, z + zz,
                            materials.leaves, (short) 2,
                            (int) (leavesSize / 2.0f), 1, random);
                }
            }
        }
    }
}
