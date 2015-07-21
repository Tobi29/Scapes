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
import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.block.Material;
import org.tobi29.scapes.chunk.WorldServer;
import org.tobi29.scapes.chunk.terrain.TerrainServer;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;
import org.tobi29.scapes.packets.PacketEntityChange;
import org.tobi29.scapes.vanilla.basics.VanillaBasics;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;
import org.tobi29.scapes.vanilla.basics.material.item.ItemIngot;

public class EntityForgeServer extends EntityAbstractFurnaceServer {
    public EntityForgeServer(WorldServer world) {
        this(world, Vector3d.ZERO);
    }

    public EntityForgeServer(WorldServer world, Vector3 pos) {
        super(world, pos, new Inventory(world.registry(), 9), 4, 3,
                Float.POSITIVE_INFINITY, 1.006f, 10, 50);
    }

    @Override
    public void update(double delta) {
        super.update(delta);
        VanillaBasics plugin =
                (VanillaBasics) world.plugins().plugin("VanillaBasics");
        VanillaMaterial materials = plugin.getMaterials();
        synchronized (this) {
            int max = items + fuel.length + 1;
            for (int i = fuel.length + 1; i < max; i++) {
                ItemStack item = inventory.item(i);
                if (item.amount() == 1) {
                    Material type = item.material();
                    if (type instanceof ItemIngot) {
                        if (((ItemIngot) type).temperature(item) >=
                                ((ItemIngot) type).meltingPoint(item) &&
                                item.data() == 1) {
                            if (inventory.item(8)
                                    .take(new ItemStack(materials.mold,
                                            (short) 1)) != null) {
                                item.setData((short) 0);
                                world.connection()
                                        .send(new PacketEntityChange(this));
                            }
                        }
                    }
                }
            }
        }
        int xx = pos.intX();
        int yy = pos.intY();
        int zz = pos.intZ();
        if (temperature > 10.0) {
            if (world.getTerrain().data(xx, yy, zz) == 0) {
                world.getTerrain().queue(handle -> {
                    if (handle.type(xx, yy, zz) == materials.forge &&
                            handle.data(xx, yy, zz) == 0) {
                        handle.data(xx, yy, zz, (short) 0);
                    }
                });
            }
        } else if (world.getTerrain().data(xx, yy, zz) == 1) {
            world.getTerrain().queue(handle -> {
                if (handle.type(xx, yy, zz) == materials.forge &&
                        handle.data(xx, yy, zz) == 1) {
                    handle.data(xx, yy, zz, (short) 1);
                }
            });
        }
    }

    @Override
    protected boolean isValidOn(TerrainServer terrain, int x, int y, int z) {
        VanillaBasics plugin = (VanillaBasics) terrain.world().plugins()
                .plugin("VanillaBasics");
        VanillaMaterial materials = plugin.getMaterials();
        return terrain.type(x, y, z) == materials.forge;
    }
}
