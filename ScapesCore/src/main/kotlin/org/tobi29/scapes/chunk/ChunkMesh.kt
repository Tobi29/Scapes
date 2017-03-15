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

package org.tobi29.scapes.chunk

import org.tobi29.scapes.block.models.SmoothLight
import org.tobi29.scapes.chunk.terrain.TerrainClient
import org.tobi29.scapes.engine.ScapesEngine
import org.tobi29.scapes.engine.graphics.Model
import org.tobi29.scapes.engine.graphics.ModelAttribute
import org.tobi29.scapes.engine.graphics.RenderType
import org.tobi29.scapes.engine.graphics.VertexType
import org.tobi29.scapes.engine.utils.math.*
import java.util.*

class ChunkMesh(private val arrays: VertexArrays) {
    private val triple = SmoothLight.FloatTriple()
    private val aabb = AABB(Double.NaN, Double.NaN, Double.NaN, Double.NaN,
            Double.NaN, Double.NaN)
    private var pos = 0
    private var remaining = 0

    init {
        remaining = arrays.vertexArray.size / 3
    }

    fun addVertex(terrain: TerrainClient,
                  side: Face,
                  x: Double,
                  y: Double,
                  z: Double,
                  xx: Double,
                  yy: Double,
                  zz: Double,
                  tx: Double,
                  ty: Double,
                  r: Double,
                  g: Double,
                  b: Double,
                  a: Double,
                  lod: Boolean,
                  anim: Byte) {
        addVertex(terrain, side, x, y, z, xx, yy, zz, Double.NaN,
                Double.NaN,
                Double.NaN, tx, ty, r, g, b, a, lod, anim)
    }

    fun addVertex(terrain: TerrainClient,
                  side: Face,
                  x: Double,
                  y: Double,
                  z: Double,
                  xx: Double,
                  yy: Double,
                  zz: Double,
                  nx: Double,
                  ny: Double,
                  nz: Double,
                  tx: Double,
                  ty: Double,
                  r: Double,
                  g: Double,
                  b: Double,
                  a: Double,
                  lod: Boolean,
                  anim: Byte) {
        val r2: Double
        val g2: Double
        val b2: Double
        val light: Double
        val sunLight: Double
        if (lod) {
            SmoothLight.calcLight(triple, side, x, y, z, terrain)
            light = triple.a.toDouble()
            sunLight = triple.b.toDouble()
            r2 = r * triple.c
            g2 = g * triple.c
            b2 = b * triple.c
        } else {
            val xxx = floor(x + side.x)
            val yyy = floor(y + side.y)
            val zzz = floor(z + side.z)
            light = terrain.blockLight(xxx, yyy, zzz) / 15.0
            sunLight = terrain.sunLight(xxx, yyy, zzz) / 15.0
            r2 = r
            g2 = g
            b2 = b
        }
        if (remaining <= 0) {
            val growth: Int
            if (pos == 0) {
                growth = START_SIZE
            } else {
                growth = pos
            }
            changeArraySize(pos + growth)
            remaining += growth
        }
        var i = pos * 3
        arrays.vertexArray[i++] = xx.toFloat()
        arrays.vertexArray[i++] = yy.toFloat()
        arrays.vertexArray[i] = zz.toFloat()
        i = pos * 3
        arrays.normalArray[i++] = nx.toFloat()
        arrays.normalArray[i++] = ny.toFloat()
        arrays.normalArray[i] = nz.toFloat()
        i = pos shl 2
        arrays.colorArray[i++] = r2.toFloat()
        arrays.colorArray[i++] = g2.toFloat()
        arrays.colorArray[i++] = b2.toFloat()
        arrays.colorArray[i] = a.toFloat()
        i = pos shl 1
        arrays.textureArray[i++] = tx.toFloat()
        arrays.textureArray[i] = ty.toFloat()
        i = pos shl 1
        arrays.lightArray[i++] = light.toFloat()
        arrays.lightArray[i] = sunLight.toFloat()
        arrays.animationArray[pos++] = anim.toInt()
        aabb.minX = min(aabb.minX, xx)
        aabb.minY = min(aabb.minY, yy)
        aabb.minZ = min(aabb.minZ, zz)
        aabb.maxX = max(aabb.maxX, xx)
        aabb.maxY = max(aabb.maxY, yy)
        aabb.maxZ = max(aabb.maxZ, zz)
        remaining--
    }

    private fun changeArraySize(size: Int) {
        val newVertexArray = FloatArray(size * 3)
        val newColorArray = FloatArray(size shl 2)
        val newTextureArray = FloatArray(size shl 1)
        val newNormalArray = FloatArray(size * 3)
        val newLightArray = FloatArray(size shl 1)
        val newAnimationArray = IntArray(size)
        System.arraycopy(arrays.vertexArray, 0, newVertexArray, 0,
                min(arrays.vertexArray.size, newVertexArray.size))
        System.arraycopy(arrays.colorArray, 0, newColorArray, 0,
                min(arrays.colorArray.size, newColorArray.size))
        System.arraycopy(arrays.textureArray, 0, newTextureArray, 0,
                min(arrays.textureArray.size, newTextureArray.size))
        System.arraycopy(arrays.normalArray, 0, newNormalArray, 0,
                min(arrays.normalArray.size, newNormalArray.size))
        System.arraycopy(arrays.lightArray, 0, newLightArray, 0,
                min(arrays.lightArray.size, newLightArray.size))
        System.arraycopy(arrays.animationArray, 0, newAnimationArray, 0,
                min(arrays.animationArray.size,
                        newAnimationArray.size))
        arrays.vertexArray = newVertexArray
        arrays.colorArray = newColorArray
        arrays.textureArray = newTextureArray
        arrays.normalArray = newNormalArray
        arrays.lightArray = newLightArray
        arrays.animationArray = newAnimationArray
    }

    private fun computeNormals() {
        var ii1 = 0
        var ii2 = 0
        var i = 0
        while (i < pos) {
            if (arrays.normalArray[ii2].isNaN()) {
                val x1 = arrays.vertexArray[ii1++]
                val y1 = arrays.vertexArray[ii1++]
                val z1 = arrays.vertexArray[ii1++]
                val x2 = arrays.vertexArray[ii1++]
                val y2 = arrays.vertexArray[ii1++]
                val z2 = arrays.vertexArray[ii1++]
                val x3 = arrays.vertexArray[ii1++]
                val y3 = arrays.vertexArray[ii1++]
                val z3 = arrays.vertexArray[ii1++]
                val x4 = arrays.vertexArray[ii1++]
                val y4 = arrays.vertexArray[ii1++]
                val z4 = arrays.vertexArray[ii1++]
                var ux = x2 - x1
                var uy = y2 - y1
                var uz = z2 - z1
                val vx = x3 - x1
                val vy = y3 - y1
                val vz = z3 - z1
                var nx = uy * vz - uz * vy
                var ny = uz * vx - ux * vz
                var nz = ux * vy - uy * vx
                ux = x4 - x1
                uy = y4 - y1
                uz = z4 - z1
                nx += vy * uz - vz * uy
                ny += vz * ux - vx * uz
                nz += vx * uy - vy * ux
                nx *= 0.5f
                ny *= 0.5f
                nz *= 0.5f
                val length = sqrt(nx * nx + ny * ny + nz * nz)
                nx /= length
                ny /= length
                nz /= length
                arrays.normalArray[ii2++] = nx
                arrays.normalArray[ii2++] = ny
                arrays.normalArray[ii2++] = nz
                arrays.normalArray[ii2++] = nx
                arrays.normalArray[ii2++] = ny
                arrays.normalArray[ii2++] = nz
                arrays.normalArray[ii2++] = nx
                arrays.normalArray[ii2++] = ny
                arrays.normalArray[ii2++] = nz
                arrays.normalArray[ii2++] = nx
                arrays.normalArray[ii2++] = ny
                arrays.normalArray[ii2++] = nz
            } else {
                ii1 += 12
                ii2 += 12
            }
            i += 4
        }
    }

    fun finish(engine: ScapesEngine): Model {
        computeNormals()
        val indexArray = IntArray(pos * 3 / 2)
        var i = 0
        var p = 0
        while (i < indexArray.size) {
            indexArray[i++] = p
            indexArray[i++] = p + 1
            indexArray[i++] = p + 2
            indexArray[i++] = p
            indexArray[i++] = p + 2
            indexArray[i++] = p + 3
            p += 4
        }
        val attributes = ArrayList<ModelAttribute>(6)
        attributes.add(ModelAttribute(0, 3, arrays.vertexArray, pos * 3,
                false, 0, VertexType.HALF_FLOAT))
        attributes.add(ModelAttribute(1, 4, arrays.colorArray, pos shl 2, true,
                0, VertexType.UNSIGNED_BYTE))
        attributes.add(ModelAttribute(2, 2, arrays.textureArray, pos shl 1,
                true, 0, VertexType.UNSIGNED_SHORT))
        attributes.add(ModelAttribute(3, 3, arrays.normalArray, pos * 3, true,
                0, VertexType.BYTE))
        attributes.add(ModelAttribute(4, 2, arrays.lightArray, pos shl 1, true,
                0, VertexType.UNSIGNED_BYTE))
        attributes.add(ModelAttribute(5, 1, arrays.animationArray, pos, 0,
                VertexType.UNSIGNED_BYTE))
        return engine.graphics.createModelStatic(attributes, pos, indexArray,
                RenderType.TRIANGLES)
    }

    fun size(): Int {
        return (pos * 1.5).toInt()
    }

    fun aabb(): AABB {
        return aabb
    }

    class VertexArrays {
        var vertexArray = EMPTY_FLOAT
        var colorArray = EMPTY_FLOAT
        var textureArray = EMPTY_FLOAT
        var normalArray = EMPTY_FLOAT
        var lightArray = EMPTY_FLOAT
        var animationArray = EMPTY_INT
    }

    companion object {
        private val EMPTY_FLOAT = floatArrayOf()
        private val EMPTY_INT = intArrayOf()
        private val START_SIZE = 6 * 6000
    }
}
