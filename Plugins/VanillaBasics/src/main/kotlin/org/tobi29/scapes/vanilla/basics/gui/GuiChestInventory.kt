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

import org.tobi29.scapes.engine.gui.GuiComponent
import org.tobi29.scapes.engine.gui.GuiStyle
import org.tobi29.scapes.vanilla.basics.entity.client.EntityChestClient
import org.tobi29.scapes.vanilla.basics.entity.client.MobPlayerClientMainVB
import java.util.*

class GuiChestInventory(container: EntityChestClient,
                        player: MobPlayerClientMainVB,
                        style: GuiStyle) : GuiContainerInventory<EntityChestClient>(
        "Chest", player, container, style) {
    init {
        container.inventories().access("Container") { inventory ->
            var x = -1
            var y = 0
            var xx: Int
            var yy = 91
            val buttons = ArrayList<GuiComponent>(10)
            for (i in 0..inventory.size() - 1) {
                if (++x >= 10) {
                    y++
                    yy = y * 35 + 91
                    x = 0
                    selection(buttons)
                    buttons.clear()
                }
                xx = x * 35 + 27
                buttons.add(buttonContainer(xx, yy, 30, 30, i))
            }
            selection(buttons)
        }
    }
}
