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

import mu.KLogging
import org.tobi29.scapes.engine.GameState
import org.tobi29.scapes.engine.gui.Gui
import org.tobi29.scapes.engine.gui.GuiComponentText
import org.tobi29.scapes.engine.gui.GuiEvent
import org.tobi29.scapes.engine.gui.GuiStyle

class GuiDebug(state: GameState,
               previous: Gui,
               style: GuiStyle) : GuiMenuSingle(
        state, "Options", previous, style) {

    init {
        val profiler = row(pane) { button(it, "Profiler (Shift + F3)") }
        val performance = row(pane) { button(it, "Performance (Control + F3)") }
        val debugValues = row(pane) { button(it, "Debug values (F3)") }
        pane.addVert(16.0, 5.0, -1.0, 18.0) { GuiComponentText(it, """
Press Control + Shift + F3
for debug crash report

Press F6 in game to show
debug tools
        """) }

        selection(profiler)
        selection(performance)
        selection(debugValues)

        profiler.on(GuiEvent.CLICK_LEFT) {
            engine.profiler.visible = !engine.profiler.visible
        }
        performance.on(GuiEvent.CLICK_LEFT) {
            engine.performance.visible = !engine.performance.visible
        }
        debugValues.on(GuiEvent.CLICK_LEFT) {
            engine.debugValues.visible = !engine.debugValues.visible
        }
    }

    companion object : KLogging()
}
