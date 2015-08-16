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

public class BlockOreBismuthinite extends BlockOre {
    public BlockOreBismuthinite(VanillaMaterial materials,
            GameRegistry.Registry<StoneType> stoneRegistry) {
        super(materials, "vanilla.basics.block.OreBismuthinite", stoneRegistry);
    }

    @Override
    public boolean destroy(TerrainServer.TerrainMutable terrain, int x, int y,
            int z, Face face, MobPlayerServer player, ItemStack item) {
        if ("Pickaxe".equals(item.material().toolType(item)) &&
                !canBeBroken(item.material().toolLevel(item),
                        terrain.data(x, y, z))) {
            terrain.world()
                    .dropItem(new ItemStack(materials.oreChunk, (short) 10),
                            x + face.getX(), y + face.getY(), z + face.getZ());
            terrain.type(x, y, z, materials.stoneRaw);
            return false;
        }
        return true;
    }

    @Override
    public List<ItemStack> drops(ItemStack item, int data) {
        if ("Pickaxe".equals(item.material().toolType(item)) &&
                canBeBroken(item.material().toolLevel(item), data)) {
            return Arrays.asList(new ItemStack(materials.oreChunk, (short) 10),
                    new ItemStack(materials.stoneRock, data,
                            new Random().nextInt(4) + 8));
        }
        return Collections.emptyList();
    }

    @Override
    public int maxStackSize(ItemStack item) {
        return 4;
    }

    @Override
    public String name(ItemStack item) {
        return stoneName(item) + " Bismuthinite Ore";
    }

    @Override
    protected String oreTexture() {
        return "Bismuthinite";
    }
}
