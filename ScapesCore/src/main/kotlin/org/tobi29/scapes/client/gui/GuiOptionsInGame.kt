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

import org.tobi29.scapes.client.states.GameStateGameMP
import org.tobi29.scapes.engine.ScapesEngineConfig
import org.tobi29.scapes.engine.fullscreen
import org.tobi29.scapes.engine.gui.GuiAction
import org.tobi29.scapes.engine.gui.GuiComponentTextButton
import org.tobi29.scapes.engine.gui.GuiEvent
import org.tobi29.scapes.engine.gui.GuiStyle
import org.tobi29.scapes.engine.setVolume
import org.tobi29.scapes.engine.volume

class GuiOptionsInGame(state: GameStateGameMP,
                       style: GuiStyle) : GuiMenuSingle(state,
        "Options", style) {
    init {
        val config = state.engine[ScapesEngineConfig.COMPONENT]
        val musicVolume = row(pane) {
            slider(it, "Music", config.volume("music"))
        }
        val soundVolume = row(pane) {
            slider(it, "Sound", config.volume("sound"))
        }
        val fullscreen: GuiComponentTextButton
        if (config.fullscreen) {
            fullscreen = row(pane) { button(it, "Fullscreen: ON") }
        } else {
            fullscreen = row(pane) { button(it, "Fullscreen: OFF") }
        }

        musicVolume.on(GuiEvent.CHANGE) { event ->
            config.setVolume("music", musicVolume.value())
        }
        soundVolume.on(GuiEvent.CHANGE) { event ->
            config.setVolume("sound", soundVolume.value())
        }
        fullscreen.on(GuiEvent.CLICK_LEFT) { event ->
            if (!config.fullscreen) {
                fullscreen.setText("Fullscreen: ON")
                config.fullscreen = true
            } else {
                fullscreen.setText("Fullscreen: OFF")
                config.fullscreen = false
            }
            state.engine.container.updateContainer()
        }
        on(GuiAction.BACK) { state.client().mob { it.closeGui() } }
    }
}
