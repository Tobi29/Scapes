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
import org.tobi29.scapes.engine.graphics.Texture
import org.tobi29.scapes.engine.gui.Gui
import org.tobi29.scapes.engine.gui.GuiComponentImage
import org.tobi29.scapes.engine.gui.GuiStyle
import org.tobi29.scapes.engine.resource.Resource

class GuiScreenshot(
    state: GameState, previous: Gui, texture: Resource<Texture>, style: GuiStyle
) : GuiMenuSingle(state, "Screenshots", previous, style) {
    init {
        texture.onLoaded { tex ->
            pane.add(
                16.0, 5.0, 368.0,
                (tex.height().toDouble() / tex.width() * 368).toInt().toDouble()
            ) {
                GuiComponentImage(it, texture)
            }
        }
    }
}
