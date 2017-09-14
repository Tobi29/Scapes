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
import org.tobi29.scapes.chunk.terrain.TerrainClient
import org.tobi29.scapes.engine.utils.Pool
import org.tobi29.scapes.engine.utils.UUID
import org.tobi29.scapes.engine.utils.math.PointerPane
import org.tobi29.scapes.entity.client.EntityClient
import org.tobi29.scapes.packets.PacketBlockChange
import org.tobi29.scapes.terrain.infinite.TerrainInfiniteSection

class TerrainInfiniteRenderSection : TerrainInfiniteSection<BlockType, TerrainInfiniteChunkClient, TerrainInfiniteClient>(), TerrainClient {
    override val blocks get() = terrain.blocks
    override val world get() = terrain.world
    override val renderer get() = terrain.renderer

    fun pointerPanes(x: Int,
                     y: Int,
                     z: Int,
                     range: Int,
                     pool: Pool<PointerPane>) {
        terrain.pointerPanes(x, y, z, range, pool)
    }

    override fun addEntity(entity: EntityClient,
                           spawn: Boolean): Boolean {
        return terrain.addEntity(entity, spawn)
    }

    override fun removeEntity(entity: EntityClient): Boolean {
        return terrain.removeEntity(entity)
    }

    override fun hasEntity(entity: EntityClient): Boolean {
        return terrain.hasEntity(entity)
    }

    override fun getEntity(uuid: UUID): EntityClient? {

        return terrain.getEntity(uuid)
    }

    override fun getEntities(): Sequence<EntityClient> {
        return terrain.getEntities()
    }

    override fun getEntities(x: Int,
                             y: Int,
                             z: Int): Sequence<EntityClient> {
        return terrain.getEntities(x, y, z)
    }

    override fun getEntitiesAtLeast(minX: Int,
                                    minY: Int,
                                    minZ: Int,
                                    maxX: Int,
                                    maxY: Int,
                                    maxZ: Int): Sequence<EntityClient> {
        return terrain.getEntitiesAtLeast(minX, minY, minZ, maxX, maxY, maxZ)
    }

    override fun entityAdded(entity: EntityClient,
                             spawn: Boolean) {
        terrain.entityAdded(entity, spawn)
    }

    override fun entityRemoved(entity: EntityClient) {
        terrain.entityRemoved(entity)
    }

    override fun update(delta: Double) {
        terrain.update(delta)
    }

    override fun toggleStaticRenderDistance() {
        terrain.toggleStaticRenderDistance()
    }

    override fun reloadGeometry() {
        terrain.reloadGeometry()
    }

    override fun process(packet: PacketBlockChange) {
        terrain.process(packet)
    }
}
