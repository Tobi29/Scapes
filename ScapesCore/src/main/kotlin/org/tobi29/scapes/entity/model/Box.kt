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

package org.tobi29.scapes.entity.model

import org.tobi29.scapes.engine.ScapesEngine
import org.tobi29.scapes.engine.graphics.*

class Box constructor(engine: ScapesEngine,
                      tbs: Float,
                      minX: Float,
                      minY: Float,
                      minZ: Float,
                      maxX: Float,
                      maxY: Float,
                      maxZ: Float,
                      tX: Float,
                      tY: Float,
                      culling: Boolean = true) {

    val minX: Float
    val minY: Float
    val minZ: Float
    val maxX: Float
    val maxY: Float
    val maxZ: Float
    private val model: Model

    init {
        var minX = minX
        var minY = minY
        var minZ = minZ
        var maxX = maxX
        var maxY = maxY
        var maxZ = maxZ
        var tX = tX
        var tY = tY
        tX *= tbs
        tY *= tbs
        val lX = (maxX - minX) * tbs
        val lY = (maxY - minY) * tbs
        val lZ = (maxZ - minZ) * tbs
        minX /= 16f
        minY /= 16f
        minZ /= 16f
        maxX /= 16f
        maxY /= 16f
        maxZ /= 16f
        val indices: IntArray
        if (culling) {
            indices = INDICES
        } else {
            indices = INDICES_NO_CULL
        }
        model = engine.graphics.createVTNI(
                floatArrayOf(minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY,
                        minZ, minX, maxY, minZ, minX, minY, maxZ, minX, minY,
                        minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY,
                        maxZ, minX, maxY, maxZ, minX, maxY, minZ, minX, minY,
                        minZ, maxX, minY, maxZ, maxX, minY, minZ, maxX, maxY,
                        minZ, maxX, maxY, maxZ, minX, minY, maxZ, maxX, minY,
                        maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, minX, minY,
                        minZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY,
                        minZ),
                floatArrayOf(lY + tX, tY, lY + lX + tX, tY, lY + lX + tX,
                        lZ + tY, lY + tX, lZ + tY, lY * 2 + lX + tX, tY,
                        lY * 2 + lX + tX, lZ + tY, lY * 2 + lX * 2 + tX,
                        lZ + tY, lY * 2 + lX * 2 + tX, tY, tX, tY, lY + tX, tY,
                        lY + tX, lZ + tY, tX, lZ + tY, lY * 2 + lX + tX, tY,
                        lY * 2 + lX + tX, lZ + tY, lY + lX + tX, lZ + tY,
                        lY + lX + tX, tY, lY + tX, lZ + tY, lY + lX + tX,
                        lZ + tY, lY + lX + tX, lZ + lY + tY, lY + tX,
                        lZ + lY + tY, lY * 2 + lX + tX, lZ + tY,
                        lY * 2 + lX + tX, lZ + lY + tY, lY * 2 + lX * 2 + tX,
                        lZ + lY + tY, lY * 2 + lX * 2 + tX, lZ + tY),
                floatArrayOf(0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f,
                        0.0f, 0.0f, 1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f,
                        0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, -1.0f, 0.0f,
                        0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f,
                        0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f,
                        0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f,
                        1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f,
                        -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f,
                        -1.0f), indices, RenderType.TRIANGLES)
        this.minX = minX
        this.minY = minY
        this.minZ = minZ
        this.maxX = maxX
        this.maxY = maxY
        this.maxZ = maxZ
    }

    fun render(r: Float,
               g: Float,
               b: Float,
               a: Float,
               gl: GL,
               shader: Shader) {
        gl.setAttribute4f(GL.COLOR_ATTRIBUTE, r, g, b, a)
        model.render(gl, shader)
    }

    companion object {
        private val INDICES: IntArray
        private val INDICES_NO_CULL: IntArray

        init {
            INDICES = intArrayOf(0, 1, 2, 0, 2, 3, 4, 5, 6, 4, 6, 7, 8, 9, 10,
                    8, 10, 11, 12, 13, 14, 12, 14, 15, 16, 17, 18, 16, 18, 19,
                    20, 21, 22, 20, 22, 23)
            INDICES_NO_CULL = IntArray(INDICES.size shl 1)
            System.arraycopy(INDICES, 0, INDICES_NO_CULL, 0, INDICES.size)
            for (i in INDICES.indices) {
                INDICES_NO_CULL[INDICES_NO_CULL.size - i - 1] = INDICES[i]
            }
        }
    }
}
