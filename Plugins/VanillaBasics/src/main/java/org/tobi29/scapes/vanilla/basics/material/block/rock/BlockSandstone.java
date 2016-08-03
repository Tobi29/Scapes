/*
 * Copyright 2012-2016 Tobi29
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

import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;
import org.tobi29.scapes.vanilla.basics.material.block.BlockSimpleData;

public class BlockSandstone extends BlockSimpleData {
    private static final String[] TEXTURES =
            {"VanillaBasics:image/terrain/stone/sandstone/1.png",
                    "VanillaBasics:image/terrain/stone/sandstone/2.png",
                    "VanillaBasics:image/terrain/stone/sandstone/3.png",
                    "VanillaBasics:image/terrain/stone/sandstone/4.png",
                    "VanillaBasics:image/terrain/stone/sandstone/5.png",
                    "VanillaBasics:image/terrain/stone/sandstone/6.png"};

    public BlockSandstone(VanillaMaterial materials) {
        super(materials, "vanilla.basics.block.Sandstone");
    }

    @Override
    public double resistance(ItemStack item, int data) {
        return "Pickaxe".equals(item.material().toolType(item)) ? 4 : -1;
    }

    @Override
    public String footStepSound(int data) {
        return "VanillaBasics:sound/footsteps/Stone.ogg";
    }

    @Override
    public String breakSound(ItemStack item, int data) {
        return "VanillaBasics:sound/blocks/Stone.ogg";
    }

    @Override
    public String name(ItemStack item) {
        return "Sandstone";
    }

    @Override
    public int maxStackSize(ItemStack item) {
        return 16;
    }

    @Override
    protected int types() {
        return TEXTURES.length;
    }

    @Override
    protected String texture(int data) {
        return TEXTURES[data];
    }
}
