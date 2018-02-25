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

package org.tobi29.scapes.chunk.terrain.infinite

import org.tobi29.utils.Pool
import org.tobi29.stdex.math.sqr
import org.tobi29.math.vector.MutableVector2i
import org.tobi29.scapes.terrain.infinite.TerrainInfiniteBaseChunk

class TerrainInfiniteChunkLoader<out C : TerrainInfiniteBaseChunk<*>>(
        chunks: Sequence<C>,
        private val cxMin: Int,
        private val cxMax: Int,
        private val cyMin: Int,
        private val cyMax: Int
) {
    private val REQUIRED_POSITION = MutableVector2i()
    private val pool = Pool { MutableVector2i() to Priority() }
    private val heldChunks = HashMap<MutableVector2i, Priority>()

    val outsideChunks = chunks.filter {
        REQUIRED_POSITION.setXY(it.pos.x, it.pos.y) !in heldChunks
    }

    fun requiredChunks(filter: (MutableVector2i) -> Boolean) =
            heldChunks.asSequence().filter { filter(it.key) }
                    .sortedBy { it.value.value }.map { it.key }


    fun require(x: Int,
                y: Int,
                priority: Int) {
        REQUIRED_POSITION.setXY(x, y)
        heldChunks[REQUIRED_POSITION]?.also {
            it.value = it.value.coerceAtMost(-priority)
        } ?: run {
            pool.push().let { (pos, pri) ->
                pos.x = x
                pos.y = y
                pri.value = -priority
                heldChunks[pos] = pri
            }
        }
    }

    fun requireCircle(x: Int,
                      y: Int,
                      loadingRadius: Int,
                      circleExtension: Int = 48) {
        // Add 48 to provide circular sendable area
        // Note: 48 was found to be just enough to avoid request spam
        val loadingRadiusSqr = sqr(loadingRadius) + circleExtension
        for (xx in -loadingRadius..loadingRadius) {
            val xxx = xx + x
            for (yy in -loadingRadius..loadingRadius) {
                val yyy = yy + y
                if (xxx in cxMin..cxMax && yyy >= cyMin && yyy <= cyMax) {
                    val distanceSqr = sqr(xx) + sqr(yy)
                    if (distanceSqr <= loadingRadiusSqr) {
                        require(xxx, yyy, -distanceSqr)
                    }
                }
            }
        }
    }

    fun reset() {
        heldChunks.clear()
        pool.reset()
    }

    private class Priority {
        var value = 0
    }
}
