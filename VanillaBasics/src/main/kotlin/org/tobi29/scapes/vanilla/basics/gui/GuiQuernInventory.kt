/*
 * Copyright 2012-2018 Tobi29
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

import org.tobi29.scapes.engine.gui.GuiComponentGroupSlab
import org.tobi29.scapes.engine.gui.GuiEvent
import org.tobi29.scapes.engine.gui.GuiStyle
import org.tobi29.scapes.vanilla.basics.entity.client.EntityQuernClient
import org.tobi29.scapes.vanilla.basics.entity.client.MobPlayerClientMainVB
import org.tobi29.scapes.vanilla.basics.packet.PacketQuern

class GuiQuernInventory(
    container: EntityQuernClient,
    player: MobPlayerClientMainVB,
    style: GuiStyle
) : GuiContainerInventory<EntityQuernClient>(
    "Quern", player, container, style
) {
    init {
        topPane.spacer()
        val bar = topPane.addVert(0.0, 0.0, -1.0, 120.0) {
            GuiComponentGroupSlab(it)
        }
        bar.spacer()
        bar.addHori(5.0, 5.0, 30.0, 30.0) {
            buttonContainer(it, "Container", 0)
        }
        bar.spacer()
        topPane.spacer()
        val quern = topPane.addVert(16.0, 5.0, 120.0, 30.0) {
            button(it, "Quern")
        }

        quern.on(GuiEvent.CLICK_LEFT) {
            player.connection().send(PacketQuern(player.registry, container))
        }
    }
}
