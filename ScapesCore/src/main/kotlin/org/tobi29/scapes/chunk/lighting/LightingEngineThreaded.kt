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
package org.tobi29.scapes.chunk.lighting

import org.tobi29.scapes.chunk.terrain.Terrain
import org.tobi29.scapes.engine.utils.Pool
import org.tobi29.scapes.engine.utils.math.clamp
import org.tobi29.scapes.engine.utils.math.max
import org.tobi29.scapes.engine.utils.math.vector.MutableVector3i
import org.tobi29.scapes.engine.utils.math.vector.Vector3i
import org.tobi29.scapes.engine.utils.profiler.profilerSection
import org.tobi29.scapes.engine.utils.task.Joiner
import org.tobi29.scapes.engine.utils.task.TaskExecutor
import java.util.concurrent.ConcurrentLinkedQueue

class LightingEngineThreaded(private val terrain: Terrain,
                             taskExecutor: TaskExecutor) : LightingEngine {
    private val updates = ConcurrentLinkedQueue<Vector3i>()
    private val joiner: Joiner

    init {
        joiner = taskExecutor.runThread({ run(it) }, "Lighting-Engine")
    }

    override fun updateLight(x: Int,
                             y: Int,
                             z: Int) {
        updates.add(Vector3i(x, y, z))
        joiner.wake()
    }

    override fun dispose() {
        joiner.join()
    }

    private fun updateBlockLight(updates: Pool<MutableVector3i>,
                                 newUpdates: Pool<MutableVector3i>,
                                 x: Int,
                                 y: Int,
                                 z: Int) {
        var updatesTake = updates
        var updatesNext = newUpdates
        updatesTake.push().set(x, y, z)
        while (updatesTake.isNotEmpty()) {
            for (update in updatesTake) {
                if (terrain.isBlockLoaded(update.x, update.y,
                        update.z)) {
                    val block = terrain.block(update.x, update.y,
                            update.z)
                    val type = terrain.type(block)
                    val data = terrain.data(block)
                    val lightTrough = type.lightTrough(terrain, update.x,
                            update.y, update.z)
                    var light = type.lightEmit(terrain, update.x,
                            update.y, update.z, data)
                    light = max(
                            terrain.blockLight(update.x - 1, update.y,
                                    update.z) + lightTrough,
                            light.toInt()).toByte()
                    light = max(
                            terrain.blockLight(update.x + 1, update.y,
                                    update.z) + lightTrough,
                            light.toInt()).toByte()
                    light = max(
                            terrain.blockLight(update.x, update.y - 1,
                                    update.z) + lightTrough,
                            light.toInt()).toByte()
                    light = max(
                            terrain.blockLight(update.x, update.y + 1,
                                    update.z) + lightTrough,
                            light.toInt()).toByte()
                    light = max(terrain.blockLight(update.x, update.y,
                            update.z - 1) + lightTrough,
                            light.toInt()).toByte()
                    light = max(terrain.blockLight(update.x, update.y,
                            update.z + 1) + lightTrough,
                            light.toInt()).toByte()
                    light = clamp(light.toInt(), 0, 15).toByte()
                    if (light.toInt() != terrain.blockLight(update.x,
                            update.y,
                            update.z)) {
                        terrain.blockLight(update.x, update.y,
                                update.z, light.toInt())
                        updatesNext.push().set(update.x - 1, update.y,
                                update.z)
                        updatesNext.push().set(update.x + 1, update.y,
                                update.z)
                        updatesNext.push().set(update.x, update.y - 1,
                                update.z)
                        updatesNext.push().set(update.x, update.y + 1,
                                update.z)
                        updatesNext.push().set(update.x, update.y,
                                update.z - 1)
                        updatesNext.push().set(update.x, update.y,
                                update.z + 1)
                    }
                }
            }
            val swapUpdates = updatesTake
            swapUpdates.reset()
            updatesTake = updatesNext
            updatesNext = swapUpdates
        }
    }

    private fun updateSunLight(updates: Pool<MutableVector3i>,
                               newUpdates: Pool<MutableVector3i>,
                               x: Int,
                               y: Int,
                               z: Int) {
        var updatesTake = updates
        var updatesNext = newUpdates
        updatesTake.push().set(x, y, z)
        while (updatesTake.isNotEmpty()) {
            for (update in updatesTake) {
                if (terrain.isBlockLoaded(update.x, update.y,
                        update.z)) {
                    val type = terrain.type(update.x, update.y,
                            update.z)
                    val lightTrough = type.lightTrough(terrain, update.x,
                            update.y, update.z)
                    var light = calcSunLightAt(update.x, update.y,
                            update.z)
                    light = max(
                            terrain.sunLight(update.x - 1, update.y,
                                    update.z) + lightTrough,
                            light.toInt()).toByte()
                    light = max(
                            terrain.sunLight(update.x + 1, update.y,
                                    update.z) + lightTrough,
                            light.toInt()).toByte()
                    light = max(
                            terrain.sunLight(update.x, update.y - 1,
                                    update.z) + lightTrough,
                            light.toInt()).toByte()
                    light = max(
                            terrain.sunLight(update.x, update.y + 1,
                                    update.z) + lightTrough,
                            light.toInt()).toByte()
                    light = max(terrain.sunLight(update.x, update.y,
                            update.z - 1) + lightTrough,
                            light.toInt()).toByte()
                    light = max(terrain.sunLight(update.x, update.y,
                            update.z + 1) + lightTrough,
                            light.toInt()).toByte()
                    light = clamp(light.toInt(), 0, 15).toByte()
                    if (light.toInt() != terrain.sunLight(update.x,
                            update.y,
                            update.z)) {
                        terrain.sunLight(update.x, update.y,
                                update.z, light.toInt())
                        updatesNext.push().set(update.x - 1, update.y,
                                update.z)
                        updatesNext.push().set(update.x + 1, update.y,
                                update.z)
                        updatesNext.push().set(update.x, update.y - 1,
                                update.z)
                        updatesNext.push().set(update.x, update.y + 1,
                                update.z)
                        updatesNext.push().set(update.x, update.y,
                                update.z - 1)
                        updatesNext.push().set(update.x, update.y,
                                update.z + 1)
                    }
                }
            }
            val swapUpdates = updatesTake
            swapUpdates.reset()
            updatesTake = updatesNext
            updatesNext = swapUpdates
        }
    }

    private fun calcSunLightAt(x: Int,
                               y: Int,
                               z: Int): Byte {
        var sunLight: Byte = 15
        var zz = terrain.highestBlockZAt(x, y)
        while (zz >= z && zz >= 0) {
            val type = terrain.type(x, y, zz)
            if (type.isSolid(terrain, x, y, zz) || !type.isTransparent(terrain,
                    x, y, zz)) {
                sunLight = clamp(sunLight + type.lightTrough(terrain, x, y, zz),
                        0, 15).toByte()
            }
            zz--
        }
        return sunLight
    }

    fun run(joiner: Joiner.Joinable) {
        while (!joiner.marked) {
            val updates = Pool { MutableVector3i() }
            val newUpdates = Pool { MutableVector3i() }
            while (!this.updates.isEmpty()) {
                val update = this.updates.poll()
                if (update != null) {
                    profilerSection("BlockLight") {
                        updateBlockLight(updates, newUpdates, update.x,
                                update.y, update.z)
                    }
                    profilerSection("SunLight") {
                        updateSunLight(updates, newUpdates, update.x,
                                update.y, update.z)
                    }
                }
            }
            joiner.sleep()
        }
    }
}
