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

import org.tobi29.scapes.block.ShaderAnimation
import org.tobi29.scapes.block.TerrainTexture
import org.tobi29.scapes.block.TerrainTextureRegistry
import org.tobi29.scapes.chunk.ChunkMesh
import org.tobi29.scapes.chunk.terrain.TerrainClient
import org.tobi29.scapes.engine.graphics.*
import org.tobi29.scapes.engine.utils.graphics.marginX
import org.tobi29.scapes.engine.utils.graphics.marginY
import org.tobi29.scapes.engine.utils.math.Face
import org.tobi29.scapes.engine.utils.math.cos
import org.tobi29.scapes.engine.utils.math.sin
import org.tobi29.scapes.engine.utils.math.toRad
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.engine.utils.math.vector.plus
import org.tobi29.scapes.engine.utils.math.vector.times

class BlockModelComplex(private val registry: TerrainTextureRegistry,
                        private val shapes: List<BlockModelComplex.Shape>,
                        scale: Double) : BlockModel {
    private val model: Model
    private val modelInventory: Model

    init {
        shapes.forEach { shape ->
            shape.scale(scale)
            shape.center()
        }
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
        shapes.forEach {
            it.addToChunkMesh(mesh, terrain, x.toDouble(), y.toDouble(),
                    z.toDouble(), xx, yy, zz, r, g, b, a, lod)
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
        shapes.forEach { it.addToMesh(mesh, inventory) }
    }

    abstract class Shape {
        protected var minX: Double = 0.0
        protected var minY: Double = 0.0
        protected var minZ: Double = 0.0
        protected var maxX: Double = 0.0
        protected var maxY: Double = 0.0
        protected var maxZ: Double = 0.0
        protected var r: Double = 0.0
        protected var g: Double = 0.0
        protected var b: Double = 0.0
        protected var a: Double = 0.0
        protected lateinit var lll: Vector3d
        protected lateinit var hll: Vector3d
        protected lateinit var hhl: Vector3d
        protected lateinit var lhl: Vector3d
        protected lateinit var llh: Vector3d
        protected lateinit var hlh: Vector3d
        protected lateinit var hhh: Vector3d
        protected lateinit var lhh: Vector3d
        protected lateinit var tlll: Vector3d
        protected lateinit var thll: Vector3d
        protected lateinit var thhl: Vector3d
        protected lateinit var tlhl: Vector3d
        protected lateinit var tllh: Vector3d
        protected lateinit var thlh: Vector3d
        protected lateinit var thhh: Vector3d
        protected lateinit var tlhh: Vector3d

        private fun rotateX(a: Vector3d,
                            cos: Double,
                            sin: Double): Vector3d {
            val x = a.x
            var y = a.y
            var z = a.z
            val yy = y
            y = yy * cos - z * sin
            z = yy * sin + z * cos
            return Vector3d(x, y, z)
        }

        private fun rotateY(a: Vector3d,
                            cos: Double,
                            sin: Double): Vector3d {
            var x = a.x
            val y = a.y
            var z = a.z
            val xx = x
            x = xx * cos - z * sin
            z = xx * sin + z * cos
            return Vector3d(x, y, z)
        }

        private fun rotateZ(a: Vector3d,
                            cos: Double,
                            sin: Double): Vector3d {
            var x = a.x
            var y = a.y
            val z = a.z
            val xx = x
            x = xx * cos - y * sin
            y = xx * sin + y * cos
            return Vector3d(x, y, z)
        }

        fun scale(scale: Double) {
            lll = lll.times(scale)
            hll = hll.times(scale)
            hhl = hhl.times(scale)
            lhl = lhl.times(scale)
            llh = llh.times(scale)
            hlh = hlh.times(scale)
            hhh = hhh.times(scale)
            lhh = lhh.times(scale)
            minX *= scale
            minY *= scale
            minZ *= scale
            maxX *= scale
            maxY *= scale
            maxZ *= scale
        }

        fun center() {
            tlll = lll.plus(0.5)
            thll = hll.plus(0.5)
            thhl = hhl.plus(0.5)
            tlhl = lhl.plus(0.5)
            tllh = llh.plus(0.5)
            thlh = hlh.plus(0.5)
            thhh = hhh.plus(0.5)
            tlhh = lhh.plus(0.5)
            minX += 0.5f
            minY += 0.5f
            minZ += 0.5f
            maxX += 0.5f
            maxY += 0.5f
            maxZ += 0.5f
            val z = minZ
            minZ = 1.0 - maxZ
            maxZ = 1.0 - z
        }

        fun translate(x: Double,
                      y: Double,
                      z: Double) {
            lll = lll.plus(Vector3d(x, y, z))
            hll = hll.plus(Vector3d(x, y, z))
            hhl = hhl.plus(Vector3d(x, y, z))
            lhl = lhl.plus(Vector3d(x, y, z))
            llh = llh.plus(Vector3d(x, y, z))
            hlh = hlh.plus(Vector3d(x, y, z))
            hhh = hhh.plus(Vector3d(x, y, z))
            lhh = lhh.plus(Vector3d(x, y, z))
        }

        fun rotateX(dir: Double) {
            val dirRad = dir.toRad()
            val cos = cos(dirRad)
            val sin = sin(dirRad)
            lll = rotateX(lll, cos, sin)
            hll = rotateX(hll, cos, sin)
            hhl = rotateX(hhl, cos, sin)
            lhl = rotateX(lhl, cos, sin)
            llh = rotateX(llh, cos, sin)
            hlh = rotateX(hlh, cos, sin)
            hhh = rotateX(hhh, cos, sin)
            lhh = rotateX(lhh, cos, sin)
        }

        fun rotateY(dir: Double) {
            val dirRad = dir.toRad()
            val cos = cos(dirRad)
            val sin = sin(dirRad)
            lll = rotateY(lll, cos, sin)
            hll = rotateY(hll, cos, sin)
            hhl = rotateY(hhl, cos, sin)
            lhl = rotateY(lhl, cos, sin)
            llh = rotateY(llh, cos, sin)
            hlh = rotateY(hlh, cos, sin)
            hhh = rotateY(hhh, cos, sin)
            lhh = rotateY(lhh, cos, sin)
        }

        fun rotateZ(dir: Double) {
            val dirRad = dir.toRad()
            val cos = cos(dirRad)
            val sin = sin(dirRad)
            lll = rotateZ(lll, cos, sin)
            hll = rotateZ(hll, cos, sin)
            hhl = rotateZ(hhl, cos, sin)
            lhl = rotateZ(lhl, cos, sin)
            llh = rotateZ(llh, cos, sin)
            hlh = rotateZ(hlh, cos, sin)
            hhh = rotateZ(hhh, cos, sin)
            lhh = rotateZ(lhh, cos, sin)
        }

        abstract fun addToChunkMesh(mesh: ChunkMesh,
                                    terrain: TerrainClient,
                                    x: Double,
                                    y: Double,
                                    z: Double,
                                    xx: Double,
                                    yy: Double,
                                    zz: Double,
                                    r: Double,
                                    g: Double,
                                    b: Double,
                                    a: Double,
                                    lod: Boolean)

        abstract fun addToMesh(mesh: Mesh,
                               inventory: Boolean)
    }

    class ShapeBox(private val texTop: TerrainTexture?,
                   private val texBottom: TerrainTexture?,
                   private val texSide1: TerrainTexture?,
                   private val texSide2: TerrainTexture?,
                   private val texSide3: TerrainTexture?,
                   private val texSide4: TerrainTexture?,
                   minX: Double,
                   minY: Double,
                   minZ: Double,
                   maxX: Double,
                   maxY: Double,
                   maxZ: Double,
                   r: Double,
                   g: Double,
                   b: Double,
                   a: Double) : Shape() {

        init {
            this.minX = minX
            this.minY = minY
            this.minZ = minZ
            this.maxX = maxX
            this.maxY = maxY
            this.maxZ = maxZ
            lll = Vector3d(minX, minY, minZ)
            hll = Vector3d(maxX, minY, minZ)
            hhl = Vector3d(maxX, maxY, minZ)
            lhl = Vector3d(minX, maxY, minZ)
            llh = Vector3d(minX, minY, maxZ)
            hlh = Vector3d(maxX, minY, maxZ)
            hhh = Vector3d(maxX, maxY, maxZ)
            lhh = Vector3d(minX, maxY, maxZ)
            this.r = r
            this.g = g
            this.b = b
            this.a = a
        }

        override fun addToChunkMesh(mesh: ChunkMesh,
                                    terrain: TerrainClient,
                                    x: Double,
                                    y: Double,
                                    z: Double,
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
            if (texTop != null) {
                val anim = texTop.shaderAnimation().id()
                val texMinX = texTop.marginX(minX)
                val texMaxX = texTop.marginX(maxX)
                val texMinY = texTop.marginY(minY)
                val texMaxY = texTop.marginY(maxY)
                mesh.addVertex(terrain, Face.UP, x + tllh.x,
                        y + tllh.y, z + tllh.z,
                        xx + tllh.x, yy + tllh.y,
                        zz + tllh.z, texMinX,
                        texMinY, r2, g2, b2, a2, lod, anim)
                mesh.addVertex(terrain, Face.UP, x + thlh.x,
                        y + thlh.y, z + thlh.z,
                        xx + thlh.x, yy + thlh.y,
                        zz + thlh.z, texMaxX,
                        texMinY, r2, g2, b2, a2, lod, anim)
                mesh.addVertex(terrain, Face.UP, x + thhh.x,
                        y + thhh.y, z + thhh.z,
                        xx + thhh.x, yy + thhh.y,
                        zz + thhh.z, texMaxX,
                        texMaxY, r2, g2, b2, a2, lod, anim)
                mesh.addVertex(terrain, Face.UP, x + tlhh.x,
                        y + tlhh.y, z + tlhh.z,
                        xx + tlhh.x, yy + tlhh.y,
                        zz + tlhh.z, texMinX,
                        texMaxY, r2, g2, b2, a2, lod, anim)
            }
            if (texBottom != null) {
                val anim = texBottom.shaderAnimation().id()
                val texMinX = texBottom.marginX(minX)
                val texMaxX = texBottom.marginX(maxX)
                val texMinY = texBottom.marginY(minY)
                val texMaxY = texBottom.marginY(maxY)
                mesh.addVertex(terrain, Face.DOWN, x + tlhl.x,
                        y + tlhl.y, z + tlhl.z,
                        xx + tlhl.x, yy + tlhl.y,
                        zz + tlhl.z, texMinX,
                        texMaxY, r2, g2, b2, a2, lod, anim)
                mesh.addVertex(terrain, Face.DOWN, x + thhl.x,
                        y + thhl.y, z + thhl.z,
                        xx + thhl.x, yy + thhl.y,
                        zz + thhl.z, texMaxX,
                        texMaxY, r2, g2, b2, a2, lod, anim)
                mesh.addVertex(terrain, Face.DOWN, x + thll.x,
                        y + thll.y, z + thll.z,
                        xx + thll.x, yy + thll.y,
                        zz + thll.z, texMaxX,
                        texMinY, r2, g2, b2, a2, lod, anim)
                mesh.addVertex(terrain, Face.DOWN, x + tlll.x,
                        y + tlll.y, z + tlll.z,
                        xx + tlll.x, yy + tlll.y,
                        zz + tlll.z, texMinX,
                        texMinY, r2, g2, b2, a2, lod, anim)
            }
            if (texSide1 != null) {
                val anim = texSide1.shaderAnimation().id()
                val texMinX = texSide1.marginX(minX)
                val texMaxX = texSide1.marginX(maxX)
                val texMinY = texSide1.marginY(minZ)
                val texMaxY = texSide1.marginY(maxZ)
                mesh.addVertex(terrain, Face.NORTH, x + thlh.x,
                        y + thlh.y, z + thlh.z,
                        xx + thlh.x, yy + thlh.y,
                        zz + thlh.z, texMaxX,
                        texMinY, r2, g2, b2, a2, lod, anim)
                mesh.addVertex(terrain, Face.NORTH, x + tllh.x,
                        y + tllh.y, z + tllh.z,
                        xx + tllh.x, yy + tllh.y,
                        zz + tllh.z, texMinX,
                        texMinY, r2, g2, b2, a2, lod, anim)
                mesh.addVertex(terrain, Face.NORTH, x + tlll.x,
                        y + tlll.y, z + tlll.z,
                        xx + tlll.x, yy + tlll.y,
                        zz + tlll.z, texMinX,
                        texMaxY, r2, g2, b2, a2, lod, anim)
                mesh.addVertex(terrain, Face.NORTH, x + thll.x,
                        y + thll.y, z + thll.z,
                        xx + thll.x, yy + thll.y,
                        zz + thll.z, texMaxX,
                        texMaxY, r2, g2, b2, a2, lod, anim)
            }
            if (texSide2 != null) {
                val anim = texSide2.shaderAnimation().id()
                val texMinX = texSide2.marginX(minY)
                val texMaxX = texSide2.marginX(maxY)
                val texMinY = texSide2.marginY(minZ)
                val texMaxY = texSide2.marginY(maxZ)
                mesh.addVertex(terrain, Face.EAST, x + thhh.x,
                        y + thhh.y, z + thhh.z,
                        xx + thhh.x, yy + thhh.y,
                        zz + thhh.z, texMaxX,
                        texMinY, r2, g2, b2, a2, lod, anim)
                mesh.addVertex(terrain, Face.EAST, x + thlh.x,
                        y + thlh.y, z + thlh.z,
                        xx + thlh.x, yy + thlh.y,
                        zz + thlh.z, texMinX,
                        texMinY, r2, g2, b2, a2, lod, anim)
                mesh.addVertex(terrain, Face.EAST, x + thll.x,
                        y + thll.y, z + thll.z,
                        xx + thll.x, yy + thll.y,
                        zz + thll.z, texMinX,
                        texMaxY, r2, g2, b2, a2, lod, anim)
                mesh.addVertex(terrain, Face.EAST, x + thhl.x,
                        y + thhl.y, z + thhl.z,
                        xx + thhl.x, yy + thhl.y,
                        zz + thhl.z, texMaxX,
                        texMaxY, r2, g2, b2, a2, lod, anim)
            }
            if (texSide3 != null) {
                val anim = texSide3.shaderAnimation().id()
                val texMinX = texSide3.marginX(minX)
                val texMaxX = texSide3.marginX(maxX)
                val texMinZ = texSide3.marginY(minZ)
                val texMaxZ = texSide3.marginY(maxZ)
                mesh.addVertex(terrain, Face.SOUTH, x + thhl.x,
                        y + thhl.y, z + thhl.z,
                        xx + thhl.x, yy + thhl.y,
                        zz + thhl.z, texMinX,
                        texMaxZ, r2, g2, b2, a2, lod, anim)
                mesh.addVertex(terrain, Face.SOUTH, x + tlhl.x,
                        y + tlhl.y, z + tlhl.z,
                        xx + tlhl.x, yy + tlhl.y,
                        zz + tlhl.z, texMaxX,
                        texMaxZ, r2, g2, b2, a2, lod, anim)
                mesh.addVertex(terrain, Face.SOUTH, x + tlhh.x,
                        y + tlhh.y, z + tlhh.z,
                        xx + tlhh.x, yy + tlhh.y,
                        zz + tlhh.z, texMaxX,
                        texMinZ, r2, g2, b2, a2, lod, anim)
                mesh.addVertex(terrain, Face.SOUTH, x + thhh.x,
                        y + thhh.y, z + thhh.z,
                        xx + thhh.x, yy + thhh.y,
                        zz + thhh.z, texMinX,
                        texMinZ, r2, g2, b2, a2, lod, anim)
            }
            if (texSide4 != null) {
                val anim = texSide4.shaderAnimation().id()
                val texMinX = texSide4.marginX(minY)
                val texMaxX = texSide4.marginX(maxY)
                val texMinZ = texSide4.marginY(minZ)
                val texMaxZ = texSide4.marginY(maxZ)
                mesh.addVertex(terrain, Face.WEST, x + tlhl.x,
                        y + tlhl.y, z + tlhl.z,
                        xx + tlhl.x, yy + tlhl.y,
                        zz + tlhl.z, texMinX,
                        texMaxZ, r2, g2, b2, a2, lod, anim)
                mesh.addVertex(terrain, Face.WEST, x + tlll.x,
                        y + tlll.y, z + tlll.z,
                        xx + tlll.x, yy + tlll.y,
                        zz + tlll.z, texMaxX,
                        texMaxZ, r2, g2, b2, a2, lod, anim)
                mesh.addVertex(terrain, Face.WEST, x + tllh.x,
                        y + tllh.y, z + tllh.z,
                        xx + tllh.x, yy + tllh.y,
                        zz + tllh.z, texMaxX,
                        texMinZ, r2, g2, b2, a2, lod, anim)
                mesh.addVertex(terrain, Face.WEST, x + tlhh.x,
                        y + tlhh.y, z + tlhh.z,
                        xx + tlhh.x, yy + tlhh.y,
                        zz + tlhh.z, texMinX,
                        texMinZ, r2, g2, b2, a2, lod, anim)
            }
        }

        override fun addToMesh(mesh: Mesh,
                               inventory: Boolean) {
            mesh.color(r, g, b, a)
            if (texTop != null) {
                val texMinX = texTop.marginX(minX)
                val texMaxX = texTop.marginX(maxX)
                val texMinY = texTop.marginY(minY)
                val texMaxY = texTop.marginY(maxY)
                mesh.normal(0.0, 0.0, 1.0)
                mesh.texture(texMinX, texMinY)
                mesh.vertex(llh.x, llh.y, llh.z)
                mesh.texture(texMaxX, texMinY)
                mesh.vertex(hlh.x, hlh.y, hlh.z)
                mesh.texture(texMaxX, texMaxY)
                mesh.vertex(hhh.x, hhh.y, hhh.z)
                mesh.texture(texMinX, texMaxY)
                mesh.vertex(lhh.x, lhh.y, lhh.z)
            }
            if (texSide2 != null) {
                if (inventory) {
                    mesh.color(r * 0.7, g * 0.7, b * 0.7, a * 1.0)
                }
                val texMinX = texSide2.marginX(minY)
                val texMaxX = texSide2.marginX(maxY)
                val texMinY = texSide2.marginY(minZ)
                val texMaxY = texSide2.marginY(maxZ)
                mesh.normal(1.0, 0.0, 0.0)
                mesh.texture(texMinX, texMinY)
                mesh.vertex(hhh.x, hhh.y, hhh.z)
                mesh.texture(texMaxX, texMinY)
                mesh.vertex(hlh.x, hlh.y, hlh.z)
                mesh.texture(texMaxX, texMaxY)
                mesh.vertex(hll.x, hll.y, hll.z)
                mesh.texture(texMinX, texMaxY)
                mesh.vertex(hhl.x, hhl.y, hhl.z)
            }
            if (texSide3 != null) {
                if (inventory) {
                    mesh.color(r * 0.8f, g * 0.8f, b * 0.8f, a)
                }
                val texMinX = texSide3.marginX(minX)
                val texMaxX = texSide3.marginX(maxX)
                val texMinY = texSide3.marginY(minZ)
                val texMaxY = texSide3.marginY(maxZ)
                mesh.normal(0.0, 1.0, 0.0)
                mesh.texture(texMaxX, texMaxY)
                mesh.vertex(hhl.x, hhl.y, hhl.z)
                mesh.texture(texMinX, texMaxY)
                mesh.vertex(lhl.x, lhl.y, lhl.z)
                mesh.texture(texMinX, texMinY)
                mesh.vertex(lhh.x, lhh.y, lhh.z)
                mesh.texture(texMaxX, texMinY)
                mesh.vertex(hhh.x, hhh.y, hhh.z)
            }
            if (!inventory) {
                if (texSide4 != null) {
                    val texMinX = texSide4.marginX(minY)
                    val texMaxX = texSide4.marginX(maxY)
                    val texMinY = texSide4.marginY(minZ)
                    val texMaxY = texSide4.marginY(maxZ)
                    mesh.normal(-1.0, 0.0, 0.0)
                    mesh.texture(texMinX, texMaxY)
                    mesh.vertex(lhl.x, lhl.y, lhl.z)
                    mesh.texture(texMaxX, texMaxY)
                    mesh.vertex(lll.x, lll.y, lll.z)
                    mesh.texture(texMaxX, texMinY)
                    mesh.vertex(llh.x, llh.y, llh.z)
                    mesh.texture(texMinX, texMinY)
                    mesh.vertex(lhh.x, lhh.y, lhh.z)
                }
                if (texSide1 != null) {
                    val texMinX = texSide1.marginX(minX)
                    val texMaxX = texSide1.marginX(maxX)
                    val texMinY = texSide1.marginY(minZ)
                    val texMaxY = texSide1.marginY(maxZ)
                    mesh.normal(0.0, -1.0, 0.0)
                    mesh.texture(texMinX, texMinY)
                    mesh.vertex(hlh.x, hlh.y, hlh.z)
                    mesh.texture(texMaxX, texMinY)
                    mesh.vertex(llh.x, llh.y, llh.z)
                    mesh.texture(texMaxX, texMaxY)
                    mesh.vertex(lll.x, lll.y, lll.z)
                    mesh.texture(texMinX, texMaxY)
                    mesh.vertex(hll.x, hll.y, hll.z)
                }
                if (texBottom != null) {
                    val texMinX = texBottom.marginX(minX)
                    val texMaxX = texBottom.marginX(maxX)
                    val texMinY = texBottom.marginY(minY)
                    val texMaxY = texBottom.marginY(maxY)
                    mesh.normal(0.0, 0.0, -1.0)
                    mesh.texture(texMinX, texMaxY)
                    mesh.vertex(lhl.x, lhl.y, lhl.z)
                    mesh.texture(texMaxX, texMaxY)
                    mesh.vertex(hhl.x, hhl.y, hhl.z)
                    mesh.texture(texMaxX, texMinY)
                    mesh.vertex(hll.x, hll.y, hll.z)
                    mesh.texture(texMinX, texMinY)
                    mesh.vertex(lll.x, lll.y, lll.z)
                }
            }
        }
    }

    class ShapeBillboard(private val texture: TerrainTexture?,
                         minX: Double,
                         minY: Double,
                         minZ: Double,
                         maxX: Double,
                         maxY: Double,
                         maxZ: Double,
                         private val nx: Double,
                         private val ny: Double,
                         private val nz: Double,
                         r: Double,
                         g: Double,
                         b: Double,
                         a: Double) : Shape() {

        constructor(texture: TerrainTexture,
                    minX: Double,
                    minY: Double,
                    minZ: Double,
                    maxX: Double,
                    maxY: Double,
                    maxZ: Double,
                    r: Double,
                    g: Double,
                    b: Double,
                    a: Double) : this(texture, minX, minY,
                minZ, maxX, maxY, maxZ, Double.NaN, Double.NaN, Double.NaN, r,
                g, b, a)

        init {
            this.minX = minX
            this.minY = minY
            this.minZ = minZ
            this.maxX = maxX
            this.maxY = maxY
            this.maxZ = maxZ
            val middleX = (minX + maxX) / 2.0
            val middleY = (minY + maxY) / 2.0
            lll = Vector3d(middleX, minY, minZ)
            lhl = Vector3d(middleX, maxY, minZ)
            lhh = Vector3d(middleX, maxY, maxZ)
            llh = Vector3d(middleX, minY, maxZ)
            hll = Vector3d(minX, middleY, minZ)
            hhl = Vector3d(maxX, middleY, minZ)
            hhh = Vector3d(maxX, middleY, maxZ)
            hlh = Vector3d(minX, middleY, maxZ)
            this.r = r
            this.g = g
            this.b = b
            this.a = a
        }

        override fun addToChunkMesh(mesh: ChunkMesh,
                                    terrain: TerrainClient,
                                    x: Double,
                                    y: Double,
                                    z: Double,
                                    xx: Double,
                                    yy: Double,
                                    zz: Double,
                                    r: Double,
                                    g: Double,
                                    b: Double,
                                    a: Double,
                                    lod: Boolean) {
            if (texture == null) {
                return
            }
            val r2 = r * this.r
            val g2 = g * this.g
            val b2 = b * this.b
            val a2 = a * this.a
            val animation = texture.shaderAnimation()
            val animBottom = animation.id()
            val animTop: Byte
            if (animation == ShaderAnimation.TALL_GRASS) {
                animTop = ShaderAnimation.NONE.id()
            } else {
                animTop = animBottom
            }
            val texMinX = texture.marginX(minX)
            val texMaxX = texture.marginX(maxX)
            val texMinY = texture.marginY(minY)
            val texMaxY = texture.marginY(maxY)
            mesh.addVertex(terrain, Face.NONE, x + tlll.x, y + tlll.y,
                    z + tlll.z, xx + tlll.x, yy + tlll.y, zz + tlll.z, nx, ny,
                    nz, texMinX, texMaxY, r2, g2, b2, a2, lod, animTop)
            mesh.addVertex(terrain, Face.NONE, x + tlhl.x, y + tlhl.y,
                    z + tlhl.z, xx + tlhl.x, yy + tlhl.y, zz + tlhl.z, nx, ny,
                    nz, texMaxX, texMaxY, r2, g2, b2, a2, lod, animTop)
            mesh.addVertex(terrain, Face.NONE, x + tlhh.x, y + tlhh.y,
                    z + tlhh.z, xx + tlhh.x, yy + tlhh.y, zz + tlhh.z, nx, ny,
                    nz, texMaxX, texMinY, r2, g2, b2, a2, lod, animBottom)
            mesh.addVertex(terrain, Face.NONE, x + tllh.x, y + tllh.y,
                    z + tllh.z, xx + tllh.x, yy + tllh.y, zz + tllh.z, nx, ny,
                    nz, texMinX, texMinY, r2, g2, b2, a2, lod, animBottom)
            mesh.addVertex(terrain, Face.NONE, x + tllh.x, y + tllh.y,
                    z + tllh.z, xx + tllh.x, yy + tllh.y, zz + tllh.z, nx, ny,
                    nz, texMaxX, texMinY, r2, g2, b2, a2, lod, animBottom)
            mesh.addVertex(terrain, Face.NONE, x + tlhh.x, y + tlhh.y,
                    z + tlhh.z, xx + tlhh.x, yy + tlhh.y, zz + tlhh.z, nx, ny,
                    nz, texMinX, texMinY, r2, g2, b2, a2, lod, animBottom)
            mesh.addVertex(terrain, Face.NONE, x + tlhl.x, y + tlhl.y,
                    z + tlhl.z, xx + tlhl.x, yy + tlhl.y, zz + tlhl.z, nx, ny,
                    nz, texMinX, texMaxY, r2, g2, b2, a2, lod, animTop)
            mesh.addVertex(terrain, Face.NONE, x + tlll.x, y + tlll.y,
                    z + tlll.z, xx + tlll.x, yy + tlll.y, zz + tlll.z, nx, ny,
                    nz, texMaxX, texMaxY, r2, g2, b2, a2, lod, animTop)
            mesh.addVertex(terrain, Face.NONE, x + thll.x, y + thll.y,
                    z + thll.z, xx + thll.x, yy + thll.y, zz + thll.z, nx, ny,
                    nz, texMinX, texMaxY, r2, g2, b2, a2, lod, animTop)
            mesh.addVertex(terrain, Face.NONE, x + thhl.x, y + thhl.y,
                    z + thhl.z, xx + thhl.x, yy + thhl.y, zz + thhl.z, nx, ny,
                    nz, texMaxX, texMaxY, r2, g2, b2, a2, lod, animTop)
            mesh.addVertex(terrain, Face.NONE, x + thhh.x, y + thhh.y,
                    z + thhh.z, xx + thhh.x, yy + thhh.y, zz + thhh.z, nx, ny,
                    nz, texMaxX, texMinY, r2, g2, b2, a2, lod, animBottom)
            mesh.addVertex(terrain, Face.NONE, x + thlh.x, y + thlh.y,
                    z + thlh.z, xx + thlh.x, yy + thlh.y, zz + thlh.z, nx, ny,
                    nz, texMinX, texMinY, r2, g2, b2, a2, lod, animBottom)
            mesh.addVertex(terrain, Face.NONE, x + thlh.x, y + thlh.y,
                    z + thlh.z, xx + thlh.x, yy + thlh.y, zz + thlh.z, nx, ny,
                    nz, texMaxX, texMinY, r2, g2, b2, a2, lod, animBottom)
            mesh.addVertex(terrain, Face.NONE, x + thhh.x, y + thhh.y,
                    z + thhh.z, xx + thhh.x, yy + thhh.y, zz + thhh.z, nx, ny,
                    nz, texMinX, texMinY, r2, g2, b2, a2, lod, animBottom)
            mesh.addVertex(terrain, Face.NONE, x + thhl.x, y + thhl.y,
                    z + thhl.z, xx + thhl.x, yy + thhl.y, zz + thhl.z, nx, ny,
                    nz, texMinX, texMaxY, r2, g2, b2, a2, lod, animTop)
            mesh.addVertex(terrain, Face.NONE, x + thll.x, y + thll.y,
                    z + thll.z, xx + thll.x, yy + thll.y, zz + thll.z, nx, ny,
                    nz, texMaxX, texMaxY, r2, g2, b2, a2, lod, animTop)
        }

        override fun addToMesh(mesh: Mesh,
                               inventory: Boolean) {
            if (texture == null) {
                return
            }
            val texMinX = texture.marginX(minX)
            val texMaxX = texture.marginX(maxX)
            val texMinY = texture.marginY(minY)
            val texMaxY = texture.marginY(maxY)
            mesh.color(r, g, b, a)
            mesh.normal(0.0, 0.0, 1.0) // TODO: Implement proper normals
            mesh.texture(texMinX, texMaxY)
            mesh.vertex(lll.x, lll.y, lll.z)
            mesh.texture(texMaxX, texMaxY)
            mesh.vertex(lhl.x, lhl.y, lhl.z)
            mesh.texture(texMaxX, texMinY)
            mesh.vertex(lhh.x, lhh.y, lhh.z)
            mesh.texture(texMinX, texMinY)
            mesh.vertex(llh.x, llh.y, llh.z)
            mesh.texture(texMinX, texMinY)
            mesh.vertex(llh.x, llh.y, llh.z)
            mesh.texture(texMaxX, texMinY)
            mesh.vertex(lhh.x, lhh.y, lhh.z)
            mesh.texture(texMaxX, texMaxY)
            mesh.vertex(lhl.x, lhl.y, lhl.z)
            mesh.texture(texMinX, texMaxY)
            mesh.vertex(lll.x, lll.y, lll.z)
            mesh.texture(texMinX, texMaxY)
            mesh.vertex(hll.x, hll.y, hll.z)
            mesh.texture(texMaxX, texMaxY)
            mesh.vertex(hhl.x, hhl.y, hhl.z)
            mesh.texture(texMaxX, texMinY)
            mesh.vertex(hhh.x, hhh.y, hhh.z)
            mesh.texture(texMinX, texMinY)
            mesh.vertex(hlh.x, hlh.y, hlh.z)
            mesh.texture(texMinX, texMinY)
            mesh.vertex(hlh.x, hlh.y, hlh.z)
            mesh.texture(texMaxX, texMinY)
            mesh.vertex(hhh.x, hhh.y, hhh.z)
            mesh.texture(texMaxX, texMaxY)
            mesh.vertex(hhl.x, hhl.y, hhl.z)
            mesh.texture(texMinX, texMaxY)
            mesh.vertex(hll.x, hll.y, hll.z)
        }
    }
}
