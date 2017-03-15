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

package org.tobi29.scapes.vanilla.basics.material.update

import org.tobi29.scapes.block.BlockType
import org.tobi29.scapes.block.Registries
import org.tobi29.scapes.block.Update
import org.tobi29.scapes.block.UpdateType
import org.tobi29.scapes.chunk.terrain.TerrainServer
import org.tobi29.scapes.engine.utils.math.max
import org.tobi29.scapes.vanilla.basics.VanillaBasics
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial

class UpdateWaterFlow(type: UpdateType) : Update(type) {
    constructor(registry: Registries) : this(
            of(registry, "vanilla.basics.update.WaterFlow"))

    private fun flow(terrain: TerrainServer.TerrainMutable,
                     x: Int,
                     y: Int,
                     z: Int,
                     materials: VanillaMaterial): Boolean {
        val block = terrain.block(x, y, z)
        val type = terrain.type(block)
        val dataHas = terrain.data(block)
        if (type.isReplaceable(terrain, x, y, z)) {
            if (dataHas > 0 || type != materials.water) {
                var dataNeed = Int.MAX_VALUE
                if (terrain.type(x, y, z + 1) == materials.water) {
                    dataNeed = 1
                } else {
                    dataNeed = data(terrain, x - 1, y, z, dataNeed, materials)
                    dataNeed = data(terrain, x + 1, y, z, dataNeed, materials)
                    dataNeed = data(terrain, x, y - 1, z, dataNeed, materials)
                    dataNeed = data(terrain, x, y + 1, z, dataNeed, materials)
                }
                if (dataNeed <= 9) {
                    if (dataNeed != dataHas || type != materials.water) {
                        if (terrain.type(x, y, z) == materials.lava) {
                            terrain.typeData(x, y, z, materials.cobblestone,
                                    materials.plugin.stoneTypes.BASALT.id)
                        } else {
                            terrain.typeData(x, y, z, materials.water, dataNeed)
                        }
                    }
                } else if (type == materials.water) {
                    terrain.typeData(x, y, z, materials.air, 0)
                }
            }
            return false
        }
        return true
    }

    private fun data(terrain: TerrainServer,
                     x: Int,
                     y: Int,
                     z: Int,
                     oldData: Int,
                     materials: VanillaMaterial): Int {
        val block = terrain.block(x, y, z)
        val type = terrain.type(block)
        val data = terrain.data(block)
        if (type == materials.water) {
            val newData = max(0, data - 1) + 2
            if (newData < oldData) {
                return newData
            }
        }
        return oldData
    }

    override fun run(terrain: TerrainServer.TerrainMutable) {
        val plugin = terrain.world.plugins.plugin(
                "VanillaBasics") as VanillaBasics
        val materials = plugin.materials
        flow(terrain, x, y, z, materials)
        if (flow(terrain, x, y, z - 1, materials)) {
            flow(terrain, x - 1, y, z, materials)
            flow(terrain, x + 1, y, z, materials)
            flow(terrain, x, y - 1, z, materials)
            flow(terrain, x, y + 1, z, materials)
        }
    }

    override fun isValidOn(type: BlockType,
                           terrain: TerrainServer): Boolean {
        val plugin = terrain.world.plugins.plugin(
                "VanillaBasics") as VanillaBasics
        val materials = plugin.materials
        return type === materials.water
    }
}
