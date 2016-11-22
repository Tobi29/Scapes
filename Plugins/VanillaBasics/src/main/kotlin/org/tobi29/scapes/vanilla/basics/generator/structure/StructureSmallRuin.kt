/*
 * Copyright 2012-2016 Tobi29
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

package org.tobi29.scapes.vanilla.basics.generator.structure

import org.tobi29.scapes.block.BlockType
import org.tobi29.scapes.chunk.terrain.TerrainServer
import org.tobi29.scapes.engine.utils.math.*
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial
import java.util.*

fun TerrainServer.TerrainMutable.placeRandomRuin(x: Int,
                                                 y: Int,
                                                 z: Int,
                                                 materials: VanillaMaterial,
                                                 stoneType: Int,
                                                 random: Random) {
    if (type(x, y, z) != materials.air && random.nextInt(10) != 0) {
        return
    }
    val type = random.nextInt(2)
    when (type) {
        0 -> placeRuinType1(x, y, arrayOf(materials.cobblestone,
                materials.cobblestoneCracked, materials.cobblestoneMossy),
                materials.stoneTotem, stoneType, random)
        1 -> placeRuinType2(x, y, z, arrayOf(materials.cobblestone,
                materials.cobblestoneCracked, materials.cobblestoneMossy),
                materials.wood, stoneType, random)
    }
}

fun TerrainServer.TerrainMutable.placeRuinType1(x: Int,
                                                y: Int,
                                                pillar: Array<BlockType>,
                                                top: BlockType,
                                                stoneType: Int,
                                                random: Random) {
    val size = random.nextDouble() * 10 + 6
    val pillars = random.nextInt(12) + 6
    var xx: Int
    var yy: Int
    var d: Double
    for (i in 0..pillars - 1) {
        d = i.toDouble() / pillars * TWO_PI
        xx = x + floor(cosTable(d) * size)
        yy = y + floor(sinTable(d) * size)
        placePillar(xx, yy, highestTerrainBlockZAt(xx, yy),
                arrayOf(pillar[random.nextInt(pillar.size)],
                        pillar[random.nextInt(pillar.size)],
                        pillar[random.nextInt(pillar.size)], top),
                stoneType, stoneType, stoneType, stoneType)
    }
}

fun TerrainServer.TerrainMutable.placeRuinType2(x: Int,
                                                y: Int,
                                                z: Int,
                                                walls: Array<BlockType>,
                                                floor: BlockType,
                                                stoneType: Int,
                                                random: Random) {
    val sizeX = random.nextInt(6) + 4
    val sizeY = random.nextInt(6) + 4
    val minHeight = random.nextInt(12)
    val maxHeight = random.nextInt(4) + 3
    var xxx: Int
    var yyy: Int
    val woodType = random.nextInt(2).toShort()
    for (xx in -sizeX..sizeX) {
        xxx = x + xx
        for (yy in -sizeY..sizeY) {
            yyy = y + yy
            if (abs(xx) == sizeX || abs(yy) == sizeY) {
                placePillar(xxx, yyy, z,
                        walls[random.nextInt(walls.size)], stoneType,
                        random.nextInt(maxHeight) + minHeight)
                fillGround(xxx, yyy, z - 1,
                        walls[random.nextInt(walls.size)], stoneType,
                        random.nextInt(9) + 1)
            } else {
                typeData(xxx, yyy, z - 1, floor, woodType.toInt())
                fillGround(xxx, yyy, z - 2,
                        walls[random.nextInt(walls.size)], stoneType,
                        random.nextInt(9))
            }
        }
    }
}

private fun TerrainServer.TerrainMutable.placePillar(x: Int,
                                                     y: Int,
                                                     z: Int,
                                                     type: Array<BlockType>,
                                                     vararg data: Int) {
    for (i in type.indices) {
        typeData(x, y, z + i, type[i], data[i])
    }
}

private fun TerrainServer.TerrainMutable.placePillar(x: Int,
                                                     y: Int,
                                                     z: Int,
                                                     type: BlockType,
                                                     data: Int,
                                                     height: Int) {
    for (i in 0..height - 1) {
        typeData(x, y, z + i, type, data)
    }
}

private fun TerrainServer.TerrainMutable.fillGround(x: Int,
                                                    y: Int,
                                                    z: Int,
                                                    type: BlockType,
                                                    data: Int,
                                                    maxDepth: Int) {
    for (i in 0..maxDepth - 1) {
        if (type(x, y, z - i).isReplaceable(this, x, y, z - i)) {
            typeData(x, y, z - i, type, data)
        } else {
            return
        }
    }
}
