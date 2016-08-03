/*
 * Copyright 2012-2016 Tobi29
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

package org.tobi29.scapes.vanilla.basics.material.block.soil;

import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.chunk.terrain.TerrainServer;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;
import org.tobi29.scapes.entity.server.MobFlyingBlockServer;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;
import org.tobi29.scapes.vanilla.basics.material.block.VanillaBlock;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public abstract class BlockSoil extends VanillaBlock {
    protected BlockSoil(VanillaMaterial materials, String nameID) {
        super(materials, nameID);
    }

    @Override
    public String footStepSound(int data) {
        return "VanillaBasics:sound/footsteps/Dirt.ogg";
    }

    @Override
    public String breakSound(ItemStack item, int data) {
        return "VanillaBasics:sound/blocks/Stone.ogg";
    }

    @Override
    public void update(TerrainServer.TerrainMutable terrain, int x, int y,
            int z) {
        int sides = 0, xx = 0, yy = 0;
        boolean flag = false;
        if (terrain.type(x, y, z - 1).isSolid(terrain, x, y, z - 1)) {
            Random random = ThreadLocalRandom.current();
            if (!terrain.type(x - 1, y, z).isSolid(terrain, x - 1, y, z)) {
                sides++;
                if (!terrain.type(x - 1, y, z - 1)
                        .isSolid(terrain, x - 1, y, z - 1)) {
                    xx = -1;
                    flag = true;
                }
            }
            if (!terrain.type(x + 1, y, z).isSolid(terrain, x + 1, y, z)) {
                sides++;
                if (!terrain.type(x + 1, y, z - 1)
                        .isSolid(terrain, x + 1, y, z - 1)) {
                    if (xx == 0 || random.nextBoolean()) {
                        xx = 1;
                    }
                    flag = true;
                }
            }
            if (!terrain.type(x, y - 1, z).isSolid(terrain, x, y - 1, z)) {
                sides++;
                if (!terrain.type(x, y - 1, z - 1)
                        .isSolid(terrain, x, y - 1, z - 1)) {
                    if (xx == 0 || random.nextBoolean()) {
                        xx = 0;
                        yy = -1;
                    }
                    flag = true;
                }
            }
            if (!terrain.type(x, y + 1, z).isSolid(terrain, x, y + 1, z)) {
                sides++;
                if (!terrain.type(x, y + 1, z - 1)
                        .isSolid(terrain, x, y + 1, z - 1)) {
                    if (xx == 0 && yy == 0 || random.nextBoolean()) {
                        xx = 0;
                        yy = 1;
                    }
                    flag = true;
                }
            }
        } else {
            sides = 5;
            flag = true;
        }
        if (sides > 2 && flag) {
            terrain.world().addEntity(new MobFlyingBlockServer(terrain.world(),
                    new Vector3d(x + xx + 0.5, y + yy + 0.5, z + 0.5),
                    new Vector3d(0, 0, -1.0), this, terrain.data(x, y, z)));
            terrain.typeData(x, y, z, terrain.world().air(), (short) 0);
        }
    }

    @Override
    public int maxStackSize(ItemStack item) {
        return 16;
    }
}
