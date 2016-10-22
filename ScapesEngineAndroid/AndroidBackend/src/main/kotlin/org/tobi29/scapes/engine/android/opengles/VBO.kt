package org.tobi29.scapes.engine.android.opengles

import android.opengl.GLES30
import org.tobi29.scapes.engine.ScapesEngine
import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.graphics.ModelAttribute
import org.tobi29.scapes.engine.graphics.VertexType
import org.tobi29.scapes.engine.utils.math.FastMath
import org.tobi29.scapes.engine.utils.math.round
import java.nio.ByteBuffer
import java.util.*

internal class VBO(private val engine: ScapesEngine, attributes: List<ModelAttribute>,
                   length: Int) {
    private val stride: Int
    private val attributes = ArrayList<ModelAttributeData>()
    private var data: ByteBuffer? = null
    private var vertexID: Int = 0
    private var stored: Boolean = false

    init {
        var stride = 0
        for (attribute in attributes) {
            if (attribute.length() != length * attribute.size()) {
                throw IllegalArgumentException(
                        "Inconsistent attribute data length")
            }
            this.attributes.add(ModelAttributeData(attribute, stride))
            attribute.setOffset(stride)
            val size = attribute.size() * attribute.vertexType().bytes()
            stride += (size - 1 or 0x03) + 1
        }
        this.stride = stride
        val vertexBuffer = engine.allocate(length * stride)
        attributes.forEach { addToBuffer(it, length, vertexBuffer) }
        data = vertexBuffer
    }

    fun engine(): ScapesEngine {
        return engine
    }

    fun stride(): Int {
        return stride
    }

    fun replaceBuffer(gl: GL,
                      buffer: ByteBuffer) {
        assert(stored)
        gl.check()
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vertexID)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, buffer.capacity(), null,
                GLES30.GL_STREAM_DRAW)
        GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, 0, buffer.remaining(),
                buffer)
    }

    private fun storeAttribute(gl: GL,
                               attribute: ModelAttributeData) {
        gl.check()
        GLES30.glEnableVertexAttribArray(attribute.id)
        if (attribute.integer) {
            when (attribute.vertexType) {
                VertexType.FLOAT -> GLES30.glVertexAttribIPointer(attribute.id,
                        attribute.size,
                        GLES30.GL_FLOAT, stride, attribute.offset)
                VertexType.HALF_FLOAT -> GLES30.glVertexAttribIPointer(
                        attribute.id, attribute.size,
                        GLES30.GL_HALF_FLOAT, stride, attribute.offset)
                VertexType.BYTE -> GLES30.glVertexAttribIPointer(attribute.id,
                        attribute.size,
                        GLES30.GL_BYTE, stride, attribute.offset)
                VertexType.UNSIGNED_BYTE -> GLES30.glVertexAttribIPointer(
                        attribute.id, attribute.size,
                        GLES30.GL_UNSIGNED_BYTE, stride, attribute.offset)
                VertexType.SHORT -> GLES30.glVertexAttribIPointer(attribute.id,
                        attribute.size,
                        GLES30.GL_SHORT, stride, attribute.offset)
                VertexType.UNSIGNED_SHORT -> GLES30.glVertexAttribIPointer(
                        attribute.id, attribute.size,
                        GLES30.GL_UNSIGNED_SHORT, stride, attribute.offset)
                else -> throw IllegalArgumentException("Unknown vertex type!")
            }
        } else {
            when (attribute.vertexType) {
                VertexType.FLOAT -> GLES30.glVertexAttribPointer(attribute.id,
                        attribute.size,
                        GLES30.GL_FLOAT, attribute.normalized, stride,
                        attribute.offset)
                VertexType.HALF_FLOAT -> GLES30.glVertexAttribPointer(
                        attribute.id, attribute.size,
                        GLES30.GL_HALF_FLOAT, attribute.normalized, stride,
                        attribute.offset)
                VertexType.BYTE -> GLES30.glVertexAttribPointer(attribute.id,
                        attribute.size,
                        GLES30.GL_BYTE, attribute.normalized, stride,
                        attribute.offset)
                VertexType.UNSIGNED_BYTE -> GLES30.glVertexAttribPointer(
                        attribute.id, attribute.size,
                        GLES30.GL_UNSIGNED_BYTE, attribute.normalized,
                        stride, attribute.offset)
                VertexType.SHORT -> GLES30.glVertexAttribPointer(attribute.id,
                        attribute.size,
                        GLES30.GL_SHORT, attribute.normalized, stride,
                        attribute.offset)
                VertexType.UNSIGNED_SHORT -> GLES30.glVertexAttribPointer(
                        attribute.id, attribute.size,
                        GLES30.GL_UNSIGNED_SHORT, attribute.normalized,
                        stride, attribute.offset)
                else -> throw IllegalArgumentException("Unknown vertex type!")
            }
        }
        GLES30.glVertexAttribDivisor(attribute.id, attribute.divisor)
    }

    private fun addToBuffer(attribute: ModelAttribute,
                            vertices: Int,
                            buffer: ByteBuffer) {
        val floatArray = attribute.floatArray()
        if (floatArray == null) {
            val intArray = attribute.intArray() ?: throw IllegalArgumentException(
                    "Attribute contains no data")
            when (attribute.vertexType()) {
                VertexType.BYTE, VertexType.UNSIGNED_BYTE -> for (i in 0..vertices - 1) {
                    val `is` = i * attribute.size()
                    buffer.position(attribute.offset() + i * stride)
                    for (j in 0..attribute.size() - 1) {
                        val ij = `is` + j
                        buffer.put(intArray[ij].toByte())
                    }
                }
                VertexType.SHORT, VertexType.UNSIGNED_SHORT -> for (i in 0..vertices - 1) {
                    val `is` = i * attribute.size()
                    buffer.position(attribute.offset() + i * stride)
                    for (j in 0..attribute.size() - 1) {
                        val ij = `is` + j
                        buffer.putShort(intArray[ij].toShort())
                    }
                }
                else -> throw IllegalArgumentException(
                        "Invalid array in vao attribute!")
            }
        } else {
            when (attribute.vertexType()) {
                VertexType.FLOAT -> for (i in 0..vertices - 1) {
                    val `is` = i * attribute.size()
                    buffer.position(attribute.offset() + i * stride)
                    for (j in 0..attribute.size() - 1) {
                        val ij = `is` + j
                        buffer.putFloat(floatArray[ij])
                    }
                }
                VertexType.HALF_FLOAT -> for (i in 0..vertices - 1) {
                    val `is` = i * attribute.size()
                    buffer.position(attribute.offset() + i * stride)
                    for (j in 0..attribute.size() - 1) {
                        val ij = `is` + j
                        buffer.putShort(FastMath.convertFloatToHalf(
                                floatArray[ij]))
                    }
                }
                VertexType.BYTE -> if (attribute.normalized()) {
                    for (i in 0..vertices - 1) {
                        val `is` = i * attribute.size()
                        buffer.position(attribute.offset() + i * stride)
                        for (j in 0..attribute.size() - 1) {
                            val ij = `is` + j
                            buffer.put(round(floatArray[ij] * 127.0f).toByte())
                        }
                    }
                } else {
                    for (i in 0..vertices - 1) {
                        val `is` = i * attribute.size()
                        buffer.position(attribute.offset() + i * stride)
                        for (j in 0..attribute.size() - 1) {
                            val ij = `is` + j
                            buffer.put(round(floatArray[ij]).toByte())
                        }
                    }
                }
                VertexType.UNSIGNED_BYTE -> if (attribute.normalized()) {
                    for (i in 0..vertices - 1) {
                        val `is` = i * attribute.size()
                        buffer.position(attribute.offset() + i * stride)
                        for (j in 0..attribute.size() - 1) {
                            val ij = `is` + j
                            buffer.put(round(floatArray[ij] * 255.0f).toByte())
                        }
                    }
                } else {
                    for (i in 0..vertices - 1) {
                        val `is` = i * attribute.size()
                        buffer.position(attribute.offset() + i * stride)
                        for (j in 0..attribute.size() - 1) {
                            val ij = `is` + j
                            buffer.put(round(floatArray[ij]).toByte())
                        }
                    }
                }
                VertexType.SHORT -> if (attribute.normalized()) {
                    for (i in 0..vertices - 1) {
                        val `is` = i * attribute.size()
                        buffer.position(attribute.offset() + i * stride)
                        for (j in 0..attribute.size() - 1) {
                            val ij = `is` + j
                            buffer.putShort(
                                    round(floatArray[ij] * 32768.0f).toShort())
                        }
                    }
                } else {
                    for (i in 0..vertices - 1) {
                        val `is` = i * attribute.size()
                        buffer.position(attribute.offset() + i * stride)
                        for (j in 0..attribute.size() - 1) {
                            val ij = `is` + j
                            buffer.putShort(round(floatArray[ij]).toShort())
                        }
                    }
                }
                VertexType.UNSIGNED_SHORT -> if (attribute.normalized()) {
                    for (i in 0..vertices - 1) {
                        val `is` = i * attribute.size()
                        buffer.position(attribute.offset() + i * stride)
                        for (j in 0..attribute.size() - 1) {
                            val ij = `is` + j
                            buffer.putShort(
                                    round(floatArray[ij] * 65535.0f).toShort())
                        }
                    }
                } else {
                    for (i in 0..vertices - 1) {
                        val `is` = i * attribute.size()
                        buffer.position(attribute.offset() + i * stride)
                        for (j in 0..attribute.size() - 1) {
                            val ij = `is` + j
                            buffer.putShort(round(floatArray[ij]).toShort())
                        }
                    }
                }
                else -> throw IllegalArgumentException(
                        "Invalid array in vao attribute!")
            }
        }
    }

    fun canStore(): Boolean {
        return data != null
    }

    fun store(gl: GL,
              weak: Boolean) {
        assert(!stored)
        val data = data ?: throw IllegalStateException(
                "VBO cannot be stored anymore")
        stored = true
        gl.check()
        data.rewind()
        intBuffers { intBuffer ->
            GLES30.glGenBuffers(1, intBuffer)
            vertexID = intBuffer.get(0)
        }
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vertexID)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, data.remaining(), data,
                GLES30.GL_STATIC_DRAW)
        attributes.forEach { storeAttribute(gl, it) }
        if (weak) {
            this.data = null
        }
    }

    fun dispose(gl: GL) {
        assert(stored)
        stored = false
        gl.check()
        intBuffers(vertexID) { intBuffer ->
            GLES30.glDeleteBuffers(1, intBuffer)
        }
    }

    fun reset() {
        stored = false
    }

    private class ModelAttributeData(attribute: ModelAttribute, val offset: Int) {
        val vertexType: VertexType
        val id: Int
        val size: Int
        val divisor: Int
        val normalized: Boolean
        val integer: Boolean

        init {
            vertexType = attribute.vertexType()
            id = attribute.id()
            size = attribute.size()
            normalized = attribute.normalized()
            divisor = attribute.divisor()
            integer = attribute.intArray() != null
        }
    }
}
