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
package org.tobi29.scapes.vanilla.basics.material.block.rock;

import org.tobi29.scapes.block.GameRegistry;
import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.vanilla.basics.material.StoneType;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;
import org.tobi29.scapes.vanilla.basics.material.block.BlockSimpleData;

public abstract class BlockStone extends BlockSimpleData {
    protected final GameRegistry.Registry<StoneType> stoneRegistry;

    protected BlockStone(VanillaMaterial materials, String nameID,
            GameRegistry.Registry<StoneType> stoneRegistry) {
        super(materials, nameID);
        this.stoneRegistry = stoneRegistry;
    }

    @Override
    protected int types() {
        return stoneRegistry.values().size();
    }

    @Override
    public double resistance(ItemStack item, int data) {
        return "Pickaxe".equals(item.material().toolType(item)) &&
                canBeBroken(item.material().toolLevel(item), data) ?
                8 * stoneRegistry.values().get(data).resistance() : -1;
    }

    @Override
    public String footStepSound(int data) {
        return "VanillaBasics:sound/footsteps/Stone.ogg";
    }

    @Override
    public String breakSound(ItemStack item, int data) {
        return "VanillaBasics:sound/blocks/Stone.ogg";
    }

    public boolean canBeBroken(int toolLevel, int data) {
        return FastMath.round(stoneRegistry.values().get(data).resistance()) *
                10 <= toolLevel;
    }

    @Override
    public int maxStackSize(ItemStack item) {
        return 16;
    }

    protected String stoneName(ItemStack item) {
        return stoneRegistry.values().get(item.data()).name();
    }
}
