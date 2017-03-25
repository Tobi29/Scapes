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

import org.tobi29.scapes.engine.graphics.*
import org.tobi29.scapes.engine.gui.GuiComponentHeavy
import org.tobi29.scapes.engine.gui.GuiLayoutData
import org.tobi29.scapes.engine.gui.GuiRenderer
import org.tobi29.scapes.engine.utils.math.clamp
import org.tobi29.scapes.engine.utils.math.min
import org.tobi29.scapes.engine.utils.math.vector.Vector2d

class GuiComponentBar(parent: GuiLayoutData,
                      private val r: Float,
                      private val g: Float,
                      private val b: Float,
                      private val a: Float,
                      private val updateFactor: Double,
                      private val supplier: () -> Double) : GuiComponentHeavy(
        parent) {
    private var model1: Model? = null
    private var model2: Model? = null
    private var value = 0.0

    constructor(parent: GuiLayoutData,
                r: Float,
                g: Float,
                b: Float,
                a: Float,
                supplier: () -> Double) : this(parent, r, g, b, a,
            10.0, supplier)

    public override fun renderComponent(gl: GL,
                                        shader: Shader,
                                        size: Vector2d,
                                        pixelSize: Vector2d,
                                        delta: Double) {
        val factor = min(1.0, delta * updateFactor)
        val newValue = supplier()
        if (newValue == Double.NEGATIVE_INFINITY) {
            value = 0.0
        } else if (newValue == Double.POSITIVE_INFINITY) {
            value = 1.0
        } else {
            value += (clamp(newValue, 0.0, 1.0) - value) * factor
        }
        gl.textures.unbind(gl)
        val matrixStack = gl.matrixStack
        var matrix = matrixStack.push()
        matrix.scale(value.toFloat(), 1.0f, 1.0f)
        model1?.render(gl, shader)
        matrixStack.pop()
        matrix = matrixStack.push()
        matrix.translate((value * size.x).toFloat(), 0.0f, 0.0f)
        matrix.scale((1.0 - value).toFloat(), 1.0f, 1.0f)
        model2?.render(gl, shader)
        matrixStack.pop()
    }

    override fun updateMesh(renderer: GuiRenderer,
                            size: Vector2d) {
        var r1 = r
        var g1 = g
        var b1 = b
        var r2 = r1 * 0.5f
        var g2 = g1 * 0.5f
        var b2 = b1 * 0.5f
        model1 = engine.graphics.createVCTI(
                floatArrayOf(0.0f, size.floatY(), 0.0f, size.floatX(),
                        size.floatY(), 0.0f, 0.0f, 0.0f, 0.0f, size.floatX(),
                        0.0f, 0.0f),
                floatArrayOf(r2, g2, b2, a, r2, g2, b2, a, r1, g1, b1, a, r1,
                        g1, b1, a),
                floatArrayOf(0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f),
                intArrayOf(0, 1, 2, 3, 2, 1), RenderType.TRIANGLES)
        r1 *= 0.4f
        g1 *= 0.4f
        b1 *= 0.4f
        r2 *= 0.4f
        g2 *= 0.4f
        b2 *= 0.4f
        model2 = engine.graphics.createVCTI(
                floatArrayOf(0.0f, size.floatY(), 0.0f, size.floatX(),
                        size.floatY(), 0.0f, 0.0f, 0.0f, 0.0f, size.floatX(),
                        0.0f, 0.0f),
                floatArrayOf(r2, g2, b2, a, r2, g2, b2, a, r1, g1, b1, a, r1,
                        g1, b1, a),
                floatArrayOf(0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f),
                intArrayOf(0, 1, 2, 3, 2, 1), RenderType.TRIANGLES)
    }
}
