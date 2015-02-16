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
import org.tobi29.scapes.vanilla.basics.material.MetalType;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;
import org.tobi29.scapes.vanilla.basics.material.item.ItemIngot;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class EntityAlloyServer extends EntityAbstractContainerServer {
    protected final Map<String, Double> metals = new ConcurrentHashMap<>();
    protected String result = "";
    protected float temperature;

    public EntityAlloyServer(WorldServer world) {
        this(world, Vector3d.ZERO);
    }

    public EntityAlloyServer(WorldServer world, Vector3 pos) {
        super(world, pos, new Inventory(world.getRegistry(), 2));
    }

    @Override
    public TagStructure write() {
        TagStructure tag = super.write();
        TagStructure metalTag = new TagStructure();
        for (Map.Entry<String, Double> entry : metals.entrySet()) {
            metalTag.setDouble(entry.getKey(), entry.getValue());
        }
        tag.setStructure("Metals", metalTag);
        tag.setString("Result", result);
        tag.setFloat("Temperature", temperature);
        return tag;
    }

    @Override
    public void read(TagStructure tagStructure) {
        super.read(tagStructure);
        metals.clear();
        for (Map.Entry<String, Object> entry : tagStructure
                .getStructure("Metals").getTagEntrySet()) {
            metals.put(entry.getKey(), (Double) entry.getValue());
        }
        result = tagStructure.getString("Result");
        temperature = tagStructure.getFloat("Temperature");
    }

    @Override
    public boolean isValidOn(TerrainServer terrain, int x, int y, int z) {
        VanillaBasics plugin = (VanillaBasics) terrain.getWorld().getPlugins()
                .getPlugin("VanillaBasics");
        VanillaMaterial materials = plugin.getMaterials();
        return terrain.getBlockType(x, y, z) == materials.alloy;
    }

    @Override
    public void update(double delta) {
        VanillaBasics plugin =
                (VanillaBasics) world.getPlugins().getPlugin("VanillaBasics");
        VanillaMaterial materials = plugin.getMaterials();
        temperature /= 1.002f;
        ItemStack input = inventory.getItem(0);
        Material type = input.getMaterial();
        if (type instanceof ItemIngot) {
            if (((ItemIngot) type).getTemperature(input) >=
                    ((ItemIngot) type).getMeltingPoint(input)) {
                MetalType inputAlloy = plugin.getMetalType(
                        input.getMetaData("Vanilla").getString("MetalType"));
                if (inputAlloy != null) {
                    for (Map.Entry<String, Double> entry : inputAlloy
                            .getIngredients().entrySet()) {
                        if (metals.containsKey(entry.getKey())) {
                            metals.put(entry.getKey(),
                                    metals.get(entry.getKey()) +
                                            entry.getValue());
                        } else {
                            metals.put(entry.getKey(), entry.getValue());
                        }
                    }
                    temperature = FastMath.max(temperature,
                            input.getMetaData("Vanilla")
                                    .getFloat("Temperature"));
                    inventory.setItem(0,
                            new ItemStack(materials.mold, (short) 1));
                    updateResult(plugin);
                    world.getConnection()
                            .send(new PacketEntityChange(this));
                }
            }
        }
        if (!result.isEmpty()) {
            ItemStack output = inventory.getItem(1);
            if (output.getMaterial() == materials.mold &&
                    output.getData() == 1) {
                output.setMaterial(materials.ingot);
                output.setData((short) 0);
                output.getMetaData("Vanilla").setString("MetalType", result);
                output.getMetaData("Vanilla")
                        .setFloat("Temperature", temperature);
                MetalType alloy = plugin.getMetalType(result);
                for (Map.Entry<String, Double> entry : metals.entrySet()) {
                    double amount = entry.getValue() -
                            alloy.getIngredients().get(entry.getKey());
                    if (amount > 0.001f) {
                        metals.put(entry.getKey(), amount);
                    } else {
                        metals.remove(entry.getKey());
                    }
                }
                updateResult(plugin);
                world.getConnection()
                        .send(new PacketEntityChange(this));
            }
        }
    }

    private void updateResult(VanillaBasics plugin) {
        double size = 0.0;
        for (double amount : metals.values()) {
            size += amount;
        }
        double finalSize = size;
        Optional<MetalType> result = plugin.getMetalTypes().filter(metal -> {
            Map<String, Double> ingredients = metal.getIngredients();
            if (ingredients.size() != metals.size()) {
                return false;
            }
            boolean flag = true;
            for (Map.Entry<String, Double> check : ingredients.entrySet()) {
                Double amount = metals.get(check.getKey());
                if (amount == null) {
                    flag = false;
                } else if (amount / finalSize - check.getValue() > 0.001) {
                    flag = false;
                }
            }
            return flag;
        }).findFirst();
        if (result.isPresent()) {
            this.result = result.get().getID();
        } else {
            this.result = null;
        }
    }
}
