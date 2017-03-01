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
import org.tobi29.scapes.chunk.terrain.TerrainServer
import org.tobi29.scapes.engine.utils.math.PI
import org.tobi29.scapes.engine.utils.math.round
import org.tobi29.scapes.engine.utils.math.sinTable
import org.tobi29.scapes.engine.utils.math.vector.*
import java.util.*

object TreeUtil {
    fun makeBranch(terrain: TerrainServer.TerrainMutable,
                   start: Vector3i,
                   end: Vector3i,
                   type: BlockType,
                   data: Short) {
        val distance = start distance end
        if (distance > 0.0) {
            val delta = Vector3d(end - start)
            val step = 1.0 / distance
            var i = 0.0
            while (i <= 1) {
                val block = start + Vector3i(delta.times(i))
                changeBlock(terrain, block.x, block.y, block.z,
                        type, data)
                i += step
            }
        }
    }

    fun changeBlock(terrain: TerrainServer.TerrainMutable,
                    x: Int,
                    y: Int,
                    z: Int,
                    type: BlockType,
                    data: Short) {
        if (terrain.type(x, y, z).isReplaceable(terrain, x, y,
                z) || terrain.type(x, y, z).isTransparent(terrain, x, y, z)) {
            terrain.typeData(x, y, z, type, data.toInt())
        }
    }

    fun makeLeaves(terrain: TerrainServer.TerrainMutable,
                   x: Int,
                   y: Int,
                   z: Int,
                   type: BlockType,
                   data: Short,
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

    fun makeWillowLeaves(terrain: TerrainServer.TerrainMutable,
                         x: Int,
                         y: Int,
                         z: Int,
                         type: BlockType,
                         data: Short,
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

    fun makeLayer(terrain: TerrainServer.TerrainMutable,
                  x: Int,
                  y: Int,
                  z: Int,
                  type: BlockType,
                  data: Short,
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

    fun makeRandomLayer(terrain: TerrainServer.TerrainMutable,
                        x: Int,
                        y: Int,
                        z: Int,
                        type: BlockType,
                        data: Short,
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

    fun makePalmLeaves(terrain: TerrainServer.TerrainMutable,
                       x: Int,
                       y: Int,
                       z: Int,
                       type: BlockType,
                       data: Short,
                       logType: BlockType,
                       logData: Short,
                       length: Int,
                       height: Int,
                       dirX: Int,
                       dirY: Int) {
        var xx = x
        var yy = y
        for (i in 0..length - 1) {
            val h = round(sinTable(i / length * PI) * height)
            changeBlock(terrain, xx, yy, z + h, logType, logData)
            xx += dirX
            yy += dirY
        }
        xx = x
        yy = y
        for (i in 0..length - 1) {
            val h = round(sinTable(i / length * PI) * height)
            changeBlock(terrain, xx, yy, z + h + 1, type, data)
            changeBlock(terrain, xx - 1, yy, z + h, type, data)
            changeBlock(terrain, xx + 1, yy, z + h, type, data)
            changeBlock(terrain, xx, yy - 1, z + h, type, data)
            changeBlock(terrain, xx, yy + 1, z + h, type, data)
            xx += dirX
            yy += dirY
        }
    }

    fun fillGround(terrain: TerrainServer.TerrainMutable,
                   x: Int,
                   y: Int,
                   z: Int,
                   type: BlockType,
                   data: Short,
                   maxDepth: Int) {
        for (i in 0..maxDepth - 1) {
            if (terrain.type(x, y, z - i).isReplaceable(terrain, x, y, z - i)) {
                terrain.typeData(x, y, z - i, type, data.toInt())
            } else {
                return
            }
        }
    }
}
