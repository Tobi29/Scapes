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
package org.tobi29.scapes.chunk.terrain

import org.tobi29.scapes.block.BlockType
import org.tobi29.scapes.block.Update
import org.tobi29.scapes.chunk.MobSpawner
import org.tobi29.scapes.chunk.WorldClient
import org.tobi29.scapes.chunk.WorldServer
import org.tobi29.scapes.chunk.generator.ChunkGenerator
import org.tobi29.scapes.entity.Entity
import org.tobi29.scapes.entity.EntityContainer
import org.tobi29.scapes.entity.client.EntityClient
import org.tobi29.scapes.entity.server.EntityServer
import org.tobi29.scapes.entity.server.MobPlayerServer
import org.tobi29.scapes.packets.PacketBlockChange
import org.tobi29.scapes.terrain.TerrainBase
import org.tobi29.scapes.terrain.TerrainChunk

typealias Terrain = TerrainBase<BlockType>

interface TerrainEntity<E : Entity> : Terrain, EntityContainer<E>

interface TerrainClient : TerrainEntity<EntityClient> {
    val world: WorldClient
    val renderer: TerrainRenderer

    fun update(delta: Double)

    fun toggleStaticRenderDistance()

    fun reloadGeometry()

    fun process(packet: PacketBlockChange)

    fun init() {
        throw UnsupportedOperationException("Terrain not disposable")
    }

    suspend fun dispose() {
        throw UnsupportedOperationException("Terrain not disposable")
    }
}

interface TerrainServer : TerrainEntity<EntityServer> {
    val world: WorldServer
    val generator: ChunkGenerator

    fun update(delta: Double,
               spawners: Collection<MobSpawner>)

    fun <R> modify(x: Int,
                   y: Int,
                   z: Int,
                   dx: Int,
                   dy: Int,
                   dz: Int,
                   block: (TerrainMutableServer) -> R): R

    fun <R> modify(x: Int,
                   y: Int,
                   z: Int,
                   block: (TerrainMutableServer) -> R): R =
            modify(x, y, z, 1, 1, 1, block)

    fun hasDelayedUpdate(x: Int,
                         y: Int,
                         z: Int,
                         clazz: Class<out Update>): Boolean

    fun isBlockSendable(player: MobPlayerServer,
                        x: Int,
                        y: Int,
                        z: Int,
                        chunkContent: Boolean): Boolean

    fun chunks(consumer: (TerrainChunk) -> Unit)

    suspend fun dispose()
}

interface TerrainMutable : Terrain {
    fun block(x: Int,
              y: Int,
              z: Int,
              block: Long) {
        typeData(x, y, z, type(block), data(block))
    }

    fun type(x: Int,
             y: Int,
             z: Int,
             type: BlockType)

    fun data(x: Int,
             y: Int,
             z: Int,
             data: Int)

    fun typeData(x: Int,
                 y: Int,
                 z: Int,
                 type: BlockType,
                 data: Int)
}

interface TerrainMutableServer : TerrainMutable {
    fun hasDelayedUpdate(x: Int,
                         y: Int,
                         z: Int,
                         clazz: Class<out Update>): Boolean

    fun addDelayedUpdate(update: Update)
}
