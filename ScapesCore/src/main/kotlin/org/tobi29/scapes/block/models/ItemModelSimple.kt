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
import org.tobi29.scapes.block.marginX
import org.tobi29.scapes.block.marginY
import org.tobi29.scapes.engine.graphics.*
import org.tobi29.scapes.engine.resource.Resource
import org.tobi29.stdex.math.mix
import kotlin.math.max

class ItemModelSimple(gos: GraphicsObjectSupplier,
                      private val texture: Resource<Texture>,
                      private val pixelCountX: Int,
                      private val pixelCountY: Int,
                      private val texMinX: Double,
                      private val texMaxX: Double,
                      private val texMinY: Double,
                      private val texMaxY: Double,
                      private val r: Double,
                      private val g: Double,
                      private val b: Double,
                      private val a: Double) : ItemModel {
    private val model = buildVAO(gos, false)
    private val modelInventory = buildVAO(gos, true)

    constructor(gos: GraphicsObjectSupplier,
                texture: Texture,
                pixelCountX: Int,
                pixelCountY: Int,
                texMinX: Double,
                texMaxX: Double,
                texMinY: Double,
                texMaxY: Double,
                r: Double,
                g: Double,
                b: Double,
                a: Double) : this(gos, Resource(texture),
            pixelCountX, pixelCountY,
            texMinX, texMaxX,
            texMinY, texMaxY,
            r, g, b, a)

    constructor(texture: TerrainTexture,
                r: Double,
                g: Double,
                b: Double,
                a: Double
    ) : this(texture.getTexture().gos,
            Resource(texture.getTexture()),
            texture.width, texture.height,
            texture.marginX(0.0), texture.marginX(1.0),
            texture.marginY(0.0), texture.marginY(1.0),
            r, g, b, a)

    fun buildVAO(gos: GraphicsObjectSupplier,
                 inventory: Boolean): Model {
        val mesh = Mesh(false)
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
                val xTex = mix(texMinX, texMaxX, pos + 0.5 / pixelCountX)
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
                val yTex = mix(texMinY, texMaxY, pos + 0.5 / pixelCountY)
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
        return mesh.finish(gos)
    }

    override fun render(gl: GL,
                        shader: Shader) {
        texture.tryGet()?.let { texture ->
            texture.bind(gl)
            gl.matrixStack.push { matrix ->
                matrix.rotate(315.0f, 0.0f, 1.0f, 0.0f)
                model.render(gl, shader)
            }
        }
    }

    override fun renderInventory(gl: GL,
                                 shader: Shader) {
        texture.tryGet()?.let { texture ->
            texture.bind(gl)
            modelInventory.render(gl, shader)
        }
    }
}
