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
import org.tobi29.scapes.engine.gui.GuiAction
import org.tobi29.scapes.engine.gui.GuiComponentTextButton
import org.tobi29.scapes.engine.gui.GuiEvent
import org.tobi29.scapes.engine.gui.GuiStyle

class GuiOptionsInGame(state: GameStateGameMP,
                       style: GuiStyle) : GuiMenuSingle(state,
        "Options", style) {
    init {
        val musicVolume = row(pane) {
            slider(it, "Music",
                    state.engine.config.volume("music"))
        }
        val soundVolume = row(pane) {
            slider(it, "Sound",
                    state.engine.config.volume("sound"))
        }
        val fullscreen: GuiComponentTextButton
        if (state.engine.config.fullscreen) {
            fullscreen = row(pane) { button(it, "Fullscreen: ON") }
        } else {
            fullscreen = row(pane) { button(it, "Fullscreen: OFF") }
        }

        selection(musicVolume)
        selection(soundVolume)
        selection(fullscreen)

        musicVolume.on(GuiEvent.CHANGE) { event ->
            state.engine.config.setVolume("music", musicVolume.value())
        }
        soundVolume.on(GuiEvent.CHANGE) { event ->
            state.engine.config.setVolume("sound", soundVolume.value())
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
        on(GuiAction.BACK) { state.client().mob { it.closeGui() } }
    }
}
