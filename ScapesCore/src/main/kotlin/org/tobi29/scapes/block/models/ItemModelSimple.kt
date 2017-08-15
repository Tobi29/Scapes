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

package org.tobi29.scapes.block.models

import org.tobi29.scapes.block.TerrainTexture
import org.tobi29.scapes.engine.graphics.*
import org.tobi29.scapes.engine.utils.graphics.marginX
import org.tobi29.scapes.engine.utils.graphics.marginY
import org.tobi29.scapes.engine.utils.math.max

class ItemModelSimple(private val texture: TerrainTexture?,
                      private val r: Double,
                      private val g: Double,
                      private val b: Double,
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
        val texMinX = texture.marginX(0.0)
        val texMaxX = texture.marginX(1.0)
        val texMinY = texture.marginY(0.0)
        val texMaxY = texture.marginY(1.0)
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
            val pixelCountX = texture.width
            val pixelCountY = texture.height
            val pixelX = 1.0 / pixelCountX
            val pixelY = 1.0 / pixelCountY
            val halfPixel = max(pixelX, pixelY) / 2.0
            mesh.normal(0.0, 1.0, 0.0)
            mesh.texture(texMinX, texMinY)
            mesh.vertex(-0.5, halfPixel, 0.5)
            mesh.texture(texMaxX, texMinY)
            mesh.vertex(0.5, halfPixel, 0.5)
            mesh.texture(texMaxX, texMaxY)
            mesh.vertex(0.5, halfPixel, -0.5)
            mesh.texture(texMinX, texMaxY)
            mesh.vertex(-0.5, halfPixel, -0.5)
            for (x in pixelCountX - 1 downTo 0) {
                var pos = x.toDouble() / pixelCountX
                val xTex = texture.marginX(pos + 0.5 / pixelCountX)
                pos -= 0.5
                val posInv = pos + pixelX

                mesh.normal(-1.0, 0.0, 0.0)
                mesh.texture(xTex, texMinY)
                mesh.vertex(posInv, halfPixel, 0.5)
                mesh.texture(xTex, texMinY)
                mesh.vertex(posInv, -halfPixel, 0.5)
                mesh.texture(xTex, texMaxY)
                mesh.vertex(posInv, -halfPixel, -0.5)
                mesh.texture(xTex, texMaxY)
                mesh.vertex(posInv, halfPixel, -0.5)

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
            for (y in 0 until pixelCountY) {
                var pos = y.toDouble() / pixelCountY
                val yTex = texture.marginY(pos + 0.5 / pixelCountY)
                pos = 0.5 - pos
                val posInv = pos - pixelY

                mesh.normal(0.0, 0.0, -1.0)
                mesh.texture(texMaxX, yTex)
                mesh.vertex(0.5, halfPixel, posInv)
                mesh.texture(texMaxX, yTex)
                mesh.vertex(0.5, -halfPixel, posInv)
                mesh.texture(texMinX, yTex)
                mesh.vertex(-0.5, -halfPixel, posInv)
                mesh.texture(texMinX, yTex)
                mesh.vertex(-0.5, halfPixel, posInv)

                mesh.normal(0.0, 0.0, 1.0)
                mesh.texture(texMaxX, yTex)
                mesh.vertex(0.5, halfPixel, pos)
                mesh.texture(texMinX, yTex)
                mesh.vertex(-0.5, halfPixel, pos)
                mesh.texture(texMinX, yTex)
                mesh.vertex(-0.5, -halfPixel, pos)
                mesh.texture(texMaxX, yTex)
                mesh.vertex(0.5, -halfPixel, pos)
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
        return mesh.finish(texture.getTexture().engine)
    }

    override fun render(gl: GL,
                        shader: Shader) {
        if (texture == null || model == null) {
            return
        }
        texture.getTexture().bind(gl)
        gl.matrixStack.push { matrix ->
            matrix.rotate(315.0f, 0.0f, 1.0f, 0.0f)
            model.render(gl, shader)
        }
    }

    override fun renderInventory(gl: GL,
                                 shader: Shader) {
        if (texture == null || modelInventory == null) {
            return
        }
        texture.getTexture().bind(gl)
        modelInventory.render(gl, shader)
    }
}
