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

package scapes.plugin.tobi29.vanilla.basics.generator.tree

import org.tobi29.scapes.chunk.terrain.TerrainServer
import org.tobi29.scapes.engine.utils.math.floor
import scapes.plugin.tobi29.vanilla.basics.material.VanillaMaterial
import java.util.*

class TreePalm : Tree {

    companion object {
        val INSTANCE = TreePalm()
    }

    override fun gen(terrain: TerrainServer.TerrainMutable,
                     x: Int,
                     y: Int,
                     z: Int,
                     materials: VanillaMaterial,
                     random: Random) {
        val groundType = terrain.type(x, y, z - 1)
        if (groundType !== materials.grass && groundType !== materials.sand) {
            return
        }
        if (terrain.type(x, y, z) !== materials.air) {
            return
        }
        val size = random.nextInt(4) + 9
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
            xxx = floor(xx + 0.5)
            yyy = floor(yy + 0.5)
            TreeUtil.changeBlock(terrain, xxx, yyy, z + i, materials.log,
                    3.toShort())
            TreeUtil.changeBlock(terrain, xxx - 1, yyy, z + i, materials.log,
                    3.toShort())
            TreeUtil.changeBlock(terrain, xxx - 1, yyy - 1, z + i,
                    materials.log, 3.toShort())
            TreeUtil.changeBlock(terrain, xxx, yyy - 1, z + i, materials.log,
                    3.toShort())
            i++
            TreeUtil.changeBlock(terrain, xxx, yyy, z + i, materials.log,
                    3.toShort())
            TreeUtil.changeBlock(terrain, xxx - 1, yyy, z + i, materials.log,
                    3.toShort())
            TreeUtil.changeBlock(terrain, xxx - 1, yyy - 1, z + i,
                    materials.log, 3.toShort())
            TreeUtil.changeBlock(terrain, xxx, yyy - 1, z + i, materials.log,
                    3.toShort())
            xx += dirX * i / size
            yy += dirY * i / size
        }
        val leavesSize = random.nextInt(3) + 7
        val leavesHeight = random.nextInt(3) + 1
        if (random.nextBoolean()) {
            TreeUtil.makePalmLeaves(terrain, xxx, yyy, z + size,
                    materials.leaves, 3.toShort(), materials.log, 3.toShort(),
                    leavesSize, leavesHeight, 1, 1)
            TreeUtil.makePalmLeaves(terrain, xxx - 1, yyy, z + size,
                    materials.leaves, 3.toShort(), materials.log, 3.toShort(),
                    leavesSize, leavesHeight, -1, 1)
            TreeUtil.makePalmLeaves(terrain, xxx - 1, yyy - 1, z + size,
                    materials.leaves, 3.toShort(), materials.log, 3.toShort(),
                    leavesSize, leavesHeight, -1, -1)
            TreeUtil.makePalmLeaves(terrain, xxx, yyy - 1, z + size,
                    materials.leaves, 3.toShort(), materials.log, 3.toShort(),
                    leavesSize, leavesHeight, 1, -1)
        } else {
            TreeUtil.makePalmLeaves(terrain, xxx, yyy, z + size,
                    materials.leaves, 3.toShort(), materials.log, 3.toShort(),
                    leavesSize, leavesHeight, 1, 0)
            TreeUtil.makePalmLeaves(terrain, xxx - 1, yyy, z + size,
                    materials.leaves, 3.toShort(), materials.log, 3.toShort(),
                    leavesSize, leavesHeight, 0, -1)
            TreeUtil.makePalmLeaves(terrain, xxx - 1, yyy - 1, z + size,
                    materials.leaves, 3.toShort(), materials.log, 3.toShort(),
                    leavesSize, leavesHeight, -1, 0)
            TreeUtil.makePalmLeaves(terrain, xxx, yyy - 1, z + size,
                    materials.leaves, 3.toShort(), materials.log, 3.toShort(),
                    leavesSize, leavesHeight, 0, 1)
        }
    }
}
