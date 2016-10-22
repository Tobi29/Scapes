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

package org.tobi29.scapes.tests

import org.junit.Assert
import org.junit.Test
import org.tobi29.scapes.chunk.data.ChunkArraySection1x16
import org.tobi29.scapes.chunk.data.ChunkArraySection1x8
import org.tobi29.scapes.chunk.data.ChunkArraySection2x4
import org.tobi29.scapes.chunk.data.ChunkData
import java.security.SecureRandom
import java.util.*

class ChunkDataTests {
    @Test
    fun testChunkData16() {
        testChunkData16(2, 2, 2, 2, 2, 2)
    }

    private fun testChunkData16(xSectionBits: Int,
                                ySectionBits: Int,
                                zSectionBits: Int,
                                xSizeBits: Int,
                                ySizeBits: Int,
                                zSizeBits: Int) {
        val seed = SecureRandom().nextLong()
        var random = Random(seed)
        val chunkData = ChunkData(xSectionBits, ySectionBits, zSectionBits,
                xSizeBits, ySizeBits, zSizeBits, ::ChunkArraySection1x16)
        val xSize = 1 shl xSizeBits + xSectionBits
        val ySize = 1 shl ySizeBits + ySectionBits
        val zSize = 1 shl zSizeBits + zSectionBits
        for (z in 0..zSize - 1) {
            for (y in 0..ySize - 1) {
                for (x in 0..xSize - 1) {
                    val value = random.nextInt(0x100)
                    chunkData.setData(x, y, z, 0, value)
                }
            }
        }
        random = Random(seed)
        for (z in 0..zSize - 1) {
            for (y in 0..ySize - 1) {
                for (x in 0..xSize - 1) {
                    val requirement = random.nextInt(0x100)
                    val value = chunkData.getData(x, y, z, 0)
                    Assert.assertEquals("Returned data not equal to stored",
                            requirement.toLong(), value.toLong())
                }
            }
        }
    }

    @Test
    fun testChunkData8() {
        testChunkData8(2, 2, 2, 2, 2, 2)
    }

    private fun testChunkData8(xSectionBits: Int,
                               ySectionBits: Int,
                               zSectionBits: Int,
                               xSizeBits: Int,
                               ySizeBits: Int,
                               zSizeBits: Int) {
        val seed = SecureRandom().nextLong()
        var random = Random(seed)
        val chunkData = ChunkData(xSectionBits, ySectionBits, zSectionBits,
                xSizeBits, ySizeBits, zSizeBits, ::ChunkArraySection1x8)
        val xSize = 1 shl xSizeBits + xSectionBits
        val ySize = 1 shl ySizeBits + ySectionBits
        val zSize = 1 shl zSizeBits + zSectionBits
        for (z in 0..zSize - 1) {
            for (y in 0..ySize - 1) {
                for (x in 0..xSize - 1) {
                    val value = random.nextInt(0x80)
                    chunkData.setData(x, y, z, 0, value)
                }
            }
        }
        random = Random(seed)
        for (z in 0..zSize - 1) {
            for (y in 0..ySize - 1) {
                for (x in 0..xSize - 1) {
                    val requirement = random.nextInt(0x80)
                    val value = chunkData.getData(x, y, z, 0)
                    Assert.assertEquals("Returned data not equal to stored",
                            requirement.toLong(), value.toLong())
                }
            }
        }
    }

    @Test
    fun testChunkLight() {
        testChunkLight(2, 2, 2, 2, 2, 2)
    }

    private fun testChunkLight(xSectionBits: Int,
                               ySectionBits: Int,
                               zSectionBits: Int,
                               xSizeBits: Int,
                               ySizeBits: Int,
                               zSizeBits: Int) {
        val seed = SecureRandom().nextLong()
        var random = Random(seed)
        val chunkData = ChunkData(xSectionBits, ySectionBits, zSectionBits,
                xSizeBits, ySizeBits, zSizeBits, ::ChunkArraySection2x4)
        val xSize = 1 shl xSizeBits + xSectionBits
        val ySize = 1 shl ySizeBits + ySectionBits
        val zSize = 1 shl zSizeBits + zSectionBits
        for (z in 0..zSize - 1) {
            for (y in 0..ySize - 1) {
                for (x in 0..xSize - 1) {
                    var value = random.nextInt(0x10)
                    chunkData.setData(x, y, z, 0, value)
                    value = random.nextInt(0x10)
                    chunkData.setData(x, y, z, 1, value)
                }
            }
        }
        random = Random(seed)
        for (z in 0..zSize - 1) {
            for (y in 0..ySize - 1) {
                for (x in 0..xSize - 1) {
                    var requirement = random.nextInt(0x10)
                    var value = chunkData.getData(x, y, z, 0)
                    Assert.assertEquals("Returned data not equal to stored",
                            requirement.toLong(), value.toLong())
                    requirement = random.nextInt(0x10)
                    value = chunkData.getData(x, y, z, 1)
                    Assert.assertEquals("Returned data not equal to stored",
                            requirement.toLong(), value.toLong())
                }
            }
        }
    }
}
