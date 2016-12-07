/*
 * Copyright 2012-2016 Tobi29
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

import mu.KLogging
import org.tobi29.scapes.engine.sql.SQLDatabase
import org.tobi29.scapes.engine.utils.BufferCreator
import org.tobi29.scapes.engine.utils.io.ByteBufferStream
import org.tobi29.scapes.engine.utils.io.tag.TagStructure
import org.tobi29.scapes.engine.utils.io.tag.binary.TagStructureBinary
import org.tobi29.scapes.engine.utils.math.vector.Vector2i
import org.tobi29.scapes.server.format.TerrainInfiniteFormat
import java.io.IOException
import java.nio.ByteBuffer
import java.util.*

class SQLTerrainInfiniteFormat(private val database: SQLDatabase,
                               private val table: String,
                               private val world: String) : TerrainInfiniteFormat {
    private val stream = ByteBufferStream({ BufferCreator.bytes(it) },
            { it + 1048576 })
    private val getChunk = database.compileQuery(table, arrayOf("Data"),
            "World", "X", "Y")

    @Synchronized override fun chunkTags(
            chunks: List<Vector2i>): List<TagStructure?> {
        val tagStructures = ArrayList<TagStructure?>(chunks.size)
        for (chunk in chunks) {
            try {
                tagStructures.add(chunkTag(chunk.x, chunk.y))
            } catch (e: IOException) {
                logger.error { "Failed to load chunk: $e" }
                tagStructures.add(null)
            }
        }
        return tagStructures
    }

    @Synchronized override fun putChunkTags(
            chunks: List<Pair<Vector2i, TagStructure>>) {
        val values = ArrayList<Array<Any>>(chunks.size)
        for (chunk in chunks) {
            stream.buffer().clear()
            TagStructureBinary.write(stream, chunk.second, 1.toByte())
            stream.buffer().flip()
            val array = ByteArray(stream.buffer().remaining())
            stream.buffer().get(array)
            values.add(arrayOf<Any>(world, chunk.first.x, chunk.first.y, array))
            if (values.size >= 64) {
                database.replace(table, arrayOf("World", "X", "Y", "Data"),
                        values)
                values.clear()
            }
        }
        if (!values.isEmpty()) {
            database.replace(table, arrayOf("World", "X", "Y", "Data"), values)
        }
    }

    override fun dispose() {
    }

    private fun chunkTag(x: Int,
                         y: Int): TagStructure? {
        val rows = getChunk.run(world, x, y)
        if (!rows.isEmpty()) {
            val row = rows[0]
            if (row[0] is ByteArray) {
                val array = row[0] as ByteArray
                val tagStructure = TagStructureBinary.read(
                        ByteBufferStream(ByteBuffer.wrap(array)),
                        TagStructure())
                return tagStructure
            }
        }
        return null
    }

    companion object : KLogging()
}
