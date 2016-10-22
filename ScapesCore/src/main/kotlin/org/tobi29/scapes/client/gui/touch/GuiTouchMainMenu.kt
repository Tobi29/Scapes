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

package org.tobi29.scapes.client.gui.touch

import org.tobi29.scapes.client.gui.GuiComponentLogo
import org.tobi29.scapes.client.states.scenes.SceneMenu
import org.tobi29.scapes.engine.GameState
import org.tobi29.scapes.engine.gui.*

class GuiTouchMainMenu(state: GameState, scene: SceneMenu, style: GuiStyle) : GuiTouch(
        state, style) {
    init {
        val pane = addHori(0.0, 0.0, 288.0, -1.0, ::GuiComponentVisiblePane)
        val space = spacer()
        pane.addVert(0.0, 10.0, 5.0, 20.0, -1.0, 160.0
        ) {  GuiComponentLogo(it, 160, 36) }
        val singlePlayer = space.addVert(10.0, 10.0, 320.0, 80.0
        ) {  button(it, 48, "Singleplayer") }
        val multiPlayer = space.addVert(10.0, 10.0, 320.0, 80.0
        ) {  button(it, 48, "Multiplayer") }
        val options = space.addVert(10.0, 10.0, 320.0, 80.0) {
            button(it, 48, "Options")
        }
        val playlists = space.addVert(10.0, 10.0, 320.0, 80.0) {
            button(it, 48, "Playlists")
        }

        selection(singlePlayer)
        selection(multiPlayer)
        selection(options)
        selection(playlists)

        singlePlayer.on(GuiEvent.CLICK_LEFT) { event ->
            state.engine.guiStack.swap(this,
                    GuiTouchSaveSelect(state, this, scene, style))
        }
        multiPlayer.on(GuiEvent.CLICK_LEFT) { event ->
            state.engine.guiStack.swap(this,
                    GuiTouchServerSelect(state, this, style))
        }
        options.on(GuiEvent.CLICK_LEFT) { event ->
            state.engine.guiStack.add("10-Menu",
                    GuiTouchOptions(state, this, style))
        }
        playlists.on(GuiEvent.CLICK_LEFT) { event ->
            state.engine.guiStack.add("10-Menu",
                    GuiTouchPlaylists(state, this, style))
        }
    }
}