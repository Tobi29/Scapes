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
import org.tobi29.scapes.engine.utils.math.min
import org.tobi29.scapes.engine.utils.math.vector.Vector2d

open class GuiDesktop(state: GameState,
                      style: GuiStyle) : GuiState(state,
        style) {

    override fun baseSize(): Vector2d {
        val container = engine.container
        val width = container.containerWidth().toDouble()
        val height = container.containerHeight().toDouble()
        val scale = min(540.0 / height, 1.0)
        return Vector2d(width * scale, height * scale)
    }

    protected fun row(pane: GuiContainerRow): GuiComponentGroupSlab {
        return pane.addVert(11.0, 0.0, -1.0, 40.0, ::GuiComponentGroupSlab)
    }

    protected fun <T : GuiComponent> row(pane: GuiContainerRow,
                                         component: (GuiLayoutDataFlow) -> T): T {
        return pane.addVert(16.0, 5.0, -1.0, 30.0, component)
    }

    protected fun <T : GuiComponent> rowCenter(pane: GuiContainerRow,
                                               component: (GuiLayoutDataFlow) -> T): T {
        return pane.addVert(112.0, 5.0, 176.0, 30.0, component)
    }

    protected fun button(parent: GuiLayoutData,
                         text: String): GuiComponentTextButton {
        return button(parent, 18, text)
    }

    protected fun button(parent: GuiLayoutData,
                         textSize: Int,
                         text: String): GuiComponentTextButton {
        return GuiComponentTextButton(parent, textSize, text)
    }

    protected fun slider(parent: GuiLayoutData,
                         text: String,
                         value: Double): GuiComponentSlider {
        return GuiComponentSlider(parent, 18, text, value)
    }

    protected fun slider(parent: GuiLayoutData,
                         text: String,
                         value: Double,
                         filter: (String, Double) -> String): GuiComponentSlider {
        return GuiComponentSlider(parent, 18, text, value, filter)
    }
}
