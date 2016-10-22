package org.tobi29.scapes.engine.android.opengles

import android.opengl.GLES30
import org.tobi29.scapes.engine.Container
import org.tobi29.scapes.engine.ScapesEngine
import org.tobi29.scapes.engine.graphics.*
import org.tobi29.scapes.engine.utils.graphics.Image
import org.tobi29.scapes.engine.utils.math.max
import org.tobi29.scapes.engine.utils.math.pow
import org.tobi29.scapes.engine.utils.shader.CompiledShader
import java.nio.ByteBuffer
import java.nio.FloatBuffer

class GLAndroidGL(engine: ScapesEngine, container: Container) : GL(engine,
        container) {

    override fun createTexture(width: Int,
                               height: Int,
                               buffer: ByteBuffer,
                               mipmaps: Int,
                               minFilter: TextureFilter,
                               magFilter: TextureFilter,
                               wrapS: TextureWrap,
                               wrapT: TextureWrap): Texture {
        return TextureGL(engine, width, height, buffer, mipmaps,
                minFilter, magFilter, wrapS, wrapT)
    }

    override fun createFramebuffer(width: Int,
                                   height: Int,
                                   colorAttachments: Int,
                                   depth: Boolean,
                                   hdr: Boolean,
                                   alpha: Boolean): Framebuffer {
        return FBO(engine, width, height, colorAttachments, depth, hdr,
                alpha)
    }

    override fun createModelFast(attributes: List<ModelAttribute>,
                                 length: Int,
                                 renderType: RenderType): Model {
        val vbo = VBO(engine, attributes, length)
        return VAOFast(vbo, length, renderType)
    }

    override fun createModelStatic(attributes: List<ModelAttribute>,
                                   length: Int,
                                   index: IntArray,
                                   indexLength: Int,
                                   renderType: RenderType): Model {
        val vbo = VBO(engine, attributes, length)
        return VAOStatic(vbo, index, indexLength, renderType)
    }

    override fun createModelHybrid(attributes: List<ModelAttribute>,
                                   length: Int,
                                   attributesStream: List<ModelAttribute>,
                                   lengthStream: Int,
                                   renderType: RenderType): ModelHybrid {
        val vbo = VBO(engine, attributes, length)
        val vboStream = VBO(engine, attributesStream, lengthStream)
        return VAOHybrid(vbo, vboStream, renderType)
    }

    override fun createShader(shader: CompiledShader,
                              information: ShaderCompileInformation): Shader {
        return ShaderGL(shader, information)
    }

    override fun checkError(message: String) {
        val error = GLES30.glGetError()
        if (error != GLES30.GL_NO_ERROR) {
            val errorName: String
            when (error) {
                GLES30.GL_INVALID_ENUM -> errorName = "Enum argument out of range"
                GLES30.GL_INVALID_VALUE -> errorName = "Numeric argument out of range"
                GLES30.GL_INVALID_OPERATION -> errorName = "Operation illegal in current state"
                GLES30.GL_OUT_OF_MEMORY -> errorName = "Not enough memory left to execute command"
                GLES30.GL_INVALID_FRAMEBUFFER_OPERATION -> errorName = "Framebuffer object is not complete"
                else -> errorName = "Unknown error code"
            }
            throw GraphicsException(errorName + " in " + message)
        }
    }

    override fun clear(r: Float,
                       g: Float,
                       b: Float,
                       a: Float) {
        GLES30.glClearColor(r, g, b, a)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
    }

    override fun clearDepth() {
        GLES30.glClear(GLES30.GL_DEPTH_BUFFER_BIT)
    }

    override fun disableCulling() {
        GLES30.glDisable(GLES30.GL_CULL_FACE)
    }

    override fun disableDepthTest() {
        GLES30.glDisable(GLES30.GL_DEPTH_TEST)
    }

    override fun disableDepthMask() {
        GLES30.glDepthMask(false)
    }

    override fun disableWireframe() {
        //GLES30.glPolygonMode(GLES30.GL_FRONT_AND_BACK, GLES30.GL_FILL);
    }

    override fun disableScissor() {
        GLES30.glDisable(GLES30.GL_SCISSOR_TEST)
    }

    override fun enableCulling() {
        GLES30.glEnable(GLES30.GL_CULL_FACE)
    }

    override fun enableDepthTest() {
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glDepthFunc(GLES30.GL_LEQUAL)
    }

    override fun enableDepthMask() {
        GLES30.glDepthMask(true)
    }

    override fun enableWireframe() {
        //GLES30.glPolygonMode(GLES30.GL_FRONT_AND_BACK, GLES30.GL_LINE);
    }

    override fun enableScissor(x: Int,
                               y: Int,
                               width: Int,
                               height: Int) {
        GLES30.glEnable(GLES30.GL_SCISSOR_TEST)
        val h = engine.container.contentHeight() / 540.0
        GLES30.glScissor((x * h).toInt(),
                ((540.0 - y.toDouble() - height.toDouble()) * h).toInt(),
                (width * h).toInt(), (height * h).toInt())
    }

    override fun setBlending(mode: BlendingMode) {
        when (mode) {
            BlendingMode.NONE -> GLES30.glDisable(GLES30.GL_BLEND)
            BlendingMode.NORMAL -> {
                GLES30.glEnable(GLES30.GL_BLEND)
                GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA,
                        GLES30.GL_ONE_MINUS_SRC_ALPHA)
            }
            BlendingMode.ADD -> {
                GLES30.glEnable(GLES30.GL_BLEND)
                GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_DST_ALPHA)
            }
            BlendingMode.INVERT -> {
                GLES30.glEnable(GLES30.GL_BLEND)
                GLES30.glBlendFunc(GLES30.GL_ONE_MINUS_DST_COLOR,
                        GLES30.GL_ONE_MINUS_SRC_COLOR)
            }
        }
    }

    override fun viewport(x: Int,
                          y: Int,
                          width: Int,
                          height: Int) {
        GLES30.glViewport(x, y, width, height)
    }

    override fun screenShot(x: Int,
                            y: Int,
                            width: Int,
                            height: Int): Image {
        GLES30.glReadBuffer(GLES30.GL_FRONT)
        val buffer = container.allocate(width * height shl 2)
        GLES30.glReadPixels(x, y, width, height, GLES30.GL_RGBA,
                GLES30.GL_UNSIGNED_BYTE, buffer)
        return Image(width, height, buffer)
    }

    override fun screenShotFBO(fbo: Framebuffer): Image {
        val buffer = container.allocate(fbo.width() * fbo.height() shl 2)
        //GLES30.glGetTexImage(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA,
        //        GLES30.GL_UNSIGNED_BYTE, buffer);
        return Image(fbo.width(), fbo.height(), buffer)
    }

    override fun setAttribute1f(id: Int,
                                v0: Float) {
        GLES30.glVertexAttrib1f(id, v0)
    }

    override fun setAttribute2f(id: Int,
                                v0: Float,
                                v1: Float) {
        GLES30.glVertexAttrib2f(id, v0, v1)
    }

    override fun setAttribute3f(id: Int,
                                v0: Float,
                                v1: Float,
                                v2: Float) {
        GLES30.glVertexAttrib3f(id, v0, v1, v2)
    }

    override fun setAttribute4f(id: Int,
                                v0: Float,
                                v1: Float,
                                v2: Float,
                                v3: Float) {
        GLES30.glVertexAttrib4f(id, v0, v1, v2, v3)
    }

    override fun setAttribute2f(uniform: Int,
                                values: FloatBuffer) {
        GLES30.glVertexAttrib2fv(uniform, values)
    }

    override fun setAttribute3f(uniform: Int,
                                values: FloatBuffer) {
        GLES30.glVertexAttrib3fv(uniform, values)
    }

    override fun setAttribute4f(uniform: Int,
                                values: FloatBuffer) {
        GLES30.glVertexAttrib4fv(uniform, values)
    }

    override fun replaceTexture(x: Int,
                                y: Int,
                                width: Int,
                                height: Int,
                                buffer: ByteBuffer) {
        GLES30.glTexSubImage2D(GLES30.GL_TEXTURE_2D, 0, x, y, width, height,
                GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, buffer)
    }

    override fun replaceTextureMipMap(x: Int,
                                      y: Int,
                                      width: Int,
                                      height: Int,
                                      vararg buffers: ByteBuffer?) {
        GLES30.glTexSubImage2D(GLES30.GL_TEXTURE_2D, 0, x, y, width, height,
                GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, buffers[0])
        for (i in 1..buffers.size - 1) {
            val scale = pow(2f, i.toFloat()).toInt()
            GLES30.glTexSubImage2D(GLES30.GL_TEXTURE_2D, i, x / scale,
                    y / scale, max(width / scale, 1),
                    max(height / scale, 1), GLES30.GL_RGBA,
                    GLES30.GL_UNSIGNED_BYTE, buffers[i])
        }
    }

    override fun activeTexture(i: Int) {
        if (i < 0 || i > 31) {
            throw IllegalArgumentException(
                    "Active Texture must be 0-31, was " + i)
        }
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + i)
    }
}
