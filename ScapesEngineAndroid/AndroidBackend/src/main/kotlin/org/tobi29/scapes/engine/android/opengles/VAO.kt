/*
 * Copyright 2012-2015 Tobi29
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
package org.tobi29.scapes.engine.android.opengles

import android.opengl.GLES30
import org.tobi29.scapes.engine.ScapesEngine
import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.graphics.Model
import org.tobi29.scapes.engine.graphics.Shader
import java.nio.ByteBuffer
import java.nio.ByteOrder

internal abstract class VAO protected constructor(protected val engine: ScapesEngine) : Model {
    private val floatBuffer = ByteBuffer.allocateDirect(16 shl 2).order(
            ByteOrder.nativeOrder()).asFloatBuffer()
    protected var used: Long = 0
    override var isStored = false
        protected set
    protected var markAsDisposed: Boolean = false
    override var weak: Boolean = false
    protected var detach: Function0<Unit>? = null

    override fun markAsDisposed() {
        markAsDisposed = true
    }

    abstract override fun render(gl: GL,
                                 shader: Shader): Boolean

    abstract override fun render(gl: GL,
                                 shader: Shader,
                                 length: Int): Boolean

    abstract override fun renderInstanced(gl: GL,
                                          shader: Shader,
                                          count: Int): Boolean

    abstract override fun renderInstanced(gl: GL,
                                          shader: Shader,
                                          length: Int,
                                          count: Int): Boolean

    override fun ensureStored(gl: GL): Boolean {
        if (!isStored) {
            val success = store(gl)
            used = System.currentTimeMillis()
            return success
        }
        used = System.currentTimeMillis()
        return true
    }

    override fun ensureDisposed(gl: GL) {
        if (isStored) {
            dispose(gl)
            reset()
        }
    }

    override fun isUsed(time: Long): Boolean {
        return time - used < 1000 && !markAsDisposed
    }

    override fun reset() {
        assert(isStored)
        isStored = false
        detach?.invoke()
        detach = null
        markAsDisposed = false
    }

    protected abstract fun store(gl: GL): Boolean

    protected fun shader(gl: GL,
                         shader: Shader) {
        gl.check()
        val matrix = gl.matrixStack().current()
        shader.activate(gl)
        shader.updateUniforms(gl)
        var uniformLocation = shader.uniformLocation(0)
        if (uniformLocation != -1) {
            floatBuffer.put(matrix.modelView().values()).flip()
            GLES30.glUniformMatrix4fv(uniformLocation,
                    floatBuffer.remaining() shr 4, false, floatBuffer)
            floatBuffer.clear()
        }
        uniformLocation = shader.uniformLocation(1)
        if (uniformLocation != -1) {
            floatBuffer.put(gl.modelViewProjectionMatrix().values()).flip()
            GLES30.glUniformMatrix4fv(uniformLocation,
                    floatBuffer.remaining() shr 4, false, floatBuffer)
            floatBuffer.clear()
        }
        uniformLocation = shader.uniformLocation(2)
        if (uniformLocation != -1) {
            floatBuffer.put(matrix.normal().values()).flip()
            GLES30.glUniformMatrix3fv(uniformLocation,
                    floatBuffer.remaining() / 9, false, floatBuffer)
            floatBuffer.clear()
        }
    }
}
