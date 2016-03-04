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
package org.tobi29.scapes.vanilla.basics.entity.client;

import org.tobi29.scapes.block.Inventory;
import org.tobi29.scapes.block.Material;
import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.vanilla.basics.material.item.ItemHeatable;

public abstract class EntityAbstractFurnaceClient
        extends EntityAbstractContainerClient {
    protected final int[] fuel;
    protected final float[] fuelTemperature;
    protected final float temperatureFalloff;
    protected final int items, fuelHeat, fuelTier;
    protected float temperature, maximumTemperature;
    private double heatWait = 0.05;

    protected EntityAbstractFurnaceClient(WorldClient world, Vector3 pos,
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

    public float temperature() {
        return temperature;
    }

    @Override
    public void update(double delta) {
        heatWait -= delta;
        while (heatWait <= 0.0) {
            heatWait += 0.05;
            temperature /= temperatureFalloff;
            temperature = FastMath.max(10, temperature);
            for (int i = 0; i < fuel.length; i++) {
                if (fuel[i] > 0) {
                    temperature += fuelTemperature[i];
                    fuel[i]--;
                }
            }
            int max = items + fuel.length + 1;
            inventories.access("Container", inventory -> {
                for (int i = fuel.length + 1; i < max; i++) {
                    if (inventory.item(i).amount() == 1) {
                        Material type = inventory.item(i).material();
                        if (type instanceof ItemHeatable) {
                            ((ItemHeatable) type)
                                    .heat(inventory.item(i), temperature);
                        }
                    }
                }
            });
        }
    }
}
