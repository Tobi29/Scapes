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

import org.tobi29.scapes.block.BlockType
import org.tobi29.scapes.block.Material
import org.tobi29.scapes.block.Registries
import org.tobi29.scapes.chunk.terrain.TerrainEntity
import org.tobi29.scapes.engine.utils.ConcurrentHashMap
import org.tobi29.scapes.engine.utils.Pool
import org.tobi29.scapes.engine.utils.UUID
import org.tobi29.scapes.engine.utils.math.PointerPane
import org.tobi29.scapes.engine.utils.toArray
import org.tobi29.scapes.entity.Entity
import org.tobi29.scapes.terrain.infinite.TerrainInfiniteBase
import org.tobi29.scapes.terrain.infinite.TerrainInfiniteChunkManager
import kotlin.coroutines.experimental.CoroutineContext

abstract class TerrainInfinite<E : Entity, C : TerrainInfiniteChunk<E>>(
        zSize: Int,
        taskExecutor: CoroutineContext,
        air: BlockType,
        registry: Registries,
        chunkManager: TerrainInfiniteChunkManager<C>,
        radius: Int = 0x8000000 - 16
) : TerrainInfiniteBase<BlockType, C>(zSize, taskExecutor, air,
        registry.get<Material>("Core",
                "Material").values.asSequence().map { it as? BlockType }.toArray(),
        chunkManager, radius), TerrainEntity<E> {
    protected val materials = registry.get<Material>("Core", "Material")
    override val blocks: Array<out BlockType?> = materials.values.asSequence()
            .map { it as? BlockType }.toArray()
    protected val entityMap: MutableMap<UUID, E> = ConcurrentHashMap()

    fun pointerPanes(x: Int,
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
        if (chunk(x, y, { it.removeEntity(entity) }) ?: false) {
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

    override fun entityAdded(entity: E,
                             spawn: Boolean) {
        val removed = entityMap.put(entity.uuid, entity)
        if (removed != null) {
            throw IllegalStateException("Duplicate entity: ${removed.uuid}")
        }
    }

    override fun entityRemoved(entity: E) {
        entityMap.remove(entity.uuid)
    }
}
