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

import org.tobi29.scapes.block.BlockType
import org.tobi29.scapes.chunk.terrain.TerrainMutable
import org.tobi29.scapes.chunk.terrain.block
import org.tobi29.math.Random
import org.tobi29.math.sinTable
import org.tobi29.math.vector.*
import kotlin.math.PI
import kotlin.math.roundToInt

object TreeUtil {
    fun makeBranch(terrain: TerrainMutable,
                   start: Vector3i,
                   end: Vector3i,
                   type: BlockType,
                   data: Int) {
        val distance = start distance end
        if (distance > 0.0) {
            val delta = Vector3d(end - start)
            val step = 1.0 / distance
            var i = 0.0
            while (i <= 1) {
                val block = start + delta.times(i).roundToInt()
                changeBlock(terrain, block.x, block.y, block.z, type, data)
                i += step
            }
        }
    }

    fun changeBlock(terrain: TerrainMutable,
                    x: Int,
                    y: Int,
                    z: Int,
                    type: BlockType,
                    data: Int) {
        if (terrain.block(x, y, z) {
            isReplaceable(terrain, x, y, z) || isTransparent(it)
        }) {
            terrain.typeData(x, y, z, type, data)
        }
    }

    fun makeLeaves(terrain: TerrainMutable,
                   x: Int,
                   y: Int,
                   z: Int,
                   type: BlockType,
                   data: Int,
                   size: Int) {
        for (xx in -size..size) {
            for (yy in -size..size) {
                for (zz in -size..size) {
                    if (xx * xx + yy * yy + zz * zz <= size) {
                        changeBlock(terrain, x + xx, y + yy, z + zz, type,
                                data)
                    }
                }
            }
        }
    }

    fun makeWillowLeaves(terrain: TerrainMutable,
                         x: Int,
                         y: Int,
                         z: Int,
                         type: BlockType,
                         data: Int,
                         size: Int,
                         vineLength: Int,
                         vineLengthRandom: Int,
                         vineChance: Int,
                         random: Random) {
        for (xx in -size..size) {
            for (yy in -size..size) {
                for (zz in -size..size) {
                    if (xx * xx + yy * yy + zz * zz <= size) {
                        changeBlock(terrain, x + xx, y + yy, z + zz, type,
                                data)
                    }
                }
                if (random.nextInt(vineChance) == 0) {
                    if (xx * xx + yy * yy <= size) {
                        val length = vineLength + random.nextInt(
                                vineLengthRandom)
                        for (zz in 0..length) {
                            changeBlock(terrain, x + xx, y + yy, z - zz, type,
                                    data)
                        }
                    }
                }
            }
        }
    }

    fun makeLayer(terrain: TerrainMutable,
                  x: Int,
                  y: Int,
                  z: Int,
                  type: BlockType,
                  data: Int,
                  size: Int) {
        val sizeSqr = size * size
        for (yy in -size..size) {
            val yyy = y + yy
            for (xx in -size..size) {
                val xxx = x + xx
                if (xx * xx + yy * yy <= sizeSqr) {
                    changeBlock(terrain, xxx, yyy, z, type, data)
                }
            }
        }
    }

    fun makeRandomLayer(terrain: TerrainMutable,
                        x: Int,
                        y: Int,
                        z: Int,
                        type: BlockType,
                        data: Int,
                        size: Int,
                        sizeRandom: Int,
                        random: Random) {
        val sizeSqr = size * size
        val randomSize = sizeRandom + size
        for (yy in -size..size) {
            val yyy = y + yy
            for (xx in -size..size) {
                val xxx = x + xx
                if (xx * xx + yy * yy <= sizeSqr - random.nextInt(randomSize)) {
                    changeBlock(terrain, xxx, yyy, z, type, data)
                }
            }
        }
    }

    fun makePalmLeaves(terrain: TerrainMutable,
                       x: Int,
                       y: Int,
                       z: Int,
                       type: BlockType,
                       data: Int,
                       logType: BlockType,
                       logData: Int,
                       length: Int,
                       height: Int,
                       dirX: Int,
                       dirY: Int) {
        var xx = x
        var yy = y
        for (i in 0 until length) {
            val h = (sinTable(i / length * PI) * height).roundToInt()
            changeBlock(terrain, xx, yy, z + h, logType, logData)
            xx += dirX
            yy += dirY
        }
        xx = x
        yy = y
        for (i in 0 until length) {
            val h = (sinTable(i / length * PI) * height).roundToInt()
            changeBlock(terrain, xx, yy, z + h + 1, type, data)
            changeBlock(terrain, xx - 1, yy, z + h, type, data)
            changeBlock(terrain, xx + 1, yy, z + h, type, data)
            changeBlock(terrain, xx, yy - 1, z + h, type, data)
            changeBlock(terrain, xx, yy + 1, z + h, type, data)
            xx += dirX
            yy += dirY
        }
    }

    fun fillGround(terrain: TerrainMutable,
                   x: Int,
                   y: Int,
                   z: Int,
                   type: BlockType,
                   data: Int,
                   maxDepth: Int) {
        for (i in 0 until maxDepth) {
            if (terrain.type(x, y, z - i).isReplaceable(terrain, x, y, z - i)) {
                terrain.typeData(x, y, z - i, type, data)
            } else {
                return
            }
        }
    }
}
