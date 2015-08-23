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

import org.tobi29.scapes.block.GameRegistry;
import org.tobi29.scapes.block.Inventory;
import org.tobi29.scapes.chunk.WorldServer;
import org.tobi29.scapes.chunk.terrain.TerrainServer;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;
import org.tobi29.scapes.packets.PacketEntityChange;
import org.tobi29.scapes.vanilla.basics.VanillaBasics;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;

public class EntityBloomeryServer extends EntityAbstractFurnaceServer {
    private boolean hasBellows;

    public EntityBloomeryServer(WorldServer world) {
        this(world, Vector3d.ZERO);
    }

    public EntityBloomeryServer(WorldServer world, Vector3 pos) {
        super(world, pos, new Inventory(world.registry(), 14), 4, 9, 800.0f,
                1.004f, 4, 50);
    }

    @Override
    public TagStructure write() {
        TagStructure tag = super.write();
        tag.setBoolean("Bellows", hasBellows);
        return tag;
    }

    @Override
    public void read(TagStructure tagStructure) {
        super.read(tagStructure);
        hasBellows = tagStructure.getBoolean("Bellows");
        maximumTemperature = hasBellows ? Float.POSITIVE_INFINITY : 600.0f;
    }

    @Override
    public void update(double delta) {
        super.update(delta);
        VanillaBasics plugin =
                (VanillaBasics) world.plugins().plugin("VanillaBasics");
        VanillaMaterial materials = plugin.getMaterials();
        temperature /= 1.01;
        temperature = FastMath.max(10, temperature);
        int xx = pos.intX();
        int yy = pos.intY();
        int zz = pos.intZ();
        if (temperature > 10) {
            if (world.getTerrain().data(xx, yy, zz) == 0) {
                world.getTerrain().queue(handle -> {
                    if (handle.type(xx, yy, zz) == materials.bloomery &&
                            handle.data(xx, yy, zz) == 0) {
                        handle.data(xx, yy, zz, (short) 1);
                    }
                });
            }
        } else if (world.getTerrain().data(xx, yy, zz) == 1) {
            world.getTerrain().queue(handle -> {
                if (handle.type(xx, yy, zz) == materials.bloomery &&
                        handle.data(xx, yy, zz) == 1) {
                    handle.data(xx, yy, zz, (short) 0);
                }
            });
        }
    }

    @Override
    protected boolean isValidOn(TerrainServer terrain, int x, int y, int z) {
        VanillaBasics plugin = (VanillaBasics) terrain.world().plugins()
                .plugin("VanillaBasics");
        VanillaMaterial materials = plugin.getMaterials();
        return terrain.type(x, y, z) == materials.bloomery;
    }

    public void updateBellows(TerrainServer terrain) {
        WorldServer world = terrain.world();
        VanillaBasics plugin =
                (VanillaBasics) world.plugins().plugin("VanillaBasics");
        GameRegistry registry = world.registry();
        VanillaMaterial materials = plugin.getMaterials();
        boolean hasBellows = false;
        int xx = pos.intX();
        int yy = pos.intY();
        int zz = pos.intZ();
        if (terrain.type(xx - 1, yy, zz) == materials.bellows &&
                terrain.data(xx - 1, yy, zz) == 5) {
            hasBellows = true;
        }
        if (terrain.type(xx + 1, yy, zz) == materials.bellows &&
                terrain.data(xx + 1, yy, zz) == 3) {
            hasBellows = true;
        }
        if (terrain.type(xx, yy - 1, zz) == materials.bellows &&
                terrain.data(xx, yy - 1, zz) == 2) {
            hasBellows = true;
        }
        if (terrain.type(xx, yy + 1, zz) == materials.bellows &&
                terrain.data(xx, yy + 1, zz) == 4) {
            hasBellows = true;
        }
        this.hasBellows = hasBellows;
        maximumTemperature = hasBellows ? Float.POSITIVE_INFINITY : 600.0f;
        world.send(new PacketEntityChange(this));
    }
}
