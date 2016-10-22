package org.tobi29.scapes.engine.android.openal

import mu.KLogging
import org.tobi29.scapes.engine.backends.openal.openal.OpenAL
import org.tobi29.scapes.engine.sound.AudioFormat
import org.tobi29.scapes.engine.sound.SoundException
import org.tobi29.scapes.engine.utils.math.cos
import org.tobi29.scapes.engine.utils.math.sin
import org.tobi29.scapes.engine.utils.math.toRad
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import paulscode.android.sound.ALAN
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AndroidOpenAL : OpenAL {

    private val intBuffer = IntArray(1)
    private var directBuffer = ByteArray(0)

    init {
        directBuffer(4 shl 10 shl 10)
    }

    override fun checkError(message: String) {
        val error = ALAN.alGetError()
        if (error != ALAN.AL_NO_ERROR) {
            throw SoundException(
                    ALAN.alGetString(error) + " in " + message)
        }
    }

    override fun create() {
        ALAN.create()
        logger.info("OpenAL: ${ALAN.alGetString(
                ALAN.AL_VERSION)} (Vendor: ${ALAN.alGetString(
                ALAN.AL_VENDOR)}, Renderer: ${ALAN.alGetString(
                ALAN.AL_RENDERER)})")
        ALAN.alSpeedOfSound(343.3f)
        ALAN.alListenerfv(ALAN.AL_ORIENTATION,
                floatArrayOf(0.0f, 0.0f, -1.0f, 0.0f, 1.0f, 0.0f))
        ALAN.alListener3f(ALAN.AL_POSITION, 0.0f, 0.0f, 0.0f)
        ALAN.alListener3f(ALAN.AL_VELOCITY, 0.0f, 0.0f, 0.0f)
    }

    override fun destroy() {
        ALAN.destroy()
    }

    override fun setListener(position: Vector3d,
                             orientation: Vector3d,
                             velocity: Vector3d) {
        val cos = cos(orientation.floatX().toRad())
        val lookX = cos(orientation.floatZ().toRad()) * cos
        val lookY = sin(orientation.floatZ().toRad()) * cos
        val lookZ = sin(orientation.floatX().toRad())
        ALAN.alListenerfv(ALAN.AL_ORIENTATION,
                floatArrayOf(lookX, lookY, lookZ, 0.0f, 0.0f, 1.0f))
        ALAN.alListener3f(ALAN.AL_POSITION, position.floatX(),
                position.floatY(), position.floatZ())
        ALAN.alListener3f(ALAN.AL_VELOCITY, velocity.floatX(),
                velocity.floatY(), velocity.floatZ())
    }

    override fun createSource(): Int {
        ALAN.alGenSources(1, intBuffer)
        return intBuffer[0]
    }

    override fun deleteSource(id: Int) {
        intBuffer[0] = id
        ALAN.alDeleteSources(1, intBuffer)
    }

    override fun setBuffer(id: Int,
                           value: Int) {
        ALAN.alSourcei(id, ALAN.AL_BUFFER, value)
    }

    override fun setPitch(id: Int,
                          value: Float) {
        ALAN.alSourcef(id, ALAN.AL_PITCH, value)
    }

    override fun setGain(id: Int,
                         value: Float) {
        ALAN.alSourcef(id, ALAN.AL_GAIN, value)
    }

    override fun setLooping(id: Int,
                            value: Boolean) {
        ALAN.alSourcei(id, ALAN.AL_LOOPING,
                if (value) ALAN.AL_TRUE else ALAN.AL_FALSE)
    }

    override fun setRelative(id: Int,
                             value: Boolean) {
        ALAN.alSourcei(id, ALAN.AL_SOURCE_RELATIVE,
                if (value) ALAN.AL_TRUE else ALAN.AL_FALSE)
    }

    override fun setPosition(id: Int,
                             pos: Vector3d) {
        ALAN.alSource3f(id, ALAN.AL_POSITION, pos.floatX(), pos.floatY(),
                pos.floatZ())
    }

    override fun setVelocity(id: Int,
                             vel: Vector3d) {
        ALAN.alSource3f(id, ALAN.AL_VELOCITY, vel.floatX(), vel.floatY(),
                vel.floatZ())
    }

    override fun play(id: Int) {
        ALAN.alSourcePlay(id)
    }

    override fun stop(id: Int) {
        ALAN.alSourceStop(id)
    }

    override fun createBuffer(): Int {
        ALAN.alGenBuffers(1, intBuffer)
        return intBuffer[0]
    }

    override fun deleteBuffer(id: Int) {
        intBuffer[0] = id
        ALAN.alDeleteBuffers(1, intBuffer)
    }

    override fun storeBuffer(id: Int,
                             format: AudioFormat,
                             buffer: ByteBuffer,
                             rate: Int) {
        val size = buffer.remaining()
        when (format) {
            AudioFormat.MONO -> ALAN.alBufferData(id, ALAN.AL_FORMAT_MONO16,
                    direct(buffer),
                    size, rate)
            AudioFormat.STEREO -> ALAN.alBufferData(id, ALAN.AL_FORMAT_STEREO16,
                    direct(buffer),
                    size, rate)
        }
    }

    override fun isPlaying(id: Int): Boolean {
        ALAN.alGetSourcei(id, ALAN.AL_SOURCE_STATE, intBuffer)
        return intBuffer[0] == ALAN.AL_PLAYING
    }

    override fun isStopped(id: Int): Boolean {
        ALAN.alGetSourcei(id, ALAN.AL_SOURCE_STATE, intBuffer)
        val state = intBuffer[0]
        return state != ALAN.AL_PLAYING && state != ALAN.AL_PAUSED
    }

    override fun getBuffersQueued(id: Int): Int {
        ALAN.alGetSourcei(id, ALAN.AL_BUFFERS_QUEUED, intBuffer)
        return intBuffer[0]
    }

    override fun getBuffersProcessed(id: Int): Int {
        ALAN.alGetSourcei(id, ALAN.AL_BUFFERS_PROCESSED, intBuffer)
        return intBuffer[0]
    }

    override fun queue(id: Int,
                       buffer: Int) {
        intBuffer[0] = buffer
        ALAN.alSourceQueueBuffers(id, 1, intBuffer)
    }

    override fun unqueue(id: Int): Int {
        ALAN.alSourceUnqueueBuffers(id, 1, intBuffer)
        return intBuffer[0]
    }

    override fun getBuffer(id: Int): Int {
        ALAN.alGetSourcei(id, ALAN.AL_BUFFER, intBuffer)
        return intBuffer[0]
    }

    private fun direct(buffer: ByteBuffer): ByteArray {
        if (buffer.order() != ByteOrder.nativeOrder()) {
            throw IllegalArgumentException(
                    "Buffer does not use native byte order")
        }
        direct(buffer.remaining())
        buffer.get(directBuffer, 0, buffer.remaining())
        buffer.flip()
        return directBuffer
    }

    private fun direct(size: Int) {
        if (directBuffer.size < size) {
            val capacity = (size shr 10) + 1 shl 10
            logger.debug { "Resizing direct buffer: $capacity ($size)" }
            directBuffer(capacity)
        }
    }

    private fun directBuffer(capacity: Int) {
        directBuffer = ByteArray(capacity)
    }

    companion object : KLogging()
}
