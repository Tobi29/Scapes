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

import org.tobi29.scapes.block.Material
import org.tobi29.scapes.client.gui.GuiComponentItemButton
import org.tobi29.scapes.engine.gui.*
import org.tobi29.scapes.vanilla.basics.VanillaBasics
import org.tobi29.scapes.vanilla.basics.entity.client.EntityAnvilClient
import org.tobi29.scapes.vanilla.basics.entity.client.MobPlayerClientMainVB
import org.tobi29.scapes.vanilla.basics.packet.PacketAnvil

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
            buttonAction(it, materials.ingot, 0)
        }
        val actions2 = bar.addHori(0.0, 0.0, 40.0, -1.0, ::GuiComponentGroup)
        actions2.addVert(5.0, 5.0, 30.0, 30.0) {
            buttonAction(it, materials.metalPickaxe, 1)
        }
        actions2.addVert(5.0, 5.0, 30.0, 30.0) {
            buttonAction(it, materials.metalAxe, 2)
        }
        actions2.addVert(5.0, 5.0, 30.0, 30.0) {
            buttonAction(it, materials.metalShovel, 3)
        }
        val actions3 = bar.addHori(0.0, 0.0, 40.0, -1.0, ::GuiComponentGroup)
        actions3.addVert(5.0, 5.0, 30.0, 30.0) {
            buttonAction(it, materials.metalHammer, 4)
        }
        actions3.addVert(5.0, 5.0, 30.0, 30.0) {
            buttonAction(it, materials.metalSaw, 5)
        }
        actions3.addVert(5.0, 5.0, 30.0, 30.0) {
            buttonAction(it, materials.metalHoe, 6)
        }
        val actions4 = bar.addHori(0.0, 0.0, 40.0, -1.0, ::GuiComponentGroup)
        actions4.addVert(5.0, 5.0, 30.0, 30.0) {
            buttonAction(it, materials.metalSword, 7)
        }
        bar.spacer()
        topPane.spacer()
    }

    private fun buttonAction(parent: GuiLayoutData,
                             material: Material,
                             i: Int): GuiComponentItemButton {
        val icon = material.example(1)
        val item = GuiComponentItemButton(parent, icon)
        item.on(GuiEvent.CLICK_LEFT) {
            player.connection().send(PacketAnvil(player.registry, container, i))
        }
        return item
    }
}
