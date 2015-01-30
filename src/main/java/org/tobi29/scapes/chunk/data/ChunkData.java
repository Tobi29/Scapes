/*
 * Copyright 2012-2015 Tobi29
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

package org.tobi29.scapes.chunk.data;

import org.tobi29.scapes.engine.utils.io.tag.TagStructure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChunkData {
    private final int xSectionBits, ySectionBits, xSizeBits, ySizeBits,
            zSizeBits, xSize, ySize, zSize;
    private final ChunkArraySection[] data;

    public ChunkData(int xSectionBits, int ySectionBits, int zSectionBits,
            int xSizeBits, int ySizeBits, int zSizeBits,
            SectionSupplier supplier) {
        this.xSectionBits = xSectionBits;
        this.ySectionBits = ySectionBits;
        this.xSizeBits = xSizeBits;
        this.ySizeBits = ySizeBits;
        this.zSizeBits = zSizeBits;
        xSize = 1 << xSizeBits;
        ySize = 1 << ySizeBits;
        zSize = 1 << zSizeBits;
        data = new ChunkArraySection[1 <<
                xSectionBits + ySectionBits + zSectionBits];
        for (int i = 0; i < data.length; i++) {
            data[i] = supplier.get(xSizeBits, ySizeBits, zSizeBits);
        }
    }

    public ChunkArraySection getSection(int xOffset, int yOffset, int zOffset) {
        return getSection(
                (zOffset << ySectionBits | yOffset) << xSectionBits | xOffset);
    }

    public ChunkArraySection getSection(int offset) {
        if (offset < 0 || offset >= data.length) {
            return null;
        }
        return data[offset];
    }

    public int getData(int x, int y, int z, int offset) {
        return getData(x >> xSizeBits, y >> ySizeBits, z >> zSizeBits,
                x % xSize, y % ySize, z % zSize, offset);
    }

    private int getData(int xOffset, int yOffset, int zOffset, int x, int y,
            int z, int offset) {
        return getSection(xOffset, yOffset, zOffset).getData(x, y, z, offset);
    }

    public void setData(int x, int y, int z, int offset, int value) {
        setData(x >> xSizeBits, y >> ySizeBits, z >> zSizeBits, x % xSize,
                y % ySize, z % zSize, offset, value);
    }

    private void setData(int xOffset, int yOffset, int zOffset, int x, int y,
            int z, int offset, int value) {
        getSection(xOffset, yOffset, zOffset).setData(x, y, z, offset, value);
    }

    public List<TagStructure> save() {
        List<TagStructure> tags = new ArrayList<>(data.length);
        for (ChunkArraySection section : data) {
            tags.add(section.save());
        }
        boolean empty = true;
        for (int i = tags.size() - 1; i >= 0; i--) {
            if (tags.get(i) != null) {
                empty = false;
            } else if (!empty) {
                tags.set(i, new TagStructure());
            }
            if (empty) {
                tags.remove(i);
            }
        }
        return tags;
    }

    public void load(List<TagStructure> tags) {
        for (int i = 0; i < data.length; i++) {
            if (i < tags.size()) {
                data[i].load(tags.get(i));
            } else {
                data[i].load(null);
            }
        }
    }

    public boolean isEmpty(int xMin, int yMin, int zMin, int xMax, int yMax,
            int zMax) {
        xMin >>= xSizeBits;
        yMin >>= ySizeBits;
        zMin >>= zSizeBits;
        xMax >>= xSizeBits;
        yMax >>= ySizeBits;
        zMax >>= zSizeBits;
        for (int z = zMin; z <= zMax; z++) {
            for (int y = yMin; y <= yMax; y++) {
                for (int x = xMin; x <= xMax; x++) {
                    if (!getSection(x, y, z).isEmpty()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public void compress() {
        Arrays.stream(data).forEach(ChunkArraySection::compress);
    }

    @FunctionalInterface
    public interface SectionSupplier {
        ChunkArraySection get(int xSizeBits, int ySizeBits, int zSizeBits);
    }
}
