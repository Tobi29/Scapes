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
import org.tobi29.math.vector.Vector3i
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial

object TreeWillow : Tree {
    override fun gen(terrain: TerrainServer,
                     x: Int,
                     y: Int,
                     z: Int,
                     materials: VanillaMaterial,
                     random: Random) {
        if (terrain.type(x, y, z - 1) != materials.grass) {
            return
        }
        if (terrain.type(x, y, z) != materials.air) {
            return
        }
        val size = random.nextInt(2) + 4
        val data = materials.plugin.treeTypes.WILLOW.id
        terrain.modify(x - 11, y - 11, z - 9, 23, 23, size + 16) { terrain ->
            TreeUtil.fillGround(terrain, x - 1, y - 1, z - 2, materials.log,
                    data, 2 + random.nextInt(5))
            TreeUtil.fillGround(terrain, x - 1, y, z - 1, materials.log,
                    data, 2 + random.nextInt(5))
            TreeUtil.fillGround(terrain, x - 1, y + 1, z - 2, materials.log,
                    data, 2 + random.nextInt(5))
            TreeUtil.fillGround(terrain, x, y - 1, z - 1, materials.log,
                    data, 2 + random.nextInt(5))
            TreeUtil.fillGround(terrain, x, y, z - 1, materials.log,
                    data, 2 + random.nextInt(5))
            TreeUtil.fillGround(terrain, x, y + 1, z - 1, materials.log,
                    data, 2 + random.nextInt(5))
            TreeUtil.fillGround(terrain, x + 1, y - 1, z - 2, materials.log,
                    data, 2 + random.nextInt(5))
            TreeUtil.fillGround(terrain, x + 1, y, z - 1, materials.log,
                    data, 2 + random.nextInt(5))
            TreeUtil.fillGround(terrain, x + 1, y + 1, z - 2, materials.log,
                    data, 2 + random.nextInt(5))
            TreeUtil.fillGround(terrain, x - 2, y, z - 3, materials.log,
                    data, 2 + random.nextInt(5))
            TreeUtil.fillGround(terrain, x + 2, y, z - 3, materials.log,
                    data, 2 + random.nextInt(5))
            TreeUtil.fillGround(terrain, x, y - 2, z - 3, materials.log,
                    data, 2 + random.nextInt(5))
            TreeUtil.fillGround(terrain, x, y + 2, z - 3, materials.log,
                    data, 2 + random.nextInt(5))
            for (zz in 0 until size + 2) {
                TreeUtil.makeLayer(terrain, x, y, z + zz, materials.log, data,
                        1)
            }
            val branches = ArrayList<Vector3i>()
            for (i in 0 until random.nextInt(4) + 4) {
                branches.add(Vector3i(random.nextInt(9) - 4 + x,
                        random.nextInt(9) - 4 + y,
                        random.nextInt(2) + z + size))
            }
            branches.add(Vector3i(x, y, z + size))
            val begin = Vector3i(x, y, z + size)
            for (branch in branches) {
                TreeUtil.makeBranch(terrain, begin, branch, materials.log, data)
                TreeUtil.makeWillowLeaves(terrain, branch.x, branch.y, branch.z,
                        materials.leaves, data, 6, 3, 4, 10, random)
            }
        }
    }
}
