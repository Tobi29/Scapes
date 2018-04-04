/*
 * Copyright 2012-2018 Tobi29
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
import org.tobi29.graphics.*
import org.tobi29.io.*
import org.tobi29.io.filesystem.FilePath
import org.tobi29.io.filesystem.exists
import org.tobi29.io.filesystem.read
import org.tobi29.logging.KLogging
import kotlin.math.sqrt

class ServerInfo {
    val name: String
    val image: IntByteViewBitmap<RGBA>
    val buffer: ByteViewRO

    constructor(
        name: String,
        iconPath: FilePath
    ) : this(name, image(iconPath))

    constructor(
        name: String,
        image: Bitmap<*, *> = MutableIntByteViewBitmap(1, 1, RGBA)
    ) {
        this.name = name
        this.image = image.asByteViewRGBABitmap()
        val buffer = MemoryViewStreamDefault()
        buffer.putString(name)
        CompressionUtil.compress(
            MemoryViewReadableStream(this.image.data.array.viewBE),
            buffer
        )
        buffer.flip()
        this.buffer = buffer.bufferSlice().ro
    }

    constructor(buffer: ByteViewRO) {
        val stream = MemoryViewReadableStream(buffer.viewBE)
        var image: IntByteViewBitmap<RGBA> =
            MutableIntByteViewBitmap(1, 1, RGBA)
        name = stream.getString(1024)
        try {
            val imageBuffer = CompressionUtil.decompress(stream)
            val size = sqrt((imageBuffer.size shr 2).toFloat()).toInt()
            image = IntByteViewBitmap(imageBuffer, size, size, RGBA)
        } catch (e: IOException) {
            logger.warn { "Failed to decompress server icon: $e" }
        }
        this.buffer = buffer.ro
        this.image = image
    }

    companion object : KLogging()
}

private fun image(path: FilePath): IntByteViewBitmap<RGBA> {
    if (exists(path)) {
        try {
            val image = read(path) { runBlocking { decodePng(it) } }
            val width = image.width
            if (width != image.height) {
                ServerInfo.logger.warn { "The icon has to be square sized." }
            } else if (width > 256) {
                ServerInfo.logger.warn { "The icon may not be larger than 256x256." }
            } else {
                return image.asByteViewRGBABitmap()
            }
        } catch (e: IOException) {
            ServerInfo.logger.warn { "Unable to load icon: ${e.message}" }
        }

    }
    return MutableIntByteViewBitmap(1, 1, RGBA)
}
