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

import kotlinx.coroutines.experimental.runBlocking
import org.tobi29.graphics.Image
import org.tobi29.graphics.decodePNG
import org.tobi29.io.*
import org.tobi29.io.filesystem.FilePath
import org.tobi29.io.filesystem.exists
import org.tobi29.io.filesystem.read
import org.tobi29.logging.KLogging
import kotlin.math.sqrt

class ServerInfo {
    val name: String
    val image: Image
    val buffer: ByteViewRO

    constructor(name: String,
                iconPath: FilePath) : this(name,
            image(iconPath))

    constructor(name: String,
                image: Image = Image()) {
        this.name = name
        this.image = image
        val buffer = MemoryViewStreamDefault()
        buffer.putString(name)
        CompressionUtil.compress(MemoryViewStream(
                ByteArray(image.view.size).viewBE.apply {
                    setBytes(0, image.view)
                }), buffer)
        buffer.flip()
        this.buffer = buffer.bufferSlice().ro
    }

    constructor(buffer: ByteViewRO) {
        val stream = MemoryViewReadableStream(buffer.viewBE)
        var image = Image()
        name = stream.getString(1024)
        try {
            val imageBuffer = CompressionUtil.decompress(stream)
            val size = sqrt((imageBuffer.size shr 2).toFloat()).toInt()
            image = Image(size, size, imageBuffer)
        } catch (e: IOException) {
            logger.warn { "Failed to decompress server icon: $e" }
        }
        this.buffer = buffer.ro
        this.image = image
    }

    companion object : KLogging() {
        private fun image(path: FilePath): Image {
            if (exists(path)) {
                try {
                    val image = read(path) { runBlocking { decodePNG(it) } }
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
