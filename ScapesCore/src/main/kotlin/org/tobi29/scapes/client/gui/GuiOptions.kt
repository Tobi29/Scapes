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

import org.tobi29.scapes.engine.utils.logging.KLogging
import org.tobi29.scapes.client.ScapesClient
import org.tobi29.scapes.connection.Account
import org.tobi29.scapes.engine.GameState
import org.tobi29.scapes.engine.gui.Gui
import org.tobi29.scapes.engine.gui.GuiEvent
import org.tobi29.scapes.engine.gui.GuiStyle
import org.tobi29.scapes.engine.utils.IOException

class GuiOptions(state: GameState,
                 previous: Gui,
                 style: GuiStyle) : GuiMenuSingle(
        state, "Options", previous, style) {

    init {
        val game = engine.game as ScapesClient
        val musicVolume = row(pane) {
            slider(it, "Music",
                    state.engine.config.volume("music"))
        }
        val soundVolume = row(pane) {
            slider(it, "Sound",
                    state.engine.config.volume("sound"))
        }
        val controls = row(pane) { button(it, "Controls") }
        val graphics = row(pane) { button(it, "Video settings") }
        val account = row(pane) { button(it, "Account") }
        val plugins = row(pane) { button(it, "Plugins") }

        selection(musicVolume)
        selection(soundVolume)
        selection(controls)
        selection(graphics)
        selection(account)
        selection(plugins)

        musicVolume.on(GuiEvent.CHANGE) { event ->
            state.engine.config.setVolume("music", musicVolume.value())
        }
        soundVolume.on(GuiEvent.CHANGE) { event ->
            state.engine.config.setVolume("sound", soundVolume.value())
        }
        controls.on(GuiEvent.CLICK_LEFT) { event ->
            state.engine.guiStack.add("10-Menu",
                    GuiControlsList(state, this, style))
        }
        graphics.on(GuiEvent.CLICK_LEFT) { event ->
            state.engine.guiStack.add("10-Menu",
                    GuiVideoSettings(state, this, style))
        }
        account.on(GuiEvent.CLICK_LEFT) { event ->
            try {
                val account1 = Account[game.home.resolve(
                        "Account.properties")]
                state.engine.guiStack.swap(this,
                        GuiAccount(state, this, account1, style))
            } catch (e: IOException) {
                logger.error { "Failed to read account file: $e" }
            }
        }
        plugins.on(GuiEvent.CLICK_LEFT) { event ->
            state.engine.guiStack.add("10-Menu", GuiPlugins(state, this, style))
        }
    }

    companion object : KLogging()
}
