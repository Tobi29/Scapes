package org.tobi29.scapes.engine.android.opengles

import android.opengl.GLES30
import org.tobi29.scapes.engine.ScapesEngine
import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.graphics.TextureFilter
import org.tobi29.scapes.engine.graphics.TextureWrap
import java.nio.ByteBuffer

internal abstract class TextureFBO protected constructor(engine: ScapesEngine, width: Int, height: Int,
                                                         buffer: ByteBuffer?, mipmaps: Int, minFilter: TextureFilter,
                                                         magFilter: TextureFilter, wrapS: TextureWrap, wrapT: TextureWrap) : TextureGL(
        engine, width, height, buffer, mipmaps, minFilter, magFilter, wrapS,
        wrapT) {

    fun resize(width: Int,
               height: Int,
               gl: GL) {
        this.width = width
        this.height = height
        texture(gl)
    }

    override fun bind(gl: GL) {
        if (!isStored) {
            return
        }
        gl.check()
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureID)
        if (dirtyFilter.getAndSet(false)) {
            if (mipmaps > 0) {
                when (minFilter) {
                    TextureFilter.NEAREST -> GLES30.glTexParameteri(
                            GLES30.GL_TEXTURE_2D,
                            GLES30.GL_TEXTURE_MIN_FILTER,
                            GLES30.GL_NEAREST_MIPMAP_LINEAR)
                    TextureFilter.LINEAR -> GLES30.glTexParameteri(
                            GLES30.GL_TEXTURE_2D,
                            GLES30.GL_TEXTURE_MIN_FILTER,
                            GLES30.GL_LINEAR_MIPMAP_LINEAR)
                    else -> throw IllegalArgumentException(
                            "Illegal texture-filter!")
                }
            } else {
                when (minFilter) {
                    TextureFilter.NEAREST -> GLES30.glTexParameteri(
                            GLES30.GL_TEXTURE_2D,
                            GLES30.GL_TEXTURE_MIN_FILTER,
                            GLES30.GL_NEAREST)
                    TextureFilter.LINEAR -> GLES30.glTexParameteri(
                            GLES30.GL_TEXTURE_2D,
                            GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
                    else -> throw IllegalArgumentException(
                            "Illegal texture-filter!")
                }
            }
            when (magFilter) {
                TextureFilter.NEAREST -> GLES30.glTexParameteri(
                        GLES30.GL_TEXTURE_2D,
                        GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST)
                TextureFilter.LINEAR -> GLES30.glTexParameteri(
                        GLES30.GL_TEXTURE_2D,
                        GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
                else -> throw IllegalArgumentException(
                        "Illegal texture-filter!")
            }
            when (wrapS) {
                TextureWrap.REPEAT -> GLES30.glTexParameteri(
                        GLES30.GL_TEXTURE_2D,
                        GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_REPEAT)
                TextureWrap.CLAMP -> GLES30.glTexParameteri(
                        GLES30.GL_TEXTURE_2D,
                        GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
                else -> throw IllegalArgumentException("Illegal texture-wrap!")
            }
            when (wrapT) {
                TextureWrap.REPEAT -> GLES30.glTexParameteri(
                        GLES30.GL_TEXTURE_2D,
                        GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_REPEAT)
                TextureWrap.CLAMP -> GLES30.glTexParameteri(
                        GLES30.GL_TEXTURE_2D,
                        GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
                else -> throw IllegalArgumentException("Illegal texture-wrap!")
            }
        }
    }

    override fun markDisposed() {
        throw UnsupportedOperationException(
                "FBO texture should not be disposed")
    }

    override fun ensureStored(gl: GL): Boolean {
        throw UnsupportedOperationException(
                "FBO texture can only be managed by framebuffer")
    }

    override fun ensureDisposed(gl: GL) {
        throw UnsupportedOperationException(
                "FBO texture can only be managed by framebuffer")
    }

    override fun isUsed(time: Long): Boolean {
        return isStored
    }

    override fun reset() {
        assert(isStored)
        isStored = false
        markAsDisposed = false
    }

    override fun store(gl: GL) {
        assert(!isStored)
        isStored = true
        gl.check()
        intBuffers { intBuffer ->
            GLES30.glGenTextures(1, intBuffer)
            textureID = intBuffer.get(0)
        }
        texture(gl)
        dirtyFilter.set(true)
    }
}
