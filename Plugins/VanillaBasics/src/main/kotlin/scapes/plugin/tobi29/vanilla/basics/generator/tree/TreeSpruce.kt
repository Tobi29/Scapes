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
import scapes.plugin.tobi29.vanilla.basics.material.VanillaMaterial
import java.util.*

class TreeSpruce : Tree {

    companion object {
        val INSTANCE = TreeSpruce()
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
        val size = random.nextInt(4) + 12
        var leavesSize = 2.0
        TreeUtil.makeRandomLayer(terrain, x, y, z + size, materials.leaves,
                2, 1, 1, random)
        for (zz in size - 1 downTo 0) {
            TreeUtil.changeBlock(terrain, x, y, z + zz, materials.log,
                    2.toShort())
            if (zz > 5) {
                leavesSize += 0.25
            } else {
                leavesSize--
            }
            if (leavesSize > 0) {
                if (zz % 2 == 0) {
                    TreeUtil.makeRandomLayer(terrain, x, y, z + zz,
                            materials.leaves, 2, leavesSize.toInt(), 1,
                            random)
                } else {
                    TreeUtil.makeRandomLayer(terrain, x, y, z + zz,
                            materials.leaves, 2.toShort(),
                            (leavesSize / 2.0).toInt(), 1, random)
                }
            }
        }
    }
}
