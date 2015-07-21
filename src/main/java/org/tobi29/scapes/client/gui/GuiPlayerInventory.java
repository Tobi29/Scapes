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

package org.tobi29.scapes.client.gui;

import org.tobi29.scapes.block.Inventory;
import org.tobi29.scapes.engine.gui.GuiComponentTextButton;
import org.tobi29.scapes.entity.client.MobPlayerClientMain;

public class GuiPlayerInventory extends GuiInventory {
    public GuiPlayerInventory(MobPlayerClientMain player) {
        super("Inventory", player);
        Inventory inventory = player.inventory();
        GuiComponentTextButton crafting =
                new GuiComponentTextButton(16, 80, 368, 30, 18, "Crafting");
        crafting.addLeftClick(
                event -> player.openGui(new GuiCrafting(false, player)));
        pane.add(crafting);
        int i = 0;
        for (int x = 0; x < 4; x++) {
            int xx = x * 35 + 11;
            int id = i + 40;
            GuiComponentItemButton item =
                    new GuiComponentItemButton(xx, 119, 30, 30,
                            inventory.item(id));
            item.addLeftClick(event -> leftClick(id));
            item.addRightClick(event -> rightClick(id));
            item.addHover(event -> setTooltip(inventory.item(id)));
            pane.add(item);
            i++;
        }
    }
}
