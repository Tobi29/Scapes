/*
 * Copyright 2012-2017 Tobi29
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

package org.tobi29.scapes.vanilla.basics.gui

import org.tobi29.scapes.client.gui.GuiComponentItemButton
import org.tobi29.scapes.engine.gui.GuiEvent
import org.tobi29.scapes.engine.gui.GuiStyle
import org.tobi29.scapes.entity.client.EntityContainerClient
import org.tobi29.scapes.packets.PacketInventoryInteraction
import org.tobi29.scapes.vanilla.basics.entity.client.MobPlayerClientMainVB

open class GuiContainerInventory<out T : EntityContainerClient>(name: String,
                                                                player: MobPlayerClientMainVB,
                                                                protected val container: T,
                                                                style: GuiStyle) : GuiInventory(
        name, player, style) {

    protected fun buttonContainer(x: Int,
                                  y: Int,
                                  width: Int,
                                  height: Int,
                                  slot: Int): GuiComponentItemButton {
        return buttonContainer(x, y, width, height, "Container", slot)
    }

    protected fun buttonContainer(x: Int,
                                  y: Int,
                                  width: Int,
                                  height: Int,
                                  id: String,
                                  slot: Int): GuiComponentItemButton {
        val inventory = container.inventories().accessUnsafe(id)
        val button = pane.add(x.toDouble(), y.toDouble(), width.toDouble(),
                height.toDouble()
        ) { GuiComponentItemButton(it, inventory.item(slot)) }
        button.on(GuiEvent.CLICK_LEFT) { event -> leftClickContainer(id, slot) }
        button.on(GuiEvent.CLICK_RIGHT) { event ->
            rightClickContainer(id, slot)
        }
        button.on(GuiEvent.HOVER) { event -> setTooltip(inventory.item(slot)) }
        return button
    }

    protected fun leftClickContainer(id: String,
                                     i: Int) {
        player.connection().send(PacketInventoryInteraction(container,
                PacketInventoryInteraction.LEFT, id, i))
    }

    protected fun rightClickContainer(id: String,
                                      i: Int) {
        player.connection().send(PacketInventoryInteraction(container,
                PacketInventoryInteraction.RIGHT, id, i))
    }
}
