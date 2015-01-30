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

package org.tobi29.scapes.vanilla.basics.material.block.structural;

import org.tobi29.scapes.block.GameRegistry;
import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.vanilla.basics.material.TreeType;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;
import org.tobi29.scapes.vanilla.basics.material.block.BlockSimpleData;

import java.util.Collections;
import java.util.List;

public class BlockWood extends BlockSimpleData {
    private final GameRegistry.Registry<TreeType> treeRegistry;

    public BlockWood(VanillaMaterial materials,
            GameRegistry.Registry<TreeType> treeRegistry) {
        super(materials, "vanilla.basics.block.Wood");
        this.treeRegistry = treeRegistry;
    }

    @Override
    public double getResistance(ItemStack item, int data) {
        return "Axe".equals(item.getMaterial().getToolType(item)) ||
                "Saw".equals(item.getMaterial().getToolType(item)) ? 2 : -1;
    }

    @Override
    public List<ItemStack> getDrops(ItemStack item, int data) {
        if ("Saw".equals(item.getMaterial().getToolType(item))) {
            return Collections.singletonList(
                    new ItemStack(materials.stick, (short) 0, 8));
        }
        return Collections.singletonList(new ItemStack(this, data));
    }

    @Override
    public String getFootStep(int data) {
        return "VanillaBasics:sound/footsteps/Wood.ogg";
    }

    @Override
    public String getBreak(ItemStack item, int data) {
        return "Axe".equals(item.getMaterial().getToolType(item)) ?
                "VanillaBasics:sound/blocks/Axe.ogg" :
                "VanillaBasics:sound/blocks/Saw.ogg";
    }

    @Override
    protected int getTypes() {
        return treeRegistry.values().size();
    }

    @Override
    protected String getTexture(int data) {
        TreeType type = treeRegistry.get(data);
        return type.getTextureRoot() + "/planks/" +
                type.getTexture() + ".png";
    }

    @Override
    public String getName(ItemStack item) {
        return materials.log.getName(item) + " Planks";
    }

    @Override
    public int getStackSize(ItemStack item) {
        return 16;
    }
}
