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
import org.tobi29.scapes.vanilla.basics.VanillaBasics;
import org.tobi29.scapes.vanilla.basics.generator.ClimateGenerator;
import org.tobi29.scapes.vanilla.basics.generator.WorldEnvironmentOverworld;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;

public class UpdateGrassGrowth extends Update {
    public UpdateGrassGrowth(GameRegistry registry) {
    }

    public UpdateGrassGrowth() {
    }

    @Override
    public void run(TerrainServer.TerrainMutable terrain) {
        VanillaBasics plugin = (VanillaBasics) terrain.getWorld().getPlugins()
                .getPlugin("VanillaBasics");
        VanillaMaterial materials = plugin.getMaterials();
        BlockType type = terrain.getBlockType(x, y, z);
        if (type == materials.grass) {
            WorldEnvironmentOverworld environment =
                    (WorldEnvironmentOverworld) terrain.getWorld()
                            .getEnvironment();
            ClimateGenerator climateGenerator =
                    environment.getClimateGenerator();
            double humidity = climateGenerator.getHumidity(x, y, z);
            if (humidity < 0.2) {
                terrain.setBlockType(x, y, z, materials.dirt);
            }
        } else if (type == materials.dirt) {
            WorldEnvironmentOverworld environment =
                    (WorldEnvironmentOverworld) terrain.getWorld()
                            .getEnvironment();
            ClimateGenerator climateGenerator =
                    environment.getClimateGenerator();
            double humidity = climateGenerator.getHumidity(x, y, z);
            if (humidity > 0.2 && (terrain.getBlockLight(x, y, z + 1) > 8 ||
                    terrain.getSunLight(x, y, z + 1) > 8) &&
                    terrain.getBlockType(x, y, z + 1)
                            .isTransparent(terrain, x, y, z + 1)) {
                terrain.setBlockTypeAndData(x, y, z, materials.grass,
                        (short) 0);
            }
        }
    }

    @Override
    public boolean isValidOn(BlockType type, TerrainServer terrain) {
        VanillaBasics plugin = (VanillaBasics) terrain.getWorld().getPlugins()
                .getPlugin("VanillaBasics");
        VanillaMaterial materials = plugin.getMaterials();
        return type == materials.grass || type == materials.dirt;
    }
}
