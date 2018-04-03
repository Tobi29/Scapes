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

import org.tobi29.math.vector.Vector2d
import org.tobi29.scapes.engine.graphics.FontRenderer
import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.graphics.Matrix
import org.tobi29.scapes.engine.graphics.Shader
import org.tobi29.scapes.engine.gui.GuiComponentHeavy
import org.tobi29.scapes.engine.gui.GuiLayoutData
import org.tobi29.scapes.engine.gui.GuiRenderer

class GuiComponentCredits(parent: GuiLayoutData) : GuiComponentHeavy(
        parent) {
    private var y = 160.0f
    var text: String = ""
        set(value) {
            if (field != value) {
                field = value
                dirty()
            }
        }

    override fun updateMesh(renderer: GuiRenderer,
                            size: Vector2d) {
        val font = gui.style.font
        font.render(FontRenderer.to(renderer, 1.0, 1.0, 1.0, 1.0), text, size.y,
                size.x)
    }

    override fun transform(matrix: Matrix,
                           size: Vector2d) {
        matrix.translate(0f, y, 0f)
    }

    public override fun renderComponent(gl: GL,
                                        shader: Shader,
                                        size: Vector2d,
                                        pixelSize: Vector2d,
                                        delta: Double) {
        y -= (40.0f * delta).toFloat()
    }
}
