/*
 * Copyright 2012-2018 Tobi29
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
import org.tobi29.scapes.engine.gui.GuiComponentBusy
import org.tobi29.scapes.engine.gui.GuiComponentGroup
import org.tobi29.scapes.engine.gui.GuiComponentTextButton
import org.tobi29.scapes.engine.gui.GuiStyle

open class GuiBusy(
    state: GameState,
    style: GuiStyle
) : GuiDesktop(state, style) {
    private val busy: org.tobi29.scapes.engine.gui.GuiComponentBusy
    private val label: GuiComponentTextButton

    init {
        spacer()
        val pane = addHori(0.0, 0.0, 300.0, -1.0) { GuiComponentGroup(it) }
        spacer()
        pane.spacer()
        busy = pane.addVert(0.0, 10.0, 32.0, 32.0) { GuiComponentBusy(it) }
        label = row(pane) { button(it, "Loading...") }
        pane.spacer()
    }

    fun setColor(
        r: Float,
        g: Float,
        b: Float,
        a: Float
    ) {
        busy.setColor(r, g, b, a)
    }

    fun setLabel(label: String) {
        this.label.setText(label)
    }
}
