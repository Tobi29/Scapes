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

import org.tobi29.scapes.engine.GameState
import org.tobi29.scapes.engine.gui.*

open class GuiMenuDouble protected constructor(state: GameState, title: String, save: String,
                                               back: String, style: GuiStyle) : GuiDesktop(
        state, style) {
    protected val pane: GuiComponentVisiblePane
    protected val back: GuiComponentTextButton
    protected val save: GuiComponentTextButton

    protected constructor(state: GameState, title: String, previous: Gui,
                          style: GuiStyle) : this(state, title, "Save", "Back",
            previous, style) {
    }

    protected constructor(state: GameState, title: String, style: GuiStyle) : this(
            state, title, "Save", "Back", style) {
    }

    protected constructor(state: GameState, title: String, save: String,
                          back: String, previous: Gui, style: GuiStyle) : this(
            state, title, save, back, style) {
        on(GuiAction.BACK
        ) { state.engine.guiStack.swap(this, previous) }
    }

    init {
        spacer()
        pane = addHori(0.0, 0.0, 400.0, -1.0, ::GuiComponentVisiblePane)
        spacer()
        pane.addVert(16.0, 14.0, -1.0, 32.0) { GuiComponentText(it, title) }
        pane.addVert(24.0, 6.0, -1.0, 2.0, ::GuiComponentSeparator)
        pane.addVert(0.0, 0.0, 0.0, 0.0, -1.0, -1.0, Long.MIN_VALUE + 300,
                ::GuiComponentGroup)
        pane.addVert(24.0, 6.0, 24.0, 6.0, -1.0, 2.0, Long.MIN_VALUE + 200,
                ::GuiComponentSeparator)
        this.save = pane.addVert(112.0, 12.0, 112.0, 5.0, 176.0, 30.0,
                Long.MIN_VALUE + 100) { button(it, save) }
        this.back = pane.addVert(112.0, 5.0, 112.0, 12.0, 176.0, 30.0,
                Long.MIN_VALUE + 100) { button(it, back) }

        selection(this.save)
        selection(this.back)

        this.back.on(GuiEvent.CLICK_LEFT) { event ->
            fireAction(GuiAction.BACK)
        }
    }
}
