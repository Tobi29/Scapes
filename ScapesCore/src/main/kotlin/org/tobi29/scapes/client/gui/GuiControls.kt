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

package org.tobi29.scapes.client.gui

import org.tobi29.scapes.client.ScapesClient
import org.tobi29.scapes.engine.GameState
import org.tobi29.scapes.engine.gui.*
import org.tobi29.scapes.engine.input.ControllerBasic
import org.tobi29.scapes.engine.input.ControllerJoystick
import org.tobi29.scapes.engine.utils.tag.MutableTagMap
import org.tobi29.scapes.engine.utils.tag.set
import org.tobi29.scapes.engine.utils.tag.toDouble
import org.tobi29.scapes.engine.utils.math.pow
import org.tobi29.scapes.engine.utils.math.round

abstract class GuiControls(state: GameState,
                           previous: Gui,
                           game: ScapesClient,
                           style: GuiStyle) : GuiMenu(
        state, "Controls", "Save", style) {
    protected val scrollPane: GuiComponentScrollPaneViewport

    init {
        game.setFreezeInputMode(true)
        // This intentionally disable the back action to allow binding ESC
        back.on(GuiEvent.CLICK_LEFT) { event ->
            game.setFreezeInputMode(false)
            game.loadInput()
            engine.guiStack.swap(this, previous)
        }
        scrollPane = pane.addVert(16.0, 5.0, -1.0, -1.0) {
            GuiComponentScrollPane(it, 40)
        }.viewport
    }

    private fun sensitivity(value: Double): Double {
        var x = value
        x = x * 2.0 - 1.0
        return x * x * x * 10.0
    }

    private fun reverseSensitivity(value: Double): Double {
        var x = value
        x *= 0.1
        if (x >= 0.0) {
            x = pow(x, 1.0 / 3.0)
        } else {
            x = -pow(-x, 1.0 / 3.0)
        }
        return x * 0.5 + 0.5
    }

    protected fun addText(text: String) {
        scrollPane.addVert(40.0, 16.0, 40.0, 5.0, -1.0, 18.0) {
            GuiComponentText(it, text)
        }
    }

    protected fun addButton(name: String,
                            id: String,
                            tagMap: MutableTagMap,
                            controller: ControllerBasic) {
        val button = row(scrollPane) {
            GuiComponentControlsButton(it, 18, name, id, tagMap, controller)
        }
        selection(button)
    }

    protected fun addAxis(name: String,
                          id: String,
                          tagMap: MutableTagMap,
                          controller: ControllerJoystick) {
        val axis = row(scrollPane) {
            GuiComponentControlsAxis(it, 18, name, id, tagMap, controller)
        }
        selection(axis)
    }

    protected fun addSlider(name: String,
                            id: String,
                            tagMap: MutableTagMap) {
        val slider = row(scrollPane) {
            slider(it, name, reverseSensitivity(
                    tagMap[id]?.toDouble() ?: 0.0)) { text, value ->
                text + ": " + round(sensitivity(value) * 100.0) + '%'
            }
        }
        selection(slider)
        slider.on(GuiEvent.CHANGE) { event ->
            tagMap[id] = sensitivity(slider.value())
        }
    }
}
