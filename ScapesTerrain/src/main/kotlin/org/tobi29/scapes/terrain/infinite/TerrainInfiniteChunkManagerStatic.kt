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

import org.tobi29.scapes.engine.utils.Array2
import org.tobi29.scapes.engine.utils.AtomicInteger
import org.tobi29.scapes.engine.utils.StampLock
import org.tobi29.scapes.engine.utils.assert
import org.tobi29.scapes.engine.utils.math.abs
import org.tobi29.scapes.engine.utils.math.vector.MutableVector2i

class TerrainInfiniteChunkManagerStatic<C : TerrainInfiniteBaseChunk<*>>(
        private val center: MutableVector2i,
        private val radius: Int) : TerrainInfiniteChunkManager<C> {
    private val size = (radius shl 1) + 1
    private val arrayFlat =
            arrayOfNulls<TerrainInfiniteBaseChunk<*>?>(size * size)
    private val array = Array2(size, size, arrayFlat)
    private val lock = StampLock()
    private val x = AtomicInteger()
    private val y = AtomicInteger()

    override fun add(chunk: C) {
        lock.write {
            val xx = chunk.pos.x - x.get()
            val yy = chunk.pos.y - y.get()
            if (xx in 0..array.width - 1 && yy in 0..array.height - 1) {
                array[xx, yy] = chunk
            }
        }
    }

    override fun remove(x: Int,
                        y: Int): C? {
        return lock.write {
            val xx = x - this.x.get()
            val yy = y - this.y.get()
            if (xx in 0..array.width - 1 && yy in 0..array.height - 1) {
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

    override fun get(x: Int,
                     y: Int): C? {
        val value = lock.read {
            val xx = x - this.x.get()
            val yy = y - this.y.get()
            if (xx in 0..array.width - 1 && yy in 0..array.height - 1) {
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
                var yDiff = this.y.get() - yy
                val xDiffAbs = abs(xDiff)
                val yDiffAbs = abs(yDiff)
                if (xDiffAbs > size || yDiffAbs > size) {
                    clear()
                    this.x.set(xx)
                    this.y.set(yy)
                } else {
                    if (xDiffAbs > 0) {
                        for (i in 0..xDiff - 1) {
                            shiftXPositive()
                        }
                        xDiff = -xDiff
                        for (i in 0..xDiff - 1) {
                            shiftXNegative()
                        }
                        this.x.set(xx)
                    }
                    if (yDiffAbs > 0) {
                        for (i in 0..yDiff - 1) {
                            shiftYPositive()
                        }
                        yDiff = -yDiff
                        for (i in 0..yDiff - 1) {
                            shiftYNegative()
                        }
                        this.y.set(yy)
                    }
                }
            }
            return true
        }
        return false
    }

    private fun clear() {
        for (i in arrayFlat.indices) {
            arrayFlat[i]?.dispose()
            arrayFlat[i] = null
        }
    }

    private fun shiftXPositive() {
        var i = 0
        while (i < arrayFlat.size) {
            val chunk = arrayFlat[i]
            chunk?.let { it.dispose(); arrayFlat[i] = null }
            i += size
        }
        i = size - 1
        while (i < arrayFlat.size) {
            val chunk = arrayFlat[i]
            chunk?.let { it.dispose(); arrayFlat[i] = null }
            i += size
        }
        System.arraycopy(arrayFlat, 0, arrayFlat, 1, arrayFlat.size - 1)
    }

    private fun shiftXNegative() {
        var i = 0
        while (i < arrayFlat.size) {
            val chunk = arrayFlat[i]
            chunk?.let { it.dispose(); arrayFlat[i] = null }
            i += size
        }
        i = size - 1
        while (i < arrayFlat.size) {
            val chunk = arrayFlat[i]
            chunk?.let { it.dispose(); arrayFlat[i] = null }
            i += size
        }
        System.arraycopy(arrayFlat, 1, arrayFlat, 0, arrayFlat.size - 1)
    }

    private fun shiftYPositive() {
        for (i in 0..size - 1) {
            val chunk = arrayFlat[i]
            chunk?.let { it.dispose(); arrayFlat[i] = null }
        }
        for (i in arrayFlat.size - size..arrayFlat.size - 1) {
            val chunk = arrayFlat[i]
            chunk?.let { it.dispose(); arrayFlat[i] = null }
        }
        System.arraycopy(arrayFlat, 0, arrayFlat, size, arrayFlat.size - size)
    }

    private fun shiftYNegative() {
        for (i in 0..size - 1) {
            val chunk = arrayFlat[i]
            chunk?.let { it.dispose(); arrayFlat[i] = null }
        }
        for (i in arrayFlat.size - size..arrayFlat.size - 1) {
            val chunk = arrayFlat[i]
            chunk?.let { it.dispose(); arrayFlat[i] = null }
        }
        System.arraycopy(arrayFlat, size, arrayFlat, 0, arrayFlat.size - size)
    }
}
