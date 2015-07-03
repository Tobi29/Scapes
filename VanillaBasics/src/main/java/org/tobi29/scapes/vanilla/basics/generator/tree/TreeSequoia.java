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
import org.tobi29.scapes.engine.utils.Pair;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.engine.utils.math.vector.Vector3i;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class TreeSequoia implements Tree {
    public static final TreeSequoia INSTANCE =
            new TreeSequoia();

    @Override
    public void gen(TerrainServer.TerrainMutable terrain, int x, int y, int z,
            VanillaMaterial materials, Random random) {
        if (terrain.type(x, y, z - 1) != materials.grass) {
            return;
        }
        if (terrain.type(x, y, z) != materials.air) {
            return;
        }
        int size = random.nextInt(22) + 12;
        if (random.nextInt(4) == 0) {
            size += 30;
        }
        TreeUtil.fillGround(terrain, x - 1, y - 1, z - 1, materials.log,
                (short) 5, 2 + random.nextInt(5));
        TreeUtil
                .fillGround(terrain, x - 1, y, z - 1, materials.log, (short) 5,
                        2 + random.nextInt(5));
        TreeUtil.fillGround(terrain, x - 1, y + 1, z - 1, materials.log,
                (short) 5, 2 + random.nextInt(5));
        TreeUtil
                .fillGround(terrain, x, y - 1, z - 1, materials.log, (short) 5,
                        2 + random.nextInt(5));
        TreeUtil.fillGround(terrain, x, y, z - 1, materials.log, (short) 5,
                2 + random.nextInt(5));
        TreeUtil
                .fillGround(terrain, x, y + 1, z - 1, materials.log, (short) 5,
                        2 + random.nextInt(5));
        TreeUtil.fillGround(terrain, x + 1, y - 1, z - 1, materials.log,
                (short) 5, 2 + random.nextInt(5));
        TreeUtil
                .fillGround(terrain, x + 1, y, z - 1, materials.log, (short) 5,
                        2 + random.nextInt(5));
        TreeUtil.fillGround(terrain, x + 1, y + 1, z - 1, materials.log,
                (short) 5, 2 + random.nextInt(5));
        TreeUtil
                .fillGround(terrain, x - 2, y, z - 1, materials.log, (short) 5,
                        2 + random.nextInt(5));
        TreeUtil
                .fillGround(terrain, x + 2, y, z - 1, materials.log, (short) 5,
                        2 + random.nextInt(5));
        TreeUtil
                .fillGround(terrain, x, y - 2, z - 1, materials.log, (short) 5,
                        2 + random.nextInt(5));
        TreeUtil
                .fillGround(terrain, x, y + 2, z - 1, materials.log, (short) 5,
                        2 + random.nextInt(5));
        float leavesSize = 4.0f;
        List<Pair<Vector3, Vector3>> branches = new ArrayList<>();
        for (int zz = size - 1; zz >= 0; zz--) {
            TreeUtil
                    .makeLayer(terrain, x, y, z + zz, materials.log, (short) 5,
                            1);
            if (zz > 10) {
                leavesSize += 0.25f;
            } else {
                leavesSize /= 2.0f;
            }
            if (leavesSize > 1) {
                int branchCount = (int) leavesSize / 3;
                for (int i = -1; i < branchCount; i++) {
                    double dir = random.nextDouble() * FastMath.TWO_PI;
                    double distance =
                            (1.0d - FastMath.sqr(1.0d - random.nextDouble())) *
                                    leavesSize;
                    int xx = FastMath.floor(FastMath.cosTable(dir) * distance);
                    int yy = FastMath.floor(FastMath.sinTable(dir) * distance);
                    branches.add(new Pair<>(new Vector3i(x, y, zz + z),
                            new Vector3i(x + xx, y + yy,
                                    random.nextInt(6) - 2 + zz + z)));
                }
            }
        }
        branches.add(new Pair<>(new Vector3i(x, y, z + size),
                new Vector3i(x, y, z + size + 2)));
        double dir = random.nextDouble() * FastMath.TWO_PI;
        int xx = FastMath.floor(FastMath.cosTable(dir) * 2.0f);
        int yy = FastMath.floor(FastMath.sinTable(dir) * 2.0f);
        branches.add(new Pair<>(new Vector3i(x, y, z + size),
                new Vector3i(x + xx, y + yy, z + size + 1)));
        for (Pair<Vector3, Vector3> branch : branches) {
            TreeUtil.makeBranch(terrain, branch.a, branch.b, materials.log,
                    (short) 5);
            TreeUtil.makeLeaves(terrain, branch.b.intX(), branch.b.intY(),
                    branch.b.intZ(), materials.leaves, (short) 5,
                    random.nextInt(3) + 4);
        }
    }
}
