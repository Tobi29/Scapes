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
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;
import org.tobi29.scapes.packets.PacketEntityChange;
import org.tobi29.scapes.vanilla.basics.VanillaBasics;
import org.tobi29.scapes.vanilla.basics.material.AlloyType;
import org.tobi29.scapes.vanilla.basics.material.MetalType;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;
import org.tobi29.scapes.vanilla.basics.material.item.ItemIngot;
import org.tobi29.scapes.vanilla.basics.util.MetalUtil;

import java.util.Map;

public class EntityAlloyServer extends EntityAbstractContainerServer {
    protected MetalUtil.Alloy metals = new MetalUtil.Alloy();
    protected double temperature;

    public EntityAlloyServer(WorldServer world) {
        this(world, Vector3d.ZERO);
    }

    public EntityAlloyServer(WorldServer world, Vector3 pos) {
        super(world, pos, new Inventory(world.registry(), 2));
    }

    @Override
    public TagStructure write() {
        TagStructure tag = super.write();
        tag.setStructure("Alloy", MetalUtil.write(metals));
        tag.setDouble("Temperature", temperature);
        return tag;
    }

    @Override
    public void read(TagStructure tagStructure) {
        super.read(tagStructure);
        VanillaBasics plugin =
                (VanillaBasics) world.plugins().plugin("VanillaBasics");
        metals = MetalUtil.read(plugin, tagStructure.getStructure("Alloy"));
        temperature = tagStructure.getDouble("Temperature");
    }

    @Override
    public boolean isValidOn(TerrainServer terrain, int x, int y, int z) {
        VanillaBasics plugin = (VanillaBasics) terrain.world().plugins()
                .plugin("VanillaBasics");
        VanillaMaterial materials = plugin.getMaterials();
        return terrain.type(x, y, z) == materials.alloy;
    }

    @Override
    public void update(double delta) {
        VanillaBasics plugin =
                (VanillaBasics) world.plugins().plugin("VanillaBasics");
        VanillaMaterial materials = plugin.getMaterials();
        temperature /= 1.002f;
        ItemStack input = inventory.item(0);
        Material inputType = input.material();
        if (inputType instanceof ItemIngot) {
            ItemIngot ingot = (ItemIngot) inputType;
            MetalUtil.Alloy alloy = ingot.alloy(input);
            double meltingPoint = alloy.meltingPoint();
            if (ingot.temperature(input) >= meltingPoint) {
                AlloyType alloyType = alloy.type(plugin);
                for (Map.Entry<MetalType, Double> entry : alloyType
                        .ingredients().entrySet()) {
                    metals.add(entry.getKey(), entry.getValue());
                }
                temperature = FastMath.max(temperature,
                        input.metaData("Vanilla").getDouble("Temperature"));
                input.clear();
                input.setMaterial(materials.mold, 1);
                world.send(new PacketEntityChange(this));
            }
        }
        ItemStack output = inventory.item(1);
        Material outputType = output.material();
        if (outputType == materials.mold && output.data() == 1) {
            temperature = FastMath.max(temperature,
                    input.metaData("Vanilla").getFloat("Temperature"));
            output.setMaterial(materials.ingot, 0);
            output.metaData("Vanilla").setDouble("Temperature", temperature);
            materials.ingot.setAlloy(output, metals.drain(1.0));
            world.send(new PacketEntityChange(this));
        }
    }
}
