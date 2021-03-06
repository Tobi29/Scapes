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

package org.tobi29.scapes.vanilla.basics.world.structure

import org.tobi29.scapes.block.BlockType
import org.tobi29.scapes.chunk.terrain.TerrainServer
import org.tobi29.math.Random
import org.tobi29.stdex.math.ceilToInt

fun TerrainServer.genOreRock(x: Int,
                             y: Int,
                             z: Int,
                             stone: BlockType,
                             ore: BlockType,
                             data: Int,
                             oreChance: Int,
                             size: Double,
                             random: Random) {
    val ceilSize = size.ceilToInt()
    modify(x - ceilSize, y - ceilSize, z - ceilSize, (ceilSize shl 1) + 1,
            (ceilSize shl 1) + 1, (ceilSize shl 1) + 1) { terrain ->
        for (xx in -ceilSize..ceilSize) {
            for (yy in -ceilSize..ceilSize) {
                for (zz in -ceilSize..ceilSize) {
                    if (xx * xx + yy * yy + zz * zz <= size * size - random.nextDouble() * 3) {
                        val type: BlockType
                        if (random.nextInt(oreChance) == 0) {
                            type = ore
                        } else {
                            type = stone
                        }
                        terrain.typeData(x + xx, y + yy, z + zz, type, data)
                    }
                }
            }
        }
    }
}
