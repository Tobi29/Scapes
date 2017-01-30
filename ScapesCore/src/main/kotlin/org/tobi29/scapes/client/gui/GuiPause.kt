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

package org.tobi29.scapes.client.gui

import org.tobi29.scapes.client.states.GameStateGameMP
import org.tobi29.scapes.client.states.GameStateGameSP
import org.tobi29.scapes.engine.gui.GuiAction
import org.tobi29.scapes.engine.gui.GuiEvent
import org.tobi29.scapes.engine.gui.GuiStyle
import org.tobi29.scapes.entity.client.MobPlayerClientMain

class GuiPause(state: GameStateGameMP,
               player: MobPlayerClientMain,
               style: GuiStyle) : GuiMenuDouble(state, "Pause",
        if (state is GameStateGameSP)
            "Save and quit"
        else
            "Disconnect", "Back", style) {
    init {
        val options = row(pane) { button(it, "Options") }

        selection(options)

        options.on(GuiEvent.CLICK_LEFT) {
            player.openGui(GuiOptionsInGame(state, style))
        }
        save.on(GuiEvent.CLICK_LEFT) { player.connection().stop() }
        on(GuiAction.BACK) { player.closeGui() }
    }
}
