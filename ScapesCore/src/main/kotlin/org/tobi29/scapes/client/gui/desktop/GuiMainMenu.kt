/*
 * Copyright 2012-2016 Tobi29
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

package org.tobi29.scapes.client.gui.desktop

import org.tobi29.scapes.client.gui.GuiComponentLogo
import org.tobi29.scapes.client.states.scenes.SceneMenu
import org.tobi29.scapes.engine.GameState
import org.tobi29.scapes.engine.gui.GuiComponentVisiblePane
import org.tobi29.scapes.engine.gui.GuiEvent
import org.tobi29.scapes.engine.gui.GuiStyle

class GuiMainMenu(state: GameState, scene: SceneMenu, style: GuiStyle) : GuiDesktop(
        state, style) {
    init {
        val pane = addHori(0.0, 0.0, 150.0, -1.0, ::GuiComponentVisiblePane)
        pane.addVert(0.0, 10.0, 5.0, 20.0, 144.0, 80.0) {
            GuiComponentLogo(it, 80, 18)
        }
        val singlePlayer = pane.addVert(16.0, 5.0, 8.0, 5.0, -1.0, 30.0) {
            button(it, "Singleplayer")
        }
        val multiPlayer = pane.addVert(16.0, 5.0, 8.0, 5.0, -1.0, 30.0) {
            button(it, "Multiplayer")
        }
        val options = pane.addVert(16.0, 5.0, 8.0, 5.0, -1.0, 30.0) {
            button(it, "Options")
        }
        val credits = pane.addVert(16.0, 5.0, 8.0, 5.0, -1.0, 30.0) {
            button(it, "Credits")
        }
        val plugins = pane.addVert(16.0, 5.0, 8.0, 5.0, -1.0, 30.0) {
            button(it, "Plugins")
        }
        val playlists = pane.addVert(16.0, 5.0, 8.0, 5.0, -1.0, 30.0) {
            button(it, "Playlists")
        }
        val screenshots = pane.addVert(16.0, 5.0, 8.0, 5.0, -1.0,
                30.0) { button(it, "Screenshots") }
        val quit = pane.addVert(16.0, 5.0, 8.0, 5.0, -1.0, 30.0) {
            button(it, "Quit")
        }

        selection(singlePlayer)
        selection(multiPlayer)
        selection(options)
        selection(credits)
        selection(plugins)
        selection(playlists)
        selection(screenshots)
        selection(quit)

        singlePlayer.on(GuiEvent.CLICK_LEFT) { event ->
            state.engine.guiStack.swap(this,
                    GuiSaveSelect(state, this, scene, style))
        }
        multiPlayer.on(GuiEvent.CLICK_LEFT) { event ->
            state.engine.guiStack.add("10-Menu",
                    GuiServerSelect(state, this, style))
        }
        options.on(GuiEvent.CLICK_LEFT) { event ->
            state.engine.guiStack.add("10-Menu", GuiOptions(state, this, style))
        }
        credits.on(GuiEvent.CLICK_LEFT) { event ->
            state.engine.guiStack.add("10-Menu", GuiCredits(state, this, style))
        }
        plugins.on(GuiEvent.CLICK_LEFT) { event ->
            state.engine.guiStack.add("10-Menu", GuiPlugins(state, this, style))
        }
        playlists.on(GuiEvent.CLICK_LEFT) { event ->
            state.engine.guiStack.add("10-Menu",
                    GuiPlaylists(state, this, style))
        }
        screenshots.on(GuiEvent.CLICK_LEFT) { event ->
            state.engine.guiStack.add("10-Menu",
                    GuiScreenshots(state, this, style))
        }
        quit.on(GuiEvent.CLICK_LEFT) { event -> state.engine.stop() }
    }
}
