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

import org.tobi29.scapes.engine.GameState
import org.tobi29.scapes.engine.gui.Gui
import org.tobi29.scapes.engine.gui.GuiAction
import org.tobi29.scapes.engine.gui.GuiComponentTextButton
import org.tobi29.scapes.engine.gui.GuiStyle

open class GuiMenuDouble protected constructor(state: GameState, title: String, save: String,
                                               back: String, style: GuiStyle) : GuiMenu(
        state, title, back, style) {
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
        this.save = addControl(60) { button(it, save) }

        selection(this.save)
    }
}