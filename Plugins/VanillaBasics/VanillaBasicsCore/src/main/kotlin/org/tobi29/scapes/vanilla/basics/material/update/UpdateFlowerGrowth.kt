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
import org.tobi29.scapes.vanilla.basics.VanillaBasics
import java.util.*
import java.util.concurrent.ThreadLocalRandom

class UpdateFlowerGrowth(type: UpdateType) : Update(type) {
    constructor(registry: Registries) : this(
            of(registry, "vanilla.basics.update.FlowerGrowth"))

    override fun run(terrain: TerrainServer.TerrainMutable) {
        val plugin = terrain.world.plugins.plugin(
                "VanillaBasics") as VanillaBasics
        val materials = plugin.materials
        val type = terrain.type(x, y, z)
        if (type === materials.air) {
            if (terrain.sunLight(x, y, z) >= 12 && terrain.type(x, y,
                    z - 1) === materials.grass) {
                var flowers = 0
                val datas = ArrayList<Int>()
                for (xx in -4..4) {
                    for (yy in -4..4) {
                        for (zz in -4..4) {
                            val searchBlock = terrain.block(x + xx, y + yy,
                                    z + zz)
                            if (terrain.type(searchBlock) == materials.flower) {
                                if (flowers++ > 1) {
                                    return
                                }
                                datas.add(terrain.data(searchBlock))
                            }
                        }
                    }
                }
                if (datas.isEmpty()) {
                    terrain.typeData(x, y, z, materials.flower,
                            Random().nextInt(20).toShort().toInt())
                } else {
                    terrain.typeData(x, y, z, materials.flower,
                            datas[Random().nextInt(datas.size)])
                }
            }
        } else if (type === materials.snow) {
            if (terrain.sunLight(x, y, z) >= 12 && terrain.type(x, y,
                    z - 1) === materials.grass) {
                val random = ThreadLocalRandom.current()
                terrain.addDelayedUpdate(
                        UpdateFlowerGrowth(this.type).set(x, y, z,
                                random.nextDouble() * 3600.0 + 3600.0))
            }
        }
    }

    override fun isValidOn(type: BlockType,
                           terrain: TerrainServer): Boolean {
        return true
    }
}
