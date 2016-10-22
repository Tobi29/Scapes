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
import mu.KLogging
import org.tobi29.scapes.engine.graphics.FramebufferStatus
import org.tobi29.scapes.engine.graphics.RenderType
import org.tobi29.scapes.engine.utils.ThreadLocal
import org.tobi29.scapes.engine.utils.shader.CompiledShader
import org.tobi29.scapes.engine.utils.shader.ShaderGenerateException
import org.tobi29.scapes.engine.utils.shader.glsl.GLSLGenerator
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.IntBuffer

internal object GLUtils : KLogging() {
    private val SHADER_GENERATOR = ThreadLocal {
        GLSLGenerator(GLSLGenerator.Version.GLES_300)
    }

    fun renderType(renderType: RenderType): Int {
        when (renderType) {
            RenderType.TRIANGLES -> return GLES30.GL_TRIANGLES
            RenderType.LINES -> return GLES30.GL_LINES
            else -> throw IllegalArgumentException(
                    "Unknown render type: " + renderType)
        }
    }

    fun status(): FramebufferStatus {
        val status = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER)
        when (status) {
            GLES30.GL_FRAMEBUFFER_COMPLETE -> return FramebufferStatus.COMPLETE
            GLES30.GL_FRAMEBUFFER_UNSUPPORTED -> return FramebufferStatus.UNSUPPORTED
            else -> return FramebufferStatus.UNKNOWN
        }
    }

    fun drawbuffers(attachments: Int) {
        if (attachments < 0 || attachments > 15) {
            throw IllegalArgumentException(
                    "Attachments must be 0-15, was " + attachments)
        }
        val attachBuffer = ByteBuffer.allocateDirect(16 shl 2).order(
                ByteOrder.nativeOrder()).asIntBuffer()
        attachBuffer.limit(attachments)
        for (i in 0..attachments - 1) {
            attachBuffer.put(GLES30.GL_COLOR_ATTACHMENT0 + i)
        }
        attachBuffer.rewind()
        GLES30.glDrawBuffers(attachBuffer.remaining(), attachBuffer)
    }

    fun printLogShader(id: Int) {
        val lengthBuffer = ByteBuffer.allocateDirect(1 shl 2).order(
                ByteOrder.nativeOrder()).asIntBuffer()
        GLES30.glGetShaderiv(id, GLES30.GL_INFO_LOG_LENGTH, lengthBuffer)
        if (lengthBuffer.get(0) > 1) {
            val out = GLES30.glGetProgramInfoLog(id)
            logger.info { "Shader log: $out" }
        }
    }

    fun printLogProgram(id: Int) {
        val lengthBuffer = ByteBuffer.allocateDirect(1 shl 2).order(
                ByteOrder.nativeOrder()).asIntBuffer()
        GLES30.glGetProgramiv(id, GLES30.GL_INFO_LOG_LENGTH, lengthBuffer)
        if (lengthBuffer.get(0) > 1) {
            val out = GLES30.glGetProgramInfoLog(id)
            logger.info { "Program log: $out" }
        }
    }

    @Throws(IOException::class)
    fun createProgram(shader: CompiledShader,
                      properties: Map<String, String>): Pair<Int, IntArray> {
        try {
            val shaderGenerator = SHADER_GENERATOR.get()
            val vertexSource = shaderGenerator.generateVertex(shader,
                    properties)
            val fragmentSource = shaderGenerator.generateFragment(shader,
                    properties)
            val vertex = GLES30.glCreateShader(GLES30.GL_VERTEX_SHADER)
            GLES30.glShaderSource(vertex, vertexSource)
            GLES30.glCompileShader(vertex)
            printLogShader(vertex)
            val fragment = GLES30.glCreateShader(GLES30.GL_FRAGMENT_SHADER)
            GLES30.glShaderSource(fragment, fragmentSource)
            GLES30.glCompileShader(fragment)
            printLogShader(fragment)
            val program = GLES30.glCreateProgram()
            GLES30.glAttachShader(program, vertex)
            GLES30.glAttachShader(program, fragment)
            GLES30.glLinkProgram(program)
            val statusBuffer = ByteBuffer.allocateDirect(1 shl 2).order(
                    ByteOrder.nativeOrder()).asIntBuffer()
            GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, statusBuffer)
            if (statusBuffer.get(0) != GLES30.GL_TRUE) {
                logger.error { "Failed to link status bar!" }
                printLogProgram(program)
            }
            val uniforms = shader.uniforms()
            val uniformLocations = IntArray(uniforms.size)
            for (i in uniforms.indices) {
                val uniform = uniforms[i]
                if (uniform == null) {
                    uniformLocations[i] = -1
                } else {
                    uniformLocations[i] = GLES30.glGetUniformLocation(program,
                            uniform.name)
                }
            }
            GLES30.glDetachShader(program, vertex)
            GLES30.glDetachShader(program, fragment)
            GLES30.glDeleteShader(vertex)
            GLES30.glDeleteShader(fragment)
            return Pair(program, uniformLocations)
        } catch (e: ShaderGenerateException) {
            throw IOException(e)
        }
    }
}

val intBuffers = ThreadLocal {
    Array(4) {
        ByteBuffer.allocateDirect(4).order(
                ByteOrder.nativeOrder()).asIntBuffer()
    }
}

inline fun <R> intBuffers(block: (IntBuffer) -> R): R {
    val buffer0 = intBuffers.get()[0]
    try {
        return block(buffer0)
    } finally {
        buffer0.clear()
    }
}

inline fun <R> intBuffers(block: (IntBuffer, IntBuffer) -> R): R {
    val buffer0 = intBuffers.get()[0]
    val buffer1 = intBuffers.get()[1]
    try {
        return block(buffer0, buffer1)
    } finally {
        buffer0.clear()
        buffer1.clear()
    }
}

inline fun <R> intBuffers(block: (IntBuffer, IntBuffer, IntBuffer) -> R): R {
    val buffer0 = intBuffers.get()[0]
    val buffer1 = intBuffers.get()[1]
    val buffer2 = intBuffers.get()[1]
    try {
        return block(buffer0, buffer1, buffer2)
    } finally {
        buffer0.clear()
        buffer1.clear()
        buffer2.clear()
    }
}

inline fun <R> intBuffers(block: (IntBuffer, IntBuffer, IntBuffer, IntBuffer) -> R): R {
    val buffer0 = intBuffers.get()[0]
    val buffer1 = intBuffers.get()[1]
    val buffer2 = intBuffers.get()[2]
    val buffer3 = intBuffers.get()[3]
    try {
        return block(buffer0, buffer1, buffer2, buffer3)
    } finally {
        buffer0.clear()
        buffer1.clear()
        buffer2.clear()
        buffer3.clear()
    }
}

inline fun <R> intBuffers(v0: Int,
                          block: (IntBuffer) -> R): R {
    intBuffers { buffer0 ->
        buffer0.put(0, v0)
        return block(buffer0)
    }
}

inline fun <R> intBuffers(v0: Int,
                          v1: Int,
                          block: (IntBuffer, IntBuffer) -> R): R {
    intBuffers { buffer0, buffer1 ->
        buffer0.put(0, v0)
        buffer1.put(0, v1)
        return block(buffer0, buffer1)
    }
}

inline fun <R> intBuffers(v0: Int,
                          v1: Int,
                          v2: Int,
                          block: (IntBuffer, IntBuffer, IntBuffer) -> R): R {
    intBuffers { buffer0, buffer1, buffer2 ->
        buffer0.put(0, v0)
        buffer1.put(0, v1)
        buffer2.put(0, v2)
        return block(buffer0, buffer1, buffer2)
    }
}

inline fun <R> intBuffers(v0: Int,
                          v1: Int,
                          v2: Int,
                          v3: Int,
                          block: (IntBuffer, IntBuffer, IntBuffer, IntBuffer) -> R): R {
    intBuffers { buffer0, buffer1, buffer2, buffer3 ->
        buffer0.put(0, v0)
        buffer1.put(0, v1)
        buffer2.put(0, v2)
        buffer3.put(0, v3)
        return block(buffer0, buffer1, buffer2, buffer3)
    }
}
