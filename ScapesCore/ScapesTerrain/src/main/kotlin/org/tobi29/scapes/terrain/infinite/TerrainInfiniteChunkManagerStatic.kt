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

import org.tobi29.scapes.engine.utils.StampLock
import org.tobi29.scapes.engine.utils.math.abs
import org.tobi29.scapes.engine.utils.math.vector.MutableVector2i
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class TerrainInfiniteChunkManagerStatic<C : TerrainInfiniteBaseChunk<*>>(
        private val center: MutableVector2i,
        private val radius: Int) : TerrainInfiniteChunkManager<C> {
    private val size: Int
    private val array: Array<TerrainInfiniteBaseChunk<*>?>
    private val lock = StampLock()
    private val x = AtomicInteger()
    private val y = AtomicInteger()

    init {
        size = (radius shl 1) + 1
        array = arrayOfNulls(size * size)
    }

    override fun add(chunk: C) {
        lock.write {
            val xx = chunk.pos.x - x.get()
            val yy = chunk.pos.y - y.get()
            if (xx in 0..(size - 1) && yy >= 0 && yy < size) {
                val i = yy * size + xx
                array[i] = chunk
            }
        }
    }

    override fun remove(x: Int,
                        y: Int): C? {
        lock.write {
            val xx = x - this.x.get()
            val yy = y - this.y.get()
            if (xx in 0..(size - 1) && yy >= 0 && yy < size) {
                val i = yy * size + xx
                val chunk = array[i]
                if (chunk != null) {
                    assert(chunk.pos.x == x)
                    assert(chunk.pos.y == y)
                    array[i] = null
                    @Suppress("UNCHECKED_CAST")
                    return chunk as C
                }
            }
            return null
        }
    }

    override fun get(x: Int,
                     y: Int): C? {
        val value = lock.read {
            val xx = x - this.x.get()
            val yy = y - this.y.get()
            if (xx in 0..(size - 1) && yy >= 0 && yy < size) {
                val i = yy * size + xx
                array[i]
            } else {
                null
            }
        }
        value?.let {
            assert(value.pos.x == x)
            assert(value.pos.y == y)
        }
        @Suppress("UNCHECKED_CAST")
        return value as C?
    }

    override fun has(x: Int,
                     y: Int): Boolean {
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
                var xDiff = this.x.get() - xx
                val xDiffAbs = abs(xDiff)
                if (xDiffAbs > 0) {
                    if (xDiffAbs > size) {
                        clear()
                    } else {
                        for (i in 0..xDiff - 1) {
                            shiftXPositive()
                        }
                        xDiff = -xDiff
                        for (i in 0..xDiff - 1) {
                            shiftXNegative()
                        }
                    }
                    this.x.set(xx)
                }
                var yDiff = this.y.get() - yy
                val yDiffAbs = abs(yDiff)
                if (yDiffAbs > 0) {
                    if (yDiffAbs > size) {
                        clear()
                    } else {
                        for (i in 0..yDiff - 1) {
                            shiftYPositive()
                        }
                        yDiff = -yDiff
                        for (i in 0..yDiff - 1) {
                            shiftYNegative()
                        }
                    }
                    this.y.set(yy)
                }
            }
            return true
        }
        return false
    }

    private fun clear() {
        Arrays.fill(array, null)
    }

    private fun shiftXPositive() {
        var i = 0
        while (i < array.size) {
            val chunk = array[i]
            chunk?.let { it.dispose(); array[i] = null }
            i += size
        }
        i = size - 1
        while (i < array.size) {
            val chunk = array[i]
            chunk?.let { it.dispose(); array[i] = null }
            i += size
        }
        System.arraycopy(array, 0, array, 1, array.size - 1)
    }

    private fun shiftXNegative() {
        var i = 0
        while (i < array.size) {
            val chunk = array[i]
            chunk?.let { it.dispose(); array[i] = null }
            i += size
        }
        i = size - 1
        while (i < array.size) {
            val chunk = array[i]
            chunk?.let { it.dispose(); array[i] = null }
            i += size
        }
        System.arraycopy(array, 1, array, 0, array.size - 1)
    }

    private fun shiftYPositive() {
        for (i in 0..size - 1) {
            val chunk = array[i]
            chunk?.let { it.dispose(); array[i] = null }
        }
        for (i in array.size - size..array.size - 1) {
            val chunk = array[i]
            chunk?.let { it.dispose(); array[i] = null }
        }
        System.arraycopy(array, 0, array, size, array.size - size)
    }

    private fun shiftYNegative() {
        for (i in 0..size - 1) {
            val chunk = array[i]
            chunk?.let { it.dispose(); array[i] = null }
        }
        for (i in array.size - size..array.size - 1) {
            val chunk = array[i]
            chunk?.let { it.dispose(); array[i] = null }
        }
        System.arraycopy(array, size, array, 0, array.size - size)
    }
}