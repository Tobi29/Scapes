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

public class UpdateStrawDry extends Update {
    @Override
    public void run(TerrainServer.TerrainMutable terrain) {
        if (terrain.getBlockData(x, y, z) == 0) {
            terrain.setBlockData(x, y, z, (short) 1);
        }
    }

    @Override
    public boolean isValidOn(BlockType type, TerrainServer terrain) {
        VanillaBasics plugin = (VanillaBasics) terrain.getWorld().getPlugins()
                .getPlugin("VanillaBasics");
        VanillaMaterial materials = plugin.getMaterials();
        return type == materials.straw;
    }
}
