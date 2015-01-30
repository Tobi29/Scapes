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

import java.util.List;

public abstract class BlockOre extends BlockStone {
    protected BlockOre(VanillaMaterial materials, String nameID,
            GameRegistry.Registry<StoneType> stoneRegistry) {
        super(materials, nameID, stoneRegistry);
    }

    @Override
    public double getResistance(ItemStack item, int data) {
        if ("Pickaxe".equals(item.getMaterial().getToolType(item)) &&
                canBeBroken(item.getMaterial().getToolLevel(item), data)) {
            return super.getResistance(item, data);
        } else if ("Pickaxe".equals(item.getMaterial().getToolType(item))) {
            return 12.0;
        }
        return -1;
    }

    @Override
    protected String getTexture(int data) {
        return "";
    }

    @Override
    public void registerTextures(TerrainTextureRegistry registry) {
        List<StoneType> types = stoneRegistry.values();
        textures = new TerrainTexture[types.size()];
        String ore = "VanillaBasics:image/terrain/ore/block/" +
                getOreTexture() + ".png";
        int i = 0;
        for (StoneType type : types) {
            textures[i++] =
                    registry.registerTexture(type.getTextureRoot() + "/raw/" +
                            type.getTexture() +
                            ".png", ore);
        }
    }

    protected abstract String getOreTexture();
}
