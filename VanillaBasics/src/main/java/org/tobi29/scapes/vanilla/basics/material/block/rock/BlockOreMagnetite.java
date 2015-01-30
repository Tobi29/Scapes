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
import org.tobi29.scapes.chunk.terrain.TerrainServer;
import org.tobi29.scapes.engine.utils.math.Face;
import org.tobi29.scapes.entity.server.MobPlayerServer;
import org.tobi29.scapes.vanilla.basics.material.StoneType;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class BlockOreMagnetite extends BlockOre {
    public BlockOreMagnetite(VanillaMaterial materials,
            GameRegistry.Registry<StoneType> stoneRegistry) {
        super(materials, "vanilla.basics.block.OreMagnetite", stoneRegistry);
    }

    @Override
    public boolean destroy(TerrainServer.TerrainMutable terrain, int x, int y,
            int z, Face face, MobPlayerServer player, ItemStack item) {
        if ("Pickaxe".equals(item.getMaterial().getToolType(item)) &&
                !canBeBroken(item.getMaterial().getToolLevel(item),
                        terrain.getBlockData(x, y, z))) {
            terrain.getWorld()
                    .dropItem(new ItemStack(materials.oreChunk, (short) 5),
                            x + face.getX(), y + face.getY(), z + face.getZ());
            terrain.setBlockType(x, y, z, materials.stoneRaw);
            return false;
        }
        return true;
    }

    @Override
    public List<ItemStack> getDrops(ItemStack item, int data) {
        if ("Pickaxe".equals(item.getMaterial().getToolType(item)) &&
                canBeBroken(item.getMaterial().getToolLevel(item), data)) {
            return Arrays.asList(new ItemStack(materials.oreChunk, (short) 5),
                    new ItemStack(materials.stoneRock, data,
                            new Random().nextInt(4) + 8));
        }
        return Collections.emptyList();
    }

    @Override
    public int getStackSize(ItemStack item) {
        return 4;
    }

    @Override
    public String getName(ItemStack item) {
        return getStoneName(item) + " Magnetite Ore";
    }

    @Override
    protected String getOreTexture() {
        return "Magnetite";
    }
}
