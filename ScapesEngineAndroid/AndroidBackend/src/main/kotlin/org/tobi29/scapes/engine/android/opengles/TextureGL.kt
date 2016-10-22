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
import org.tobi29.scapes.engine.graphics.Texture
import org.tobi29.scapes.engine.graphics.TextureFilter
import org.tobi29.scapes.engine.graphics.TextureWrap
import org.tobi29.scapes.engine.utils.graphics.generateMipMaps
import org.tobi29.scapes.engine.utils.math.max
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

internal open class TextureGL(override val engine: ScapesEngine, width: Int, height: Int,
                              buffer: ByteBuffer?, protected val mipmaps: Int, minFilter: TextureFilter,
                              magFilter: TextureFilter, wrapS: TextureWrap, wrapT: TextureWrap) : Texture {
    protected val dirtyFilter = AtomicBoolean(true)
    protected var textureID: Int = 0
    protected var width = -1
    protected var height = -1
    protected var minFilter = TextureFilter.NEAREST
    protected var magFilter = TextureFilter.NEAREST
    protected var wrapS = TextureWrap.REPEAT
    protected var wrapT = TextureWrap.REPEAT
    override var isStored = false
        protected set
    protected var markAsDisposed: Boolean = false
    protected var used: Long = 0
    protected var detach: Function0<Unit>? = null
    protected var buffers: Array<ByteBuffer?> = emptyArray()

    init {
        this.width = width
        this.height = height
        this.minFilter = minFilter
        this.magFilter = magFilter
        this.wrapS = wrapS
        this.wrapT = wrapT
        setBuffer(buffer)
    }

    override fun bind(gl: GL) {
        ensureStored(gl)
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
        markAsDisposed = true
    }

    override fun width(): Int {
        return width
    }

    override fun height(): Int {
        return height
    }

    override fun setWrap(wrapS: TextureWrap,
                         wrapT: TextureWrap) {
        this.wrapS = wrapS
        this.wrapT = wrapT
        dirtyFilter.set(true)
    }

    override fun setFilter(magFilter: TextureFilter,
                           minFilter: TextureFilter) {
        this.magFilter = magFilter
        this.minFilter = minFilter
        dirtyFilter.set(true)
    }

    override fun buffer(i: Int): ByteBuffer? {
        return buffers[i]
    }

    override fun setBuffer(buffer: ByteBuffer?) {
        buffers = generateMipMaps(buffer, { engine.allocate(it) }, width,
                height, mipmaps, minFilter === TextureFilter.LINEAR)
    }

    override fun ensureStored(gl: GL): Boolean {
        if (!isStored) {
            store(gl)
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

    override fun dispose(gl: GL) {
        assert(isStored)
        gl.check()
        intBuffers(textureID) { intBuffer ->
            GLES30.glDeleteTextures(1, intBuffer)
        }
    }

    override fun reset() {
        assert(isStored)
        isStored = false
        detach?.invoke()
        detach = null
        markAsDisposed = false
    }

    protected open fun store(gl: GL) {
        assert(!isStored)
        isStored = true
        gl.check()
        intBuffers { intBuffer ->
            GLES30.glGenTextures(1, intBuffer)
            textureID = intBuffer.get(0)
        }
        texture(gl)
        dirtyFilter.set(true)
        detach = gl.textureTracker().attach(this)
    }

    protected open fun texture(gl: GL) {
        assert(isStored)
        gl.check()
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureID)
        if (buffers.size > 1) {
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D,
                    GLES30.GL_TEXTURE_MAX_LEVEL, buffers.size - 1)
            GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA, width,
                    height, 0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE,
                    buffers[0])
            for (i in 1..buffers.size - 1) {
                GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, i, GLES30.GL_RGBA,
                        max(width shr i, 1),
                        max(height shr i, 1), 0, GLES30.GL_RGBA,
                        GLES30.GL_UNSIGNED_BYTE, buffers[i])
            }
        } else {
            GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA, width,
                    height, 0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE,
                    buffers[0])
        }
    }
}
