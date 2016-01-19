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

package org.tobi29.scapes.vanilla.basics.material.item.tool;

import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;

public class ItemMetalSword extends ItemMetalTool {
    public ItemMetalSword(VanillaMaterial materials) {
        super(materials, "vanilla.basics.item.MetalSword");
    }

    @Override
    public boolean isWeapon(ItemStack item) {
        return true;
    }

    @Override
    public double hitRange(ItemStack item) {
        return 4;
    }

    @Override
    public String type() {
        return "Sword";
    }
}