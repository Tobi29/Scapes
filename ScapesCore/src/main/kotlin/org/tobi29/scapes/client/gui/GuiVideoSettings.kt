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
import org.tobi29.scapes.engine.ScapesEngineConfig
import org.tobi29.scapes.engine.fullscreen
import org.tobi29.scapes.engine.gui.Gui
import org.tobi29.scapes.engine.gui.GuiComponentTextButton
import org.tobi29.scapes.engine.gui.GuiEvent
import org.tobi29.scapes.engine.gui.GuiStyle
import kotlin.math.roundToInt

class GuiVideoSettings(state: GameState,
                       previous: Gui,
                       style: GuiStyle) : GuiMenuSingle(
        state, "Video Settings", previous, style) {
    init {
        val config = state.engine[ScapesEngineConfig.COMPONENT]
        val scapes = engine[ScapesClient.COMPONENT]
        val viewDistance = row(pane) {
            slider(it, "View distance",
                    (scapes.renderDistance - 10.0) / 246.0) { text, value ->
                text + ": " + (10.0 + value * 246.0).roundToInt() + 'm'
            }
        }
        val shader = row(pane) { button(it, "Shaders") }
        val fullscreen: GuiComponentTextButton
        if (config.fullscreen) {
            fullscreen = row(pane) { button(it, "Fullscreen: ON") }
        } else {
            fullscreen = row(pane) { button(it, "Fullscreen: OFF") }
        }
        val resolutionMultiplier = row(pane) {
            slider(it, "Resolution", reverseResolution(
                    scapes.resolutionMultiplier)) { text, value ->
                text + ": " + (resolution(value) * 100.0).roundToInt() + '%'
            }
        }

        viewDistance.on(GuiEvent.CHANGE) {
            scapes.renderDistance = 10.0 + viewDistance.value() * 246.0
        }
        shader.on(GuiEvent.CLICK_LEFT) {
            state.engine.guiStack.add("10-Menu",
                    GuiShaderSettings(state, this, style))
        }
        fullscreen.on(GuiEvent.CLICK_LEFT) {
            if (!config.fullscreen) {
                fullscreen.setText("Fullscreen: ON")
                config.fullscreen = true
            } else {
                fullscreen.setText("Fullscreen: OFF")
                config.fullscreen = false
            }
            state.engine.container.updateContainer()
        }
        resolutionMultiplier.on(GuiEvent.CHANGE) {
            scapes.resolutionMultiplier = resolution(
                    resolutionMultiplier.value())
        }
    }

    private fun resolution(value: Double): Double {
        return 1.0 / (11.0 - value * 10.0).roundToInt()
    }

    private fun reverseResolution(value: Double): Double {
        return 1.1 - 0.1 / value
    }
}
