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

import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.graphics.Mesh
import org.tobi29.scapes.engine.graphics.Model
import org.tobi29.scapes.engine.graphics.Shader
import org.tobi29.scapes.engine.gui.GuiComponentHeavy
import org.tobi29.scapes.engine.gui.GuiLayoutData
import org.tobi29.scapes.engine.gui.GuiRenderer
import org.tobi29.scapes.engine.utils.math.cos
import org.tobi29.scapes.engine.utils.math.max
import org.tobi29.scapes.engine.utils.math.sin
import org.tobi29.scapes.engine.utils.math.toRad
import org.tobi29.scapes.engine.utils.math.vector.Vector2d

class GuiComponentBusy(parent: GuiLayoutData) : GuiComponentHeavy(parent) {
    private var r = 1.0f
    private var g = 1.0f
    private var b = 1.0f
    private var a = 1.0f
    private var model: Model? = null
    private var value = 0.0

    fun setColor(r: Float,
                 g: Float,
                 b: Float,
                 a: Float) {
        this.r = r
        this.g = g
        this.b = b
        this.a = a
        dirty()
    }

    public override fun renderComponent(gl: GL,
                                        shader: Shader,
                                        size: Vector2d,
                                        pixelSize: Vector2d,
                                        delta: Double) {
        value += delta * 300.0
        while (value > 180.0) {
            value -= 360.0
        }
        gl.textures().unbind(gl)
        val matrixStack = gl.matrixStack()
        val matrix = matrixStack.push()
        matrix.translate(size.floatX() * 0.5f, size.floatY() * 0.5f, 0.0f)
        matrix.rotateAccurate(value, 0.0f, 0.0f, 1.0f)
        model?.render(gl, shader)
        matrixStack.pop()
    }

    override fun updateMesh(renderer: GuiRenderer,
                            size: Vector2d) {
        val mesh = Mesh()
        val w2 = size.x * 0.5
        val h2 = size.y * 0.5
        val w3 = max(w2 - 3.0, 0.0)
        val h3 = max(h2 - 3.0, 0.0)
        val pixelSize = renderer.pixelSize
        val w1 = w2 + pixelSize.x
        val h1 = h2 + pixelSize.y
        val w4 = w3 - pixelSize.x
        val h4 = h3 - pixelSize.y
        val section = 5.0
        renderPart(mesh, 40.0, 140.0, section, w1, h1, w2, h2, w3, h3, w4, h4)
        renderPart(mesh, 220.0, 320.0, section, w1, h1, w2, h2, w3, h3, w4, h4)
        model = mesh.finish(engine)
    }

    private fun renderPart(mesh: Mesh,
                           start: Double,
                           end: Double,
                           section: Double,
                           w1: Double,
                           h1: Double,
                           w2: Double,
                           h2: Double,
                           w3: Double,
                           h3: Double,
                           w4: Double,
                           h4: Double) {
        var cos = cos(start.toRad())
        var sin = sin(start.toRad())
        mesh.color(r.toDouble(), g.toDouble(), b.toDouble(), 0.0)
        var dir = start + section
        while (dir <= end) {
            val ncos = cos(dir.toRad())
            val nsin = sin(dir.toRad())
            mesh.vertex((ncos * w1).toFloat().toDouble(),
                    (nsin * h1).toFloat().toDouble(), 0.0)
            mesh.vertex((cos * w1).toFloat().toDouble(),
                    (sin * h1).toFloat().toDouble(), 0.0)
            mesh.color(r.toDouble(), g.toDouble(), b.toDouble(), a.toDouble())
            mesh.vertex((cos * w2).toFloat().toDouble(),
                    (sin * h2).toFloat().toDouble(), 0.0)
            mesh.vertex((ncos * w2).toFloat().toDouble(),
                    (nsin * h2).toFloat().toDouble(), 0.0)

            mesh.vertex((ncos * w2).toFloat().toDouble(),
                    (nsin * h2).toFloat().toDouble(), 0.0)
            mesh.vertex((cos * w2).toFloat().toDouble(),
                    (sin * h2).toFloat().toDouble(), 0.0)
            mesh.vertex((cos * w3).toFloat().toDouble(),
                    (sin * h3).toFloat().toDouble(), 0.0)
            mesh.vertex((ncos * w3).toFloat().toDouble(),
                    (nsin * h3).toFloat().toDouble(), 0.0)

            mesh.vertex((ncos * w3).toFloat().toDouble(),
                    (nsin * h3).toFloat().toDouble(), 0.0)
            mesh.vertex((cos * w3).toFloat().toDouble(),
                    (sin * h3).toFloat().toDouble(), 0.0)
            mesh.color(r.toDouble(), g.toDouble(), b.toDouble(), 0.0)
            mesh.vertex((cos * w4).toFloat().toDouble(),
                    (sin * h4).toFloat().toDouble(), 0.0)
            mesh.vertex((ncos * w4).toFloat().toDouble(),
                    (nsin * h4).toFloat().toDouble(), 0.0)
            cos = ncos
            sin = nsin
            dir += section
        }
    }
}
