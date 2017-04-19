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
import org.tobi29.scapes.chunk.terrain.TerrainEntity
import org.tobi29.scapes.engine.utils.ConcurrentHashMap
import org.tobi29.scapes.engine.utils.UUID
import org.tobi29.scapes.engine.utils.math.vector.Vector2i
import org.tobi29.scapes.engine.utils.math.vector.Vector3i
import org.tobi29.scapes.engine.utils.readOnly
import org.tobi29.scapes.entity.Entity
import org.tobi29.scapes.terrain.TerrainChunk
import org.tobi29.scapes.terrain.infinite.TerrainInfiniteBaseChunk

abstract class TerrainInfiniteChunk<E : Entity>(pos: Vector2i,
                                                private val entityTerrain: TerrainEntity<E>,
                                                zSize: Int) : TerrainInfiniteBaseChunk<BlockType>(
        pos, entityTerrain, zSize), TerrainChunk {
    override val posBlock = Vector3i(pos.x shl 4, pos.y shl 4, 0)
    override val size = Vector3i(16, 16, zSize)
    protected val entitiesMut = ConcurrentHashMap<UUID, E>()
    val entities = entitiesMut.readOnly()

    fun addEntity(entity: E) {
        mapEntity(entity)
        entityTerrain.entityAdded(entity)
    }

    fun removeEntity(entity: E): Boolean {
        if (unmapEntity(entity)) {
            entityTerrain.entityRemoved(entity)
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
}
