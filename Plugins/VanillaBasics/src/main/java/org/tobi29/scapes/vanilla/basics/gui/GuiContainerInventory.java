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
package org.tobi29.scapes.vanilla.basics.gui;

import org.tobi29.scapes.block.Inventory;
import org.tobi29.scapes.client.gui.GuiComponentItemButton;
import org.tobi29.scapes.engine.gui.GuiEvent;
import org.tobi29.scapes.engine.gui.GuiStyle;
import org.tobi29.scapes.entity.client.EntityContainerClient;
import org.tobi29.scapes.packets.PacketInventoryInteraction;
import org.tobi29.scapes.vanilla.basics.entity.client.MobPlayerClientMainVB;

public class GuiContainerInventory extends GuiInventory {
    protected final EntityContainerClient container;

    public GuiContainerInventory(String name, MobPlayerClientMainVB player,
            EntityContainerClient container, GuiStyle style) {
        super(name, player, style);
        this.container = container;
    }

    protected GuiComponentItemButton buttonContainer(int x, int y, int width,
            int height, int slot) {
        return buttonContainer(x, y, width, height, "Container", slot);
    }

    protected GuiComponentItemButton buttonContainer(int x, int y, int width,
            int height, String id, int slot) {
        Inventory inventory = container.inventories().accessUnsafe(id);
        GuiComponentItemButton button = pane.add(x, y, width, height,
                p -> new GuiComponentItemButton(p, inventory.item(slot)));
        button.on(GuiEvent.CLICK_LEFT, event -> leftClickContainer(id, slot));
        button.on(GuiEvent.CLICK_RIGHT, event -> rightClickContainer(id, slot));
        button.on(GuiEvent.HOVER, event -> setTooltip(inventory.item(slot)));
        return button;
    }

    protected void leftClickContainer(String id, int i) {
        player.connection().send(new PacketInventoryInteraction(container,
                PacketInventoryInteraction.LEFT, id, i));
    }

    protected void rightClickContainer(String id, int i) {
        player.connection().send(new PacketInventoryInteraction(container,
                PacketInventoryInteraction.RIGHT, id, i));
    }
}
