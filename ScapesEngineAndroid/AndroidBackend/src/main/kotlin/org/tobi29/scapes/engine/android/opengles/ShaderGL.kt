package org.tobi29.scapes.engine.android.opengles

import android.opengl.GLES30
import mu.KLogging
import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.graphics.Shader
import org.tobi29.scapes.engine.graphics.ShaderCompileInformation
import org.tobi29.scapes.engine.utils.shader.CompiledShader
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.util.*

internal class ShaderGL(private val shader: CompiledShader,
                        private val information: ShaderCompileInformation) : Shader {
    private val uniforms = ArrayDeque<Runnable>()
    private val floatBuffer = ByteBuffer.allocateDirect(16 shl 2).order(
            ByteOrder.nativeOrder()).asFloatBuffer()
    override var isStored = false
        private set
    private var valid: Boolean = false
    private var markAsDisposed: Boolean = false
    private var uniformLocations: IntArray? = null
    private var program: Int = 0
    private var used: Long = 0
    private var detach: Function0<Unit>? = null

    override fun ensureStored(gl: GL): Boolean {
        if (!isStored) {
            store(gl)
        }
        used = System.currentTimeMillis()
        return valid
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

    override fun dispose(gl: GL) {
        gl.check()
        GLES30.glDeleteProgram(program)
    }

    override fun reset() {
        assert(isStored)
        isStored = false
        detach?.invoke()
        detach = null
        valid = false
        markAsDisposed = false
    }

    override fun activate(gl: GL) {
        if (!ensureStored(gl)) {
            return
        }
        gl.check()
        GLES30.glUseProgram(program)
    }

    override fun updateUniforms(gl: GL) {
        gl.check()
        while (!uniforms.isEmpty()) {
            uniforms.poll().run()
        }
    }

    override fun uniformLocation(uniform: Int): Int {
        uniformLocations?.let { return it[uniform] }
        throw IllegalStateException("Shader not stored")
    }

    override fun setUniform1f(uniform: Int,
                              v0: Float) {
        uniforms.add(
                Runnable { GLES30.glUniform1f(uniformLocation(uniform), v0) })
    }

    override fun setUniform2f(uniform: Int,
                              v0: Float,
                              v1: Float) {
        uniforms.add(Runnable {
            GLES30.glUniform2f(uniformLocation(uniform), v0, v1)
        })
    }

    override fun setUniform3f(uniform: Int,
                              v0: Float,
                              v1: Float,
                              v2: Float) {
        uniforms.add(Runnable {
            GLES30.glUniform3f(uniformLocation(uniform), v0, v1, v2)
        })
    }

    override fun setUniform4f(uniform: Int,
                              v0: Float,
                              v1: Float,
                              v2: Float,
                              v3: Float) {
        uniforms.add(Runnable {
            GLES30.glUniform4f(uniformLocation(uniform), v0, v1, v2, v3)
        })
    }

    override fun setUniform1i(uniform: Int,
                              v0: Int) {
        uniforms.add(
                Runnable { GLES30.glUniform1i(uniformLocation(uniform), v0) })
    }

    override fun setUniform2i(uniform: Int,
                              v0: Int,
                              v1: Int) {
        uniforms.add(Runnable {
            GLES30.glUniform2i(uniformLocation(uniform), v0, v1)
        })
    }

    override fun setUniform3i(uniform: Int,
                              v0: Int,
                              v1: Int,
                              v2: Int) {
        uniforms.add(Runnable {
            GLES30.glUniform3i(uniformLocation(uniform), v0, v1, v2)
        })
    }

    override fun setUniform4i(uniform: Int,
                              v0: Int,
                              v1: Int,
                              v2: Int,
                              v3: Int) {
        uniforms.add(Runnable {
            GLES30.glUniform4i(uniformLocation(uniform), v0, v1, v2, v3)
        })
    }

    override fun setUniform1(uniform: Int,
                             values: FloatBuffer) {
        uniforms.add(Runnable {
            GLES30.glUniform1fv(uniformLocation(uniform),
                    values.remaining(), values)
        })
    }

    override fun setUniform2(uniform: Int,
                             values: FloatBuffer) {
        uniforms.add(Runnable {
            GLES30.glUniform2fv(uniformLocation(uniform),
                    values.remaining(), values)
        })
    }

    override fun setUniform3(uniform: Int,
                             values: FloatBuffer) {
        uniforms.add(Runnable {
            GLES30.glUniform3fv(uniformLocation(uniform),
                    values.remaining(), values)
        })
    }

    override fun setUniform4(uniform: Int,
                             values: FloatBuffer) {
        uniforms.add(Runnable {
            GLES30.glUniform4fv(uniformLocation(uniform),
                    values.remaining(), values)
        })
    }

    override fun setUniform1(uniform: Int,
                             values: IntBuffer) {
        uniforms.add(Runnable {
            GLES30.glUniform1iv(uniformLocation(uniform),
                    values.remaining(), values)
        })
    }

    override fun setUniform2(uniform: Int,
                             values: IntBuffer) {
        uniforms.add(Runnable {
            GLES30.glUniform2iv(uniformLocation(uniform),
                    values.remaining(), values)
        })
    }

    override fun setUniform3(uniform: Int,
                             values: IntBuffer) {
        uniforms.add(Runnable {
            GLES30.glUniform3iv(uniformLocation(uniform),
                    values.remaining(), values)
        })
    }

    override fun setUniform4(uniform: Int,
                             values: IntBuffer) {
        uniforms.add(Runnable {
            GLES30.glUniform4iv(uniformLocation(uniform),
                    values.remaining(), values)
        })
    }

    override fun setUniformMatrix2(uniform: Int,
                                   transpose: Boolean,
                                   matrices: FloatArray) {
        uniforms.add(Runnable {
            floatBuffer.put(matrices).flip()
            GLES30.glUniformMatrix4fv(uniform, matrices.size shr 2,
                    transpose, floatBuffer)
            floatBuffer.clear()
        })
    }

    override fun setUniformMatrix3(uniform: Int,
                                   transpose: Boolean,
                                   matrices: FloatArray) {
        uniforms.add(Runnable {
            floatBuffer.put(matrices).flip()
            GLES30.glUniformMatrix4fv(uniform, matrices.size / 9,
                    transpose, floatBuffer)
            floatBuffer.clear()
        })
    }

    override fun setUniformMatrix4(uniform: Int,
                                   transpose: Boolean,
                                   matrices: FloatArray) {
        uniforms.add(Runnable {
            floatBuffer.put(matrices).flip()
            GLES30.glUniformMatrix4fv(uniform, matrices.size shr 4,
                    transpose, floatBuffer)
            floatBuffer.clear()
        })
    }

    private fun store(gl: GL) {
        assert(!isStored)
        isStored = true
        gl.check()
        val processor = information.preCompile(gl)
        try {
            val program = GLUtils.createProgram(shader, processor.properties())
            this.program = program.first
            uniformLocations = program.second
        } catch (e: IOException) {
            logger.warn{"Failed to generate shader: ${e.message}"}
        }

        information.postCompile(gl, this)
        valid = true
        detach = gl.shaderTracker().attach(this)
    }

    companion object : KLogging()
}
