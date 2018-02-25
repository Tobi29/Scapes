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

import org.tobi29.scapes.block.copy
import org.tobi29.scapes.client.gui.GuiComponentItemButton
import org.tobi29.scapes.engine.gui.*
import org.tobi29.scapes.vanilla.basics.VanillaBasics
import org.tobi29.scapes.vanilla.basics.entity.client.EntityAnvilClient
import org.tobi29.scapes.vanilla.basics.entity.client.MobPlayerClientMainVB
import org.tobi29.scapes.vanilla.basics.packet.PacketAnvil
import org.tobi29.scapes.vanilla.basics.util.Alloy
import org.tobi29.scapes.vanilla.basics.util.createTool

class GuiAnvilInventory(container: EntityAnvilClient,
                        player: MobPlayerClientMainVB,
                        style: GuiStyle) : GuiContainerInventory<EntityAnvilClient>(
        "Anvil", player, container, style) {

    init {
        val plugin = player.connection().plugins.plugin(
                "VanillaBasics") as VanillaBasics
        val materials = plugin.materials
        topPane.spacer()
        val bar = topPane.addVert(0.0, 0.0, -1.0, 120.0,
                ::GuiComponentGroupSlab)
        bar.spacer()
        val items = bar.addHori(0.0, 0.0, 40.0, -1.0, ::GuiComponentGroup)
        items.addVert(5.0, 5.0, 30.0, 30.0) {
            buttonContainer(it, "Container", 0)
        }
        items.spacer()
        items.addVert(5.0, 5.0, 30.0, 30.0) {
            buttonContainer(it, "Container", 1)
        }
        val actions1 = bar.addHori(0.0, 0.0, 40.0, -1.0, ::GuiComponentGroup)
        actions1.addVert(5.0, 5.0, 30.0, 30.0) {
            buttonAction(it, 0)
        }
        val actions2 = bar.addHori(0.0, 0.0, 40.0, -1.0, ::GuiComponentGroup)
        actions2.addVert(5.0, 5.0, 30.0, 30.0) {
            buttonAction(it, 1)
        }
        actions2.addVert(5.0, 5.0, 30.0, 30.0) {
            buttonAction(it, 2)
        }
        actions2.addVert(5.0, 5.0, 30.0, 30.0) {
            buttonAction(it, 3)
        }
        val actions3 = bar.addHori(0.0, 0.0, 40.0, -1.0, ::GuiComponentGroup)
        actions3.addVert(5.0, 5.0, 30.0, 30.0) {
            buttonAction(it, 4)
        }
        actions3.addVert(5.0, 5.0, 30.0, 30.0) {
            buttonAction(it, 5)
        }
        actions3.addVert(5.0, 5.0, 30.0, 30.0) {
            buttonAction(it, 6)
        }
        val actions4 = bar.addHori(0.0, 0.0, 40.0, -1.0, ::GuiComponentGroup)
        actions4.addVert(5.0, 5.0, 30.0, 30.0) {
            buttonAction(it, 7)
        }
        bar.spacer()
        topPane.spacer()
    }

    private fun buttonAction(parent: GuiLayoutData,
                             i: Int): GuiComponentItemButton {
        val plugin = player.world.plugins.plugin(
                "VanillaBasics") as VanillaBasics
        val alloy = Alloy(mapOf(
                (plugin.metalType("Iron") ?: plugin.crapMetal) to 1.0))
        val item = createTool(plugin, i, alloy).copy(data = 1)
        val button = GuiComponentItemButton(parent, item)
        button.on(GuiEvent.CLICK_LEFT) {
            player.connection().send(PacketAnvil(player.registry, container, i))
        }
        return button
    }
}
