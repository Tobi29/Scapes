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

import org.tobi29.scapes.block.AABBElement
import org.tobi29.scapes.block.BlockType
import org.tobi29.scapes.block.GameRegistry
import org.tobi29.scapes.block.Material
import org.tobi29.scapes.chunk.lighting.LightingEngine
import org.tobi29.scapes.chunk.lighting.LightingEngineThreaded
import org.tobi29.scapes.chunk.terrain.TerrainEntity
import org.tobi29.scapes.engine.utils.Pool
import org.tobi29.scapes.engine.utils.math.PointerPane
import org.tobi29.scapes.engine.utils.math.clamp
import org.tobi29.scapes.engine.utils.task.TaskExecutor
import org.tobi29.scapes.engine.utils.toArray
import org.tobi29.scapes.entity.Entity
import java.util.*
import java.util.concurrent.ConcurrentHashMap

abstract class TerrainInfinite<E : Entity>(val zSize: Int,
                                           taskExecutor: TaskExecutor,
                                           override val air: BlockType,
                                           val voidBlock: BlockType,
                                           override val registry: GameRegistry,
                                           radius: Int = 0x8000000 - 16) : TerrainEntity<E> {
    protected val materials = registry.get<Material>("Core", "Material")
    internal val blocks = materials.values.asSequence()
            .map { it as? BlockType }.toArray()
    protected val entityMap: MutableMap<UUID, E> = ConcurrentHashMap()
    protected val cxMin = -radius + 1
    protected val cxMax = radius
    protected val cyMin = -radius + 1
    protected val cyMax = radius
    protected val lighting = LightingEngineThreaded(this, taskExecutor)

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
                            maxZ: Int,
                            pool: Pool<AABBElement>) {
        val minZZ = clamp(minZ, 0, zSize)
        val maxZZ = clamp(maxZ, 0, zSize)
        for (x in minX..maxX) {
            for (y in minY..maxY) {
                val chunk = chunkNoLoad(x shr 4, y shr 4)
                if (chunk != null && chunk.isLoaded) {
                    for (z in minZZ..maxZZ) {
                        if (z in 0..(zSize - 1)) {
                            chunk.typeG(x, y, z).addCollision(pool, this, x,
                                    y, z)
                        }
                    }
                } else {
                    pool.push().set(x.toDouble(), y.toDouble(),
                            minZZ.toDouble(), x + 1.0, y + 1.0, maxZZ + 1.0)
                }
            }
        }
    }

    override fun pointerPanes(x: Int,
                              y: Int,
                              z: Int,
                              range: Int,
                              pool: Pool<PointerPane>) {
        (x - range..x + range).forEach { xx ->
            (y - range..y + range).forEach { yy ->
                chunk(xx shr 4, yy shr 4) { chunk ->
                    (z - range..z + range).asSequence()
                            .filter { it in 0..(zSize - 1) }
                            .forEach { zz ->
                                val block = chunk.blockG(xx, yy, zz)
                                val type = type(block)
                                val data = data(block)
                                type.addPointerCollision(data, pool, xx, yy, zz)
                            }
                }
            }
        }
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

    override fun getEntities(): Sequence<E> {
        return entityMap.values.asSequence()
    }

    override fun getEntities(x: Int,
                             y: Int,
                             z: Int): Sequence<E> {
        var s = emptySequence<E>()
        chunk(x shr 4, y shr 4) {
            s += it.entities.values.asSequence().filter { entity ->
                val pos = entity.getCurrentPos()
                pos.intX() == x && pos.intY() == y && pos.intZ() == z
            }
        }
        return s
    }

    override fun getEntitiesAtLeast(minX: Int,
                                    minY: Int,
                                    minZ: Int,
                                    maxX: Int,
                                    maxY: Int,
                                    maxZ: Int): Sequence<E> {
        var s = emptySequence<E>()
        val minCX = minX shr 4
        val minCY = minY shr 4
        val maxCX = maxX shr 4
        val maxCY = maxY shr 4
        for (yy in minCY..maxCY) {
            for (xx in minCX..maxCX) {
                chunkNoLoad(xx, yy)?.let { chunk ->
                    s += chunk.entities.values.asSequence()
                }
            }
        }
        return s
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

    override fun type(id: Int): BlockType {
        return blocks[id] ?: throw IllegalArgumentException(
                "Non-block material: $id")
    }

    abstract fun hasChunk(x: Int,
                          y: Int): Boolean

    abstract fun chunk(x: Int,
                       y: Int): TerrainInfiniteChunk<E>?

    abstract fun chunkNoLoad(x: Int,
                             y: Int): TerrainInfiniteChunk<E>?

    abstract fun loadedChunks(): Sequence<TerrainInfiniteChunk<E>>

    fun lighting(): LightingEngine {
        return lighting
    }
}
