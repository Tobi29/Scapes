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

import org.tobi29.scapes.client.gui.GuiComponentHotbarButton
import org.tobi29.scapes.engine.gui.GuiComponentGroupSlab
import org.tobi29.scapes.engine.gui.GuiEvent
import org.tobi29.scapes.engine.gui.GuiLayoutData
import org.tobi29.scapes.vanilla.basics.entity.client.MobPlayerClientMainVB

class GuiComponentHotbar(parent: GuiLayoutData,
                         player: MobPlayerClientMainVB) : GuiComponentGroupSlab(
        parent) {
    init {
        player.inventories().access("Container") { inventory ->
            for (i in 0..9) {
                val button = addHori(5.0, 5.0, -1.0, -1.0) {
                    GuiComponentHotbarButton(it, inventory.item(i), player, i)
                }
                button.on(GuiEvent.CLICK_LEFT) {
                    player.setHotbarSelectRight(i)
                }
                button.on(GuiEvent.CLICK_RIGHT) {
                    player.setHotbarSelectLeft(i)
                }
            }
        }
    }
}
