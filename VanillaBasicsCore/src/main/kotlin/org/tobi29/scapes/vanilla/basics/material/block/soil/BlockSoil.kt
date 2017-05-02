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

package org.tobi29.scapes.vanilla.basics.material.block.soil

import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.chunk.terrain.TerrainServer
import org.tobi29.scapes.chunk.terrain.isSolid
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.engine.utils.math.threadLocalRandom
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterialType
import org.tobi29.scapes.vanilla.basics.material.block.VanillaBlock

abstract class BlockSoil(type: VanillaMaterialType) : VanillaBlock(type) {
    override fun footStepSound(data: Int): String {
        return "VanillaBasics:sound/footsteps/Dirt.ogg"
    }

    override fun breakSound(item: ItemStack,
                            data: Int): String {
        return "VanillaBasics:sound/blocks/Stone.ogg"
    }

    override fun update(terrain: TerrainServer,
                        x: Int,
                        y: Int,
                        z: Int,
                        data: Int) {
        val world = terrain.world
        terrain.modify(x - 1, y - 1, z - 1, 3, 3, 3) { terrain ->
            var sides = 0
            var xx = 0
            var yy = 0
            var flag = false
            if (terrain.isSolid(x, y, z - 1)) {
                val random = threadLocalRandom()
                if (!terrain.isSolid(x - 1, y, z)) {
                    sides++
                    if (!terrain.isSolid(x - 1, y, z - 1)) {
                        xx = -1
                        flag = true
                    }
                }
                if (!terrain.isSolid(x + 1, y, z)) {
                    sides++
                    if (!terrain.isSolid(x + 1, y, z - 1)) {
                        if (xx == 0 || random.nextBoolean()) {
                            xx = 1
                        }
                        flag = true
                    }
                }
                if (!terrain.isSolid(x, y - 1, z)) {
                    sides++
                    if (!terrain.isSolid(x, y - 1, z - 1)) {
                        if (xx == 0 || random.nextBoolean()) {
                            xx = 0
                            yy = -1
                        }
                        flag = true
                    }
                }
                if (!terrain.isSolid(x, y + 1, z)) {
                    sides++
                    if (!terrain.isSolid(x, y + 1, z - 1)) {
                        if (xx == 0 && yy == 0 || random.nextBoolean()) {
                            xx = 0
                            yy = 1
                        }
                        flag = true
                    }
                }
            } else {
                sides = 5
                flag = true
            }
            if (sides > 2 && flag) {
                world.addEntityNew(
                        materials.plugin.entityTypes.flyingBlock.createServer(
                                world).apply {
                            setPos(Vector3d(x + xx + 0.5, y + yy + 0.5,
                                    z + 0.5))
                            setSpeed(Vector3d(0.0, 0.0, -1.0))
                            setType(ItemStack(this@BlockSoil, data))
                        })
                terrain.typeData(x, y, z, terrain.air, 0)
            }
        }
    }

    override fun maxStackSize(item: ItemStack): Int {
        return 16
    }
}
