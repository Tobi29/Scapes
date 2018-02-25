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

package org.tobi29.scapes.terrain.infinite

import org.tobi29.stdex.ThreadLocal
import org.tobi29.stdex.ConcurrentHashMap
import org.tobi29.math.vector.Vector2i

class TerrainInfiniteChunkManagerDynamic<C : TerrainInfiniteBaseChunk<*>> : TerrainInfiniteChunkManager<C> {
    private val map = ConcurrentHashMap<ChunkLabel, C>()
    private var lastLookup: C? = null

    override fun add(chunk: C) {
        map.put(ChunkLabel(chunk.pos), chunk)
    }

    override fun remove(x: Int,
                        y: Int): C? {
        val label = LABEL.get().set(x, y)
        val chunk = map.remove(label)
        if (chunk === lastLookup) {
            lastLookup = null
        }
        return chunk
    }

    override fun get(x: Int,
                     y: Int): C? {
        val lastChunk = lastLookup
        if (lastChunk != null) {
            if (lastChunk.pos.x == x && lastChunk.pos.y == y) {
                return lastChunk
            }
        }
        val label = LABEL.get().set(x, y)
        val chunk = map[label]
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
        return map.containsKey(label)
    }

    override fun stream(): Sequence<C> {
        return map.values.asSequence()
    }

    override val chunks get() = map.size

    private data class ChunkLabel(var x: Int,
                                  var y: Int) {
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
