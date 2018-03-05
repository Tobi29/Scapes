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

import org.tobi29.math.AABB3
import org.tobi29.math.Face
import org.tobi29.scapes.block.models.SmoothLight
import org.tobi29.scapes.chunk.terrain.TerrainClient
import org.tobi29.scapes.engine.ScapesEngine
import org.tobi29.scapes.engine.graphics.Model
import org.tobi29.scapes.engine.graphics.ModelAttribute
import org.tobi29.scapes.engine.graphics.RenderType
import org.tobi29.scapes.engine.graphics.VertexType
import org.tobi29.stdex.copy
import org.tobi29.stdex.math.floorToInt
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class ChunkMesh(private val arrays: VertexArrays) {
    private val triple = SmoothLight.FloatTriple()
    val aabb = AABB3(Double.NaN, Double.NaN, Double.NaN, Double.NaN,
            Double.NaN, Double.NaN)
    private var pos = 0
    private var remaining = 0

    init {
        remaining = arrays.vertexArray.size / 3
    }

    fun addQuad(terrain: TerrainClient,
                side: Face,
                x0: Double,
                y0: Double,
                z0: Double,
                x1: Double,
                y1: Double,
                z1: Double,
                x2: Double,
                y2: Double,
                z2: Double,
                x3: Double,
                y3: Double,
                z3: Double,
                xx0: Double,
                yy0: Double,
                zz0: Double,
                xx1: Double,
                yy1: Double,
                zz1: Double,
                xx2: Double,
                yy2: Double,
                zz2: Double,
                xx3: Double,
                yy3: Double,
                zz3: Double,
                tx0: Double,
                ty0: Double,
                tx1: Double,
                ty1: Double,
                tx2: Double,
                ty2: Double,
                tx3: Double,
                ty3: Double,
                r: Double,
                g: Double,
                b: Double,
                a: Double,
                lod: Boolean,
                anim: Byte) =
            addQuad(terrain, side, x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3,
                    z3, xx0, yy0, zz0, xx1, yy1, zz1, xx2, yy2, zz2, xx3, yy3,
                    zz3, tx0, ty0, tx1, ty1, tx2, ty2, tx3, ty3, r, g, b, a,
                    lod, anim, anim, anim, anim)

    fun addQuad(terrain: TerrainClient,
                side: Face,
                x0: Double,
                y0: Double,
                z0: Double,
                x1: Double,
                y1: Double,
                z1: Double,
                x2: Double,
                y2: Double,
                z2: Double,
                x3: Double,
                y3: Double,
                z3: Double,
                xx0: Double,
                yy0: Double,
                zz0: Double,
                xx1: Double,
                yy1: Double,
                zz1: Double,
                xx2: Double,
                yy2: Double,
                zz2: Double,
                xx3: Double,
                yy3: Double,
                zz3: Double,
                tx0: Double,
                ty0: Double,
                tx1: Double,
                ty1: Double,
                tx2: Double,
                ty2: Double,
                tx3: Double,
                ty3: Double,
                r: Double,
                g: Double,
                b: Double,
                a: Double,
                lod: Boolean,
                anim0: Byte,
                anim1: Byte,
                anim2: Byte,
                anim3: Byte) =
            addQuad(terrain, side, x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3,
                    z3, xx0, yy0, zz0, xx1, yy1, zz1, xx2, yy2, zz2, xx3, yy3,
                    zz3, Double.NaN, Double.NaN, Double.NaN, Double.NaN,
                    Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN,
                    Double.NaN, Double.NaN, Double.NaN, tx0, ty0, tx1, ty1, tx2,
                    ty2, tx3, ty3, r, g, b, a, lod, anim0, anim1, anim2, anim3)

    fun addQuad(terrain: TerrainClient,
                side: Face,
                x0: Double,
                y0: Double,
                z0: Double,
                x1: Double,
                y1: Double,
                z1: Double,
                x2: Double,
                y2: Double,
                z2: Double,
                x3: Double,
                y3: Double,
                z3: Double,
                xx0: Double,
                yy0: Double,
                zz0: Double,
                xx1: Double,
                yy1: Double,
                zz1: Double,
                xx2: Double,
                yy2: Double,
                zz2: Double,
                xx3: Double,
                yy3: Double,
                zz3: Double,
                nx0: Double,
                ny0: Double,
                nz0: Double,
                nx1: Double,
                ny1: Double,
                nz1: Double,
                nx2: Double,
                ny2: Double,
                nz2: Double,
                nx3: Double,
                ny3: Double,
                nz3: Double,
                tx0: Double,
                ty0: Double,
                tx1: Double,
                ty1: Double,
                tx2: Double,
                ty2: Double,
                tx3: Double,
                ty3: Double,
                r: Double,
                g: Double,
                b: Double,
                a: Double,
                lod: Boolean,
                anim0: Byte,
                anim1: Byte,
                anim2: Byte,
                anim3: Byte) {
        if (lod) {
            addVertexHighLod(terrain, side, x0, y0, z0, xx0, yy0, zz0,
                    nx0, ny0, nz0, tx0, ty0,
                    r, g, b, a, anim0)
            addVertexHighLod(terrain, side, x1, y1, z1, xx1, yy1, zz1,
                    nx1, ny1, nz1, tx1, ty1,
                    r, g, b, a, anim1)
            addVertexHighLod(terrain, side, x2, y2, z2, xx2, yy2, zz2,
                    nx2, ny2, nz2, tx2, ty2,
                    r, g, b, a, anim2)
            addVertexHighLod(terrain, side, x3, y3, z3, xx3, yy3, zz3,
                    nx3, ny3, nz3, tx3, ty3,
                    r, g, b, a, anim3)
        } else {
            val xxx = ((x0 + x1 + x2 + x3) * 0.25 + side.x * 0.5).floorToInt()
            val yyy = ((y0 + y1 + y2 + y3) * 0.25 + side.y * 0.5).floorToInt()
            val zzz = ((z0 + z1 + z2 + z3) * 0.25 + side.z * 0.5).floorToInt()
            val light = terrain.blockLight(xxx, yyy, zzz) / 15.0
            val sunLight = terrain.sunLight(xxx, yyy, zzz) / 15.0
            addVertex(xx0, yy0, zz0, nx0, ny0, nz0, tx0, ty0,
                    r, g, b, a, light,
                    sunLight, anim0)
            addVertex(xx1, yy1, zz1, nx1, ny1, nz1, tx1, ty1,
                    r, g, b, a, light,
                    sunLight, anim1)
            addVertex(xx2, yy2, zz2, nx2, ny2, nz2, tx2, ty2,
                    r, g, b, a, light,
                    sunLight, anim2)
            addVertex(xx3, yy3, zz3, nx3, ny3, nz3, tx3, ty3,
                    r, g, b, a, light,
                    sunLight, anim3)
        }
    }

    fun addVertexHighLod(terrain: TerrainClient,
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
                         anim: Byte) {
        SmoothLight.calcLight(triple, side, x, y, z, terrain)
        val light = triple.a.toDouble()
        val sunLight = triple.b.toDouble()
        val r2 = r * triple.c
        val g2 = g * triple.c
        val b2 = b * triple.c
        addVertex(xx, yy, zz, nx, ny, nz, tx, ty, r2, g2, b2, a, light,
                sunLight,
                anim)
    }

    fun addVertex(x: Double,
                  y: Double,
                  z: Double,
                  nx: Double,
                  ny: Double,
                  nz: Double,
                  tx: Double,
                  ty: Double,
                  r: Double,
                  g: Double,
                  b: Double,
                  a: Double,
                  light: Double,
                  sunLight: Double,
                  anim: Byte) {
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
        arrays.vertexArray[i++] = x.toFloat()
        arrays.vertexArray[i++] = y.toFloat()
        arrays.vertexArray[i] = z.toFloat()
        i = pos * 3
        arrays.normalArray[i++] = nx.toFloat()
        arrays.normalArray[i++] = ny.toFloat()
        arrays.normalArray[i] = nz.toFloat()
        i = pos shl 2
        arrays.colorArray[i++] = r.toFloat()
        arrays.colorArray[i++] = g.toFloat()
        arrays.colorArray[i++] = b.toFloat()
        arrays.colorArray[i] = a.toFloat()
        i = pos shl 1
        arrays.textureArray[i++] = tx.toFloat()
        arrays.textureArray[i] = ty.toFloat()
        i = pos shl 1
        arrays.lightArray[i++] = light.toFloat()
        arrays.lightArray[i] = sunLight.toFloat()
        arrays.animationArray[pos++] = anim.toInt()
        aabb.min.x = if (aabb.min.x.isNaN()) x else min(aabb.min.x, x)
        aabb.min.y = if (aabb.min.y.isNaN()) y else min(aabb.min.y, y)
        aabb.min.z = if (aabb.min.z.isNaN()) z else min(aabb.min.z, z)
        aabb.max.x = if (aabb.max.x.isNaN()) x else max(aabb.max.x, x)
        aabb.max.y = if (aabb.max.y.isNaN()) y else max(aabb.max.y, y)
        aabb.max.z = if (aabb.max.z.isNaN()) z else max(aabb.max.z, z)
        remaining--
    }

    private fun changeArraySize(size: Int) {
        val newVertexArray = FloatArray(size * 3)
        val newColorArray = FloatArray(size shl 2)
        val newTextureArray = FloatArray(size shl 1)
        val newNormalArray = FloatArray(size * 3)
        val newLightArray = FloatArray(size shl 1)
        val newAnimationArray = IntArray(size)
        copy(arrays.vertexArray, newVertexArray)
        copy(arrays.colorArray, newColorArray)
        copy(arrays.textureArray, newTextureArray)
        copy(arrays.normalArray, newNormalArray)
        copy(arrays.lightArray, newLightArray)
        copy(arrays.animationArray, newAnimationArray)
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

    fun aabb(): AABB3 {
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
