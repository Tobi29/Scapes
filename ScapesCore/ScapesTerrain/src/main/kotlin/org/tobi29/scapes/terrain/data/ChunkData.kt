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

import org.tobi29.scapes.engine.utils.tag.ReadWriteTagList
import org.tobi29.scapes.engine.utils.tag.Tag
import org.tobi29.scapes.engine.utils.tag.TagMap
import org.tobi29.scapes.engine.utils.tag.toMap

class ChunkData(private val xSectionBits: Int,
                private val ySectionBits: Int,
                private val zSectionBits: Int,
                private val xSizeBits: Int,
                private val ySizeBits: Int,
                private val zSizeBits: Int,
                private val data: Array<ChunkArraySection>) {
    private val xSize: Int
    private val ySize: Int
    private val zSize: Int

    init {
        xSize = (1 shl xSizeBits) - 1
        ySize = (1 shl ySizeBits) - 1
        zSize = (1 shl zSizeBits) - 1
    }

    fun section(xOffset: Int,
                yOffset: Int,
                zOffset: Int): ChunkArraySection {
        return section(
                zOffset shl ySectionBits or yOffset shl xSectionBits or xOffset)
    }

    fun section(offset: Int): ChunkArraySection {
        if (offset < 0 || offset >= data.size) {
            throw IllegalArgumentException("Offset out of range: $offset")
        }
        return data[offset]
    }

    fun getData(x: Int,
                y: Int,
                z: Int,
                offset: Int): Int {
        return getData(x shr xSizeBits, y shr ySizeBits, z shr zSizeBits,
                x and xSize, y and ySize, z and zSize, offset)
    }

    private fun getData(xOffset: Int,
                        yOffset: Int,
                        zOffset: Int,
                        x: Int,
                        y: Int,
                        z: Int,
                        offset: Int): Int {
        return section(xOffset, yOffset, zOffset).getData(x, y, z, offset)
    }

    fun setData(x: Int,
                y: Int,
                z: Int,
                offset: Int,
                value: Int) {
        setData(x shr xSizeBits, y shr ySizeBits, z shr zSizeBits, x and xSize,
                y and ySize, z and zSize, offset, value)
    }

    private fun setData(xOffset: Int,
                        yOffset: Int,
                        zOffset: Int,
                        x: Int,
                        y: Int,
                        z: Int,
                        offset: Int,
                        value: Int) {
        section(xOffset, yOffset, zOffset).setData(x, y, z, offset, value)
    }

    fun write(list: ReadWriteTagList) {
        data.asSequence().map { TagMap { it.write(this) } }.forEach {
            list.add(it)
        }
    }

    fun read(tags: List<Tag>) {
        val iterator = tags.asSequence().mapNotNull(Tag::toMap).iterator()
        for (i in data.indices) {
            if (iterator.hasNext()) {
                data[i].read(iterator.next())
            } else {
                data[i].read(null)
            }
        }
    }

    fun isEmpty(xMin: Int,
                yMin: Int,
                zMin: Int,
                xMax: Int,
                yMax: Int,
                zMax: Int): Boolean {
        val xMinS = xMin shr xSizeBits
        val yMinS = yMin shr ySizeBits
        val zMinS = zMin shr zSizeBits
        val xMaxS = xMax shr xSizeBits
        val yMaxS = yMax shr ySizeBits
        val zMaxS = zMax shr zSizeBits
        for (z in zMinS..zMaxS) {
            for (y in yMinS..yMaxS) {
                for (x in xMinS..xMaxS) {
                    if (!section(x, y, z).isEmpty) {
                        return false
                    }
                }
            }
        }
        return true
    }

    fun compress() {
        data.forEach { it.compress() }
    }
}

inline fun ChunkData(xSectionBits: Int,
                     ySectionBits: Int,
                     zSectionBits: Int,
                     xSizeBits: Int,
                     ySizeBits: Int,
                     zSizeBits: Int,
                     supplier: (Int, Int, Int) -> ChunkArraySection): ChunkData {
    val array = Array(1 shl xSectionBits + ySectionBits + zSectionBits) {
        supplier(xSizeBits, ySizeBits, zSizeBits)
    }
    return ChunkData(xSectionBits, ySectionBits, zSectionBits, xSizeBits,
            ySizeBits, zSizeBits, array)
}
