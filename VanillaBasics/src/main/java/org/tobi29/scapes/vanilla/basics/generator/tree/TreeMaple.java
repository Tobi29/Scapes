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
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.engine.utils.math.vector.Vector3i;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class TreeMaple implements Tree {
    public static final TreeMaple INSTANCE = new TreeMaple();

    @Override
    public void gen(TerrainServer.TerrainMutable terrain, int x, int y, int z,
            VanillaMaterial materials, Random random) {
        if (terrain.getBlockType(x, y, z - 1) != materials.grass) {
            return;
        }
        if (terrain.getBlockType(x, y, z) != materials.air) {
            return;
        }
        int size = random.nextInt(4) + 3;
        TreeUtil
                .fillGround(terrain, x - 1, y, z - 1, materials.log, (short) 4,
                        2 + random.nextInt(5));
        TreeUtil
                .fillGround(terrain, x, y - 1, z - 1, materials.log, (short) 4,
                        2 + random.nextInt(5));
        TreeUtil.fillGround(terrain, x, y, z - 1, materials.log, (short) 4,
                2 + random.nextInt(5));
        TreeUtil
                .fillGround(terrain, x, y + 1, z - 1, materials.log, (short) 4,
                        2 + random.nextInt(5));
        TreeUtil
                .fillGround(terrain, x + 1, y, z - 1, materials.log, (short) 4,
                        2 + random.nextInt(5));
        for (int zz = 0; zz < size + 2; zz++) {
            TreeUtil.changeBlock(terrain, x, y, z + zz, materials.log,
                    (short) 4);
        }
        List<Vector3> branches = new ArrayList<>();
        for (int i = 0; i < random.nextInt(2) + 5; i++) {
            branches.add(new Vector3i(random.nextInt(5) - 2 + x,
                    random.nextInt(5) - 2 + y,
                    random.nextInt(2) + 1 + z + size));
        }
        for (int i = 0; i < random.nextInt(3) + 3; i++) {
            branches.add(new Vector3i(random.nextInt(3) - 1 + x,
                    random.nextInt(3) - 1 + y,
                    random.nextInt(4) + 3 + z + size));
        }
        Vector3 begin = new Vector3i(x, y, z + size);
        for (Vector3 branch : branches) {
            TreeUtil.makeBranch(terrain, begin, branch, materials.log,
                    (short) 4);
            TreeUtil.makeLeaves(terrain, branch.intX(), branch.intY(),
                    branch.intZ(), materials.leaves, (short) 4, 6);
        }
    }
}
