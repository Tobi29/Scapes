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
import org.tobi29.scapes.engine.utils.math.*
import org.tobi29.scapes.engine.utils.math.vector.Vector3i
import scapes.plugin.tobi29.vanilla.basics.material.VanillaMaterial
import java.util.*

class TreeSequoia : Tree {

    companion object {
        val INSTANCE = TreeSequoia()
    }

    override fun gen(terrain: TerrainServer.TerrainMutable,
                     x: Int,
                     y: Int,
                     z: Int,
                     materials: VanillaMaterial,
                     random: Random) {
        if (terrain.type(x, y, z - 1) !== materials.grass) {
            return
        }
        if (terrain.type(x, y, z) !== materials.air) {
            return
        }
        var size = random.nextInt(22) + 12
        if (random.nextInt(4) == 0) {
            size += 30
        }
        TreeUtil.fillGround(terrain, x - 1, y - 1, z - 1, materials.log,
                5.toShort(), 2 + random.nextInt(5))
        TreeUtil.fillGround(terrain, x - 1, y, z - 1, materials.log,
                5.toShort(),
                2 + random.nextInt(5))
        TreeUtil.fillGround(terrain, x - 1, y + 1, z - 1, materials.log,
                5.toShort(), 2 + random.nextInt(5))
        TreeUtil.fillGround(terrain, x, y - 1, z - 1, materials.log,
                5.toShort(),
                2 + random.nextInt(5))
        TreeUtil.fillGround(terrain, x, y, z - 1, materials.log, 5.toShort(),
                2 + random.nextInt(5))
        TreeUtil.fillGround(terrain, x, y + 1, z - 1, materials.log,
                5.toShort(),
                2 + random.nextInt(5))
        TreeUtil.fillGround(terrain, x + 1, y - 1, z - 1, materials.log,
                5.toShort(), 2 + random.nextInt(5))
        TreeUtil.fillGround(terrain, x + 1, y, z - 1, materials.log,
                5.toShort(),
                2 + random.nextInt(5))
        TreeUtil.fillGround(terrain, x + 1, y + 1, z - 1, materials.log,
                5.toShort(), 2 + random.nextInt(5))
        TreeUtil.fillGround(terrain, x - 2, y, z - 1, materials.log,
                5.toShort(),
                2 + random.nextInt(5))
        TreeUtil.fillGround(terrain, x + 2, y, z - 1, materials.log,
                5.toShort(),
                2 + random.nextInt(5))
        TreeUtil.fillGround(terrain, x, y - 2, z - 1, materials.log,
                5.toShort(),
                2 + random.nextInt(5))
        TreeUtil.fillGround(terrain, x, y + 2, z - 1, materials.log,
                5.toShort(),
                2 + random.nextInt(5))
        var leavesSize = 4.0f
        val branches = ArrayList<Pair<Vector3i, Vector3i>>()
        for (zz in size - 1 downTo 0) {
            TreeUtil.makeLayer(terrain, x, y, z + zz, materials.log,
                    5.toShort(),
                    1)
            if (zz > 10) {
                leavesSize += 0.25f
            } else {
                leavesSize /= 2.0f
            }
            if (leavesSize > 1) {
                val branchCount = leavesSize.toInt() / 3
                for (i in -1..branchCount - 1) {
                    val dir = random.nextDouble() * TWO_PI
                    val distance = (1.0 - sqr(
                            1.0 - random.nextDouble())) * leavesSize
                    val xx = floor(cosTable(dir) * distance)
                    val yy = floor(sinTable(dir) * distance)
                    branches.add(Pair(Vector3i(x, y, zz + z),
                            Vector3i(x + xx, y + yy,
                                    random.nextInt(6) - 2 + zz + z)))
                }
            }
        }
        branches.add(Pair(Vector3i(x, y, z + size),
                Vector3i(x, y, z + size + 2)))
        val dir = random.nextDouble() * TWO_PI
        val xx = floor(cosTable(dir) * 2.0f)
        val yy = floor(sinTable(dir) * 2.0f)
        branches.add(Pair(Vector3i(x, y, z + size),
                Vector3i(x + xx, y + yy, z + size + 1)))
        for (branch in branches) {
            TreeUtil.makeBranch(terrain, branch.first, branch.second,
                    materials.log,
                    5.toShort())
            TreeUtil.makeLeaves(terrain, branch.second.x,
                    branch.second.y,
                    branch.second.z, materials.leaves, 5.toShort(),
                    random.nextInt(3) + 4)
        }
    }
}
