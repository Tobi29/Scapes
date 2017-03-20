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

package org.tobi29.scapes.terrain.data

@Suppress("NOTHING_TO_INLINE")
class ChunkDataStruct(val xSectionBits: Int,
                      val ySectionBits: Int,
                      val zSectionBits: Int,
                      val xSizeBits: Int,
                      val ySizeBits: Int,
                      val zSizeBits: Int) {
    val xSize = (1 shl xSizeBits) - 1
    val ySize = (1 shl ySizeBits) - 1
    val zSize = (1 shl zSizeBits) - 1

    inline fun <D, R> getSection(data: Array<out D>,
                                 x: Int,
                                 y: Int,
                                 z: Int,
                                 block: D.(Int, Int, Int) -> R): R {
        return getSection(data, x, y, z).let {
            block(it, x and xSize, y and ySize, z and zSize)
        }
    }

    inline fun <D> getSection(data: Array<out D>,
                              x: Int,
                              y: Int,
                              z: Int): D {
        return section(data, x shr xSizeBits, y shr ySizeBits, z shr zSizeBits)
    }

    inline fun <D> section(data: Array<out D>,
                           xOffset: Int,
                           yOffset: Int,
                           zOffset: Int): D {
        return section(data,
                zOffset shl ySectionBits or yOffset shl xSectionBits or xOffset)
    }

    inline fun <D> section(data: Array<out D>,
                           offset: Int): D {
        return data[offset]
    }

    inline fun <D> forIn(data: Array<D>,
                         xMin: Int,
                         yMin: Int,
                         zMin: Int,
                         xMax: Int,
                         yMax: Int,
                         zMax: Int,
                         block: (D) -> Unit) {
        val xMinS = xMin shr xSizeBits
        val yMinS = yMin shr ySizeBits
        val zMinS = zMin shr zSizeBits
        val xMaxS = xMax shr xSizeBits
        val yMaxS = yMax shr ySizeBits
        val zMaxS = zMax shr zSizeBits
        for (z in zMinS..zMaxS) {
            for (y in yMinS..yMaxS) {
                for (x in xMinS..xMaxS) {
                    block(section(data, x, y, z))
                }
            }
        }
    }

    inline fun <reified D> createData(supplier: (Int, Int, Int) -> D) =
            Array(1 shl xSectionBits + ySectionBits + zSectionBits) {
                supplier(xSizeBits, ySizeBits, zSizeBits)
            }
}
