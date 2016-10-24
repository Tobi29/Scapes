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

package org.tobi29.scapes.chunk.terrain.infinite

import java8.util.stream.Stream
import org.tobi29.scapes.engine.utils.math.vector.Vector2i
import org.tobi29.scapes.engine.utils.stream
import org.tobi29.scapes.entity.server.EntityServer
import java.util.concurrent.ConcurrentHashMap

class TerrainInfiniteChunkManagerServer : TerrainInfiniteChunkManager<EntityServer> {
    private val chunks = ConcurrentHashMap<Vector2i, TerrainInfiniteChunkServer>()
    private var lastLookup: TerrainInfiniteChunkServer? = null

    fun add(chunk: TerrainInfiniteChunkServer) {
        chunks.put(chunk.pos, chunk)
    }

    fun remove(x: Int,
               y: Int): TerrainInfiniteChunkServer? {
        val chunk = chunks.remove(Vector2i(x, y))
        if (chunk === lastLookup) {
            lastLookup = null
        }
        return chunk
    }

    override fun get(x: Int,
                     y: Int): TerrainInfiniteChunkServer? {
        val lastChunk = lastLookup
        if (lastChunk != null) {
            if (lastChunk.pos.x == x && lastChunk.pos.y == y) {
                return lastChunk
            }
        }
        val chunk = chunks[Vector2i(x, y)]
        lastLookup = chunk
        if (chunk == null) {
            return null
        } else {
            return chunk
        }
    }

    override fun has(x: Int,
                     y: Int): Boolean {
        return chunks.containsKey(Vector2i(x, y))
    }

    override fun stream(): Stream<TerrainInfiniteChunkServer> {
        return chunks.values.stream()
    }

    fun chunks(): Int {
        return chunks.size
    }
}
