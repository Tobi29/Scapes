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

import org.tobi29.scapes.block.*
import org.tobi29.scapes.chunk.ChunkMesh
import org.tobi29.scapes.chunk.terrain.Terrain
import org.tobi29.scapes.chunk.terrain.TerrainClient
import org.tobi29.scapes.engine.graphics.*
import org.tobi29.math.Face
import kotlin.math.max

class BlockModelLiquid(private val block: BlockType,
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
                       private val a: Double,
                       private val min: Double,
                       max: Double,
                       private val offset: Int,
                       steps: Int) : BlockModel {
    private val diff = max - min
    private val steps = steps.toDouble()
    private val model = buildVAO(false)
    private val modelInventory = buildVAO(true)

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
        val r2 = r * this.r
        val g2 = g * this.g
        val b2 = b * this.b
        val a2 = a * this.a
        val connectStage = block.connectStage(terrain, x, y, z)
        val top = terrain.type(x, y, z + 1)
        var height00: Double
        var height01: Double
        var height11: Double
        var height10: Double
        val static00: Boolean
        val static01: Boolean
        val static11: Boolean
        val static10: Boolean
        val x1 = x + 1
        val y1 = y + 1
        var flag = top !== block
        val noAnim = ShaderAnimation.NONE.id()
        if (flag) {
            height00 = calcHeight(x, y, z, terrain, block, offset,
                    steps) * diff + min
            height01 = calcHeight(x, y + 1, z, terrain, block, offset,
                    steps) * diff + min
            height11 = calcHeight(x + 1, y + 1, z, terrain, block, offset,
                    steps) * diff + min
            height10 = calcHeight(x + 1, y, z, terrain, block, offset,
                    steps) * diff + min
            static00 = height00 > 1.5
            static01 = height01 > 1.5
            static11 = height11 > 1.5
            static10 = height10 > 1.5
            if (static00) {
                height00 -= 2.0
            }
            if (static01) {
                height01 -= 2.0
            }
            if (static11) {
                height11 -= 2.0
            }
            if (static10) {
                height10 -= 2.0
            }
            if (height00 >= 1.0 && height01 >= 1.0 && height11 >= 1.0 &&
                    height10 >= 1.0) {
                flag = top.connectStage(terrain, x, y, z + 1) < connectStage
            }
            if (flag) {
                if (texTop != null) {
                    val xx1 = xx + 1.0
                    val yy1 = yy + 1.0
                    val texMinX = texTop.marginX(0.0)
                    val texMaxX = texTop.marginX(1.0)
                    val texMinY = texTop.marginY(0.0)
                    val texMaxY = texTop.marginY(1.0)
                    val anim = texTop.shaderAnimation().id()
                    mesh.addQuad(terrain, Face.UP,
                            x.toDouble(), y.toDouble(), z + height00,
                            x1.toDouble(), y.toDouble(), z + height10,
                            x1.toDouble(), y1.toDouble(), z + height11,
                            x.toDouble(), y1.toDouble(), z + height01,
                            xx, yy, zz + height00,
                            xx1, yy, zz + height10,
                            xx1, yy1, zz + height11,
                            xx, yy1, zz + height01,
                            texMaxX, texMinY,
                            texMinX, texMinY,
                            texMinX, texMaxY,
                            texMaxX, texMaxY,
                            r2, g2, b2, a2, lod,
                            if (static00) noAnim else anim,
                            if (static10) noAnim else anim,
                            if (static11) noAnim else anim,
                            if (static01) noAnim else anim)
                }
            }
        } else {
            height00 = diff + min
            height10 = height00
            height11 = height00
            height01 = height00
            static00 = true
            static10 = true
            static11 = true
            static01 = true
        }
        var other: BlockType
        other = terrain.type(x, y, z - 1)
        if (other != block && other.connectStage(terrain, x, y,
                z - 1) <= connectStage) {
            if (texBottom != null) {
                val xx1 = xx + 1.0
                val yy1 = yy + 1.0
                val texMinX = texBottom.marginX(0.0)
                val texMaxX = texBottom.marginX(1.0)
                val texMinY = texBottom.marginY(0.0)
                val texMaxY = texBottom.marginY(1.0)
                mesh.addQuad(terrain, Face.DOWN,
                        x.toDouble(), y1.toDouble(), z.toDouble(),
                        x1.toDouble(), y1.toDouble(), z.toDouble(),
                        x1.toDouble(), y.toDouble(), z.toDouble(),
                        x.toDouble(), y.toDouble(), z.toDouble(),
                        xx, yy1, zz,
                        xx1, yy1, zz,
                        xx1, yy, zz,
                        xx, yy, zz,
                        texMaxX, texMinY,
                        texMinX, texMinY,
                        texMinX, texMaxY,
                        texMaxX, texMaxY,
                        r2, g2, b2, a2, lod, noAnim)
            }
        }
        other = terrain.type(x, y - 1, z)
        if (other != block && other.connectStage(terrain, x, y - 1,
                z) <= connectStage) {
            if (texSide1 != null) {
                val xx1 = xx + 1.0
                val anim = texSide1.shaderAnimation().id()
                val texMinX = texSide1.marginX(0.0)
                val texMaxX = texSide1.marginX(1.0)
                val texMinY00 = texSide1.marginY(max(1.0 - height00, 0.0))
                val texMinY10 = texSide1.marginY(max(1.0 - height10, 0.0))
                val texMaxY = texSide1.marginY(1.0)
                mesh.addQuad(terrain, Face.NORTH,
                        x1.toDouble(), y.toDouble(), z + height10,
                        x.toDouble(), y.toDouble(), z + height00,
                        x.toDouble(), y.toDouble(), z.toDouble(),
                        x1.toDouble(), y.toDouble(), z.toDouble(),
                        xx1, yy, zz + height10,
                        xx, yy, zz + height00,
                        xx, yy, zz,
                        xx1, yy, zz,
                        texMaxX, texMinY10,
                        texMinX, texMinY00,
                        texMinX, texMaxY,
                        texMaxX, texMaxY,
                        r2, g2, b2, a2, lod,
                        if (static10) noAnim else anim,
                        if (static00) noAnim else anim,
                        noAnim,
                        noAnim)
            }
        }
        other = terrain.type(x + 1, y, z)
        if (other != block && other.connectStage(terrain, x + 1, y,
                z) <= connectStage) {
            if (texSide2 != null) {
                val xx1 = xx + 1.0
                val yy1 = yy + 1.0
                val anim = texSide2.shaderAnimation().id()
                val texMinX = texSide2.marginX(0.0)
                val texMaxX = texSide2.marginX(1.0)
                val texMinY10 = texSide2.marginY(max(1.0 - height10, 0.0))
                val texMinY11 = texSide2.marginY(max(1.0 - height11, 0.0))
                val texMaxY = texSide2.marginY(1.0)
                mesh.addQuad(terrain, Face.EAST,
                        x1.toDouble(), y1.toDouble(), z + height11,
                        x1.toDouble(), y.toDouble(), z + height10,
                        x1.toDouble(), y.toDouble(), z.toDouble(),
                        x1.toDouble(), y1.toDouble(), z.toDouble(),
                        xx1, yy1, zz + height11,
                        xx1, yy, zz + height10,
                        xx1, yy, zz,
                        xx1, yy1, zz,
                        texMaxX, texMinY11,
                        texMinX, texMinY10,
                        texMinX, texMaxY,
                        texMaxX, texMaxY,
                        r2, g2, b2, a2, lod,
                        if (static11) noAnim else anim,
                        if (static10) noAnim else anim,
                        noAnim,
                        noAnim)
            }
        }
        other = terrain.type(x, y + 1, z)
        if (other != block && other.connectStage(terrain, x, y + 1,
                z) <= connectStage) {
            if (texSide3 != null) {
                val xx1 = xx + 1.0
                val yy1 = yy + 1.0
                val anim = texSide3.shaderAnimation().id()
                val texMinX = texSide3.marginX(0.0)
                val texMaxX = texSide3.marginX(1.0)
                val texMinY01 = texSide3.marginY(max(1.0 - height01, 0.0))
                val texMinY11 = texSide3.marginY(max(1.0 - height11, 0.0))
                val texMaxY = texSide3.marginY(1.0)
                mesh.addQuad(terrain, Face.SOUTH,
                        x1.toDouble(), y1.toDouble(), z.toDouble(),
                        x.toDouble(), y1.toDouble(), z.toDouble(),
                        x.toDouble(), y1.toDouble(), z + height01,
                        x1.toDouble(), y1.toDouble(), z + height11,
                        xx1, yy1, zz,
                        xx, yy1, zz,
                        xx, yy1, zz + height01,
                        xx1, yy1, zz + height11,
                        texMinX, texMaxY,
                        texMaxX, texMaxY,
                        texMaxX, texMinY01,
                        texMinX, texMinY11,
                        r2, g2, b2, a2, lod,
                        noAnim,
                        noAnim,
                        if (static01) noAnim else anim,
                        if (static11) noAnim else anim)
            }
        }
        other = terrain.type(x - 1, y, z)
        if (other != block && other.connectStage(terrain, x - 1, y,
                z) <= connectStage) {
            if (texSide4 != null) {
                val yy1 = yy + 1.0
                val anim = texSide4.shaderAnimation().id()
                val texMinX = texSide4.marginX(0.0)
                val texMaxX = texSide4.marginX(1.0)
                val texMinY00 = texSide4.marginY(max(1.0 - height00, 0.0))
                val texMinY01 = texSide4.marginY(max(1.0 - height01, 0.0))
                val texMaxY = texSide4.marginY(1.0)
                mesh.addQuad(terrain, Face.WEST,
                        x.toDouble(), y1.toDouble(), z.toDouble(),
                        x.toDouble(), y.toDouble(), z.toDouble(),
                        x.toDouble(), y.toDouble(), z + height00,
                        x.toDouble(), y1.toDouble(), z + height01,
                        xx, yy1, zz,
                        xx, yy, zz,
                        xx, yy, zz + height00,
                        xx, yy1, zz + height01,
                        texMinX, texMaxY,
                        texMaxX, texMaxY,
                        texMaxX, texMinY00,
                        texMinX, texMinY01,
                        r2, g2, b2, a2, lod,
                        noAnim,
                        noAnim,
                        if (static00) noAnim else anim,
                        if (static01) noAnim else anim)
            }
        }
    }

    override fun render(gl: GL,
                        shader: Shader) {
        registry.texture.bind(gl)
        model.render(gl, shader)
    }

    override fun renderInventory(gl: GL,
                                 shader: Shader) {
        registry.texture.bind(gl)
        gl.matrixStack.push { matrix ->
            matrix.translate(0.5f, 0.5f, 0.5f)
            matrix.rotate(57.5f, 1.0f, 0.0f, 0.0f)
            matrix.rotate(45.0f, 0.0f, 0.0f, 1.0f)
            modelInventory.render(gl, shader)
        }
    }

    private fun buildVAO(inventory: Boolean): Model {
        val mesh = Mesh(false)
        buildVAO(mesh, inventory)
        return mesh.finish(registry.texture.gos)
    }

    private fun buildVAO(mesh: Mesh,
                         inventory: Boolean) {
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

    companion object {
        private fun calcHeight(x: Int,
                               y: Int,
                               z: Int,
                               terrain: Terrain,
                               block: BlockType,
                               offset: Int,
                               steps: Double): Double {
            if (terrain.type(x, y, z + 1) == block ||
                    terrain.type(x - 1, y, z + 1) == block ||
                    terrain.type(x - 1, y - 1, z + 1) == block ||
                    terrain.type(x, y - 1, z + 1) == block) {
                return 3.0
            }
            val block1 = terrain.block(x, y, z)
            val other1 = terrain.type(block1) == block
            val block2 = terrain.block(x - 1, y, z)
            val other2 = terrain.type(block2) == block
            val block3 = terrain.block(x - 1, y - 1, z)
            val other3 = terrain.type(block3) == block
            val block4 = terrain.block(x, y - 1, z)
            val other4 = terrain.type(block4) == block
            if (!other1 || !other2 || !other3 || !other4) {
                val bottom1 = terrain.type(x, y, z - 1) == block
                val bottom2 = terrain.type(x - 1, y, z - 1) == block
                val bottom3 = terrain.type(x - 1, y - 1, z - 1) == block
                val bottom4 = terrain.type(x, y - 1, z - 1) == block
                if (bottom1 && !other1 || bottom2 && !other2 ||
                        bottom3 && !other3 || bottom4 && !other4) {
                    return 2.0
                }
            }
            var height = 0.0
            var heights = 0
            if (other1) {
                height += 1.0 - max(0, terrain.data(block1) - offset) / steps
                heights++
            }
            if (other2) {
                height += 1.0 - max(0, terrain.data(block2) - offset) / steps
                heights++
            }
            if (other3) {
                height += 1.0 - max(0, terrain.data(block3) - offset) / steps
                heights++
            }
            if (other4) {
                height += 1.0 - max(0, terrain.data(block4) - offset) / steps
                heights++
            }
            if (heights == 0) {
                return 0.0
            }
            return height / heights
        }
    }
}
