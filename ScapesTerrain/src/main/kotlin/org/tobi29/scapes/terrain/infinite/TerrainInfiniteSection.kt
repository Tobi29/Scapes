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

import org.tobi29.scapes.terrain.TerrainBase
import org.tobi29.scapes.terrain.TerrainLock
import org.tobi29.scapes.terrain.VoxelType

open class TerrainInfiniteSection<B : VoxelType, C : TerrainInfiniteBaseChunk<B>, T : TerrainInfiniteBase<B, C>> : TerrainBase<B>, TerrainLock {
    private var terrainMut: T? = null
    protected val terrain get() = terrainMut ?: throw IllegalStateException(
            "Terrain section not initialized")
    private var chunks = emptyArray<TerrainInfiniteBaseChunk<*>?>()
    private var chunksLength = 0
    private var chunksWidth = 0
    private var chunksHeight = 0
    private var delegate = false
    private var blockX = 0
    private var blockY = 0
    private var blockZ = 0
    private var blockMaxX = 0
    private var blockMaxY = 0
    private var blockMaxZ = 0
    private var relX = 0
    private var relY = 0
    override var locked = false
        protected set

    fun init(terrain: T,
             x: Int,
             y: Int,
             z: Int,
             dx: Int,
             dy: Int,
             dz: Int,
             delegate: Boolean = false) {
        this.terrainMut = terrain
        blockX = x
        blockY = y
        blockZ = z
        blockMaxX = x + dx - 1
        blockMaxY = y + dy - 1
        blockMaxZ = z + dz - 1
        relX = (x shr 4) shl 4
        relY = (y shr 4) shl 4
        this.delegate = delegate
        initChunks()
    }

    private fun initChunks() {
        val terrain = terrain
        val minCX = blockX shr 4
        val minCY = blockY shr 4
        val maxCX = blockMaxX shr 4
        val maxCY = blockMaxY shr 4
        chunksWidth = maxCX - minCX + 1
        chunksHeight = maxCY - minCY + 1
        chunksLength = chunksWidth * chunksHeight
        if (chunks.size < chunksLength) {
            chunks = arrayOfNulls(chunksLength)
        }
        var i = 0
        for (yy in minCY..maxCY) {
            for (xx in minCX..maxCX) {
                terrain.chunk(xx, yy)?.let { chunks[i] = it }
                i++
            }
        }
    }

    fun lockChunks() {
        lock()
        for (i in 0 until chunksLength) {
            chunks[i]?.lockWriteExternal(this)
        }
    }

    fun unlockChunks() {
        unlock()
        for (i in 0 until chunksLength) {
            chunks[i]?.unlockWriteExternal(this)
        }
    }

    override fun getThreadContext() = this

    override fun lock() {
        if (locked) {
            throw IllegalStateException("Locking twice on the same thread")
        }
        locked = true
    }

    override fun unlock() {
        if (!locked) {
            throw IllegalStateException("Unlocking twice on the same thread")
        }
        locked = false
    }

    fun clear() {
        terrainMut = null
        chunks.fill(null)
    }

    private fun checkCoords(x: Int,
                            y: Int) =
            x in blockX..blockMaxX && y in blockY..blockMaxY

    private fun checkCoords(x: Int,
                            y: Int,
                            z: Int) =
            checkCoords(x, y) && z in blockZ..blockMaxZ

    private fun verifyCoords(x: Int,
                             y: Int) {
        if (!delegate && !checkCoords(x, y)) {
            throw IllegalArgumentException(
                    "Coords not mapped: $x $y (Currently mapped: $blockX..$blockMaxX $blockY..$blockMaxY $blockZ..$blockMaxZ)")
        }
    }

    private fun verifyCoords(x: Int,
                             y: Int,
                             z: Int) {
        if (!delegate && !checkCoords(x, y, z)) {
            throw IllegalArgumentException(
                    "Coords not mapped: $x $y $z (Currently mapped: $blockX..$blockMaxX $blockY..$blockMaxY $blockZ..$blockMaxZ)")
        }
    }

    protected fun chunkFor(x: Int,
                           y: Int): C? {
        val inside = checkCoords(x, y)
        if (!inside) {
            if (!delegate) {
                throw IllegalArgumentException(
                        "Coords not mapped: $x $y (Currently mapped: $blockX..$blockMaxX $blockY..$blockMaxY $blockZ..$blockMaxZ)")
            }
            return terrain.chunk(x shr 4, y shr 4)
        }
        val rx = x - relX
        val ry = y - relY
        @Suppress("UNCHECKED_CAST")
        return chunk(rx shr 4, ry shr 4) as C?
    }

    protected fun chunkFor(x: Int,
                           y: Int,
                           z: Int): C? {
        val inside = checkCoords(x, y, z)
        if (!inside) {
            if (!delegate) {
                throw IllegalArgumentException(
                        "Coords not mapped: $x $y $z (Currently mapped: $blockX..$blockMaxX $blockY..$blockMaxY $blockZ..$blockMaxZ)")
            }
            if (z < 0 || z >= terrain.zSize) {
                return null
            }
            return terrain.chunk(x shr 4, y shr 4)
        }
        val rx = x - relX
        val ry = y - relY
        if (z < 0 || z >= terrain.zSize) {
            return null
        }
        @Suppress("UNCHECKED_CAST")
        return chunk(rx shr 4, ry shr 4) as C?
    }

    private fun chunk(x: Int,
                      y: Int) = chunks[y * chunksWidth + x]

    override val air get() = terrain.air
    override val blocks get() = terrain.blocks

    override fun sunLight(x: Int,
                          y: Int,
                          z: Int,
                          light: Int) {
        throw UnsupportedOperationException()
    }

    override fun blockLight(x: Int,
                            y: Int,
                            z: Int,
                            light: Int) {
        throw UnsupportedOperationException()
    }

    override fun block(x: Int,
                       y: Int,
                       z: Int) =
            chunkFor(x, y, z)?.let {
                if (locked) {
                    it.blockGLocked(x, y, z)
                } else {
                    it.blockG(x, y, z)
                }
            } ?: -1L

    override fun type(x: Int,
                      y: Int,
                      z: Int) =
            chunkFor(x, y, z)?.let {
                if (locked) {
                    it.typeGLocked(x, y, z)
                } else {
                    it.typeG(x, y, z)
                }
            } ?: air

    override fun type(id: Int) = terrain.type(id)

    override fun light(x: Int,
                       y: Int,
                       z: Int) =
            chunkFor(x, y, z)?.lightG(x, y, z) ?: 0

    override fun sunLight(x: Int,
                          y: Int,
                          z: Int) =
            chunkFor(x, y, z)?.sunLightG(x, y, z) ?: 0

    override fun blockLight(x: Int,
                            y: Int,
                            z: Int) =
            chunkFor(x, y, z)?.blockLightG(x, y, z) ?: 0

    override fun highestBlockZAt(x: Int,
                                 y: Int): Int {
        if (!delegate) {
            throw UnsupportedOperationException()
        }
        return chunkFor(x, y)?.highestBlockZAtG(x, y) ?: 1
    }

    override fun highestTerrainBlockZAt(x: Int,
                                        y: Int): Int {
        if (!delegate) {
            throw UnsupportedOperationException()
        }
        return chunkFor(x, y)?.highestTerrainBlockZAtG(x, y) ?: 1
    }

    override fun isBlockLoaded(x: Int,
                               y: Int,
                               z: Int): Boolean {
        verifyCoords(x, y, z)
        return terrain.isBlockLoaded(x, y, z)
    }

    override fun isBlockTicking(x: Int,
                                y: Int,
                                z: Int): Boolean {
        verifyCoords(x, y, z)
        return terrain.isBlockTicking(x, y, z)
    }

    override fun sunLightReduction(x: Int,
                                   y: Int): Int {
        verifyCoords(x, y)
        return terrain.sunLightReduction(x, y)
    }
}
