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
import org.tobi29.scapes.block.GameRegistry;
import org.tobi29.scapes.block.Update;
import org.tobi29.scapes.chunk.terrain.TerrainServer;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.vanilla.basics.VanillaBasics;
import org.tobi29.scapes.vanilla.basics.material.StoneType;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;

public class UpdateWaterFlow extends Update {
    private static boolean flow(TerrainServer.TerrainMutable terrain, int x,
            int y, int z, VanillaMaterial materials,
            GameRegistry.Registry<StoneType> stoneRegistry) {
        BlockType type = terrain.type(x, y, z);
        if (type.isReplaceable(terrain, x, y, z)) {
            int dataHas = terrain.data(x, y, z);
            if (dataHas > 0 || type != materials.water) {
                short dataNeed = 6;
                if (terrain.type(x, y, z + 1) == materials.water) {
                    dataNeed = 1;
                } else {
                    dataNeed =
                            getData(terrain, x - 1, y, z, dataNeed, materials);
                    dataNeed =
                            getData(terrain, x + 1, y, z, dataNeed, materials);
                    dataNeed =
                            getData(terrain, x, y - 1, z, dataNeed, materials);
                    dataNeed =
                            getData(terrain, x, y + 1, z, dataNeed, materials);
                }
                dataNeed++;
                if (dataNeed <= 6) {
                    if (dataNeed != dataHas || type != materials.water) {
                        if (terrain.type(x, y, z) == materials.lava) {
                            terrain.typeData(x, y, z, materials.cobblestone,
                                    stoneRegistry.get(StoneType.BASALT));
                        } else {
                            terrain.typeData(x, y, z, materials.water,
                                    dataNeed);
                        }
                    }
                } else if (type == materials.water) {
                    terrain.typeData(x, y, z, materials.air, (short) 0);
                }
            }
            return false;
        }
        return true;
    }

    private static short getData(TerrainServer terrain, int x, int y, int z,
            short oldData, VanillaMaterial materials) {
        if (terrain.type(x, y, z) == materials.water) {
            short data = (short) (FastMath.max(0, terrain.data(x, y, z) - 1) +
                    1);
            if (data < oldData) {
                return data;
            }
        }
        return oldData;
    }

    @Override
    public void run(TerrainServer.TerrainMutable terrain) {
        VanillaBasics plugin = (VanillaBasics) terrain.world().getPlugins()
                .getPlugin("VanillaBasics");
        GameRegistry.Registry<StoneType> stoneRegistry =
                terrain.world().getRegistry()
                        .get("VanillaBasics", "StoneType");
        VanillaMaterial materials = plugin.getMaterials();
        flow(terrain, x, y, z, materials, stoneRegistry);
        if (flow(terrain, x, y, z - 1, materials, stoneRegistry)) {
            flow(terrain, x - 1, y, z, materials, stoneRegistry);
            flow(terrain, x + 1, y, z, materials, stoneRegistry);
            flow(terrain, x, y - 1, z, materials, stoneRegistry);
            flow(terrain, x, y + 1, z, materials, stoneRegistry);
        }
    }

    @Override
    public boolean isValidOn(BlockType type, TerrainServer terrain) {
        VanillaBasics plugin = (VanillaBasics) terrain.world().getPlugins()
                .getPlugin("VanillaBasics");
        VanillaMaterial materials = plugin.getMaterials();
        return type == materials.water;
    }
}
