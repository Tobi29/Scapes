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
import org.tobi29.scapes.engine.utils.math.Random
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial

object TreeSpruce : Tree {
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
        val size = random.nextInt(4) + 12
        val range = 3 + ((size - 5) shr 2)
        val data = materials.plugin.treeTypes.SPRUCE.id
        terrain.modify(x - range, y - range, z, (range shl 1) + 1,
                (range shl 1) + 1, size + 1) { terrain ->
            var leavesSize = 2.0
            TreeUtil.makeRandomLayer(terrain, x, y, z + size, materials.leaves,
                    data, 1, 1, random)
            for (zz in size - 1 downTo 0) {
                TreeUtil.changeBlock(terrain, x, y, z + zz, materials.log, data)
                if (zz > 5) {
                    leavesSize += 0.25
                } else {
                    leavesSize--
                }
                if (leavesSize > 0) {
                    if (zz % 2 == 0) {
                        TreeUtil.makeRandomLayer(terrain, x, y, z + zz,
                                materials.leaves, data, leavesSize.toInt(), 1,
                                random)
                    } else {
                        TreeUtil.makeRandomLayer(terrain, x, y, z + zz,
                                materials.leaves, data,
                                (leavesSize / 2.0).toInt(), 1, random)
                    }
                }
            }
        }
    }
}
