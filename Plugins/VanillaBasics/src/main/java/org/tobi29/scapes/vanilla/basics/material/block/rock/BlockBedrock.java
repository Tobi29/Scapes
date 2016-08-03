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
import org.tobi29.scapes.block.TerrainTextureRegistry;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;
import org.tobi29.scapes.vanilla.basics.material.block.BlockSimple;

public class BlockBedrock extends BlockSimple {
    public BlockBedrock(VanillaMaterial materials) {
        super(materials, "vanilla.basics.block.Bedrock");
    }

    @Override
    public double resistance(ItemStack item, int data) {
        return -1;
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
    public void registerTextures(TerrainTextureRegistry registry) {
        texture = registry.registerTexture(
                "VanillaBasics:image/terrain/Bedrock.png");
    }

    @Override
    public String name(ItemStack item) {
        return "Bedrock";
    }

    @Override
    public int maxStackSize(ItemStack item) {
        return 16;
    }
}
