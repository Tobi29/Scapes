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
import org.tobi29.scapes.block.Material;
import org.tobi29.scapes.chunk.WorldServer;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.packets.PacketEntityChange;
import org.tobi29.scapes.vanilla.basics.VanillaBasics;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;
import org.tobi29.scapes.vanilla.basics.material.item.ItemHeatable;

public abstract class EntityAbstractFurnaceServer
        extends EntityAbstractContainerServer {
    protected final int[] fuel;
    protected final float[] fuelTemperature;
    protected final float temperatureFalloff;
    protected final int items, fuelHeat, fuelTier;
    protected float temperature, maximumTemperature;
    private double heatWait = 0.05;

    protected EntityAbstractFurnaceServer(WorldServer world, Vector3 pos,
            Inventory inventory, int fuel, int items, float maximumTemperature,
            float temperatureFalloff, int fuelHeat, int fuelTier) {
        super(world, pos, inventory);
        this.fuel = new int[fuel];
        fuelTemperature = new float[fuel];
        this.items = items;
        this.maximumTemperature = maximumTemperature;
        this.temperatureFalloff = temperatureFalloff;
        this.fuelHeat = fuelHeat;
        this.fuelTier = fuelTier;
    }

    @Override
    public TagStructure write() {
        TagStructure tagStructure = super.write();
        for (int i = 0; i < fuel.length; i++) {
            tagStructure.setInteger("Fuel" + i, fuel[i]);
        }
        for (int i = 0; i < fuelTemperature.length; i++) {
            tagStructure.setFloat("FuelTemperature" + i, fuelTemperature[i]);
        }
        tagStructure.setFloat("Temperature", temperature);
        return tagStructure;
    }

    @Override
    public void read(TagStructure tagStructure) {
        super.read(tagStructure);
        for (int i = 0; i < fuel.length; i++) {
            fuel[i] = tagStructure.getInteger("Fuel" + i);
        }
        for (int i = 0; i < fuelTemperature.length; i++) {
            fuelTemperature[i] = tagStructure.getFloat("FuelTemperature" + i);
        }
        temperature = tagStructure.getFloat("Temperature");
    }

    public float getTemperature() {
        return temperature;
    }

    @Override
    public void update(double delta) {
        VanillaBasics plugin =
                (VanillaBasics) world.getPlugins().getPlugin("VanillaBasics");
        VanillaMaterial materials = plugin.getMaterials();
        heatWait -= delta;
        while (heatWait <= 0.0) {
            heatWait += 0.05;
            temperature /= temperatureFalloff;
            temperature = FastMath.max(10, temperature);
            synchronized (this) {
                for (int i = 0; i < fuel.length; i++) {
                    if (fuel[i] > 0) {
                        temperature += fuelTemperature[i];
                        fuel[i]--;
                    } else {
                        int j = i;
                        plugin.getFuel(inventory.getItem(j))
                                .ifPresent(fuelType -> {
                                    if (inventory.getItem(j)
                                            .canTake(fuelType.getFuel()) ==
                                            fuelType.getFuel().getAmount() &&
                                            fuelType.getTier() >= fuelTier) {
                                        fuel[j] = fuelType.getTime() * fuelHeat;
                                        fuelTemperature[j] =
                                                fuelType.getTemperature() *
                                                        fuelHeat;
                                        inventory.getItem(j)
                                                .take(fuelType.getFuel());
                                        world.getConnection()
                                                .send(new PacketEntityChange(
                                                        this));
                                    }
                                });
                    }
                }
                int max = items + fuel.length + 1;
                for (int i = fuel.length + 1; i < max; i++) {
                    if (inventory.getItem(i).getAmount() == 1) {
                        Material type = inventory.getItem(i).getMaterial();
                        if (type instanceof ItemHeatable) {
                            ((ItemHeatable) type)
                                    .heat(inventory.getItem(i), temperature);
                        }
                    } else if (inventory.getItem(i).isEmpty() &&
                            !inventory.getItem(fuel.length).isEmpty()) {
                        int j = i;
                        inventory.getItem(fuel.length).take(1)
                                .ifPresent(item -> {
                                    inventory.getItem(j).stack(item);
                                    world.getConnection()
                                            .send(new PacketEntityChange(this));
                                });
                    }
                }
            }
            if (temperature > maximumTemperature) {
                world.getTerrain().queueBlockChanges(handler -> handler
                        .setBlockTypeAndData(pos.intX(), pos.intY(), pos.intZ(),
                                materials.air, (short) 0));
            }
        }
    }
}
