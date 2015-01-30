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
import org.tobi29.scapes.entity.client.EntityContainerClient;
import org.tobi29.scapes.entity.client.MobPlayerClientMain;
import org.tobi29.scapes.packets.PacketInventoryInteraction;

public class GuiContainerInventory extends GuiInventory {
    protected final EntityContainerClient container;

    public GuiContainerInventory(String name, MobPlayerClientMain player,
            EntityContainerClient container) {
        super(name, player);
        this.container = container;
    }

    protected void addButton(int x, int y, int width, int height, int slot) {
        Inventory inventory = container.getInventory();
        GuiComponentItemButton button =
                new GuiComponentItemButton(x, y, width, height,
                        inventory.getItem(slot));
        button.addLeftClick(event -> leftClickContainer(slot));
        button.addRightClick(event -> rightClickContainer(slot));
        button.addHover(event -> setTooltip(inventory.getItem(slot)));
        pane.add(button);
    }

    protected void leftClickContainer(int i) {
        player.getConnection().send(new PacketInventoryInteraction(container,
                PacketInventoryInteraction.LEFT, i));
    }

    protected void rightClickContainer(int i) {
        player.getConnection().send(new PacketInventoryInteraction(container,
                PacketInventoryInteraction.RIGHT, i));
    }
}
