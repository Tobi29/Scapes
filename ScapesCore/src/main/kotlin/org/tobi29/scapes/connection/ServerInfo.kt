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

package org.tobi29.scapes.connection

import mu.KLogging
import org.tobi29.scapes.engine.utils.ByteBuffer
import org.tobi29.scapes.engine.utils.UnsupportedJVMException
import org.tobi29.scapes.engine.utils.graphics.Image
import org.tobi29.scapes.engine.utils.graphics.decodePNG
import org.tobi29.scapes.engine.utils.io.ByteBufferStream
import org.tobi29.scapes.engine.utils.io.CompressionUtil
import org.tobi29.scapes.engine.utils.io.filesystem.FilePath
import org.tobi29.scapes.engine.utils.io.filesystem.exists
import org.tobi29.scapes.engine.utils.io.filesystem.read
import org.tobi29.scapes.engine.utils.math.sqrt
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

class ServerInfo {
    val name: String
    val image: Image
    private val buffer: ByteBuffer

    constructor(name: String,
                iconPath: FilePath) : this(name,
            image(iconPath))

    constructor(name: String,
                image: Image = Image()) {
        this.name = name
        this.image = image
        val buffer: ByteBuffer
        try {
            buffer = CompressionUtil.compress(ByteBufferStream(image.buffer))
        } catch (e: IOException) {
            throw UnsupportedJVMException(e)
        }

        buffer.flip()
        val array = name.toByteArray(StandardCharsets.UTF_8)
        this.buffer = ByteBuffer(1 + array.size + buffer.remaining())
        this.buffer.put(array.size.toByte())
        this.buffer.put(array)
        this.buffer.put(buffer)
        this.buffer.flip()
    }

    constructor(buffer: ByteBuffer) {
        var image = Image()
        val array = ByteArray(buffer.get().toInt())
        buffer.get(array)
        name = String(array, StandardCharsets.UTF_8)
        try {
            val imageBuffer = CompressionUtil.decompress(
                    ByteBufferStream(buffer))
            imageBuffer.flip()
            val size = sqrt((imageBuffer.remaining() shr 2).toFloat()).toInt()
            image = Image(size, size, imageBuffer)
        } catch (e: IOException) {
            logger.warn { "Failed to decompress server icon: $e" }
        }

        this.buffer = buffer
        this.image = image
    }

    fun getBuffer(): ByteBuffer {
        return buffer.asReadOnlyBuffer()
    }

    companion object : KLogging() {
        private fun image(path: FilePath): Image {
            if (exists(path)) {
                try {
                    val image = read(path) { decodePNG(it) }
                    val width = image.width
                    if (width != image.height) {
                        logger.warn { "The icon has to be square sized." }
                    } else if (width > 256) {
                        logger.warn { "The icon may not be larger than 256x256." }
                    } else {
                        return image
                    }
                } catch (e: IOException) {
                    logger.warn { "Unable to load icon: ${e.message}" }
                }

            }
            return Image()
        }
    }
}
