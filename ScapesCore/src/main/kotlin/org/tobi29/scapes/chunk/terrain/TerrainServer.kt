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
package org.tobi29.scapes.chunk.terrain

import org.tobi29.scapes.block.BlockType
import org.tobi29.scapes.block.Update
import org.tobi29.scapes.chunk.MobSpawner
import org.tobi29.scapes.chunk.WorldServer
import org.tobi29.scapes.chunk.generator.ChunkGenerator
import org.tobi29.scapes.entity.server.EntityServer
import org.tobi29.scapes.entity.server.MobPlayerServer

interface TerrainServer : TerrainEntity<EntityServer> {
    val world: WorldServer
    val generator: ChunkGenerator

    fun update(delta: Double,
               spawners: Collection<MobSpawner>)

    fun queue(blockChanges: (TerrainServer.TerrainMutable) -> Unit)

    fun queue(blockChanges: TerrainServer.BlockChanges) {
        queue { blockChanges.run(it) }
    }

    fun addDelayedUpdate(update: Update)

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

    fun dispose()

    interface BlockChanges {
        fun run(handler: TerrainMutable)
    }

    interface TerrainMutable : TerrainServer {
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
                     block: BlockType,
                     data: Int)
    }
}
