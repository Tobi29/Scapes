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
import org.tobi29.scapes.vanilla.basics.entity.client.EntityForgeClient
import org.tobi29.scapes.vanilla.basics.entity.client.MobPlayerClientMainVB

class GuiForgeInventory(container: EntityForgeClient,
                        player: MobPlayerClientMainVB,
                        style: GuiStyle) : GuiContainerInventory<EntityForgeClient>(
        "Forge", player, container, style) {
    private val temperatureText: GuiComponentText

    init {
        selection(buttonContainer(16, 210, 30, 30, 0))
        selection(buttonContainer(56, 210, 30, 30, 1))
        selection(buttonContainer(96, 210, 30, 30, 2))
        selection(buttonContainer(136, 210, 30, 30, 3))
        selection(buttonContainer(16, 80, 30, 30, 4))
        selection(buttonContainer(16, 120, 30, 30, 5))
        selection(buttonContainer(56, 120, 30, 30, 6))
        selection(buttonContainer(96, 120, 30, 30, 7))
        selection(buttonContainer(96, 80, 30, 30, 8))
        temperatureText = pane.add(220.0, 170.0, -1.0,
                24.0) { GuiComponentText(it, "") }
        updateTemperatureText()
    }

    override fun updateComponent(delta: Double) {
        super.updateComponent(delta)
        updateTemperatureText()
    }

    private fun updateTemperatureText() {
        temperatureText.text = "${floor(container.temperature())}°C"
    }
}
