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

package org.tobi29.scapes.vanilla.basics.material.block.structural;

import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.chunk.terrain.Terrain;
import org.tobi29.scapes.chunk.terrain.TerrainServer;
import org.tobi29.scapes.engine.utils.math.Face;
import org.tobi29.scapes.entity.server.MobPlayerServer;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;
import org.tobi29.scapes.vanilla.basics.material.block.BlockSimpleData;
import org.tobi29.scapes.vanilla.basics.material.update.UpdateStrawDry;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class BlockStraw extends BlockSimpleData {
    private static final String[] TEXTURES =
            {"VanillaBasics:image/terrain/structure/WetStraw.png",
                    "VanillaBasics:image/terrain/structure/Straw.png"};

    public BlockStraw(VanillaMaterial materials) {
        super(materials, "vanilla.basics.block.Straw");
    }

    @Override
    public boolean place(TerrainServer.TerrainMutable terrain, int x, int y,
            int z, Face face, MobPlayerServer player) {
        Random random = ThreadLocalRandom.current();
        terrain.addDelayedUpdate(new UpdateStrawDry()
                .set(x, y, z, random.nextDouble() * 800.0 + 800.0));
        return true;
    }

    @Override
    public double resistance(ItemStack item, int data) {
        return 0.1;
    }

    @Override
    public List<ItemStack> drops(ItemStack item, int data) {
        return Collections
                .singletonList(new ItemStack(materials.grassBundle, data, 2));
    }

    @Override
    public String footStepSound(int data) {
        return "VanillaBasics:sound/footsteps/Grass.ogg";
    }

    @Override
    public String breakSound(ItemStack item, int data) {
        return "VanillaBasics:sound/blocks/Foliage.ogg";
    }

    @Override
    public byte lightTrough(Terrain terrain, int x, int y, int z) {
        return -5;
    }

    @Override
    public String name(ItemStack item) {
        switch (item.data()) {
            case 1:
                return "Straw";
            default:
                return "Wet Straw";
        }
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
