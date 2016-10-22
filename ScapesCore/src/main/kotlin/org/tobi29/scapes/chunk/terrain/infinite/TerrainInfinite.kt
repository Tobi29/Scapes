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
import org.tobi29.scapes.block.AABBElement
import org.tobi29.scapes.block.BlockType
import org.tobi29.scapes.chunk.lighting.LightingEngine
import org.tobi29.scapes.chunk.lighting.LightingEngineThreaded
import org.tobi29.scapes.chunk.terrain.TerrainEntity
import org.tobi29.scapes.engine.utils.Pool
import org.tobi29.scapes.engine.utils.ThreadLocal
import org.tobi29.scapes.engine.utils.math.PointerPane
import org.tobi29.scapes.engine.utils.math.clamp
import org.tobi29.scapes.engine.utils.stream
import org.tobi29.scapes.engine.utils.task.TaskExecutor
import org.tobi29.scapes.entity.Entity
import java.util.*
import java.util.concurrent.ConcurrentHashMap

abstract class TerrainInfinite<E : Entity>(val zSize: Int,
                                           taskExecutor: TaskExecutor,
                                           override val air: BlockType,
                                           val voidBlock: BlockType,
                                           protected val blocks: Array<BlockType?>) : TerrainEntity<E> {
    protected val entityMap: MutableMap<UUID, E> = ConcurrentHashMap()
    protected val cxMin: Int
    protected val cxMax: Int
    protected val cyMin: Int
    protected val cyMax: Int
    protected val lighting: LightingEngine

    init {
        val radius = 0x8000000 - 16
        cxMin = -radius + 1
        cxMax = radius
        cyMin = -radius + 1
        cyMax = radius
        lighting = LightingEngineThreaded(this, taskExecutor)
    }

    override fun sunLight(x: Int,
                          y: Int,
                          z: Int,
                          light: Int) {
        if (z < 0 || z >= zSize) {
            return
        }
        chunk(x shr 4, y shr 4, { it.sunLightG(x, y, z, light) })
    }

    override fun blockLight(x: Int,
                            y: Int,
                            z: Int,
                            light: Int) {
        if (z < 0 || z >= zSize) {
            return
        }
        chunk(x shr 4, y shr 4, { it.blockLightG(x, y, z, light) })
    }

    override fun block(x: Int,
                       y: Int,
                       z: Int): Long {
        if (z < 0 || z >= zSize) {
            return voidBlock.id.toLong() shl 32
        }
        return chunk(x shr 4, y shr 4) {
            it.blockG(x, y, z)
        } ?: (voidBlock.id.toLong() shl 32)
    }

    override fun type(x: Int,
                      y: Int,
                      z: Int): BlockType {
        if (z < 0 || z >= zSize) {
            return voidBlock
        }
        return chunk(x shr 4, y shr 4) { it.typeG(x, y, z) } ?: voidBlock
    }

    override fun light(x: Int,
                       y: Int,
                       z: Int): Int {
        if (z < 0 || z >= zSize) {
            return 0
        }
        return chunk(x shr 4, y shr 4) { it.lightG(x, y, z) } ?: 0
    }

    override fun sunLight(x: Int,
                          y: Int,
                          z: Int): Int {
        if (z < 0 || z >= zSize) {
            return 0
        }
        return chunk(x shr 4, y shr 4) { it.sunLightG(x, y, z) } ?: 0
    }

    override fun blockLight(x: Int,
                            y: Int,
                            z: Int): Int {
        if (z < 0 || z >= zSize) {
            return 0
        }
        return chunk(x shr 4, y shr 4) { it.blockLightG(x, y, z) } ?: 0
    }

    override fun highestBlockZAt(x: Int,
                                 y: Int): Int {
        return chunk(x shr 4, y shr 4) { it.highestBlockZAtG(x, y) } ?: 1
    }

    override fun highestTerrainBlockZAt(x: Int,
                                        y: Int): Int {
        return chunk(x shr 4, y shr 4) { chunk ->
            chunk.highestTerrainBlockZAtG(x, y)
        } ?: 1
    }

    override fun isBlockLoaded(x: Int,
                               y: Int,
                               z: Int): Boolean {
        if (z < 0 || z >= zSize) {
            return false
        }
        return chunkNoLoad(x shr 4, y shr 4)?.isLoaded ?: false
    }

    override fun isBlockTicking(x: Int,
                                y: Int,
                                z: Int): Boolean {
        return chunkNoLoad(x shr 4, y shr 4)?.isLoaded ?: false
    }

    override fun collisions(minX: Int,
                            minY: Int,
                            minZ: Int,
                            maxX: Int,
                            maxY: Int,
                            maxZ: Int): Pool<AABBElement> {
        val aabbs = AABBS.get()
        aabbs.reset()
        val minZZ = clamp(minZ, 0, zSize)
        val maxZZ = clamp(maxZ, 0, zSize)
        for (x in minX..maxX) {
            for (y in minY..maxY) {
                val chunk = chunkNoLoad(x shr 4, y shr 4)
                if (chunk != null && chunk.isLoaded) {
                    for (z in minZZ..maxZZ) {
                        if (z >= 0 && z < zSize) {
                            chunk.typeG(x, y, z).addCollision(aabbs, this, x,
                                    y, z)
                        }
                    }
                } else {
                    aabbs.push().set(x.toDouble(), y.toDouble(),
                            minZZ.toDouble(), x + 1.0, y + 1.0, maxZZ + 1.0)
                }
            }
        }
        return aabbs
    }

    override fun pointerPanes(x: Int,
                              y: Int,
                              z: Int,
                              range: Int): Pool<PointerPane> {
        val pointerPanes = POINTER_PANES.get()
        for (xx in -range..range) {
            val xxx = x + xx
            for (yy in -range..range) {
                val yyy = y + yy
                for (zz in -range..range) {
                    val zzz = z + zz
                    if (zzz >= 0 && zzz < zSize) {
                        chunk(xxx shr 4, yyy shr 4) { chunk ->
                            val block = chunk.blockG(xxx, yyy, zzz)
                            val type = type(block)
                            val data = data(block)
                            type.addPointerCollision(data, pointerPanes, xxx,
                                    yyy, zzz)
                        }
                    }
                }
            }
        }
        return pointerPanes
    }

    override fun removeEntity(entity: E): Boolean {
        val pos = entity.getCurrentPos()
        val x = pos.intX() shr 4
        val y = pos.intY() shr 4
        if (chunk(x, y,
                { it.removeEntity(entity) }) ?: false) {
            return true
        }
        for (chunk in loadedChunks()) {
            if (chunk.removeEntity(entity)) {
                return true
            }
        }
        return false
    }

    override fun hasEntity(entity: E): Boolean {
        return entityMap.containsValue(entity)
    }

    override fun getEntity(uuid: UUID): E? {
        return entityMap[uuid]
    }

    override fun getEntities(consumer: (Stream<E>) -> Unit) {
        consumer(entityMap.values.stream())
    }

    override fun getEntities(x: Int,
                             y: Int,
                             z: Int,
                             consumer: (Stream<E>) -> Unit) {
        chunk(x shr 4, y shr 4, { chunk ->
            consumer(chunk.entities().filter(
                    { entity ->
                        val pos = entity.getCurrentPos()
                        pos.intX() == x && pos.intY() == y && pos.intZ() == z
                    }))
        })
    }

    override fun getEntitiesAtLeast(minX: Int,
                                    minY: Int,
                                    minZ: Int,
                                    maxX: Int,
                                    maxY: Int,
                                    maxZ: Int,
                                    consumer: (Stream<E>) -> Unit) {
        val minCX = minX shr 4
        val minCY = minY shr 4
        val maxCX = maxX shr 4
        val maxCY = maxY shr 4
        for (yy in minCY..maxCY) {
            for (xx in minCX..maxCX) {
                chunkNoLoad(xx, yy)?.let { chunk ->
                    consumer(chunk.entities())
                }
            }
        }
    }

    override fun entityAdded(entity: E) {
        val removed = entityMap.put(entity.getUUID(), entity)
        if (removed != null) {
            throw IllegalStateException(
                    "Duplicate entity: " + removed.getUUID())
        }
    }

    override fun entityRemoved(entity: E) {
        entityMap.remove(entity.getUUID())
    }

    override fun type(block: Long): BlockType {
        val id = (block shr 32).toInt()
        return blocks[id] ?: throw IllegalArgumentException(
                "Invalid block id: $id")
    }

    abstract fun hasChunk(x: Int,
                          y: Int): Boolean

    abstract fun chunk(x: Int,
                       y: Int): TerrainInfiniteChunk<E>?

    abstract fun chunkNoLoad(x: Int,
                             y: Int): TerrainInfiniteChunk<E>?

    abstract fun loadedChunks(): Collection<TerrainInfiniteChunk<E>>

    fun lighting(): LightingEngine {
        return lighting
    }

    companion object {
        protected val AABBS = ThreadLocal { Pool { AABBElement() } }
        protected val POINTER_PANES = ThreadLocal { Pool { PointerPane() } }
    }
}
