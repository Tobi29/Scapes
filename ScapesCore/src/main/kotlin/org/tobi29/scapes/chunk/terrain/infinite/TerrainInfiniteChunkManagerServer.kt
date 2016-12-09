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

import org.tobi29.scapes.engine.utils.ThreadLocal
import org.tobi29.scapes.engine.utils.math.vector.Vector2i
import org.tobi29.scapes.entity.server.EntityServer
import java.util.concurrent.ConcurrentHashMap

class TerrainInfiniteChunkManagerServer : TerrainInfiniteChunkManager<EntityServer> {
    private val chunks = ConcurrentHashMap<ChunkLabel, TerrainInfiniteChunkServer>()
    private var lastLookup: TerrainInfiniteChunkServer? = null

    fun add(chunk: TerrainInfiniteChunkServer) {
        chunks.put(ChunkLabel(chunk.pos), chunk)
    }

    fun remove(x: Int,
               y: Int): TerrainInfiniteChunkServer? {
        val label = LABEL.get().set(x, y)
        val chunk = chunks.remove(label)
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
        val label = LABEL.get().set(x, y)
        val chunk = chunks[label]
        lastLookup = chunk
        if (chunk == null) {
            return null
        } else {
            return chunk
        }
    }

    override fun has(x: Int,
                     y: Int): Boolean {
        val label = LABEL.get().set(x, y)
        return chunks.containsKey(label)
    }

    override fun stream(): Sequence<TerrainInfiniteChunkServer> {
        return chunks.values.asSequence()
    }

    fun chunks(): Int {
        return chunks.size
    }

    private data class ChunkLabel(var x: Int, var y: Int) {
        constructor(pos: Vector2i) : this(pos.x, pos.y)

        fun set(x: Int,
                y: Int): ChunkLabel {
            this.x = x
            this.y = y
            return this
        }
    }

    companion object {
        private val LABEL = ThreadLocal { ChunkLabel(0, 0) }
    }
}
