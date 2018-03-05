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

import org.tobi29.arrays.array2OfNulls
import org.tobi29.arrays.shift
import org.tobi29.math.vector.MutableVector2i
import org.tobi29.stdex.assert
import org.tobi29.stdex.atomic.AtomicInt
import org.tobi29.utils.StampLock

class TerrainInfiniteChunkManagerStatic<C : TerrainInfiniteBaseChunk<*>>(
    private val center: MutableVector2i,
    private val radius: Int
) : TerrainInfiniteChunkManager<C> {
    private val size = (radius shl 1) + 1
    private val array = array2OfNulls<TerrainInfiniteBaseChunk<*>?>(size, size)
    private val lock = StampLock()
    private val x = AtomicInt()
    private val y = AtomicInt()

    override fun add(chunk: C) {
        lock.write {
            val xx = chunk.pos.x - x.get()
            val yy = chunk.pos.y - y.get()
            if (xx in 0 until array.width && yy in 0 until array.height) {
                array[xx, yy] = chunk
            }
        }
    }

    override fun remove(
        x: Int,
        y: Int
    ): C? {
        return lock.write {
            val xx = x - this.x.get()
            val yy = y - this.y.get()
            if (xx in 0 until array.width && yy in 0 until array.height) {
                val chunk = array[xx, yy]
                if (chunk != null) {
                    assert { chunk.pos.x == x }
                    assert { chunk.pos.y == y }
                    array[xx, yy] = null
                    @Suppress("UNCHECKED_CAST")
                    chunk as C
                } else {
                    null
                }
            } else {
                null
            }
        }
    }

    override fun get(
        x: Int,
        y: Int
    ): C? {
        val value = lock.read {
            val xx = x - this.x.get()
            val yy = y - this.y.get()
            if (xx in 0 until array.width && yy in 0 until array.height) {
                array[xx, yy]
            } else {
                null
            }
        }
        value?.let {
            assert { value.pos.x == x }
            assert { value.pos.y == y }
        }
        @Suppress("UNCHECKED_CAST")
        return value as C?
    }

    override fun has(
        x: Int,
        y: Int
    ): Boolean {
        return get(x, y) != null
    }

    override fun stream(): Sequence<C> {
        return array.asSequence().filterNotNull().map {
            @Suppress("UNCHECKED_CAST")
            it as C
        }
    }

    override fun update(): Boolean {
        val x = center.x
        val y = center.y
        val xx = x - radius
        val yy = y - radius
        if (xx != this.x.get() || yy != this.y.get()) {
            lock.write {
                val xDiff = this.x.get() - xx
                val yDiff = this.y.get() - yy
                this.x.set(xx)
                this.y.set(yy)
                array.shift(
                    xDiff, yDiff,
                    { chunk, _, _ -> chunk?.dispose() }, { x, y -> null }
                )
            }
            return true
        }
        return false
    }
}
