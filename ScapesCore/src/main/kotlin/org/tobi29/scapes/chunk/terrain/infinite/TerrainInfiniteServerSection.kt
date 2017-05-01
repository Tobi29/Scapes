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
import org.tobi29.scapes.block.Update
import org.tobi29.scapes.block.UpdateBlockUpdate
import org.tobi29.scapes.block.UpdateBlockUpdateUpdateTile
import org.tobi29.scapes.chunk.terrain.TerrainMutableServer
import org.tobi29.scapes.engine.utils.assert
import org.tobi29.scapes.packets.PacketBlockChange
import org.tobi29.scapes.packets.PacketBlockChangeAir
import org.tobi29.scapes.terrain.infinite.TerrainInfiniteMutableSection

class TerrainInfiniteServerSection : TerrainInfiniteMutableSection<BlockType, TerrainInfiniteChunkServer, TerrainInfiniteServer>(), TerrainMutableServer {
    private val changes = ArrayList<BlockChange>()
    private val updates = ArrayList<Update>()

    override fun type(x: Int,
                      y: Int,
                      z: Int,
                      type: BlockType) {
        assert { locked }
        chunkFor(x, y, z)?.let { chunk ->
            chunk.blockTypeG(x, y, z, type)?.let { oldType ->
                update(x, y, z, chunk, type, chunk.dataGLocked(x, y, z),
                        oldType.causesTileUpdate())
            }
        }
    }

    override fun data(x: Int,
                      y: Int,
                      z: Int,
                      data: Int) {
        assert { locked }
        chunkFor(x, y, z)?.let { chunk ->
            chunk.dataG(x, y, z, data)?.let { oldType ->
                update(x, y, z, chunk, chunk.typeGLocked(x, y, z), data,
                        oldType.causesTileUpdate())
            }
        }
    }

    override fun typeData(x: Int,
                          y: Int,
                          z: Int,
                          type: BlockType,
                          data: Int) {
        assert { locked }
        chunkFor(x, y, z)?.let { chunk ->
            chunk.typeDataG(x, y, z, type, data)?.let { oldType ->
                update(x, y, z, chunk, type, data, oldType.causesTileUpdate())
            }
        }
    }

    override fun block(x: Int,
                       y: Int,
                       z: Int,
                       block: Long) =
            super<TerrainMutableServer>.block(x, y, z, block)

    override fun hasDelayedUpdate(x: Int,
                                  y: Int,
                                  z: Int,
                                  clazz: Class<out Update>) =
            chunkFor(x, y, z)?.hasDelayedUpdate(x, y, z, clazz) ?: false

    override fun addDelayedUpdate(update: Update) {
        assert { locked }
        chunkFor(update.x(), update.y(), update.z())?.let {
            it.addDelayedUpdate(update)
            updates.add(update)
        }
    }

    fun flushPackets() {
        for ((x, y, z, type, data) in changes) {
            if (type == terrain.air) {
                terrain.world.send(
                        PacketBlockChangeAir(terrain.world.registry, x, y, z))
            } else {
                terrain.world.send(
                        PacketBlockChange(terrain.world.registry, x, y, z,
                                type.id, data))
            }
        }
        changes.clear()
    }

    fun flushUpdates() {
        updates.forEach { it.isPaused = false }
        updates.clear()
    }

    private fun update(x: Int,
                       y: Int,
                       z: Int,
                       chunk: TerrainInfiniteChunkServer,
                       type: BlockType,
                       data: Int,
                       updateTile: Boolean) {
        if (chunk.isLoaded) {
            if (chunk.isSendable) {
                changes.add(BlockChange(x, y, z, type, data))
            }
            terrain.lighting().updateLight(x, y, z)
        }
        if (updateTile) {
            UpdateBlockUpdateUpdateTile(terrain.world.registry).set(x, y, z,
                    0.0).let {
                chunk.addDelayedUpdate(it)
                updates.add(it)
            }
        } else {
            UpdateBlockUpdate(terrain.world.registry).set(x, y, z, 0.0).let {
                chunk.addDelayedUpdate(it)
                updates.add(it)
            }
        }
    }

    private data class BlockChange(val x: Int,
                                   val y: Int,
                                   val z: Int,
                                   val type: BlockType,
                                   val data: Int)
}
