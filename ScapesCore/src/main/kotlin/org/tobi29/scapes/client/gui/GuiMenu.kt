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

import org.tobi29.scapes.engine.GameState
import org.tobi29.scapes.engine.gui.*
import org.tobi29.scapes.engine.utils.math.Face
import org.tobi29.scapes.engine.utils.math.vector.Vector2d

open class GuiMenu(state: GameState,
                   title: String,
                   style: GuiStyle) : GuiDesktop(state, style) {
    protected val view: GuiComponentVisiblePane
    protected val pane: GuiComponentScrollPaneViewport
    protected val controls: GuiComponentGroup

    fun <T : GuiComponent> addControl(child: (GuiLayoutDataFlow) -> T) =
            addControl(0, child)

    fun <T : GuiComponent> addControl(priority: Long,
                                      child: (GuiLayoutDataFlow) -> T): T {
        return controls.addVert(5.0, 5.0, 5.0, 5.0, -1.0, 30.0, priority, child)
    }

    init {
        spacer()
        view = addHori(0.0, 0.0, 400.0, -1.0, ::GuiComponentVisiblePane)
        spacer()
        view.addVert(16.0, 14.0, -1.0, 32.0) {
            GuiComponentText(it, title)
        }
        view.addVert(24.0, 6.0, -1.0, 2.0, ::GuiComponentSeparator)
        pane = view.addVert(0.0, 0.0, 0.0, 0.0, -1.0, -1.0) {
            GuiComponentScrollPaneHidden(it, 40)
        }.viewport
        view.addVert(24.0, 6.0, 24.0, 6.0, -1.0, 2.0, Long.MIN_VALUE + 400,
                ::GuiComponentSeparator)

        controls = view.addVert(0.0, 0.0, 0.0, 0.0, -1.0, -1.0,
                Long.MIN_VALUE + 200) {
            GuiComponentMenuControls(it)
        }
    }

    fun controlSelection(vararg components: GuiComponent) {
        components.forEach { component ->
            selection({ if (!compactControls()) Face.NONE else Face.SOUTH },
                    component)
        }
        selection({ if (compactControls()) Face.NONE else Face.NORTH },
                *components)
    }

    private inner class GuiComponentMenuControls(
            parent: GuiLayoutData
    ) : GuiComponentGroup(parent) {
        init {
            parent.preferredSize = { size, maxSize ->
                val layout = layoutManager(mangleSize(size, maxSize))
                layout.layout()
                layout.size()
            }
        }

        override fun newLayoutManager(size: Vector2d): GuiLayoutManager {
            return if (compactControls()) {
                GuiLayoutManagerHorizontal(Vector2d(24.0, 0.0),
                        Vector2d(size.x - 48.0, 40.0), components)
            } else {
                GuiLayoutManagerVertical(Vector2d(56.0, 0.0),
                        Vector2d(size.x - 112.0, size.y), components)
            }
        }
    }

    private fun compactControls() = view.size()?.let { it.y < 540.0 } ?: false
}
