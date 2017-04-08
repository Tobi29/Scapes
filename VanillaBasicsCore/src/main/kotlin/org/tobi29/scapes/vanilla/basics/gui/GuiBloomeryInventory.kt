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

import org.tobi29.scapes.engine.gui.GuiComponentText
import org.tobi29.scapes.engine.gui.GuiStyle
import org.tobi29.scapes.engine.utils.math.floor
import org.tobi29.scapes.vanilla.basics.entity.client.EntityBloomeryClient
import org.tobi29.scapes.vanilla.basics.entity.client.MobPlayerClientMainVB

class GuiBloomeryInventory(container: EntityBloomeryClient,
                           player: MobPlayerClientMainVB,
                           style: GuiStyle) : GuiContainerInventory<EntityBloomeryClient>(
        "Bloomery", player, container, style) {
    private val temperatureText: GuiComponentText
    private val bellowsText: GuiComponentText

    init {
        selection(buttonContainer(16, 210, 30, 30, 0))
        selection(buttonContainer(56, 210, 30, 30, 1))
        selection(buttonContainer(96, 210, 30, 30, 2))
        selection(buttonContainer(136, 210, 30, 30, 3))
        selection(buttonContainer(16, 80, 30, 30, 4))
        selection(buttonContainer(16, 120, 30, 30, 5))
        selection(buttonContainer(56, 120, 30, 30, 6))
        selection(buttonContainer(96, 120, 30, 30, 7))
        selection(buttonContainer(136, 120, 30, 30, 8))
        selection(buttonContainer(176, 120, 30, 30, 9))
        selection(buttonContainer(216, 120, 30, 30, 10))
        selection(buttonContainer(256, 120, 30, 30, 11))
        selection(buttonContainer(296, 120, 30, 30, 12))
        selection(buttonContainer(336, 120, 30, 30, 13))
        temperatureText = pane.add(40.0, 170.0, -1.0, 24.0) {
            GuiComponentText(it, "")
        }
        bellowsText = pane.add(120.0, 170.0, -1.0, 24.0) {
            GuiComponentText(it, "No bellows attached!")
        }
        updateTemperatureText()
    }

    override fun updateComponent(delta: Double) {
        super.updateComponent(delta)
        updateTemperatureText()
        bellowsText.visible = !container.hasBellows
    }

    private fun updateTemperatureText() {
        temperatureText.text = "${floor(container.temperature)}°C"
    }
}
