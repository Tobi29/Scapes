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

package scapes.plugin.tobi29.vanilla.basics.material.block.soil

import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.chunk.terrain.TerrainServer
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.entity.server.MobFlyingBlockServer
import scapes.plugin.tobi29.vanilla.basics.material.VanillaMaterial
import scapes.plugin.tobi29.vanilla.basics.material.block.VanillaBlock
import java.util.concurrent.ThreadLocalRandom

abstract class BlockSoil protected constructor(materials: VanillaMaterial,
                                               nameID: String) : VanillaBlock(
        materials, nameID) {

    override fun footStepSound(data: Int): String {
        return "VanillaBasics:sound/footsteps/Dirt.ogg"
    }

    override fun breakSound(item: ItemStack,
                            data: Int): String {
        return "VanillaBasics:sound/blocks/Stone.ogg"
    }

    override fun update(terrain: TerrainServer.TerrainMutable,
                        x: Int,
                        y: Int,
                        z: Int,
                        data: Int) {
        var sides = 0
        var xx = 0
        var yy = 0
        var flag = false
        if (terrain.type(x, y, z - 1).isSolid(terrain, x, y, z - 1)) {
            val random = ThreadLocalRandom.current()
            if (!terrain.type(x - 1, y, z).isSolid(terrain, x - 1, y, z)) {
                sides++
                if (!terrain.type(x - 1, y, z - 1).isSolid(terrain, x - 1, y,
                        z - 1)) {
                    xx = -1
                    flag = true
                }
            }
            if (!terrain.type(x + 1, y, z).isSolid(terrain, x + 1, y, z)) {
                sides++
                if (!terrain.type(x + 1, y, z - 1).isSolid(terrain, x + 1, y,
                        z - 1)) {
                    if (xx == 0 || random.nextBoolean()) {
                        xx = 1
                    }
                    flag = true
                }
            }
            if (!terrain.type(x, y - 1, z).isSolid(terrain, x, y - 1, z)) {
                sides++
                if (!terrain.type(x, y - 1, z - 1).isSolid(terrain, x, y - 1,
                        z - 1)) {
                    if (xx == 0 || random.nextBoolean()) {
                        xx = 0
                        yy = -1
                    }
                    flag = true
                }
            }
            if (!terrain.type(x, y + 1, z).isSolid(terrain, x, y + 1, z)) {
                sides++
                if (!terrain.type(x, y + 1, z - 1).isSolid(terrain, x, y + 1,
                        z - 1)) {
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
            terrain.world.addEntity(MobFlyingBlockServer(terrain.world,
                    Vector3d(x.toDouble() + xx.toDouble() + 0.5,
                            y.toDouble() + yy.toDouble() + 0.5, z + 0.5),
                    Vector3d(0.0, 0.0, -1.0), this, data))
            terrain.typeData(x, y, z, terrain.air,
                    0.toShort().toInt())
        }
    }

    override fun maxStackSize(item: ItemStack): Int {
        return 16
    }
}
