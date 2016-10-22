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
import org.tobi29.scapes.engine.graphics.*
import org.tobi29.scapes.engine.gui.GuiState
import org.tobi29.scapes.engine.gui.GuiStyle
import org.tobi29.scapes.engine.utils.math.vector.Vector2d

class GuiHud(state: GameState, style: GuiStyle) : GuiState(state, style) {
    private val cross: Model

    init {
        cross = createVCTI(state.engine,
                floatArrayOf(-CROSS_SIZE, -CROSS_SIZE, 0.0f, CROSS_SIZE,
                        -CROSS_SIZE, 0.0f, CROSS_SIZE, CROSS_SIZE, 0.0f,
                        -CROSS_SIZE, CROSS_SIZE, 0.0f),
                floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f,
                        1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f),
                floatArrayOf(0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f),
                intArrayOf(0, 1, 2, 0, 2, 3), RenderType.TRIANGLES)
    }

    override fun render(gl: GL,
                        shader: Shader,
                        size: Vector2d,
                        pixelSize: Vector2d,
                        delta: Double) {
        super.render(gl, shader, size, pixelSize, delta)
        if (isVisible) {
            val matrixStack = gl.matrixStack()
            val matrix = matrixStack.push()
            matrix.translate(
                    gl.sceneWidth().toFloat() / gl.sceneHeight() * 270.0f,
                    270.0f,
                    0.0f)
            gl.textures().bind("Scapes:image/gui/Cross", gl)
            gl.setBlending(BlendingMode.INVERT)
            cross.render(gl, shader)
            gl.setBlending(BlendingMode.NORMAL)
            matrixStack.pop()
        }
    }

    companion object {
        private val CROSS_SIZE = 8.0f
    }
}
