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

package org.tobi29.scapes.terrain.infinite

import org.tobi29.scapes.engine.utils.task.TaskExecutor
import org.tobi29.scapes.terrain.TerrainBase
import org.tobi29.scapes.terrain.VoxelType
import org.tobi29.scapes.terrain.lighting.LightingEngine
import org.tobi29.scapes.terrain.lighting.LightingEngineThreaded

abstract class TerrainInfiniteBase<B : VoxelType, C : TerrainInfiniteBaseChunk<B>>(
        val zSize: Int,
        taskExecutor: TaskExecutor,
        override val air: B,
        override val blocks: Array<out B?>,
        val chunkManager: TerrainInfiniteChunkManager<C>,
        radius: Int = 0x8000000 - 16) : TerrainBase<B> {
    protected val cxMin = -radius + 1
    protected val cxMax = radius
    protected val cyMin = -radius + 1
    protected val cyMax = radius
    protected val lighting = LightingEngineThreaded(this, taskExecutor)

    override abstract fun getThreadContext(): TerrainInfiniteMutableSection<B, C, *>

    override fun sunLight(x: Int,
                          y: Int,
                          z: Int,
                          light: Int) {
        if (z < 0 || z >= zSize) {
            return
        }
        chunk(x shr 4, y shr 4) {
            it.lockWrite { it.sunLightG(x, y, z, light) }
        }
    }

    override fun blockLight(x: Int,
                            y: Int,
                            z: Int,
                            light: Int) {
        if (z < 0 || z >= zSize) {
            return
        }
        chunk(x shr 4, y shr 4) {
            it.lockWrite { it.blockLightG(x, y, z, light) }
        }
    }

    override fun block(x: Int,
                       y: Int,
                       z: Int): Long {
        if (z < 0 || z >= zSize) {
            return -1L
        }
        return chunk(x shr 4, y shr 4) {
            it.blockG(x, y, z)
        } ?: -1L
    }

    override fun type(x: Int,
                      y: Int,
                      z: Int): B {
        if (z < 0 || z >= zSize) {
            return air
        }
        return chunk(x shr 4, y shr 4) { it.typeG(x, y, z) } ?: air
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

    override fun type(id: Int): B {
        if (id < 0 || id >= blocks.size) {
            throw IllegalArgumentException("Invalid material: $id")
        }
        return blocks[id] ?: throw IllegalArgumentException(
                "Invalid material: $id")
    }

    abstract fun addChunk(x: Int,
                          y: Int): C?

    fun hasChunk(x: Int,
                 y: Int): Boolean {
        return chunkManager.has(x, y)
    }

    fun chunk(x: Int,
              y: Int): C? {
        val chunk = chunkManager[x, y]
        if (chunk != null) {
            chunk.lastAccess = System.currentTimeMillis()
            return chunk
        }
        return addChunk(x, y)
    }

    fun chunkNoLoad(x: Int,
                    y: Int): C? {
        return chunkManager[x, y]
    }

    fun loadedChunks() = chunkManager.stream()

    fun lighting(): LightingEngine {
        return lighting
    }
}
