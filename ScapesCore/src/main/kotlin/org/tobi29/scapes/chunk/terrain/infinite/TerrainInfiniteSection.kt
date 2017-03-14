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
import org.tobi29.scapes.chunk.terrain.TerrainClient
import org.tobi29.scapes.engine.utils.Pool
import org.tobi29.scapes.engine.utils.math.PointerPane
import org.tobi29.scapes.engine.utils.math.vector.Vector2i
import org.tobi29.scapes.entity.client.EntityClient
import org.tobi29.scapes.packets.PacketBlockChange
import java.util.*

class TerrainInfiniteSection : TerrainClient {
    override val air get() = access { it.air }
    override val registry get() = access { it.registry }
    override val world get() = access { it.world }
    override val renderer get() = access { it.renderer }
    private val chunks = arrayOfNulls<TerrainInfiniteChunkClient?>(9)
    private var terrain: TerrainInfiniteClient? = null
    private var zSize = 0
    private var x = 0
    private var y = 0

    fun init(terrain: TerrainInfiniteClient,
             pos: Vector2i) {
        this.terrain = terrain
        zSize = terrain.zSize
        x = pos.x - 1
        y = pos.y - 1
        chunks[0] = terrain.chunk(x, y)
        chunks[1] = terrain.chunk(x + 1, y)
        chunks[2] = terrain.chunk(x + 2, y)
        chunks[3] = terrain.chunk(x, y + 1)
        chunks[4] = terrain.chunk(x + 1, y + 1)
        chunks[5] = terrain.chunk(x + 2, y + 1)
        chunks[6] = terrain.chunk(x, y + 2)
        chunks[7] = terrain.chunk(x + 1, y + 2)
        chunks[8] = terrain.chunk(x + 2, y + 2)
    }

    fun clear() {
        terrain = null
        zSize = 0
        x = 0
        y = 0
        Arrays.fill(chunks, null)
    }

    override fun sunLight(x: Int,
                          y: Int,
                          z: Int,
                          light: Int) {
        access { terrain ->
            if (z < 0 || z >= zSize) {
                return
            }
            get(x, y)?.let { chunk ->
                chunk.sunLightG(x, y, z, light)
                return
            }
            terrain.sunLight(x, y, z, light)
        }
    }

    override fun blockLight(x: Int,
                            y: Int,
                            z: Int,
                            light: Int) {
        access { terrain ->
            if (z < 0 || z >= zSize) {
                return
            }
            get(x, y)?.let { chunk ->
                chunk.blockLightG(x, y, z, light)
                return
            }
            terrain.blockLight(x, y, z, light)
        }
    }

    override fun block(x: Int,
                       y: Int,
                       z: Int): Long {
        access { terrain ->
            if (z < 0 || z >= zSize) {
                return terrain.voidBlock.id.toLong() shl 32
            }
            get(x, y)?.let { chunk ->
                return chunk.blockG(x, y, z)
            }
            return terrain.block(x, y, z)
        }
    }

    override fun type(x: Int,
                      y: Int,
                      z: Int): BlockType {
        access { terrain ->
            if (z < 0 || z >= zSize) {
                return terrain.voidBlock
            }
            get(x, y)?.let { chunk ->
                return chunk.typeG(x, y, z)
            }
            return terrain.type(x, y, z)
        }
    }

    override fun light(x: Int,
                       y: Int,
                       z: Int): Int {
        access { terrain ->
            if (z < 0 || z >= zSize) {
                return 0
            }
            get(x, y)?.let { chunk ->
                return chunk.lightG(x, y, z)
            }
            return terrain.light(x, y, z)
        }
    }

    override fun sunLight(x: Int,
                          y: Int,
                          z: Int): Int {
        access { terrain ->
            if (z < 0 || z >= zSize) {
                return 0
            }
            get(x, y)?.let { chunk2 ->
                return chunk2.sunLightG(x, y, z)
            }
            return terrain.sunLight(x, y, z)
        }
    }

    override fun blockLight(x: Int,
                            y: Int,
                            z: Int): Int {
        access { terrain ->
            if (z < 0 || z >= zSize) {
                return 0
            }
            get(x, y)?.let { chunk2 ->
                return chunk2.blockLightG(x, y, z)
            }
            return terrain.blockLight(x, y, z)
        }
    }

    override fun sunLightReduction(x: Int,
                                   y: Int): Int {
        access { terrain ->
            return terrain.sunLightReduction(x, y)
        }
    }

    override fun highestBlockZAt(x: Int,
                                 y: Int): Int {
        access { terrain ->
            get(x, y)?.let { chunk2 ->
                return chunk2.highestBlockZAtG(x, y)
            }
            return terrain.highestBlockZAt(x, y)
        }
    }

    override fun highestTerrainBlockZAt(x: Int,
                                        y: Int): Int {
        access { terrain ->
            get(x, y)?.let { chunk2 ->
                return chunk2.highestTerrainBlockZAtG(x, y)
            }
            return terrain.highestTerrainBlockZAt(x, y)
        }
    }

    override fun isBlockLoaded(x: Int,
                               y: Int,
                               z: Int): Boolean {
        access { terrain ->
            return terrain.isBlockLoaded(x, y, z)
        }
    }

    override fun isBlockTicking(x: Int,
                                y: Int,
                                z: Int): Boolean {
        access { terrain ->
            return terrain.isBlockTicking(x, y, z)
        }
    }

    override fun collisions(minX: Int,
                            minY: Int,
                            minZ: Int,
                            maxX: Int,
                            maxY: Int,
                            maxZ: Int,
                            pool: Pool<AABBElement>) {
        access { terrain ->
            terrain.collisions(minX, minY, minZ, maxX, maxY, maxZ, pool)
        }
    }

    override fun pointerPanes(x: Int,
                              y: Int,
                              z: Int,
                              range: Int,
                              pool: Pool<PointerPane>) {
        access { terrain ->
            terrain.pointerPanes(x, y, z, range, pool)
        }
    }

    override fun addEntity(entity: EntityClient): Boolean {
        access { terrain ->
            return terrain.addEntity(entity)
        }
    }

    override fun removeEntity(entity: EntityClient): Boolean {
        access { terrain ->
            return terrain.removeEntity(entity)
        }
    }

    override fun hasEntity(entity: EntityClient): Boolean {
        access { terrain ->
            return terrain.hasEntity(entity)
        }
    }

    override fun getEntity(uuid: UUID): EntityClient? {
        access { terrain ->
            return terrain.getEntity(uuid)
        }
    }

    override fun getEntities(): Sequence<EntityClient> {
        access { return it.getEntities() }
    }

    override fun getEntities(x: Int,
                             y: Int,
                             z: Int): Sequence<EntityClient> {
        access { return it.getEntities(x, y, z) }
    }

    override fun getEntitiesAtLeast(minX: Int,
                                    minY: Int,
                                    minZ: Int,
                                    maxX: Int,
                                    maxY: Int,
                                    maxZ: Int): Sequence<EntityClient> {
        access {
            return it.getEntitiesAtLeast(minX, minY, minZ, maxX, maxY, maxZ)
        }
    }

    override fun entityAdded(entity: EntityClient) {
        access { terrain ->
            terrain.entityAdded(entity)
        }
    }

    override fun entityRemoved(entity: EntityClient) {
        access { terrain ->
            terrain.entityRemoved(entity)
        }
    }

    private operator fun get(x: Int,
                             y: Int): TerrainInfiniteChunkClient? {
        access { terrain ->
            val xx = (x shr 4) - this.x
            val yy = (y shr 4) - this.y
            if (xx < 0 || xx >= 3 || yy < 0 || yy >= 3) {
                return null
            }
            return chunks[yy * 3 + xx]
        }
    }

    override fun update(delta: Double) {
        access { terrain ->
            terrain.update(delta)
        }
    }

    override fun toggleStaticRenderDistance() {
        access(
                TerrainInfiniteClient::toggleStaticRenderDistance)
    }

    override fun reloadGeometry() {
        access(
                TerrainInfiniteClient::reloadGeometry)
    }

    override fun process(packet: PacketBlockChange) {
        access { terrain ->
            terrain.process(packet)
        }
    }

    override fun dispose() {
        throw UnsupportedOperationException("Terrain not disposable")
    }

    override fun type(id: Int): BlockType {
        access { terrain ->
            return terrain.type(id)
        }
    }

    private inline fun <R> access(receiver: (TerrainInfiniteClient) -> R): R {
        val terrain = terrain ?: throw IllegalStateException(
                "Terrain section not initialized")
        return receiver(terrain)
    }
}
