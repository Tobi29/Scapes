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
import org.tobi29.scapes.block.TerrainTexture;
import org.tobi29.scapes.block.TerrainTextureRegistry;
import org.tobi29.scapes.vanilla.basics.material.StoneType;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;

import java.util.Collections;
import java.util.List;

public class BlockCobblestoneMossy extends BlockStone {
    public BlockCobblestoneMossy(VanillaMaterial materials,
            GameRegistry.Registry<StoneType> stoneRegistry) {
        super(materials, "vanilla.basics.block.CobblestoneMossy",
                stoneRegistry);
    }

    @Override
    public List<ItemStack> drops(ItemStack item, int data) {
        if ("Pickaxe".equals(item.material().toolType(item)) &&
                canBeBroken(item.material().toolLevel(item), data)) {
            return Collections
                    .singletonList(new ItemStack(materials.stoneRock, data, 9));
        }
        return Collections.emptyList();
    }

    @Override
    protected String texture(int data) {
        return "";
    }

    @Override
    public void registerTextures(TerrainTextureRegistry registry) {
        List<StoneType> types = stoneRegistry.values();
        textures = new TerrainTexture[types.size()];
        int i = 0;
        for (StoneType type : types) {
            textures[i++] =
                    registry.registerTexture(type.textureRoot() + "/raw/" +
                                    type.texture() + ".png",
                            "VanillaBasics:image/terrain/stone/overlay/Cobble.png",
                            "VanillaBasics:image/terrain/stone/overlay/Mossy.png");
        }
    }

    @Override
    public String name(ItemStack item) {
        return "Mossy " + stoneName(item) + " Cobblestone";
    }
}
