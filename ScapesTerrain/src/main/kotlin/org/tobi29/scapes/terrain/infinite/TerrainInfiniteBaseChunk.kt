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

import org.tobi29.math.vector.Vector2i
import org.tobi29.math.vector.Vector3i
import org.tobi29.utils.Pool
import org.tobi29.utils.StampLock
import org.tobi29.scapes.terrain.TerrainChunk
import org.tobi29.scapes.terrain.TerrainGlobals
import org.tobi29.scapes.terrain.TerrainLock
import org.tobi29.scapes.terrain.VoxelType
import org.tobi29.scapes.terrain.data.ChunkArraySection1x16
import org.tobi29.scapes.terrain.data.ChunkArraySection2x4
import org.tobi29.scapes.terrain.data.ChunkDataStruct
import org.tobi29.io.tag.MutableTagMap
import org.tobi29.io.tag.mapMut
import org.tobi29.stdex.ThreadLocal
import org.tobi29.stdex.assert
import org.tobi29.stdex.math.clamp
import org.tobi29.stdex.math.lb
import kotlin.math.max

abstract class TerrainInfiniteBaseChunk<B : VoxelType>(val pos: Vector2i,
                                                       val parentTerrain: TerrainGlobals<B>,
                                                       val zSize: Int) : TerrainChunk {
    override val posBlock = Vector3i(pos.x shl 4, pos.y shl 4, 0)
    override val size = Vector3i(16, 16, zSize)
    protected val blocks = parentTerrain.blocks
    protected val data = ChunkDatas(lb(zSize shr 4))
    protected val lock = StampLock()
    protected val heightMap = IntArray(256)
    protected var state = State.NEW
    protected var metaData = MutableTagMap()
    var lastAccess = System.currentTimeMillis()
        internal set

    protected inline fun <R> lockRead(crossinline block: ChunkDatas.() -> R): R {
        assert { lock.isHeld() || !parentTerrain.getThreadContext().locked }
        return lock.read { block(data) }
    }

    inline fun <R> lockWrite(block: () -> R): R {
        val threadContext = parentTerrain.getThreadContext()
        lockWrite(threadContext)
        try {
            return block()
        } finally {
            unlockWrite(threadContext)
        }
    }

    fun lockWrite(context: TerrainLock) {
        if (!lock.isHeld()) {
            context.lock()
        }
        lock.lock()
    }

    fun unlockWrite(context: TerrainLock) {
        lock.unlock()
        if (!lock.isHeld()) {
            context.unlock()
        }
    }

    fun lockWriteExternal(context: TerrainLock) {
        assert { context.locked }
        lock.lock()
    }

    fun unlockWriteExternal(context: TerrainLock) {
        assert { !context.locked }
        lock.unlock()
    }

    @Suppress("NOTHING_TO_INLINE")
    protected inline fun type(id: Int): B {
        if (id < 0 || id >= blocks.size) {
            throw IllegalArgumentException("Invalid material: $id")
        }
        return blocks[id] ?: throw IllegalArgumentException(
                "Invalid material: $id")
    }

    @Suppress("NOTHING_TO_INLINE")
    protected inline fun checkCoords(x: Int,
                                     y: Int) {
        if (x < 0 || x >= 16 || y < 0 || y >= 16) {
            throw ChunkMissException(
                    "Tried to access block $x $y in chunk ${pos.x} ${pos.y}")
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    protected inline fun checkCoords(x: Int,
                                     y: Int,
                                     z: Int) {
        if (x < 0 || x >= 16 || y < 0 || y >= 16 || z < 0 || z >= zSize) {
            throw ChunkMissException(
                    "Tried to access block $x $y $z in chunk ${pos.x} ${pos.y}")
        }
    }

    abstract fun update(x: Int,
                        y: Int,
                        z: Int,
                        updateTile: Boolean)

    abstract fun updateLight(x: Int,
                             y: Int,
                             z: Int)

    fun updateSunLight() {
        val spreadPools = SPREAD_POOLS.get()
        var spreads = spreadPools.first
        var newSpreads = spreadPools.second
        assert { lock.isHeld() }
        for (x in 0..15) {
            for (y in 0..15) {
                var sunLight: Byte = 15
                var spread: Int
                if (x in 1..14 && y > 0 && y < 15) {
                    spread = heightMap[y shl 4 or x - 1]
                    spread = max(heightMap[y shl 4 or x + 1], spread)
                    spread = max(heightMap[y - 1 shl 4 or x], spread)
                    spread = max(heightMap[y + 1 shl 4 or x], spread)
                    spread--
                } else {
                    spread = -1
                }
                val light = heightMap[y shl 4 or x]
                for (z in max(light, spread) downTo 0) {
                    if (z < light) {
                        val id = this.data.id(x, y, z)
                        if (id != 0) {
                            val type = type(id)
                            val data = this.data.data(x, y, z)
                            if (type.isSolid(data) || !type.isTransparent(
                                    data)) {
                                sunLight = clamp(
                                        sunLight + type.lightTrough(data),
                                        0, 15).toByte()
                            }
                        }
                        this.data.setSunLight(x, y, z, sunLight.toInt())
                    }
                    if (z < spread && sunLight > 0) {
                        spreads.push().set(x - 1, y, z, sunLight.toInt())
                        spreads.push().set(x + 1, y, z, sunLight.toInt())
                        spreads.push().set(x, y - 1, z, sunLight.toInt())
                        spreads.push().set(x, y + 1, z, sunLight.toInt())
                    }
                }
            }
        }
        while (spreads.isNotEmpty()) {
            for (s in spreads) {
                if (s.x in 0..15 && s.y >= 0 && s.y < 16 && s.z >= 0 &&
                        s.z < zSize) {
                    val type = type(this.data.id(s.x, s.y, s.z))
                    val data = this.data.data(s.x, s.y, s.z)
                    s.l = clamp(s.l + type.lightTrough(data), 0, 15)
                    if (s.l > this.data.sunLight(s.x, s.y, s.z)) {
                        this.data.setSunLight(s.x, s.y, s.z, s.l)
                        newSpreads.push().set(s.x - 1, s.y, s.z, s.l)
                        newSpreads.push().set(s.x + 1, s.y, s.z, s.l)
                        newSpreads.push().set(s.x, s.y - 1, s.z, s.l)
                        newSpreads.push().set(s.x, s.y + 1, s.z, s.l)
                        newSpreads.push().set(s.x, s.y, s.z - 1, s.l)
                        newSpreads.push().set(s.x, s.y, s.z + 1, s.l)
                    }
                }
            }
            val swapUpdates = spreads
            swapUpdates.reset()
            spreads = newSpreads
            newSpreads = swapUpdates
        }
    }

    protected fun initSunLight() {
        // We do not need to lock here as the chunk is not added anywhere
        // yet and this might get triggered in a locked context causing
        // a crash
        val data = data
        for (x in 0..15) {
            for (y in 0..15) {
                var sunLight: Byte = 15
                var z = zSize - 1
                while (z >= 0 && sunLight > 0) {
                    val id = data.id(x, y, z)
                    if (id != 0) {
                        val type = type(id)
                        val blockData = data.data(x, y, z)
                        if (type.isSolid(blockData) || !type.isTransparent(
                                blockData)) {
                            sunLight = clamp(
                                    sunLight + type.lightTrough(blockData), 0,
                                    15).toByte()
                        }
                    }
                    data.setSunLight(x, y, z, sunLight.toInt())
                    z--
                }
            }
        }
    }

    override fun metaData(id: String) = metaData.mapMut(id)

    val isLoaded: Boolean
        get() = state.id >= State.LOADED.id

    fun isEmpty(i: Int): Boolean {
        val j = i shl 4
        var flag = true
        lockRead {
            struct.forIn(idData, 0, 0, j, 15, 15, j + 15) {
                if (!it.isEmpty) {
                    flag = false
                    return@forIn
                }
            }
        }
        return flag
    }

    fun highestBlockZAtG(x: Int,
                         y: Int): Int {
        return highestBlockZAtL(x - posBlock.x, y - posBlock.y)
    }

    fun highestBlockZAtL(x: Int,
                         y: Int): Int {
        checkCoords(x, y)
        return heightMap[y shl 4 or x] + 1
    }

    fun highestTerrainBlockZAtG(x: Int,
                                y: Int): Int {
        return highestTerrainBlockZAtL(x - posBlock.x, y - posBlock.y)
    }

    fun highestTerrainBlockZAtL(x: Int,
                                y: Int): Int {
        checkCoords(x, y)
        for (z in heightMap[y shl 4 or x] downTo 0) {
            val block = blockL(x, y, z)
            val type = parentTerrain.type(block)
            if (type != parentTerrain.air) {
                val data = parentTerrain.data(block)
                if (type.isSolid(data) && !type.isTransparent(data)) {
                    return z + 1
                }
            }
        }
        return 0
    }

    fun blockGLocked(x: Int,
                     y: Int,
                     z: Int) = blockLLocked(x - posBlock.x, y - posBlock.y, z)

    fun blockLLocked(x: Int,
                     y: Int,
                     z: Int): Long {
        checkCoords(x, y, z)
        assert { lock.isHeld() }
        return data.id(x, y, z).toLong() or (data.data(x, y, z).toLong() shl 32)
    }

    fun typeGLocked(x: Int,
                    y: Int,
                    z: Int) = typeLLocked(x - posBlock.x, y - posBlock.y, z)

    fun typeLLocked(x: Int,
                    y: Int,
                    z: Int): B {
        checkCoords(x, y, z)
        assert { lock.isHeld() }
        return type(data.id(x, y, z))
    }

    fun dataGLocked(x: Int,
                    y: Int,
                    z: Int) = dataLLocked(x - posBlock.x, y - posBlock.y, z)

    fun dataLLocked(x: Int,
                    y: Int,
                    z: Int): Int {
        checkCoords(x, y, z)
        assert { lock.isHeld() }
        return data.data(x, y, z)
    }

    fun blockG(x: Int,
               y: Int,
               z: Int) = blockL(x - posBlock.x, y - posBlock.y, z)

    fun blockL(x: Int,
               y: Int,
               z: Int): Long {
        checkCoords(x, y, z)
        return lockRead {
            id(x, y, z).toLong() or (data(x, y, z).toLong() shl 32)
        }
    }

    fun typeG(x: Int,
              y: Int,
              z: Int) = typeL(x - posBlock.x, y - posBlock.y, z)

    fun typeL(x: Int,
              y: Int,
              z: Int): B {
        checkCoords(x, y, z)
        return type(lockRead { id(x, y, z) })
    }

    fun dataG(x: Int,
              y: Int,
              z: Int) = dataL(x - posBlock.x, y - posBlock.y, z)

    fun dataL(x: Int,
              y: Int,
              z: Int): Int {
        checkCoords(x, y, z)
        return lockRead { data(x, y, z) }
    }

    fun lightG(x: Int,
               y: Int,
               z: Int) = lightL(x - posBlock.x, y - posBlock.y, z)

    fun lightL(x: Int,
               y: Int,
               z: Int): Int {
        checkCoords(x, y, z)
        return lockRead {
            max(blockLight(x, y, z),
                    sunLight(x, y, z) - parentTerrain.sunLightReduction(
                            x + posBlock.x, y + posBlock.y))
        }
    }

    fun sunLightG(x: Int,
                  y: Int,
                  z: Int): Int {
        return sunLightL(x - posBlock.x, y - posBlock.y, z)
    }

    fun sunLightL(x: Int,
                  y: Int,
                  z: Int): Int {
        checkCoords(x, y, z)
        return lockRead { sunLight(x, y, z) }
    }

    fun blockLightG(x: Int,
                    y: Int,
                    z: Int): Int {
        return blockLightL(x - posBlock.x, y - posBlock.y, z)
    }

    fun blockLightL(x: Int,
                    y: Int,
                    z: Int): Int {
        checkCoords(x, y, z)
        return lockRead { blockLight(x, y, z) }
    }

    fun blockTypeG(x: Int,
                   y: Int,
                   z: Int,
                   type: B) =
            blockTypeL(x - posBlock.x, y - posBlock.y, z, type)

    fun blockTypeL(x: Int,
                   y: Int,
                   z: Int,
                   type: B): B? {
        checkCoords(x, y, z)
        val oldType = type(lockRead { id(x, y, z) })
        if (oldType != type) {
            assert { lock.isHeld() }
            data.setID(x, y, z, type.id)
            updateHeightMap(x, y, z, type)
            update(x, y, z, oldType.causesTileUpdate())
            return oldType
        }
        return null
    }

    fun typeDataG(x: Int,
                  y: Int,
                  z: Int,
                  type: B,
                  data: Int) =
            typeDataL(x - posBlock.x, y - posBlock.y, z, type, data)

    fun typeDataL(x: Int,
                  y: Int,
                  z: Int,
                  type: B,
                  data: Int): B? {
        checkCoords(x, y, z)
        assert { lock.isHeld() }
        val oldType = type(this.data.id(x, y, z))
        if (oldType != type || this.data.data(x, y, z) != data) {
            this.data.setID(x, y, z, type.id)
            this.data.setData(x, y, z, data)
            updateHeightMap(x, y, z, type)
            update(x, y, z, oldType.causesTileUpdate())
            return oldType
        }
        return null
    }

    fun dataG(x: Int,
              y: Int,
              z: Int,
              data: Int) =
            dataL(x - posBlock.x, y - posBlock.y, z, data)

    fun dataL(x: Int,
              y: Int,
              z: Int,
              data: Int): B? {
        checkCoords(x, y, z)
        assert { lock.isHeld() }
        if (this.data.data(x, y, z) != data) {
            this.data.setData(x, y, z, data)
            val oldType = type(this.data.id(x, y, z))
            update(x, y, z, oldType.causesTileUpdate())
            return oldType
        }
        return null
    }

    fun sunLightG(x: Int,
                  y: Int,
                  z: Int,
                  light: Int) {
        sunLightL(x - posBlock.x, y - posBlock.y, z, light)
    }

    fun sunLightL(x: Int,
                  y: Int,
                  z: Int,
                  light: Int) {
        checkCoords(x, y, z)
        assert { lock.isHeld() }
        data.setSunLight(x, y, z, light)
        updateLight(x, y, z)
    }

    fun blockLightG(x: Int,
                    y: Int,
                    z: Int,
                    light: Int) {
        blockLightL(x - posBlock.x, y - posBlock.y, z, light)
    }

    fun blockLightL(x: Int,
                    y: Int,
                    z: Int,
                    light: Int) {
        checkCoords(x, y, z)
        assert { lock.isHeld() }
        data.setBlockLight(x, y, z, light)
        updateLight(x, y, z)
    }

    open val isInitialized: Boolean get() = true

    open suspend fun awaitInitialized() {}

    open fun dispose() {}

    protected fun initHeightMap() {
        // We do not need to lock here as the chunk is not added anywhere
        // yet and this might get triggered in a locked context causing
        // a crash
        val data = data
        for (x in 0..15) {
            for (y in 0..15) {
                for (z in zSize - 1 downTo 1) {
                    if (data.id(x, y, z) != 0) {
                        heightMap[y shl 4 or x] = z
                        break
                    }
                }
            }
        }
    }

    protected fun updateHeightMap(x: Int,
                                  y: Int,
                                  z: Int,
                                  type: B) {
        val height = heightMap[y shl 4 or x]
        if (z > height && type !== parentTerrain.air) {
            heightMap[y shl 4 or x] = z
        } else if (height == z && type == parentTerrain.air) {
            var zzz = 0
            for (zz in height downTo 0) {
                if (lockRead { id(x, y, zz) } != 0) {
                    zzz = zz
                    break
                }
            }
            heightMap[y shl 4 or x] = zzz
        }
    }

    protected enum class State(id: Int) {
        NEW(0),
        SHOULD_POPULATE(1),
        POPULATING(2),
        POPULATED(3),
        BORDER(4),
        LOADED(5),
        SENDABLE(6);

        val id = id.toByte()
    }

    protected class ChunkDatas(zBits: Int) {
        val struct = ChunkDataStruct(0, 0, zBits, 4, 4, 4)
        val idData = struct.createData(::ChunkArraySection1x16)
        val dataData = struct.createData(::ChunkArraySection1x16)
        val lightData = struct.createData(::ChunkArraySection2x4)

        fun id(x: Int,
               y: Int,
               z: Int) =
                struct.getSection(idData, x, y, z) { x, y, z ->
                    getData(x, y, z)
                }

        fun setID(x: Int,
                  y: Int,
                  z: Int,
                  value: Int) =
                struct.getSection(idData, x, y, z) { x, y, z ->
                    setData(x, y, z, value)
                }

        fun data(x: Int,
                 y: Int,
                 z: Int) =
                struct.getSection(dataData, x, y, z) { x, y, z ->
                    getData(x, y, z)
                }

        fun setData(x: Int,
                    y: Int,
                    z: Int,
                    value: Int) =
                struct.getSection(dataData, x, y, z) { x, y, z ->
                    setData(x, y, z, value)
                }

        fun sunLight(x: Int,
                     y: Int,
                     z: Int) =
                struct.getSection(lightData, x, y, z) { x, y, z ->
                    getData(x, y, z, false)
                }

        fun setSunLight(x: Int,
                        y: Int,
                        z: Int,
                        value: Int) =
                struct.getSection(lightData, x, y,
                        z) { x, y, z ->
                    setData(x, y, z, false, value)
                }

        fun blockLight(x: Int,
                       y: Int,
                       z: Int) =
                struct.getSection(lightData, x, y, z) { x, y, z ->
                    getData(x, y, z, true)
                }

        fun setBlockLight(x: Int,
                          y: Int,
                          z: Int,
                          value: Int) =
                struct.getSection(lightData, x, y, z) { x, y, z ->
                    setData(x, y, z, true, value)
                }
    }

    private class LightSpread {
        var x = 0
        var y = 0
        var z = 0
        var l = 0

        fun set(x: Int,
                y: Int,
                z: Int,
                l: Int) {
            this.x = x
            this.y = y
            this.z = z
            this.l = l
        }
    }

    companion object {
        private val SPREAD_POOLS = ThreadLocal {
            Pair(Pool { LightSpread() }, Pool { LightSpread() })
        }
    }
}
