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
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;

import java.util.Random;

public final class TreePalm implements Tree {
    public static final TreePalm INSTANCE = new TreePalm();

    @Override
    public void gen(TerrainServer.TerrainMutable terrain, int x, int y, int z,
            VanillaMaterial materials, Random random) {
        BlockType groundType = terrain.type(x, y, z - 1);
        if (groundType != materials.grass && groundType != materials.sand) {
            return;
        }
        if (terrain.type(x, y, z) != materials.air) {
            return;
        }
        int size = random.nextInt(4) + 9;
        double dirX, dirY;
        switch (random.nextInt(4)) {
            case 1:
                dirX = -1.0;
                dirY = 1.0;
                break;
            case 2:
                dirX = 1.0;
                dirY = -1.0;
                break;
            case 3:
                dirX = -1.0;
                dirY = -1.0;
                break;
            default:
                dirX = 1.0;
                dirY = 1.0;
                break;
        }
        double xx = x, yy = y;
        int xxx = 0, yyy = 0;
        int i = 0;
        while (i < size) {
            xxx = FastMath.floor(xx + 0.5);
            yyy = FastMath.floor(yy + 0.5);
            TreeUtil.changeBlock(terrain, xxx, yyy, z + i, materials.log,
                    (short) 3);
            TreeUtil.changeBlock(terrain, xxx - 1, yyy, z + i, materials.log,
                    (short) 3);
            TreeUtil.changeBlock(terrain, xxx - 1, yyy - 1, z + i,
                    materials.log, (short) 3);
            TreeUtil.changeBlock(terrain, xxx, yyy - 1, z + i, materials.log,
                    (short) 3);
            i++;
            TreeUtil.changeBlock(terrain, xxx, yyy, z + i, materials.log,
                    (short) 3);
            TreeUtil.changeBlock(terrain, xxx - 1, yyy, z + i, materials.log,
                    (short) 3);
            TreeUtil.changeBlock(terrain, xxx - 1, yyy - 1, z + i,
                    materials.log, (short) 3);
            TreeUtil.changeBlock(terrain, xxx, yyy - 1, z + i, materials.log,
                    (short) 3);
            xx += dirX * i / size;
            yy += dirY * i / size;
        }
        int leavesSize = random.nextInt(3) + 7, leavesHeight =
                random.nextInt(3) + 1;
        if (random.nextBoolean()) {
            TreeUtil.makePalmLeaves(terrain, xxx, yyy, z + size,
                    materials.leaves, (short) 3, materials.log, (short) 3,
                    leavesSize, leavesHeight, 1, 1);
            TreeUtil.makePalmLeaves(terrain, xxx - 1, yyy, z + size,
                    materials.leaves, (short) 3, materials.log, (short) 3,
                    leavesSize, leavesHeight, -1, 1);
            TreeUtil.makePalmLeaves(terrain, xxx - 1, yyy - 1, z + size,
                    materials.leaves, (short) 3, materials.log, (short) 3,
                    leavesSize, leavesHeight, -1, -1);
            TreeUtil.makePalmLeaves(terrain, xxx, yyy - 1, z + size,
                    materials.leaves, (short) 3, materials.log, (short) 3,
                    leavesSize, leavesHeight, 1, -1);
        } else {
            TreeUtil.makePalmLeaves(terrain, xxx, yyy, z + size,
                    materials.leaves, (short) 3, materials.log, (short) 3,
                    leavesSize, leavesHeight, 1, 0);
            TreeUtil.makePalmLeaves(terrain, xxx - 1, yyy, z + size,
                    materials.leaves, (short) 3, materials.log, (short) 3,
                    leavesSize, leavesHeight, 0, -1);
            TreeUtil.makePalmLeaves(terrain, xxx - 1, yyy - 1, z + size,
                    materials.leaves, (short) 3, materials.log, (short) 3,
                    leavesSize, leavesHeight, -1, 0);
            TreeUtil.makePalmLeaves(terrain, xxx, yyy - 1, z + size,
                    materials.leaves, (short) 3, materials.log, (short) 3,
                    leavesSize, leavesHeight, 0, 1);
        }
    }
}
