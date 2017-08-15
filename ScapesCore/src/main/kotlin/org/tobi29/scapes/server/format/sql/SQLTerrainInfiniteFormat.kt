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

package org.tobi29.scapes.server.format.sql

import org.tobi29.scapes.engine.sql.SQLQuery
import org.tobi29.scapes.engine.sql.SQLReplace
import org.tobi29.scapes.engine.sql.invoke
import org.tobi29.scapes.engine.utils.io.ByteBuffer
import org.tobi29.scapes.engine.utils.io.ByteBufferStream
import org.tobi29.scapes.engine.utils.io.IOException
import org.tobi29.scapes.engine.utils.io.tag.binary.readBinary
import org.tobi29.scapes.engine.utils.io.tag.binary.writeBinary
import org.tobi29.scapes.engine.utils.logging.KLogging
import org.tobi29.scapes.engine.utils.math.vector.Vector2i
import org.tobi29.scapes.engine.utils.tag.TagMap
import org.tobi29.scapes.server.format.TerrainInfiniteFormat

class SQLTerrainInfiniteFormat(private val getChunk: SQLQuery,
                               private val replaceChunk: SQLReplace) : TerrainInfiniteFormat {
    private val stream = ByteBufferStream(growth = { it + 1048576 })

    @Synchronized override fun chunkTags(chunks: List<Vector2i>): List<TagMap?> {
        val maps = ArrayList<TagMap?>(chunks.size)
        for (chunk in chunks) {
            try {
                maps.add(chunkTag(chunk.x, chunk.y))
            } catch (e: IOException) {
                logger.error { "Failed to load chunk: $e" }
                maps.add(null)
            }
        }
        return maps
    }

    @Synchronized override fun putChunkTags(
            chunks: List<Pair<Vector2i, TagMap>>) {
        val values = ArrayList<Array<Any>>(chunks.size)
        for ((pos, map) in chunks) {
            stream.buffer().clear()
            map.writeBinary(stream, 1)
            stream.buffer().flip()
            val array = ByteArray(stream.buffer().remaining())
            stream.buffer().get(array)
            values.add(arrayOf(pos.x, pos.y, array))
            if (values.size >= 64) {
                replaceChunk(*values.toTypedArray())
                values.clear()
            }
        }
        if (!values.isEmpty()) {
            replaceChunk(*values.toTypedArray())
        }
    }

    override fun dispose() {
    }

    private fun chunkTag(x: Int,
                         y: Int): TagMap? {
        val rows = getChunk(x, y)
        if (!rows.isEmpty()) {
            val row = rows[0]
            if (row[0] is ByteArray) {
                val array = row[0] as ByteArray
                return readBinary(ByteBufferStream(ByteBuffer.wrap(array)))
            }
        }
        return null
    }

    companion object : KLogging()
}
