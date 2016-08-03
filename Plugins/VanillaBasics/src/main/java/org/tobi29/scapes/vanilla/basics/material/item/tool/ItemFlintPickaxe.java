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

package org.tobi29.scapes.vanilla.basics.material.item.tool;

import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.entity.server.MobPlayerServer;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;

public class ItemFlintPickaxe extends ItemFlintTool {
    public ItemFlintPickaxe(VanillaMaterial materials) {
        super(materials, "vanilla.basics.item.FlintPickaxe");
    }

    @Override
    public void click(MobPlayerServer entity, ItemStack item) {
        if (item.data() == 0) {
            ItemStack itemHandle = new ItemStack(materials.stick, 0);
            ItemStack itemString = new ItemStack(materials.string, 0, 2);
            entity.inventories().modify("Container", inventory -> {
                if (inventory.canTake(itemHandle) &&
                        inventory.canTake(itemString)) {
                    inventory.take(itemHandle);
                    inventory.take(itemString);
                    item.setData(1);
                }
            });
        }
    }

    @Override
    public String type() {
        return "Pickaxe";
    }
}
