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

package org.tobi29.scapes.vanilla.basics.world.tree

import org.tobi29.scapes.chunk.terrain.TerrainServer
import org.tobi29.math.Random
import org.tobi29.stdex.math.floorToInt
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial

object TreePalm : Tree {
    override fun gen(terrain: TerrainServer,
                     x: Int,
                     y: Int,
                     z: Int,
                     materials: VanillaMaterial,
                     random: Random) {
        val groundType = terrain.type(x, y, z - 1)
        if (groundType != materials.grass && groundType != materials.sand) {
            return
        }
        if (terrain.type(x, y, z) != materials.air) {
            return
        }
        val size = random.nextInt(4) + 9
        val data = materials.plugin.treeTypes.PALM.id
        terrain.modify(x - 21, y - 21, z, 43, 43, size + 4) { terrain ->
            val dirX: Double
            val dirY: Double
            when (random.nextInt(4)) {
                1 -> {
                    dirX = -1.0
                    dirY = 1.0
                }
                2 -> {
                    dirX = 1.0
                    dirY = -1.0
                }
                3 -> {
                    dirX = -1.0
                    dirY = -1.0
                }
                else -> {
                    dirX = 1.0
                    dirY = 1.0
                }
            }
            var xx = x.toDouble()
            var yy = y.toDouble()
            var xxx = 0
            var yyy = 0
            var i = 0
            while (i < size) {
                xxx = (xx + 0.5).floorToInt()
                yyy = (yy + 0.5).floorToInt()
                TreeUtil.changeBlock(terrain, xxx, yyy, z + i,
                        materials.log, data)
                TreeUtil.changeBlock(terrain, xxx - 1, yyy, z + i,
                        materials.log, data)
                TreeUtil.changeBlock(terrain, xxx - 1, yyy - 1, z + i,
                        materials.log, data)
                TreeUtil.changeBlock(terrain, xxx, yyy - 1, z + i,
                        materials.log, data)
                i++
                TreeUtil.changeBlock(terrain, xxx, yyy, z + i,
                        materials.log, data)
                TreeUtil.changeBlock(terrain, xxx - 1, yyy, z + i,
                        materials.log, data)
                TreeUtil.changeBlock(terrain, xxx - 1, yyy - 1, z + i,
                        materials.log, data)
                TreeUtil.changeBlock(terrain, xxx, yyy - 1, z + i,
                        materials.log, data)
                xx += dirX * i / size
                yy += dirY * i / size
            }
            val leavesSize = random.nextInt(3) + 7
            val leavesHeight = random.nextInt(3) + 1
            if (random.nextBoolean()) {
                TreeUtil.makePalmLeaves(terrain, xxx, yyy, z + size,
                        materials.leaves, data, materials.log, data, leavesSize,
                        leavesHeight, 1, 1)
                TreeUtil.makePalmLeaves(terrain, xxx - 1, yyy, z + size,
                        materials.leaves, data, materials.log, data, leavesSize,
                        leavesHeight, -1, 1)
                TreeUtil.makePalmLeaves(terrain, xxx - 1, yyy - 1, z + size,
                        materials.leaves, data, materials.log, data, leavesSize,
                        leavesHeight, -1, -1)
                TreeUtil.makePalmLeaves(terrain, xxx, yyy - 1, z + size,
                        materials.leaves, data, materials.log, data, leavesSize,
                        leavesHeight, 1, -1)
            } else {
                TreeUtil.makePalmLeaves(terrain, xxx, yyy, z + size,
                        materials.leaves, data, materials.log, data, leavesSize,
                        leavesHeight, 1, 0)
                TreeUtil.makePalmLeaves(terrain, xxx - 1, yyy, z + size,
                        materials.leaves, data, materials.log, data, leavesSize,
                        leavesHeight, 0, -1)
                TreeUtil.makePalmLeaves(terrain, xxx - 1, yyy - 1, z + size,
                        materials.leaves, data, materials.log, data, leavesSize,
                        leavesHeight, -1, 0)
                TreeUtil.makePalmLeaves(terrain, xxx, yyy - 1, z + size,
                        materials.leaves, data, materials.log, data, leavesSize,
                        leavesHeight, 0, 1)
            }
        }
    }
}
