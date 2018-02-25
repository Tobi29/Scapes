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

package org.tobi29.scapes.chunk.terrain.infinite

import org.tobi29.arrays.fill
import org.tobi29.graphics.Cam
import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.graphics.Model
import org.tobi29.scapes.engine.graphics.Shader
import org.tobi29.scapes.engine.graphics.push
import org.tobi29.stdex.atomic.AtomicBoolean
import org.tobi29.stdex.math.sqr

class TerrainInfiniteRendererChunk(private val chunk: TerrainInfiniteChunkClient,
                                   private val renderer: TerrainInfiniteRenderer) {
    private val vao: Array<TerrainInfiniteChunkModel?>
    private val geometryDirty: Array<AtomicBoolean>
    private val geometryInit: BooleanArray
    private val solid: BooleanArray
    private val visible: BooleanArray
    private val prepareVisible: BooleanArray
    private val culled: BooleanArray

    init {
        val zSections = chunk.size.z shr 4
        geometryDirty = Array(zSections) { AtomicBoolean(true) }
        geometryInit = BooleanArray(zSections + 1)
        vao = arrayOfNulls(zSections)
        solid = BooleanArray(zSections)
        visible = BooleanArray(zSections)
        prepareVisible = BooleanArray(zSections)
        culled = BooleanArray(zSections) { true }
        renderer.addToQueue(this)
    }

    fun chunk(): TerrainInfiniteChunkClient {
        return chunk
    }

    fun zSections(): Int {
        return vao.size
    }

    fun render(gl: GL,
               shader1: Shader,
               shader2: Shader,
               cam: Cam) {
        val relativeX = chunk.posBlock.x - cam.position.x
        val relativeY = chunk.posBlock.y - cam.position.y
        for (i in vao.indices) {
            val relativeZ = (i shl 4) - cam.position.z
            val distance = sqr(relativeX + 8) +
                    sqr(relativeY + 8) +
                    sqr(relativeZ + 8)
            val newLod = distance < sqr(renderer.lodDistance)
            val vao = this.vao[i]
            if (vao != null && vao.model != null) {
                if (vao.lod != newLod) {
                    setGeometryDirty(i)
                }
                if (cam.frustum.inView(vao.model.second) != 0) {
                    val animated = distance < 2304
                    gl.matrixStack.push { matrix ->
                        matrix.translate(relativeX.toFloat(),
                                relativeY.toFloat(), relativeZ.toFloat())
                        if (!vao.model.first.render(gl,
                                if (animated) shader1 else shader2)) {
                            setGeometryDirty(i)
                        }
                    }
                } else {
                    vao.model.first.ensureStored(gl)
                }
            }
        }
    }

    fun renderAlpha(gl: GL,
                    shader1: Shader,
                    shader2: Shader,
                    cam: Cam) {
        val relativeX = chunk.posBlock.x - cam.position.x
        val relativeY = chunk.posBlock.y - cam.position.y
        for (i in vao.indices) {
            val vao = this.vao[i]
            if (vao != null && vao.modelAlpha != null) {
                if (cam.frustum.inView(vao.modelAlpha.second) != 0) {
                    val relativeZ = (i shl 4) - cam.position.z
                    val distance = sqr(relativeX + 8) +
                            sqr(relativeY + 8) +
                            sqr(relativeZ + 8)
                    val animated = distance < 2304
                    gl.matrixStack.push { matrix ->
                        matrix.translate(relativeX.toFloat(),
                                relativeY.toFloat(), relativeZ.toFloat())
                        if (!vao.modelAlpha.first.render(gl,
                                if (animated) shader1 else shader2)) {
                            setGeometryDirty(i)
                        }
                    }
                } else {
                    vao.modelAlpha.first.ensureStored(gl)
                }
            }
        }
    }

    val isLoaded: Boolean
        get() = geometryInit[0] && chunk.isLoaded

    fun renderFrame(gl: GL,
                    frame: Model,
                    shader: Shader,
                    cam: Cam) {
        for (i in vao.indices) {
            val vao = this.vao[i]
            if (vao != null && vao.model != null) {
                gl.matrixStack.push { matrix ->
                    gl.setAttribute2f(4, 1.0f, 1.0f)
                    if (!chunk.isLoaded) {
                        gl.setAttribute4f(GL.COLOR_ATTRIBUTE, 1.0f, 0.0f, 0.0f,
                                1.0f)
                    } else if (geometryDirty[i].get()) {
                        gl.setAttribute4f(GL.COLOR_ATTRIBUTE, 1.0f, 1.0f, 0.0f,
                                1.0f)
                    } else {
                        gl.setAttribute4f(GL.COLOR_ATTRIBUTE, 0.0f, 1.0f, 0.0f,
                                1.0f)
                    }
                    matrix.translate(
                            (vao.model.second.minX - cam.position.x).toFloat(),
                            (vao.model.second.minY - cam.position.y).toFloat(),
                            (vao.model.second.minZ - cam.position.z).toFloat())
                    matrix.scale(
                            (vao.model.second.maxX - vao.model.second.minX).toFloat(),
                            (vao.model.second.maxY - vao.model.second.minY).toFloat(),
                            (vao.model.second.maxZ - vao.model.second.minZ).toFloat())
                    frame.render(gl, shader)
                }
            }
        }
    }

    @Synchronized
    fun replaceMesh(i: Int,
                    model: TerrainInfiniteChunkModel?) {
        if (visible[i] && model != null) {
            model.model?.let { it.first.weak = true }
            model.modelAlpha?.let { it.first.weak = true }
            model.model?.second?.add(chunk.posBlock.x.toDouble(),
                    chunk.posBlock.y.toDouble(),
                    (i shl 4).toDouble())
            model.modelAlpha?.second?.add(chunk.posBlock.x.toDouble(),
                    chunk.posBlock.y.toDouble(),
                    (i shl 4).toDouble())
            vao[i] = model
        } else {
            vao[i] = null
        }
        if (!geometryInit[0]) {
            geometryInit[i + 1] = true
            if (geometryInit.indices.any { geometryInit[it] }) {
                geometryInit[0] = true
            }
        }
    }

    fun setGeometryDirty() {
        for (i in vao.indices) {
            geometryDirty[i].set(true)
        }
        renderer.addToQueue(this)
    }

    fun setGeometryDirty(i: Int) {
        if (!geometryDirty[i].getAndSet(true)) {
            renderer.addToQueue(this, i)
        }
    }

    fun setSolid(i: Int,
                 value: Boolean) {
        if (i >= 0 && i < solid.size) {
            solid[i] = value
        }
    }

    fun resetPrepareVisible() {
        prepareVisible.fill { false }
    }

    fun setPrepareVisible(i: Int) {
        if (i >= 0 && i < prepareVisible.size) {
            prepareVisible[i] = true
        }
    }

    fun updateVisible() {
        for (i in visible.indices) {
            val oldVisible = visible[i]
            if (prepareVisible[i] && !oldVisible) {
                visible[i] = true
                setGeometryDirty(i)
            } else if (!prepareVisible[i] && oldVisible) {
                visible[i] = false
                vao[i] = null
            }
        }
    }

    fun setCulled(value: Boolean) {
        culled.fill { value }
    }

    fun setCulled(i: Int,
                  value: Boolean): Boolean {
        if (i >= 0 && i < culled.size) {
            val old = culled[i]
            culled[i] = value
            return old != value
        }
        return false
    }

    fun unsetGeometryDirty(i: Int): Boolean {
        return geometryDirty[i].getAndSet(false)
    }

    fun isSolid(i: Int): Boolean {
        return i < 0 || i >= solid.size || solid[i]
    }

    fun isVisible(i: Int): Boolean {
        return !(i < 0 || i >= visible.size) && visible[i]
    }

    fun isCulled(i: Int): Boolean {
        return !(i < 0 || i >= culled.size) && culled[i]
    }

    fun reset() {
        vao.fill { null }
        solid.fill { true }
        geometryDirty.forEach { it.set(true) }
        geometryInit.fill { false }
        culled.fill { false }
        renderer.addToQueue(this)
    }
}
