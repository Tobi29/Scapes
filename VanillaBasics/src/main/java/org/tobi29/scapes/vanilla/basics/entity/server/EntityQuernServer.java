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

package org.tobi29.scapes.vanilla.basics.entity.server;

import org.tobi29.scapes.block.Inventory;
import org.tobi29.scapes.chunk.WorldServer;
import org.tobi29.scapes.chunk.terrain.TerrainServer;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;
import org.tobi29.scapes.vanilla.basics.VanillaBasics;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;

public class EntityQuernServer extends EntityAbstractContainerServer {
    public EntityQuernServer(WorldServer world) {
        this(world, Vector3d.ZERO);
    }

    public EntityQuernServer(WorldServer world, Vector3 pos) {
        super(world, pos, new Inventory(world.registry(), 2));
    }

    /*@Override
    public Gui getGui(MobPlayerClientMain player) {
        return new GuiQuernInventory(this, player);
    }*/

    @Override
    public boolean isValidOn(TerrainServer terrain, int x, int y, int z) {
        VanillaBasics plugin = (VanillaBasics) terrain.world().plugins()
                .plugin("VanillaBasics");
        VanillaMaterial materials = plugin.getMaterials();
        return terrain.type(x, y, z) == materials.quern;
    }
}
