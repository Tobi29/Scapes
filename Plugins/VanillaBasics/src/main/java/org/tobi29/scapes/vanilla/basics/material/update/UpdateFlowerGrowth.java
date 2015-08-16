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

package org.tobi29.scapes.vanilla.basics.material.update;

import org.tobi29.scapes.block.BlockType;
import org.tobi29.scapes.block.Update;
import org.tobi29.scapes.chunk.terrain.TerrainServer;
import org.tobi29.scapes.vanilla.basics.VanillaBasics;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class UpdateFlowerGrowth extends Update {
    @Override
    public void run(TerrainServer.TerrainMutable terrain) {
        VanillaBasics plugin = (VanillaBasics) terrain.world().plugins()
                .plugin("VanillaBasics");
        VanillaMaterial materials = plugin.getMaterials();
        BlockType type = terrain.type(x, y, z);
        if (type == materials.air) {
            if (terrain.sunLight(x, y, z) >= 12 &&
                    terrain.type(x, y, z - 1) == materials.grass) {
                int flowers = 0;
                List<Integer> datas = new ArrayList<>();
                for (int xx = -4; xx < 5; xx++) {
                    for (int yy = -4; yy < 5; yy++) {
                        for (int zz = -4; zz < 5; zz++) {
                            if (terrain.type(x + xx, y + yy, z + zz) ==
                                    materials.flower) {
                                if (flowers++ > 1) {
                                    return;
                                }
                                datas.add(terrain.data(x + xx, y + yy, z + zz));
                            }
                        }
                    }
                }
                if (datas.isEmpty()) {
                    terrain.typeData(x, y, z, materials.flower,
                            (short) new Random().nextInt(20));
                } else {
                    terrain.typeData(x, y, z, materials.flower,
                            datas.get(new Random().nextInt(datas.size())));
                }
            }
        } else if (type == materials.snow) {
            if (terrain.sunLight(x, y, z) >= 12 &&
                    terrain.type(x, y, z - 1) == materials.grass) {
                Random random = ThreadLocalRandom.current();
                terrain.addDelayedUpdate(new UpdateFlowerGrowth()
                        .set(x, y, z, random.nextDouble() * 3600.0 + 3600.0));
            }
        }
    }

    @Override
    public boolean isValidOn(BlockType type, TerrainServer terrain) {
        return true;
    }
}
