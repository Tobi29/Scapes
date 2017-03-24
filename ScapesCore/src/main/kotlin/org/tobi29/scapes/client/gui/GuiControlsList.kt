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
import org.tobi29.scapes.engine.gui.Gui
import org.tobi29.scapes.engine.gui.GuiEvent
import org.tobi29.scapes.engine.gui.GuiStyle

class GuiControlsList(state: GameState,
                      previous: Gui,
                      style: GuiStyle) : GuiMenuSingle(
        state, "Controls", previous, style) {
    init {
        (state.engine.game as ScapesClient).inputModes.forEach { inputMode ->
            val controls = row(pane) { button(it, inputMode.toString()) }
            selection(controls)
            controls.on(GuiEvent.CLICK_LEFT
            ) { event ->
                state.engine.guiStack.swap(this,
                        inputMode.createControlsGUI(state, this))
            }
        }
    }
}
