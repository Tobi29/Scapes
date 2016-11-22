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

import org.tobi29.scapes.engine.GameState
import org.tobi29.scapes.engine.gui.*

open class GuiTouch(state: GameState, style: GuiStyle) : GuiState(state,
        style) {

    protected fun row(pane: GuiContainerRow): GuiComponentGroupSlab {
        return pane.addVert(102.0, 0.0, -1.0, 80.0, ::GuiComponentGroupSlab)
    }

    protected fun <T : GuiComponent> row(pane: GuiContainerRow,
                                         component: (GuiLayoutDataFlow) -> T): T {
        return pane.addVert(112.0, 10.0, -1.0, 60.0, component)
    }

    protected fun <T : GuiComponent> rowCenter(pane: GuiContainerRow,
                                               component: (GuiLayoutDataFlow) -> T): T {
        return pane.addVert(301.0, 10.0, -1.0, 60.0, component)
    }

    protected fun button(parent: GuiLayoutData,
                         text: String): GuiComponentTextButton {
        return button(parent, 36, text)
    }

    protected fun button(parent: GuiLayoutData,
                         textSize: Int,
                         text: String): GuiComponentTextButton {
        return GuiComponentTextButton(parent, textSize, text)
    }

    protected fun slider(parent: GuiLayoutData,
                         text: String,
                         value: Double): GuiComponentSlider {
        return GuiComponentSlider(parent, 36, text, value)
    }

    protected fun slider(parent: GuiLayoutData,
                         text: String,
                         value: Double,
                         filter: (String, Double) -> String): GuiComponentSlider {
        return GuiComponentSlider(parent, 36, text, value, filter)
    }
}
