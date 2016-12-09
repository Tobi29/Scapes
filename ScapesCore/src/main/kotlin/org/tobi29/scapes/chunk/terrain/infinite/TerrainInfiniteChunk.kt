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

import org.tobi29.scapes.block.BlockType
import org.tobi29.scapes.chunk.data.ChunkArraySection1x16
import org.tobi29.scapes.chunk.data.ChunkArraySection2x4
import org.tobi29.scapes.chunk.data.ChunkData
import org.tobi29.scapes.chunk.terrain.TerrainChunk
import org.tobi29.scapes.engine.utils.Pool
import org.tobi29.scapes.engine.utils.StampLock
import org.tobi29.scapes.engine.utils.ThreadLocal
import org.tobi29.scapes.engine.utils.io.tag.TagStructure
import org.tobi29.scapes.engine.utils.math.clamp
import org.tobi29.scapes.engine.utils.math.lb
import org.tobi29.scapes.engine.utils.math.max
import org.tobi29.scapes.engine.utils.math.vector.Vector2i
import org.tobi29.scapes.engine.utils.math.vector.Vector3i
import org.tobi29.scapes.engine.utils.readOnly
import org.tobi29.scapes.entity.Entity
import java.util.*
import java.util.concurrent.ConcurrentHashMap

abstract class TerrainInfiniteChunk<E : Entity>(val pos: Vector2i,
                                                val terrain: TerrainInfinite<E>,
                                                val zSize: Int,
                                                protected val blocks: Array<BlockType?>) : TerrainChunk {
    override val posBlock = Vector3i(pos.x shl 4, pos.y shl 4, 0)
    override val size = Vector3i(16, 16, zSize)
    protected val data = ChunkDatas(lb(zSize shr 4))
    protected val lock = StampLock()
    protected val heightMap = IntArray(256)
    protected val entitiesMut = ConcurrentHashMap<UUID, E>()
    val entities = entitiesMut.readOnly()
    protected var state = State.NEW
    protected var metaData = TagStructure()

    protected inline fun <R> lockRead(block: ChunkDatas.() -> R): R {
        return lock.read { block(data) }
    }

    protected inline fun <R> lockWrite(block: ChunkDatas.() -> R): R {
        return lock.write { block(data) }
    }

    @Suppress("NOTHING_TO_INLINE")
    protected inline fun block(id: Int): BlockType {
        return blocks[id] ?: throw IllegalArgumentException()
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
        for (x in 0..15) {
            for (y in 0..15) {
                var sunLight: Byte = 15
                var spread: Int
                if (x > 0 && x < 15 && y > 0 && y < 15) {
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
                        val id = lockRead { bID.getData(x, y, z, 0) }
                        if (id != 0) {
                            val type = block(id)
                            if (type.isSolid(terrain, x + posBlock.x,
                                    y + posBlock.x,
                                    z) || !type.isTransparent(terrain,
                                    x + posBlock.x,
                                    y + posBlock.x, z)) {
                                sunLight = clamp(
                                        sunLight + type.lightTrough(terrain,
                                                x + posBlock.x,
                                                y + posBlock.x, z), 0,
                                        15).toByte()
                            }
                        }
                        lockWrite {
                            bLight.setData(x, y, z, 0, sunLight.toInt())
                        }
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
                if (s.x >= 0 && s.x < 16 && s.y >= 0 && s.y < 16 && s.z >= 0 &&
                        s.z < zSize) {
                    s.l += block(lockRead {
                        bID.getData(s.x, s.y, s.z, 0)
                    }).lightTrough(
                            terrain, s.x + posBlock.x,
                            s.y + posBlock.y, s.z).toInt()
                    s.l = clamp(s.l, 0.toByte().toInt(), 15.toByte().toInt())
                    lockWrite {
                        if (s.l > bLight.getData(s.x, s.y, s.z, 0)) {
                            bLight.setData(s.x, s.y, s.z, 0, s.l)
                            newSpreads.push().set(s.x - 1, s.y, s.z, s.l)
                            newSpreads.push().set(s.x + 1, s.y, s.z, s.l)
                            newSpreads.push().set(s.x, s.y - 1, s.z, s.l)
                            newSpreads.push().set(s.x, s.y + 1, s.z, s.l)
                            newSpreads.push().set(s.x, s.y, s.z - 1, s.l)
                            newSpreads.push().set(s.x, s.y, s.z + 1, s.l)
                        }
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
        for (x in 0..15) {
            for (y in 0..15) {
                var sunLight: Byte = 15
                var z = zSize - 1
                while (z >= 0 && sunLight > 0) {
                    val id = lockRead { bID.getData(x, y, z, 0) }
                    if (id != 0) {
                        val type = block(id)
                        if (type.isSolid(terrain, x + posBlock.x,
                                y + posBlock.x, z) || !type.isTransparent(
                                terrain,
                                x + posBlock.x,
                                y + posBlock.x, z)) {
                            sunLight = clamp(
                                    sunLight + type.lightTrough(terrain,
                                            x + posBlock.x,
                                            y + posBlock.x, z), 0,
                                    15).toByte()
                        }
                    }
                    lockWrite { bLight.setData(x, y, z, 0, sunLight.toInt()) }
                    z--
                }
            }
        }
    }

    override fun metaData(id: String): TagStructure {
        return metaData.structure(id)
    }

    val isLoaded: Boolean
        get() = state.id >= State.LOADED.id

    fun isEmpty(i: Int): Boolean {
        val j = i shl 4
        return lockRead { bID.isEmpty(0, 0, j, 15, 15, j + 15) }
    }

    fun highestBlockZAtG(x: Int,
                         y: Int): Int {
        return highestBlockZAtL(x - posBlock.x, y - posBlock.y)
    }

    fun highestBlockZAtL(x: Int,
                         y: Int): Int {
        if (x >= 0 && x < 16 && y >= 0 && y < 16) {
            return heightMap[y shl 4 or x] + 1
        }
        throw ChunkMissException(
                "Tried to access block $x $y in chunk ${pos.x} ${pos.y}")
    }

    fun highestTerrainBlockZAtG(x: Int,
                                y: Int): Int {
        return highestTerrainBlockZAtL(x - posBlock.x, y - posBlock.y)
    }

    fun highestTerrainBlockZAtL(x: Int,
                                y: Int): Int {
        if (x >= 0 && x < 16 && y >= 0 && y < 16) {
            for (z in heightMap[y shl 4 or x] downTo 0) {
                val id = lockRead { bID.getData(x, y, z, 0) }
                if (id != 0) {
                    val type = block(id)
                    if (type.isSolid(terrain, x + posBlock.x,
                            y + posBlock.x, z) && !type.isTransparent(
                            terrain, x + posBlock.x,
                            y + posBlock.x, z)) {
                        return z + 1
                    }
                }
            }
            return 0
        }
        throw ChunkMissException(
                "Tried to access block $x $y in chunk ${pos.x} ${pos.y}")
    }

    fun blockG(x: Int,
               y: Int,
               z: Int): Long {
        return blockL(x - posBlock.x, y - posBlock.y, z)
    }

    fun blockL(x: Int,
               y: Int,
               z: Int): Long {
        if (x >= 0 && x < 16 && y >= 0 && y < 16 && z >= 0 && z < zSize) {
            return lockRead {
                (bID.getData(x, y, z, 0).toLong() shl 32) or bData.getData(x,
                        y, z, 0).toLong()
            }
        }
        throw ChunkMissException(
                "Tried to access block $x $y $z in chunk ${pos.x} ${pos.y}")
    }

    fun typeG(x: Int,
              y: Int,
              z: Int): BlockType {
        return typeL(x - posBlock.x, y - posBlock.y, z)
    }

    fun typeL(x: Int,
              y: Int,
              z: Int): BlockType {
        if (x >= 0 && x < 16 && y >= 0 && y < 16 && z >= 0 && z < zSize) {
            return block(lockRead { bID.getData(x, y, z, 0) })
        }
        throw ChunkMissException(
                "Tried to access block $x $y $z in chunk ${pos.x} ${pos.y}")
    }

    fun lightG(x: Int,
               y: Int,
               z: Int): Int {
        return lightL(x - posBlock.x, y - posBlock.y, z)
    }

    fun lightL(x: Int,
               y: Int,
               z: Int): Int {
        if (x >= 0 && x < 16 && y >= 0 && y < 16 && z >= 0 && z < zSize) {
            return lockRead {
                max(bLight.getData(x, y, z, 1),
                        bLight.getData(x, y, z, 0) - terrain.sunLightReduction(
                                x + posBlock.x,
                                y + posBlock.y))
            }
        }
        throw ChunkMissException(
                "Tried to access block $x $y $z in chunk ${pos.x} ${pos.y}")
    }

    fun sunLightG(x: Int,
                  y: Int,
                  z: Int): Int {
        return sunLightL(x - posBlock.x, y - posBlock.y, z)
    }

    fun sunLightL(x: Int,
                  y: Int,
                  z: Int): Int {
        if (x >= 0 && x < 16 && y >= 0 && y < 16 && z >= 0 && z < zSize) {
            return lockRead { bLight.getData(x, y, z, 0) }
        }
        throw ChunkMissException(
                "Tried to access block $x $y $z in chunk ${pos.x} ${pos.y}")
    }

    fun blockLightG(x: Int,
                    y: Int,
                    z: Int): Int {
        return blockLightL(x - posBlock.x, y - posBlock.y, z)
    }

    fun blockLightL(x: Int,
                    y: Int,
                    z: Int): Int {
        if (x >= 0 && x < 16 && y >= 0 && y < 16 && z >= 0 && z < zSize) {
            return lockRead { bLight.getData(x, y, z, 1) }
        }
        throw ChunkMissException(
                "Tried to access block $x $y $z in chunk ${pos.x} ${pos.y}")
    }

    fun blockTypeG(x: Int,
                   y: Int,
                   z: Int,
                   type: BlockType) {
        blockTypeL(x - posBlock.x, y - posBlock.y, z, type)
    }

    fun blockTypeL(x: Int,
                   y: Int,
                   z: Int,
                   type: BlockType) {
        if (x >= 0 && x < 16 && y >= 0 && y < 16 && z >= 0 && z < zSize) {
            val oldType = block(lockRead { bID.getData(x, y, z, 0) })
            if (oldType !== type) {
                lockWrite { bID.setData(x, y, z, 0, type.id().toInt()) }
                updateHeightMap(x, y, z, type)
                update(x, y, z, oldType.causesTileUpdate())
            }
        } else {
            throw ChunkMissException(
                    "Tried to access block $x $y $z in chunk ${pos.x} ${pos.y}")
        }
    }

    fun typeDataG(x: Int,
                  y: Int,
                  z: Int,
                  type: BlockType,
                  data: Int) {
        typeDataL(x - posBlock.x, y - posBlock.y, z, type, data)
    }

    fun typeDataL(x: Int,
                  y: Int,
                  z: Int,
                  type: BlockType,
                  data: Int) {
        if (x >= 0 && x < 16 && y >= 0 && y < 16 && z >= 0 && z < zSize) {
            val oldType = block(lockRead { bID.getData(x, y, z, 0) })
            if (oldType !== type || lockRead {
                bData.getData(x, y, z, 0)
            } != data) {
                lockWrite {
                    bID.setData(x, y, z, 0, type.id().toInt())
                    bData.setData(x, y, z, 0, data)
                }
                updateHeightMap(x, y, z, type)
                update(x, y, z, oldType.causesTileUpdate())
            }
        } else {
            throw ChunkMissException(
                    "Tried to access block $x $y $z in chunk ${pos.x} ${pos.y}")
        }
    }

    fun dataG(x: Int,
              y: Int,
              z: Int,
              data: Int) {
        dataL(x - posBlock.x, y - posBlock.y, z, data)
    }

    fun dataL(x: Int,
              y: Int,
              z: Int,
              data: Int) {
        if (x >= 0 && x < 16 && y >= 0 && y < 16 && z >= 0 && z < zSize) {
            if (lockRead { bData.getData(x, y, z, 0) } != data) {
                val oldType = block(lockWrite {
                    bData.setData(x, y, z, 0, data)
                    bID.getData(x, y, z, 0)
                })
                update(x, y, z, oldType.causesTileUpdate())
            }
        } else {
            throw ChunkMissException(
                    "Tried to access block $x $y $z in chunk ${pos.x} ${pos.y}")
        }
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
        if (x >= 0 && x < 16 && y >= 0 && y < 16 && z >= 0 && z < zSize) {
            lockWrite { bLight.setData(x, y, z, 0, light) }
            updateLight(x, y, z)
        } else {
            throw ChunkMissException(
                    "Tried to access block " + x + ' ' + y + ' ' + z +
                            " in chunk " + pos.x + ' ' + pos.y)
        }
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
        if (x >= 0 && x < 16 && y >= 0 && y < 16 && z >= 0 && z < zSize) {
            lockWrite { bLight.setData(x, y, z, 1, light) }
            updateLight(x, y, z)
        } else {
            throw ChunkMissException(
                    "Tried to access block $x $y $z in chunk ${pos.x} ${pos.y}")
        }
    }

    fun addEntity(entity: E) {
        mapEntity(entity)
        terrain.entityAdded(entity)
    }

    fun removeEntity(entity: E): Boolean {
        if (unmapEntity(entity)) {
            terrain.entityRemoved(entity)
            return true
        }
        return false
    }

    internal fun mapEntity(entity: E) {
        val removed = entitiesMut.put(entity.getUUID(), entity)
        if (removed != null) {
            throw IllegalStateException(
                    "Duplicate entity in chunk: ${removed.getUUID()}")
        }
    }

    internal fun unmapEntity(entity: E): Boolean {
        return entitiesMut.remove(entity.getUUID()) != null
    }

    protected fun initHeightMap() {
        for (x in 0..15) {
            for (y in 0..15) {
                for (z in zSize - 1 downTo 1) {
                    if (lockRead { bID.getData(x, y, z, 0) } != 0) {
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
                                  type: BlockType) {
        val height = heightMap[y shl 4 or x]
        if (z > height && type !== terrain.air) {
            heightMap[y shl 4 or x] = z
        } else if (height == z && type === terrain.air) {
            var zzz = 0
            for (zz in height downTo 0) {
                if (lockRead { bID.getData(x, y, zz, 0) } != 0) {
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

        internal val id: Byte

        init {
            this.id = id.toByte()
        }
    }

    protected class ChunkDatas(zBits: Int) {
        val bID = ChunkData(0, 0, zBits, 4, 4, 4,
                ::ChunkArraySection1x16)
        val bData = ChunkData(0, 0, zBits, 4, 4, 4,
                ::ChunkArraySection1x16)
        val bLight = ChunkData(0, 0, zBits, 4, 4, 4,
                ::ChunkArraySection2x4)
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
