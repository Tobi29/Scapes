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

import org.tobi29.scapes.engine.utils.io.tag.TagStructure
import org.tobi29.scapes.engine.utils.math.vector.Vector2i
import org.tobi29.scapes.engine.utils.profiler.profilerSection
import org.tobi29.scapes.entity.client.EntityClient
import org.tobi29.scapes.entity.client.MobClient
import org.tobi29.scapes.packets.PacketRequestChunk
import java.util.concurrent.atomic.AtomicBoolean

class TerrainInfiniteChunkClient(pos: Vector2i,
                                 val terrain2: TerrainInfiniteClient,
                                 zSize: Int,
                                 renderer: TerrainInfiniteRenderer) : TerrainInfiniteChunk<EntityClient>(
        pos, terrain2, zSize, terrain2.world.registry.blocks()) {
    private val rendererChunk: TerrainInfiniteRendererChunk
    private val requested = AtomicBoolean(false)

    init {
        rendererChunk = TerrainInfiniteRendererChunk(this, renderer)
    }

    fun checkLoaded(): Boolean {
        if (state.id < TerrainInfiniteChunk.State.LOADED.id) {
            val terrainClient = terrain2
            if (!requested.get() && terrainClient.requestedChunks() < 3) {
                requested.set(true)
                terrainClient.changeRequestedChunks(1)
                terrain2.world.send(
                        PacketRequestChunk(pos.x, pos.y))
            }
            return true
        }
        return false
    }

    fun setLoaded() {
        state = TerrainInfiniteChunk.State.LOADED
    }

    fun updateClient(delta: Double) {
        profilerSection("Entities") {
            entitiesMut.values.forEach { entity ->
                entity.update(delta)
                if (entity is MobClient) {
                    entity.move(delta)
                }
                val pos = entity.getCurrentPos()
                val x = pos.intX() shr 4
                val y = pos.intY() shr 4
                if ((x != this.pos.x || y != this.pos.y) && unmapEntity(
                        entity)) {
                    if (terrain2.chunkC(x, y, { chunk ->
                        chunk.mapEntity(entity)
                        true
                    }) == null) {
                        terrain.entityRemoved(entity)
                    }
                }
            }
        }
    }

    fun dispose() {
        entitiesMut.values.forEach { terrain.entityRemoved(it) }
    }

    override fun update(x: Int,
                        y: Int,
                        z: Int,
                        updateTile: Boolean) {
        terrain2.renderer.blockChange(x + posBlock.x,
                y + posBlock.y,
                z)
        if (state.id >= TerrainInfiniteChunk.State.LOADED.id) {
            terrain.lighting().updateLight(x + posBlock.x,
                    y + posBlock.y, z)
        }
    }

    override fun updateLight(x: Int,
                             y: Int,
                             z: Int) {
        terrain2.renderer.blockChange(x + posBlock.x,
                y + posBlock.y,
                z)
    }

    fun rendererChunk(): TerrainInfiniteRendererChunk {
        return rendererChunk
    }

    fun resetRequest() {
        requested.set(false)
    }

    fun load(tagStructure: TagStructure) {
        lockWrite {
            tagStructure.getList("BlockID")?.let { bID.load(it) }
            tagStructure.getList("BlockData")?.let { bData.load(it) }
            tagStructure.getList("BlockLight")?.let { bLight.load(it) }
        }
        initHeightMap()
        tagStructure.getStructure("MetaData")?.let { metaData = it }
        initHeightMap()
    }
}
