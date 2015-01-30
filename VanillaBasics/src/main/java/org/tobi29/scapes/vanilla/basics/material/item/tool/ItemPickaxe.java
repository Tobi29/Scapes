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

package org.tobi29.scapes.vanilla.basics.material.item.tool;

import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.entity.server.MobPlayerServer;
import org.tobi29.scapes.packets.PacketUpdateInventory;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;

public class ItemPickaxe extends ItemTool {
    public ItemPickaxe(VanillaMaterial materials) {
        super(materials, "vanilla.basics.item.Pickaxe");
    }

    @Override
    public void click(MobPlayerServer entity, ItemStack item) {
        if (item.getData() == 0) {
            ItemStack itemHandle = new ItemStack(materials.stick, (short) 0);
            ItemStack itemString =
                    new ItemStack(materials.string, (short) 0, 2);
            if (entity.getInventory().canTake(itemHandle) &&
                    entity.getInventory().canTake(itemString)) {
                entity.getInventory().take(itemHandle);
                entity.getInventory().take(itemString);
                item.setData((short) 1);
                entity.getConnection().send(new PacketUpdateInventory(entity));
            }
        }
    }

    @Override
    public String getType() {
        return "Pickaxe";
    }
}
