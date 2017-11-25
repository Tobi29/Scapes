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

import org.tobi29.scapes.engine.utils.andNull
import org.tobi29.scapes.engine.math.vector.Vector2i
import org.tobi29.scapes.engine.utils.profiler.profilerSection
import org.tobi29.scapes.engine.utils.tag.*
import org.tobi29.scapes.entity.client.EntityClient
import org.tobi29.scapes.entity.client.MobClient
import org.tobi29.scapes.terrain.infinite.TerrainInfiniteBaseChunk

class TerrainInfiniteChunkClient(pos: Vector2i,
                                 val terrain: TerrainInfiniteClient,
                                 zSize: Int,
                                 renderer: TerrainInfiniteRenderer) : TerrainInfiniteChunk<EntityClient>(
        pos, terrain, zSize) {
    private val rendererChunk = TerrainInfiniteRendererChunk(this, renderer)

    fun setLoaded() {
        state = TerrainInfiniteBaseChunk.State.LOADED
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
                    if (terrain.chunkC(x, y, { chunk ->
                        chunk.mapEntity(entity)
                        true
                    }) == null) {
                        terrain.entityRemoved(entity)
                    }
                }
            }
        }
    }

    override fun dispose() {
        entitiesMut.values.forEach { terrain.entityRemoved(it) }
    }

    override fun update(x: Int,
                        y: Int,
                        z: Int,
                        updateTile: Boolean) {
        terrain.renderer.blockChange(x + posBlock.x, y + posBlock.y, z)
        if (state.id >= TerrainInfiniteBaseChunk.State.LOADED.id) {
            terrain.lighting().updateLight(x + posBlock.x, y + posBlock.y, z)
        }
    }

    override fun updateLight(x: Int,
                             y: Int,
                             z: Int) {
        terrain.renderer.blockChange(x + posBlock.x,
                y + posBlock.y, z)
    }

    fun rendererChunk(): TerrainInfiniteRendererChunk {
        return rendererChunk
    }

    fun read(map: TagMap) {
        val data = data
        map["BlockID"]?.toList()?.let {
            (data.idData.asSequence() zip it.asSequence().mapNotNull(
                    Tag::toMap).andNull()).forEach { (data, tag) ->
                data.read(tag)
            }
        }
        map["BlockData"]?.toList()?.let {
            (data.dataData.asSequence() zip it.asSequence().mapNotNull(
                    Tag::toMap).andNull()).forEach { (data, tag) ->
                data.read(tag)
            }
        }
        map["BlockLight"]?.toList()?.let {
            (data.lightData.asSequence() zip it.asSequence().mapNotNull(
                    Tag::toMap).andNull()).forEach { (data, tag) ->
                data.read(tag)
            }
        }
        map["MetaData"]?.toMap()?.let { metaData = it.toMutTag() }
        initHeightMap()
    }
}
