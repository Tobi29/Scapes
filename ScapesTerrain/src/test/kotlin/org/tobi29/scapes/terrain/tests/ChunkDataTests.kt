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

package org.tobi29.scapes.terrain.tests

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.tobi29.scapes.engine.test.assertions.shouldEqual
import org.tobi29.scapes.engine.utils.Random
import org.tobi29.scapes.terrain.data.ChunkArraySection1x16
import org.tobi29.scapes.terrain.data.ChunkArraySection1x8
import org.tobi29.scapes.terrain.data.ChunkArraySection2x4
import org.tobi29.scapes.terrain.data.ChunkDataStruct

object ChunkDataTests : Spek({
    describe("2 x 4-bit chunk data") {
        given("an instance filled with random data") {
            val xSectionBits = 2
            val ySectionBits = 2
            val zSectionBits = 2
            val xSizeBits = 2
            val ySizeBits = 2
            val zSizeBits = 2
            val random = Random(0)
            val struct = ChunkDataStruct(xSectionBits, ySectionBits,
                    zSectionBits, xSizeBits, ySizeBits, zSizeBits)
            val data = struct.createData(::ChunkArraySection2x4)
            val xSize = 1 shl xSizeBits + xSectionBits
            val ySize = 1 shl ySizeBits + ySectionBits
            val zSize = 1 shl zSizeBits + zSectionBits
            for (z in 0..zSize - 1) {
                for (y in 0..ySize - 1) {
                    for (x in 0..xSize - 1) {
                        val value1 = random.nextInt(0x10)
                        val value2 = random.nextInt(0x10)
                        if (random.nextBoolean()) {
                            struct.getSection(data, x, y, z) { x, y, z ->
                                setData(x, y, z, false, value1)
                                setData(x, y, z, true, value2)
                            }
                        } else {
                            struct.getSection(data, x, y, z) { x, y, z ->
                                setData(x, y, z, true, value2)
                                setData(x, y, z, false, value1)
                            }
                        }
                    }
                }
            }
            it("should contain the same values") {
                val random = Random(0)
                for (z in 0..zSize - 1) {
                    for (y in 0..ySize - 1) {
                        for (x in 0..xSize - 1) {
                            val requirement1 = random.nextInt(0x10)
                            val value1 = struct.getSection(data, x, y,
                                    z) { x, y, z -> getData(x, y, z, false) }
                            val requirement2 = random.nextInt(0x10)
                            val value2 = struct.getSection(data, x, y,
                                    z) { x, y, z -> getData(x, y, z, true) }
                            // We do not need this here, but we need to keep
                            // the seeding consistent
                            random.nextBoolean()
                            value1 shouldEqual requirement1
                            value2 shouldEqual requirement2
                        }
                    }
                }
            }
        }
    }
    describe("8-bit chunk data") {
        given("an instance filled with random data") {
            val xSectionBits = 2
            val ySectionBits = 2
            val zSectionBits = 2
            val xSizeBits = 2
            val ySizeBits = 2
            val zSizeBits = 2
            val random = Random(0)
            val struct = ChunkDataStruct(xSectionBits, ySectionBits,
                    zSectionBits, xSizeBits, ySizeBits, zSizeBits)
            val data = struct.createData(::ChunkArraySection1x8)
            val xSize = 1 shl xSizeBits + xSectionBits
            val ySize = 1 shl ySizeBits + ySectionBits
            val zSize = 1 shl zSizeBits + zSectionBits
            for (z in 0..zSize - 1) {
                for (y in 0..ySize - 1) {
                    for (x in 0..xSize - 1) {
                        val value = random.nextInt(0x100).toByte()
                        struct.getSection(data, x, y, z) { x, y, z ->
                            setData(x, y, z, value.toInt())
                        }
                    }
                }
            }
            it("should contain the same values") {
                val random = Random(0)
                for (z in 0..zSize - 1) {
                    for (y in 0..ySize - 1) {
                        for (x in 0..xSize - 1) {
                            val requirement = random.nextInt(0x100).toByte()
                            val value = struct.getSection(data, x, y,
                                    z) { x, y, z ->
                                getData(x, y, z)
                            }.toByte()
                            value shouldEqual requirement
                        }
                    }
                }
            }
        }
    }
    describe("16-bit chunk data") {
        given("an instance filled with random data") {
            val xSectionBits = 2
            val ySectionBits = 2
            val zSectionBits = 2
            val xSizeBits = 2
            val ySizeBits = 2
            val zSizeBits = 2
            val random = Random(0)
            val struct = ChunkDataStruct(xSectionBits, ySectionBits,
                    zSectionBits, xSizeBits, ySizeBits, zSizeBits)
            val data = struct.createData(::ChunkArraySection1x16)
            val xSize = 1 shl xSizeBits + xSectionBits
            val ySize = 1 shl ySizeBits + ySectionBits
            val zSize = 1 shl zSizeBits + zSectionBits
            for (z in 0..zSize - 1) {
                for (y in 0..ySize - 1) {
                    for (x in 0..xSize - 1) {
                        val value = random.nextInt(0x10000).toShort()
                        struct.getSection(data, x, y, z) { x, y, z ->
                            setData(x, y, z, value.toInt())
                        }
                    }
                }
            }
            it("should contain the same values") {
                val random = Random(0)
                for (z in 0..zSize - 1) {
                    for (y in 0..ySize - 1) {
                        for (x in 0..xSize - 1) {
                            val requirement = random.nextInt(0x10000).toShort()
                            val value = struct.getSection(data, x, y,
                                    z) { x, y, z ->
                                getData(x, y, z)
                            }.toShort()
                            value shouldEqual requirement
                        }
                    }
                }
            }
        }
    }
})
