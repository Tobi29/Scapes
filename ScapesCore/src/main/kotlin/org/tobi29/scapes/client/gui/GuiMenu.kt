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

import org.tobi29.scapes.engine.gui.*

open class GuiMenu(state: org.tobi29.scapes.engine.GameState,
                   title: String,
                   back: String,
                   style: org.tobi29.scapes.engine.gui.GuiStyle) : org.tobi29.scapes.client.gui.GuiDesktop(state, style) {
    protected val view: org.tobi29.scapes.client.gui.GuiComponentMenuPane
    protected val pane: org.tobi29.scapes.engine.gui.GuiComponentScrollPaneViewport
    protected val back: org.tobi29.scapes.engine.gui.GuiComponentTextButton

    protected constructor(state: org.tobi29.scapes.engine.GameState, title: String, previous: org.tobi29.scapes.engine.gui.Gui,
                          style: org.tobi29.scapes.engine.gui.GuiStyle) : this(state, title, "Back",
            previous, style) {
    }

    protected constructor(state: org.tobi29.scapes.engine.GameState, title: String, style: org.tobi29.scapes.engine.gui.GuiStyle) : this(
            state, title, "Back", style) {
    }

    protected constructor(state: org.tobi29.scapes.engine.GameState, title: String, back: String, previous: org.tobi29.scapes.engine.gui.Gui,
                          style: org.tobi29.scapes.engine.gui.GuiStyle) : this(state, title, back, style) {
        on(org.tobi29.scapes.engine.gui.GuiAction.BACK
        ) { state.engine.guiStack.swap(this, previous) }
    }

    fun <T : org.tobi29.scapes.engine.gui.GuiComponent> addControl(priority: Int,
                                                                   child: (org.tobi29.scapes.client.gui.GuiLayoutDataMenuControl) -> T): T {
        if (priority < 0 || priority > 100) {
            throw IllegalArgumentException("Priority out of bounds: $priority")
        }
        return view.addControl(112.0, 5.0, 112.0, 5.0, -1.0, 30.0, 5.0,
                5.0, 5.0, 5.0, -1.0, 30.0, Long.MIN_VALUE + priority, child)
    }

    init {
        spacer()
        view = addHori(0.0, 0.0, 400.0, -1.0) {
            org.tobi29.scapes.client.gui.GuiComponentMenuPane(it, 540.0)
        }
        spacer()
        view.addVert(16.0, 14.0, -1.0, 32.0) {
            org.tobi29.scapes.engine.gui.GuiComponentText(it, title)
        }
        view.addVert(24.0, 6.0, -1.0, 2.0, ::GuiComponentSeparator)
        pane = view.addVert(0.0, 0.0, 0.0, 0.0, -1.0, -1.0) {
            GuiComponentScrollPaneHidden(it, 40)
        }.viewport
        view.addVert(24.0, 6.0, 24.0, 6.0, -1.0, 2.0, Long.MIN_VALUE + 400,
                ::GuiComponentSeparator)
        view.addControl(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 48.0,
                0.0, Long.MIN_VALUE + 300, ::GuiComponentGroup)
        view.addControl(0.0, 0.0, 0.0, 0.0, 0.0, 6.0, 0.0, 0.0, 0.0, 0.0, 48.0,
                0.0, Long.MIN_VALUE, ::GuiComponentGroup)
        this.back = addControl(50) { button(it, back) }

        selection(this.back)

        this.back.on(GuiEvent.CLICK_LEFT) { event ->
            fireAction(GuiAction.BACK)
        }
    }
}
