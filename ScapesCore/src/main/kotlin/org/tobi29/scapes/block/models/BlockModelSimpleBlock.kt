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

import org.tobi29.scapes.block.BlockType
import org.tobi29.scapes.block.TerrainTexture
import org.tobi29.scapes.block.TerrainTextureRegistry
import org.tobi29.scapes.chunk.data.ChunkMesh
import org.tobi29.scapes.chunk.terrain.TerrainClient
import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.graphics.Mesh
import org.tobi29.scapes.engine.graphics.Model
import org.tobi29.scapes.engine.graphics.Shader
import org.tobi29.scapes.engine.utils.graphics.marginX
import org.tobi29.scapes.engine.utils.graphics.marginY
import org.tobi29.scapes.engine.utils.math.Face

class BlockModelSimpleBlock(private val block: BlockType,
                            private val registry: TerrainTextureRegistry,
                            private val texTop: TerrainTexture?,
                            private val texBottom: TerrainTexture?,
                            private val texSide1: TerrainTexture?,
                            private val texSide2: TerrainTexture?,
                            private val texSide3: TerrainTexture?,
                            private val texSide4: TerrainTexture?,
                            private val r: Double,
                            private val g: Double,
                            private val b: Double,
                            private val a: Double) : BlockModel {
    private val model: Model
    private val modelInventory: Model

    init {
        model = buildVAO(false)
        modelInventory = buildVAO(true)
    }

    override fun addToChunkMesh(mesh: ChunkMesh,
                                terrain: TerrainClient,
                                x: Int,
                                y: Int,
                                z: Int,
                                xx: Double,
                                yy: Double,
                                zz: Double,
                                r: Double,
                                g: Double,
                                b: Double,
                                a: Double,
                                lod: Boolean) {
        addToChunkMesh(mesh, terrain, x, y, z, xx, yy, zz, texTop, texBottom,
                texSide1, texSide2, texSide3, texSide4, r, g, b, a, lod)
    }

    override fun render(gl: GL,
                        shader: Shader) {
        registry.texture.bind(gl)
        model.render(gl, shader)
    }

    override fun renderInventory(gl: GL,
                                 shader: Shader) {
        registry.texture.bind(gl)
        val matrixStack = gl.matrixStack()
        val matrix = matrixStack.push()
        matrix.translate(0.5f, 0.5f, 0.5f)
        matrix.rotate(57.5f, 1.0f, 0.0f, 0.0f)
        matrix.rotate(45.0f, 0.0f, 0.0f, 1.0f)
        modelInventory.render(gl, shader)
        matrixStack.pop()
    }

    private fun addToChunkMesh(mesh: ChunkMesh,
                               terrain: TerrainClient,
                               x: Int,
                               y: Int,
                               z: Int,
                               xx: Double,
                               yy: Double,
                               zz: Double,
                               texTop: TerrainTexture?,
                               texBottom: TerrainTexture?,
                               texSide1: TerrainTexture?,
                               texSide2: TerrainTexture?,
                               texSide3: TerrainTexture?,
                               texSide4: TerrainTexture?,
                               r: Double,
                               g: Double,
                               b: Double,
                               a: Double,
                               lod: Boolean) {
        val r2 = r * this.r
        val g2 = g * this.g
        val b2 = b * this.b
        val a2 = a * this.a
        val connectStage = block.connectStage(terrain, x, y, z)
        val x0 = x - 1
        val y0 = y - 1
        val z0 = z - 1
        val x1 = x + 1
        val y1 = y + 1
        val z1 = z + 1
        if (texTop != null && terrain.type(x, y, z1).connectStage(terrain, x, y,
                z1) < connectStage) {
            val xx1 = xx + 1.0
            val yy1 = yy + 1.0
            val zz1 = zz + 1.0
            val anim = texTop.shaderAnimation().id()
            val texMinX = texTop.marginX(0.0)
            val texMaxX = texTop.marginX(1.0)
            val texMinY = texTop.marginY(0.0)
            val texMaxY = texTop.marginY(1.0)
            mesh.addVertex(terrain, Face.UP, x.toDouble(), y.toDouble(),
                    z1.toDouble(), xx, yy, zz1, texMinX, texMinY, r2, g2, b2,
                    a2, lod, anim)
            mesh.addVertex(terrain, Face.UP, x1.toDouble(), y.toDouble(),
                    z1.toDouble(), xx1, yy, zz1, texMaxX, texMinY, r2, g2, b2,
                    a2, lod, anim)
            mesh.addVertex(terrain, Face.UP, x1.toDouble(), y1.toDouble(),
                    z1.toDouble(), xx1, yy1, zz1, texMaxX, texMaxY, r2, g2, b2,
                    a2, lod, anim)
            mesh.addVertex(terrain, Face.UP, x.toDouble(), y1.toDouble(),
                    z1.toDouble(), xx, yy1, zz1, texMinX, texMaxY, r2, g2, b2,
                    a2, lod, anim)
        }
        if (texBottom != null && terrain.type(x, y, z0).connectStage(terrain, x,
                y, z0) < connectStage) {
            val xx1 = xx + 1.0
            val yy1 = yy + 1.0
            val anim = texBottom.shaderAnimation().id()
            val texMinX = texBottom.marginX(0.0)
            val texMaxX = texBottom.marginX(1.0)
            val texMinY = texBottom.marginY(0.0)
            val texMaxY = texBottom.marginY(1.0)
            mesh.addVertex(terrain, Face.DOWN, x.toDouble(), y1.toDouble(),
                    z.toDouble(), xx, yy1, zz, texMinX, texMaxY, r2, g2, b2, a2,
                    lod, anim)
            mesh.addVertex(terrain, Face.DOWN, x1.toDouble(), y1.toDouble(),
                    z.toDouble(), xx1, yy1, zz, texMaxX, texMaxY, r2, g2, b2,
                    a2, lod, anim)
            mesh.addVertex(terrain, Face.DOWN, x1.toDouble(), y.toDouble(),
                    z.toDouble(), xx1, yy, zz, texMaxX, texMinY, r2, g2, b2, a2,
                    lod, anim)
            mesh.addVertex(terrain, Face.DOWN, x.toDouble(), y.toDouble(),
                    z.toDouble(), xx, yy, zz, texMinX, texMinY, r2, g2, b2, a2,
                    lod, anim)
        }
        if (texSide1 != null && terrain.type(x, y0, z).connectStage(terrain, x,
                y0, z) < connectStage) {
            val xx1 = xx + 1.0
            val zz1 = zz + 1.0
            val anim = texSide1.shaderAnimation().id()
            val texMinX = texSide1.marginX(0.0)
            val texMaxX = texSide1.marginX(1.0)
            val texMinY = texSide1.marginY(0.0)
            val texMaxY = texSide1.marginY(1.0)
            mesh.addVertex(terrain, Face.NORTH, x1.toDouble(), y.toDouble(),
                    z1.toDouble(), xx1, yy, zz1, texMaxX, texMinY, r2, g2, b2,
                    a2, lod, anim)
            mesh.addVertex(terrain, Face.NORTH, x.toDouble(), y.toDouble(),
                    z1.toDouble(), xx, yy, zz1, texMinX, texMinY, r2, g2, b2,
                    a2, lod, anim)
            mesh.addVertex(terrain, Face.NORTH, x.toDouble(), y.toDouble(),
                    z.toDouble(), xx, yy, zz, texMinX, texMaxY, r2, g2, b2, a2,
                    lod, anim)
            mesh.addVertex(terrain, Face.NORTH, x1.toDouble(), y.toDouble(),
                    z.toDouble(), xx1, yy, zz, texMaxX, texMaxY, r2, g2, b2, a2,
                    lod, anim)
        }
        if (texSide2 != null && terrain.type(x1, y, z).connectStage(terrain, x1,
                y, z) < connectStage) {
            val xx1 = xx + 1.0
            val yy1 = yy + 1.0
            val zz1 = zz + 1.0
            val anim = texSide2.shaderAnimation().id()
            val texMinX = texSide2.marginX(0.0)
            val texMaxX = texSide2.marginX(1.0)
            val texMinY = texSide2.marginY(0.0)
            val texMaxY = texSide2.marginY(1.0)
            mesh.addVertex(terrain, Face.EAST, x1.toDouble(), y1.toDouble(),
                    z1.toDouble(), xx1, yy1, zz1, texMaxX, texMinY, r2, g2, b2,
                    a2, lod, anim)
            mesh.addVertex(terrain, Face.EAST, x1.toDouble(), y.toDouble(),
                    z1.toDouble(), xx1, yy, zz1, texMinX, texMinY, r2, g2, b2,
                    a2, lod, anim)
            mesh.addVertex(terrain, Face.EAST, x1.toDouble(), y.toDouble(),
                    z.toDouble(), xx1, yy, zz, texMinX, texMaxY, r2, g2, b2, a2,
                    lod, anim)
            mesh.addVertex(terrain, Face.EAST, x1.toDouble(), y1.toDouble(),
                    z.toDouble(), xx1, yy1, zz, texMaxX, texMaxY, r2, g2, b2,
                    a2, lod, anim)
        }
        if (texSide3 != null && terrain.type(x, y1, z).connectStage(terrain, x,
                y1, z) < connectStage) {
            val xx1 = xx + 1.0
            val yy1 = yy + 1.0
            val zz1 = zz + 1.0
            val anim = texSide3.shaderAnimation().id()
            val texMinX = texSide3.marginX(0.0)
            val texMaxX = texSide3.marginX(1.0)
            val texMinY = texSide3.marginY(0.0)
            val texMaxY = texSide3.marginY(1.0)
            mesh.addVertex(terrain, Face.SOUTH, x1.toDouble(), y1.toDouble(),
                    z.toDouble(), xx1, yy1, zz, texMinX, texMaxY, r2, g2, b2,
                    a2, lod, anim)
            mesh.addVertex(terrain, Face.SOUTH, x.toDouble(), y1.toDouble(),
                    z.toDouble(), xx, yy1, zz, texMaxX, texMaxY, r2, g2, b2, a2,
                    lod, anim)
            mesh.addVertex(terrain, Face.SOUTH, x.toDouble(), y1.toDouble(),
                    z1.toDouble(), xx, yy1, zz1, texMaxX, texMinY, r2, g2, b2,
                    a2, lod, anim)
            mesh.addVertex(terrain, Face.SOUTH, x1.toDouble(), y1.toDouble(),
                    z1.toDouble(), xx1, yy1, zz1, texMinX, texMinY, r2, g2, b2,
                    a2, lod, anim)
        }
        if (texSide4 != null && terrain.type(x0, y, z).connectStage(terrain, x0,
                y, z) < connectStage) {
            val yy1 = yy + 1.0
            val zz1 = zz + 1.0
            val anim = texSide4.shaderAnimation().id()
            val texMinX = texSide4.marginX(0.0)
            val texMaxX = texSide4.marginX(1.0)
            val texMinY = texSide4.marginY(0.0)
            val texMaxY = texSide4.marginY(1.0)
            mesh.addVertex(terrain, Face.WEST, x.toDouble(), y1.toDouble(),
                    z.toDouble(), xx, yy1, zz, texMinX, texMaxY, r2, g2, b2, a2,
                    lod, anim)
            mesh.addVertex(terrain, Face.WEST, x.toDouble(), y.toDouble(),
                    z.toDouble(), xx, yy, zz, texMaxX, texMaxY, r2, g2, b2, a2,
                    lod, anim)
            mesh.addVertex(terrain, Face.WEST, x.toDouble(), y.toDouble(),
                    z1.toDouble(), xx, yy, zz1, texMaxX, texMinY, r2, g2, b2,
                    a2, lod, anim)
            mesh.addVertex(terrain, Face.WEST, x.toDouble(), y1.toDouble(),
                    z1.toDouble(), xx, yy1, zz1, texMinX, texMinY, r2, g2, b2,
                    a2, lod, anim)
        }
    }

    private fun buildVAO(inventory: Boolean): Model {
        val mesh = Mesh(false)
        buildVAO(mesh, inventory, texTop, texBottom, texSide1, texSide2,
                texSide3, texSide4)
        return mesh.finish(registry.engine)
    }

    private fun buildVAO(mesh: Mesh,
                         inventory: Boolean,
                         texTop: TerrainTexture?,
                         texBottom: TerrainTexture?,
                         texSide1: TerrainTexture?,
                         texSide2: TerrainTexture?,
                         texSide3: TerrainTexture?,
                         texSide4: TerrainTexture?) {
        mesh.color(r, g, b, a)
        if (texTop != null) {
            val texMinX = texTop.marginX(0.0)
            val texMaxX = texTop.marginX(1.0)
            val texMinY = texTop.marginY(0.0)
            val texMaxY = texTop.marginY(1.0)
            mesh.normal(0.0, 0.0, 1.0)
            mesh.texture(texMinX, texMinY)
            mesh.vertex(-0.5, -0.5, 0.5)
            mesh.texture(texMaxX, texMinY)
            mesh.vertex(0.5, -0.5, 0.5)
            mesh.texture(texMaxX, texMaxY)
            mesh.vertex(0.5, 0.5, 0.5)
            mesh.texture(texMinX, texMaxY)
            mesh.vertex(-0.5, 0.5, 0.5)
        }
        if (texSide2 != null) {
            if (inventory) {
                mesh.color(r * 0.7, g * 0.7, b * 0.7, a * 1.0)
            }
            val texMinX = texSide2.marginX(0.0)
            val texMaxX = texSide2.marginX(1.0)
            val texMinY = texSide2.marginY(0.0)
            val texMaxY = texSide2.marginY(1.0)
            mesh.normal(1.0, 0.0, 0.0)
            mesh.texture(texMinX, texMinY)
            mesh.vertex(0.5, 0.5, 0.5)
            mesh.texture(texMaxX, texMinY)
            mesh.vertex(0.5, -0.5, 0.5)
            mesh.texture(texMaxX, texMaxY)
            mesh.vertex(0.5, -0.5, -0.5)
            mesh.texture(texMinX, texMaxY)
            mesh.vertex(0.5, 0.5, -0.5)
        }
        if (texSide3 != null) {
            if (inventory) {
                mesh.color(r * 0.8, g * 0.8, b * 0.8, a)
            }
            val texMinX = texSide3.marginX(0.0)
            val texMaxX = texSide3.marginX(1.0)
            val texMinY = texSide3.marginY(0.0)
            val texMaxY = texSide3.marginY(1.0)
            mesh.normal(0.0, 1.0, 0.0)
            mesh.texture(texMaxX, texMaxY)
            mesh.vertex(0.5, 0.5, -0.5)
            mesh.texture(texMinX, texMaxY)
            mesh.vertex(-0.5, 0.5, -0.5)
            mesh.texture(texMinX, texMinY)
            mesh.vertex(-0.5, 0.5, 0.5)
            mesh.texture(texMaxX, texMinY)
            mesh.vertex(0.5, 0.5, 0.5)
        }
        if (!inventory) {
            if (texSide4 != null) {
                val texMinX = texSide4.marginX(0.0)
                val texMaxX = texSide4.marginX(1.0)
                val texMinY = texSide4.marginY(0.0)
                val texMaxY = texSide4.marginY(1.0)
                mesh.normal(-1.0, 0.0, 0.0)
                mesh.texture(texMinX, texMaxY)
                mesh.vertex(-0.5, 0.5, -0.5)
                mesh.texture(texMaxX, texMaxY)
                mesh.vertex(-0.5, -0.5, -0.5)
                mesh.texture(texMaxX, texMinY)
                mesh.vertex(-0.5, -0.5, 0.5)
                mesh.texture(texMinX, texMinY)
                mesh.vertex(-0.5, 0.5, 0.5)
            }
            if (texSide1 != null) {
                val texMinX = texSide1.marginX(0.0)
                val texMaxX = texSide1.marginX(1.0)
                val texMinY = texSide1.marginY(0.0)
                val texMaxY = texSide1.marginY(1.0)
                mesh.normal(0.0, -1.0, 0.0)
                mesh.texture(texMinX, texMinY)
                mesh.vertex(0.5, -0.5, 0.5)
                mesh.texture(texMaxX, texMinY)
                mesh.vertex(-0.5, -0.5, 0.5)
                mesh.texture(texMaxX, texMaxY)
                mesh.vertex(-0.5, -0.5, -0.5)
                mesh.texture(texMinX, texMaxY)
                mesh.vertex(0.5, -0.5, -0.5)
            }
            if (texBottom != null) {
                val texMinX = texBottom.marginX(0.0)
                val texMaxX = texBottom.marginX(1.0)
                val texMinY = texBottom.marginY(0.0)
                val texMaxY = texBottom.marginY(1.0)
                mesh.normal(0.0, 0.0, -1.0)
                mesh.texture(texMinX, texMaxY)
                mesh.vertex(-0.5, 0.5, -0.5)
                mesh.texture(texMaxX, texMaxY)
                mesh.vertex(0.5, 0.5, -0.5)
                mesh.texture(texMaxX, texMinY)
                mesh.vertex(0.5, -0.5, -0.5)
                mesh.texture(texMinX, texMinY)
                mesh.vertex(-0.5, -0.5, -0.5)
            }
        }
    }
}
