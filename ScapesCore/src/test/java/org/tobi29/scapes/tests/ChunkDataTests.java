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

package org.tobi29.scapes.tests;

import org.junit.Assert;
import org.junit.Test;
import org.tobi29.scapes.chunk.data.ChunkArraySection1x16;
import org.tobi29.scapes.chunk.data.ChunkArraySection1x8;
import org.tobi29.scapes.chunk.data.ChunkArraySection2x4;
import org.tobi29.scapes.chunk.data.ChunkData;

import java.security.SecureRandom;
import java.util.Random;

public class ChunkDataTests {
    @Test
    public void testChunkData16() {
        testChunkData16(2, 2, 2, 2, 2, 2);
    }

    private void testChunkData16(int xSectionBits, int ySectionBits,
            int zSectionBits, int xSizeBits, int ySizeBits, int zSizeBits) {
        long seed = new SecureRandom().nextLong();
        Random random = new Random(seed);
        ChunkData chunkData =
                new ChunkData(xSectionBits, ySectionBits, zSectionBits,
                        xSizeBits, ySizeBits, zSizeBits,
                        ChunkArraySection1x16::new);
        int xSize = 1 << xSizeBits + xSectionBits;
        int ySize = 1 << ySizeBits + ySectionBits;
        int zSize = 1 << zSizeBits + zSectionBits;
        for (int z = 0; z < zSize; z++) {
            for (int y = 0; y < ySize; y++) {
                for (int x = 0; x < xSize; x++) {
                    int value = random.nextInt(0x100);
                    chunkData.setData(x, y, z, 0, value);
                }
            }
        }
        random = new Random(seed);
        for (int z = 0; z < zSize; z++) {
            for (int y = 0; y < ySize; y++) {
                for (int x = 0; x < xSize; x++) {
                    int requirement = random.nextInt(0x100);
                    int value = chunkData.getData(x, y, z, 0);
                    Assert.assertEquals("Returned data not equal to stored",
                            requirement, value);
                }
            }
        }
    }

    @Test
    public void testChunkData8() {
        testChunkData8(2, 2, 2, 2, 2, 2);
    }

    private void testChunkData8(int xSectionBits, int ySectionBits,
            int zSectionBits, int xSizeBits, int ySizeBits, int zSizeBits) {
        long seed = new SecureRandom().nextLong();
        Random random = new Random(seed);
        ChunkData chunkData =
                new ChunkData(xSectionBits, ySectionBits, zSectionBits,
                        xSizeBits, ySizeBits, zSizeBits,
                        ChunkArraySection1x8::new);
        int xSize = 1 << xSizeBits + xSectionBits;
        int ySize = 1 << ySizeBits + ySectionBits;
        int zSize = 1 << zSizeBits + zSectionBits;
        for (int z = 0; z < zSize; z++) {
            for (int y = 0; y < ySize; y++) {
                for (int x = 0; x < xSize; x++) {
                    int value = random.nextInt(0x80);
                    chunkData.setData(x, y, z, 0, value);
                }
            }
        }
        random = new Random(seed);
        for (int z = 0; z < zSize; z++) {
            for (int y = 0; y < ySize; y++) {
                for (int x = 0; x < xSize; x++) {
                    int requirement = random.nextInt(0x80);
                    int value = chunkData.getData(x, y, z, 0);
                    Assert.assertEquals("Returned data not equal to stored",
                            requirement, value);
                }
            }
        }
    }

    @Test
    public void testChunkLight() {
        testChunkLight(2, 2, 2, 2, 2, 2);
    }

    private void testChunkLight(int xSectionBits, int ySectionBits,
            int zSectionBits, int xSizeBits, int ySizeBits, int zSizeBits) {
        long seed = new SecureRandom().nextLong();
        Random random = new Random(seed);
        ChunkData chunkData =
                new ChunkData(xSectionBits, ySectionBits, zSectionBits,
                        xSizeBits, ySizeBits, zSizeBits,
                        ChunkArraySection2x4::new);
        int xSize = 1 << xSizeBits + xSectionBits;
        int ySize = 1 << ySizeBits + ySectionBits;
        int zSize = 1 << zSizeBits + zSectionBits;
        for (int z = 0; z < zSize; z++) {
            for (int y = 0; y < ySize; y++) {
                for (int x = 0; x < xSize; x++) {
                    int value = random.nextInt(0x10);
                    chunkData.setData(x, y, z, 0, value);
                    value = random.nextInt(0x10);
                    chunkData.setData(x, y, z, 1, value);
                }
            }
        }
        random = new Random(seed);
        for (int z = 0; z < zSize; z++) {
            for (int y = 0; y < ySize; y++) {
                for (int x = 0; x < xSize; x++) {
                    int requirement = random.nextInt(0x10);
                    int value = chunkData.getData(x, y, z, 0);
                    Assert.assertEquals("Returned data not equal to stored",
                            requirement, value);
                    requirement = random.nextInt(0x10);
                    value = chunkData.getData(x, y, z, 1);
                    Assert.assertEquals("Returned data not equal to stored",
                            requirement, value);
                }
            }
        }
    }
}
