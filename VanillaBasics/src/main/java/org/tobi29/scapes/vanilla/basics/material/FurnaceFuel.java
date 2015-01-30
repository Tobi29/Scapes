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

package org.tobi29.scapes.vanilla.basics.material;

import org.tobi29.scapes.block.ItemStack;

public class FurnaceFuel {
    private final ItemStack fuel;
    private final float temperature;
    private final int time, tier;

    public FurnaceFuel(ItemStack fuel, int time, float temperature, int tier) {
        this.fuel = fuel;
        this.time = time;
        this.temperature = temperature;
        this.tier = tier;
    }

    public ItemStack getFuel() {
        return fuel;
    }

    public float getTemperature() {
        return temperature;
    }

    public int getTier() {
        return tier;
    }

    public int getTime() {
        return time;
    }
}
