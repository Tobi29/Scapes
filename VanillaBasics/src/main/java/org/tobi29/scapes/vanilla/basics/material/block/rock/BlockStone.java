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
import org.tobi29.scapes.chunk.terrain.TerrainClient;
import org.tobi29.scapes.engine.utils.math.Face;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.vanilla.basics.material.StoneType;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;
import org.tobi29.scapes.vanilla.basics.material.block.BlockSimpleData;

import java.util.Optional;

public abstract class BlockStone extends BlockSimpleData {
    protected final GameRegistry.Registry<StoneType> stoneRegistry;

    protected BlockStone(VanillaMaterial materials, String nameID,
            GameRegistry.Registry<StoneType> stoneRegistry) {
        super(materials, nameID);
        this.stoneRegistry = stoneRegistry;
    }

    @Override
    protected int getTypes() {
        return stoneRegistry.values().size();
    }

    @Override
    public Optional<TerrainTexture> getParticleTexture(Face face,
            TerrainClient terrain, int x, int y, int z) {
        return Optional.of(textures[terrain.data(x, y, z)]);
    }

    @Override
    public double getResistance(ItemStack item, int data) {
        return "Pickaxe".equals(item.getMaterial().getToolType(item)) &&
                canBeBroken(item.getMaterial().getToolLevel(item), data) ?
                8 * stoneRegistry.values().get(data).getResistance() : -1;
    }

    @Override
    public String getFootStep(int data) {
        return "VanillaBasics:sound/footsteps/Stone.ogg";
    }

    @Override
    public String getBreak(ItemStack item, int data) {
        return "VanillaBasics:sound/blocks/Stone.ogg";
    }

    public boolean canBeBroken(int toolLevel, int data) {
        return FastMath
                .round(stoneRegistry.values().get(data).getResistance()) * 10 <=
                toolLevel;
    }

    @Override
    public int getStackSize(ItemStack item) {
        return 16;
    }

    protected String getStoneName(ItemStack item) {
        return stoneRegistry.values().get(item.getData()).getName();
    }
}
