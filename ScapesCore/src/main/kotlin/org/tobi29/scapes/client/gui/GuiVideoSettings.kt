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

import org.tobi29.scapes.engine.GameState
import org.tobi29.scapes.engine.gui.Gui
import org.tobi29.scapes.engine.gui.GuiComponentTextButton
import org.tobi29.scapes.engine.gui.GuiEvent
import org.tobi29.scapes.engine.gui.GuiStyle
import org.tobi29.scapes.engine.utils.io.tag.mapMut
import org.tobi29.scapes.engine.utils.io.tag.set
import org.tobi29.scapes.engine.utils.io.tag.toDouble
import org.tobi29.scapes.engine.utils.math.round

class GuiVideoSettings(state: GameState,
                       previous: Gui,
                       style: GuiStyle) : GuiMenu(
        state, "Video Settings", previous, style) {
    init {
        val scapesTag = state.engine.configMap.mapMut("Scapes")
        val viewDistance = row(pane) {
            slider(it, "View distance",
                    ((scapesTag["RenderDistance"]?.toDouble() ?: 128.0) - 10.0) / 246.0) { text, value ->
                text + ": " + round(10.0 + value * 246.0) + 'm'
            }
        }
        val shader = row(pane) { button(it, "Shaders") }
        val fullscreen: GuiComponentTextButton
        if (state.engine.config.fullscreen) {
            fullscreen = row(pane) { button(it, "Fullscreen: ON") }
        } else {
            fullscreen = row(pane) { button(it, "Fullscreen: OFF") }
        }
        val resolutionMultiplier = row(pane) {
            slider(it, "Resolution", reverseResolution(
                    state.engine.config.resolutionMultiplier)) { text, value ->
                text + ": " +
                        round(resolution(value) * 100.0) +
                        '%'
            }
        }

        selection(viewDistance)
        selection(shader)
        selection(fullscreen)
        selection(resolutionMultiplier)

        viewDistance.on(GuiEvent.CHANGE) { event ->
            scapesTag["RenderDistance"] = 10.0 + viewDistance.value() * 246.0
        }
        shader.on(GuiEvent.CLICK_LEFT) { event ->
            state.engine.guiStack.add("10-Menu",
                    GuiShaderSettings(state, this, style))
        }
        fullscreen.on(GuiEvent.CLICK_LEFT) { event ->
            if (!state.engine.config.fullscreen) {
                fullscreen.setText("Fullscreen: ON")
                state.engine.config.fullscreen = true
            } else {
                fullscreen.setText("Fullscreen: OFF")
                state.engine.config.fullscreen = false
            }
            state.engine.container.updateContainer()
        }
        resolutionMultiplier.on(GuiEvent.CHANGE) { event ->
            state.engine.config.resolutionMultiplier = resolution(
                    resolutionMultiplier.value())
        }
    }

    private fun resolution(value: Double): Double {
        return 1.0 / round(11.0 - value * 10.0)
    }

    private fun reverseResolution(value: Double): Double {
        return 1.1 - 0.1 / value
    }
}
