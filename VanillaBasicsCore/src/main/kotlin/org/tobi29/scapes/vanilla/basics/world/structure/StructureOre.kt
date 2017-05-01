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
import org.tobi29.scapes.engine.utils.Random
import org.tobi29.scapes.engine.utils.math.sqr

fun TerrainServer.genOre(x: Int,
                         y: Int,
                         z: Int,
                         stone: BlockType,
                         ore: BlockType,
                         sizeX: Int,
                         sizeY: Int,
                         sizeZ: Int,
                         chance: Int,
                         random: Random): Int {
    var ores = 0
    modify(x - sizeX, y - sizeY, z - sizeZ, (sizeX shl 1) + 1,
            (sizeY shl 1) + 1, (sizeZ shl 1) + 1) {
        for (xx in -sizeX..sizeX) {
            val xxx = x + xx
            for (yy in -sizeY..sizeY) {
                val yyy = y + yy
                for (zz in -sizeZ..sizeZ) {
                    val zzz = z + zz
                    if (sqr(xx.toDouble() / sizeX) +
                            sqr(yy.toDouble() / sizeY) +
                            sqr(zz.toDouble() / sizeZ) < random.nextDouble() * 0.1 + 0.9) {
                        if (random.nextInt(chance) == 0) {
                            if (type(xxx, yyy, zzz) == stone) {
                                it.type(xxx, yyy, zzz, ore)
                                ores++
                            }
                        }
                    }
                }
            }
        }
    }
    return ores
}
