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

package scapes.plugin.tobi29.vanilla.basics.gui

import org.tobi29.scapes.block.Material
import org.tobi29.scapes.client.gui.GuiComponentItemButton
import org.tobi29.scapes.engine.gui.GuiEvent
import org.tobi29.scapes.engine.gui.GuiStyle
import scapes.plugin.tobi29.vanilla.basics.VanillaBasics
import scapes.plugin.tobi29.vanilla.basics.entity.client.EntityAnvilClient
import scapes.plugin.tobi29.vanilla.basics.entity.client.MobPlayerClientMainVB
import scapes.plugin.tobi29.vanilla.basics.packet.PacketAnvil

class GuiAnvilInventory(container: EntityAnvilClient,
                        player: MobPlayerClientMainVB, style: GuiStyle) : GuiContainerInventory<EntityAnvilClient>(
        "Anvil", player, container, style) {

    init {
        val plugin = player.connection().plugins.plugin(
                "VanillaBasics") as VanillaBasics
        val materials = plugin.materials
        selection(buttonContainer(16, 120, 30, 30, 0))
        selection(buttonContainer(16, 210, 30, 30, 1))
        selection(addAction(66, 120, materials.ingot, 0))
        selection(addAction(106, 120, materials.metalPickaxe, 1))
        selection(addAction(106, 160, materials.metalAxe, 2))
        selection(addAction(106, 200, materials.metalShovel, 3))
        selection(addAction(146, 120, materials.metalHammer, 4))
        selection(addAction(146, 160, materials.metalSaw, 5))
        selection(addAction(146, 200, materials.metalHoe, 6))
        selection(addAction(186, 120, materials.metalSword, 7))
    }

    private fun addAction(x: Int,
                          y: Int,
                          material: Material,
                          i: Int): GuiComponentItemButton {
        val icon = material.example(1)
        val item = pane.add(x.toDouble(), y.toDouble(), 30.0, 30.0
        ) {  GuiComponentItemButton(it, icon) }
        item.on(GuiEvent.CLICK_LEFT) { event ->
            player.connection().send(PacketAnvil(container, i))
        }
        return item
    }
}
