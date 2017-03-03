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

package org.tobi29.scapes.vanilla.basics.entity.server

import org.tobi29.scapes.chunk.WorldServer
import org.tobi29.scapes.chunk.terrain.TerrainServer
import org.tobi29.scapes.engine.utils.io.tag.*
import org.tobi29.scapes.engine.utils.math.max
import org.tobi29.scapes.engine.utils.math.min
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.entity.server.EntityServer
import org.tobi29.scapes.vanilla.basics.VanillaBasics
import org.tobi29.scapes.vanilla.basics.material.CropType

class EntityFarmlandServer(world: WorldServer,
                           pos: Vector3d = Vector3d.ZERO,
                           private var nutrientA: Float = 0.0f,
                           private var nutrientB: Float = 0.0f,
                           private var nutrientC: Float = 0.0f) : EntityServer(
        world, pos) {
    private var time = 0.0f
    private var stage: Byte = 0
    private var cropType: CropType? = null
    private var updateBlock = false

    override fun write(map: ReadWriteTagMap) {
        super.write(map)
        map["NutrientA"] = nutrientA
        map["NutrientB"] = nutrientB
        map["NutrientC"] = nutrientC
        map["Time"] = time
        map["Stage"] = stage
        cropType?.let {
            map["CropType"] = it.id
        }
    }

    override fun read(map: TagMap) {
        super.read(map)
        map["NutrientA"]?.toFloat()?.let { nutrientA = it }
        map["NutrientB"]?.toFloat()?.let { nutrientB = it }
        map["NutrientC"]?.toFloat()?.let { nutrientC = it }
        map["Time"]?.toFloat()?.let { time = it }
        map["Stage"]?.toByte()?.let { stage = it }
        if (map.containsKey("CropType")) {
            map["CropType"]?.toInt()?.let { cropType = CropType[registry, it] }
            updateBlock = true
        } else {
            cropType = null
        }
    }

    override fun update(delta: Double) {
        growth(delta)
        if (updateBlock) {
            val plugin = world.plugins.plugin(
                    "VanillaBasics") as VanillaBasics
            val materials = plugin.materials
            val cropType = this.cropType
            if (cropType == null) {
                world.terrain.queue { handler ->
                    handler.typeData(pos.intX(), pos.intY(), pos.intZ() + 1,
                            materials.air, 0)
                }
            } else {
                val stage = this.stage.toInt()
                world.terrain.queue { handler ->
                    handler.typeData(pos.intX(), pos.intY(), pos.intZ() + 1,
                            materials.crop,
                            stage + (cropType.id shl 3) - 1)
                }
            }
            updateBlock = false
        }
    }

    override fun updateTile(terrain: TerrainServer,
                            x: Int,
                            y: Int,
                            z: Int,
                            data: Int) {
        val world = terrain.world
        val plugin = world.plugins.plugin("VanillaBasics") as VanillaBasics
        val materials = plugin.materials
        if (terrain.type(pos.intX(), pos.intY(),
                pos.intZ()) !== materials.farmland) {
            world.removeEntity(this)
        } else if (stage > 0 && terrain.type(pos.intX(), pos.intY(),
                pos.intZ() + 1) !== materials.crop) {
            cropType = null
        }
    }

    override fun tickSkip(oldTick: Long,
                          newTick: Long) {
        growth(((newTick - oldTick) / 20.0f).toDouble())
    }

    fun seed(cropType: CropType) {
        this.cropType = cropType
        stage = 0
        time = 0.0f
    }

    private fun growth(delta: Double) {
        nutrientA = min(nutrientA + 0.0000002 * delta, 1.0).toFloat()
        nutrientB = min(nutrientB + 0.0000002 * delta, 1.0).toFloat()
        nutrientC = min(nutrientC + 0.0000002 * delta, 1.0).toFloat()
        val cropType = cropType
        if (cropType == null) {
            stage = 0
            time = 0.0f
        } else {
            if (stage < 8) {
                when (cropType.nutrient) {
                    1 -> {
                        time += (nutrientB * delta).toFloat()
                        nutrientB = max(nutrientB - 0.0000005 * delta,
                                0.0).toFloat()
                    }
                    2 -> {
                        time += (nutrientC * delta).toFloat()
                        nutrientC = max(nutrientC - 0.0000005 * delta,
                                0.0).toFloat()
                    }
                    else -> {
                        time += (nutrientA * delta).toFloat()
                        nutrientA = max(nutrientA - 0.0000005 * delta,
                                0.0).toFloat()
                    }
                }
                while (time >= cropType.time) {
                    stage++
                    if (stage >= 8) {
                        time = 0.0f
                        stage = 8
                    } else {
                        time -= cropType.time.toFloat()
                    }
                    updateBlock = true
                }
            }
        }
    }
}
