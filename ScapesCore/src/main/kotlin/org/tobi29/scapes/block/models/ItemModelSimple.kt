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

package org.tobi29.scapes.block.models

import org.tobi29.scapes.block.TerrainTexture
import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.graphics.Mesh
import org.tobi29.scapes.engine.graphics.Model
import org.tobi29.scapes.engine.graphics.Shader

class ItemModelSimple(private val texture: TerrainTexture?, private val r: Double, private val g: Double, private val b: Double,
                      private val a: Double) : ItemModel {
    private val model: Model?
    private val modelInventory: Model?

    init {
        model = buildVAO(false)
        modelInventory = buildVAO(true)
    }

    fun buildVAO(inventory: Boolean): Model? {
        val mesh = Mesh(false)
        if (texture == null) {
            return null
        }
        val texMinX = texture.x()
        val texMinY = texture.y()
        val texSize = texture.size()
        val texMaxX = texSize + texMinX
        val texMaxY = texSize + texMinY
        mesh.color(r, g, b, a)
        if (inventory) {
            mesh.texture(texMinX, texMinY)
            mesh.vertex(0.0, 0.0, 0.0)
            mesh.texture(texMaxX, texMinY)
            mesh.vertex(1.0, 0.0, 0.0)
            mesh.texture(texMaxX, texMaxY)
            mesh.vertex(1.0, 1.0, 0.0)
            mesh.texture(texMinX, texMaxY)
            mesh.vertex(0.0, 1.0, 0.0)
        } else {
            val pixelCount = texture.resolution().toInt()
            val pixel = 1.0 / pixelCount
            val halfPixel = pixel / 2.0
            mesh.normal(0.0, 1.0, 0.0)
            mesh.texture(texMinX, texMinY)
            mesh.vertex(-0.5, halfPixel, 0.5)
            mesh.texture(texMaxX, texMinY)
            mesh.vertex(0.5, halfPixel, 0.5)
            mesh.texture(texMaxX, texMaxY)
            mesh.vertex(0.5, halfPixel, -0.5)
            mesh.texture(texMinX, texMaxY)
            mesh.vertex(-0.5, halfPixel, -0.5)
            for (x in pixelCount - 1 downTo 0) {
                var pos = x.toDouble() / pixelCount
                var xTex = (pos + 0.5 / pixelCount) * texSize
                val yTex = xTex + texMinY
                xTex += texMinX
                pos -= 0.5
                val posInv = pos + pixel
                val pos2 = -pos
                val posInv2 = pos2 - pixel
                mesh.normal(-1.0, 0.0, 0.0)
                mesh.texture(xTex, texMinY)
                mesh.vertex(posInv, halfPixel, 0.5)
                mesh.texture(xTex, texMinY)
                mesh.vertex(posInv, -halfPixel, 0.5)
                mesh.texture(xTex, texMaxY)
                mesh.vertex(posInv, -halfPixel, -0.5)
                mesh.texture(xTex, texMaxY)
                mesh.vertex(posInv, halfPixel, -0.5)

                mesh.normal(0.0, 0.0, -1.0)
                mesh.texture(texMaxX, yTex)
                mesh.vertex(0.5, halfPixel, posInv2)
                mesh.texture(texMaxX, yTex)
                mesh.vertex(0.5, -halfPixel, posInv2)
                mesh.texture(texMinX, yTex)
                mesh.vertex(-0.5, -halfPixel, posInv2)
                mesh.texture(texMinX, yTex)
                mesh.vertex(-0.5, halfPixel, posInv2)

                mesh.normal(0.0, 0.0, 1.0)
                mesh.texture(texMaxX, yTex)
                mesh.vertex(0.5, halfPixel, pos2)
                mesh.texture(texMinX, yTex)
                mesh.vertex(-0.5, halfPixel, pos2)
                mesh.texture(texMinX, yTex)
                mesh.vertex(-0.5, -halfPixel, pos2)
                mesh.texture(texMaxX, yTex)
                mesh.vertex(0.5, -halfPixel, pos2)

                mesh.normal(1.0, 0.0, 0.0)
                mesh.texture(xTex, texMinY)
                mesh.vertex(pos, halfPixel, 0.5)
                mesh.texture(xTex, texMaxY)
                mesh.vertex(pos, halfPixel, -0.5)
                mesh.texture(xTex, texMaxY)
                mesh.vertex(pos, -halfPixel, -0.5)
                mesh.texture(xTex, texMinY)
                mesh.vertex(pos, -halfPixel, 0.5)
            }
            mesh.normal(0.0, -1.0, 0.0)
            mesh.texture(texMaxX, texMinY)
            mesh.vertex(0.5, -halfPixel, 0.5)
            mesh.texture(texMinX, texMinY)
            mesh.vertex(-0.5, -halfPixel, 0.5)
            mesh.texture(texMinX, texMaxY)
            mesh.vertex(-0.5, -halfPixel, -0.5)
            mesh.texture(texMaxX, texMaxY)
            mesh.vertex(0.5, -halfPixel, -0.5)
        }
        return mesh.finish(texture.texture().engine)
    }

    override fun render(gl: GL,
                        shader: Shader) {
        if (texture == null || model == null) {
            return
        }
        texture.texture().bind(gl)
        val matrixStack = gl.matrixStack()
        val matrix = matrixStack.push()
        matrix.rotate(315.0f, 0.0f, 1.0f, 0.0f)
        model.render(gl, shader)
        matrixStack.pop()
    }

    override fun renderInventory(gl: GL,
                                 shader: Shader) {
        if (texture == null || modelInventory == null) {
            return
        }
        texture.texture().bind(gl)
        modelInventory.render(gl, shader)
    }
}