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

import java8.util.Optional;
import java8.util.stream.Collectors;
import org.tobi29.scapes.engine.utils.Streams;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;

import java.util.ArrayList;
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
        xSize = (1 << xSizeBits) - 1;
        ySize = (1 << ySizeBits) - 1;
        zSize = (1 << zSizeBits) - 1;
        data = new ChunkArraySection[1 <<
                xSectionBits + ySectionBits + zSectionBits];
        for (int i = 0; i < data.length; i++) {
            data[i] = supplier.get(xSizeBits, ySizeBits, zSizeBits);
        }
    }

    public ChunkArraySection section(int xOffset, int yOffset, int zOffset) {
        return section(
                (zOffset << ySectionBits | yOffset) << xSectionBits | xOffset);
    }

    public ChunkArraySection section(int offset) {
        if (offset < 0 || offset >= data.length) {
            throw new IllegalArgumentException(
                    "Offset out of range: " + offset);
        }
        return data[offset];
    }

    public int getData(int x, int y, int z, int offset) {
        return getData(x >> xSizeBits, y >> ySizeBits, z >> zSizeBits,
                x & xSize, y & ySize, z & zSize, offset);
    }

    private int getData(int xOffset, int yOffset, int zOffset, int x, int y,
            int z, int offset) {
        return section(xOffset, yOffset, zOffset).data(x, y, z, offset);
    }

    public void setData(int x, int y, int z, int offset, int value) {
        setData(x >> xSizeBits, y >> ySizeBits, z >> zSizeBits, x & xSize,
                y & ySize, z & zSize, offset, value);
    }

    private void setData(int xOffset, int yOffset, int zOffset, int x, int y,
            int z, int offset, int value) {
        section(xOffset, yOffset, zOffset).data(x, y, z, offset, value);
    }

    public void setDataUnsafe(int x, int y, int z, int offset, int value) {
        setDataUnsafe(x >> xSizeBits, y >> ySizeBits, z >> zSizeBits, x & xSize,
                y & ySize, z & zSize, offset, value);
    }

    private void setDataUnsafe(int xOffset, int yOffset, int zOffset, int x,
            int y, int z, int offset, int value) {
        section(xOffset, yOffset, zOffset).dataUnsafe(x, y, z, offset, value);
    }

    public List<TagStructure> save() {
        List<Optional<TagStructure>> tags =
                Streams.of(data).map(ChunkArraySection::save)
                        .collect(Collectors.toList());
        List<TagStructure> tagStructures = new ArrayList<>(data.length);
        boolean empty = true;
        for (int i = tags.size() - 1; i >= 0; i--) {
            Optional<TagStructure> tag = tags.get(i);
            if (tag.isPresent()) {
                empty = false;
                tagStructures.add(0, tag.get());
            } else if (!empty) {
                tagStructures.add(0, new TagStructure());
            }
        }
        return tagStructures;
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
                    if (!section(x, y, z).isEmpty()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public void compress() {
        Streams.forEach(data, ChunkArraySection::compress);
    }

    public interface SectionSupplier {
        ChunkArraySection get(int xSizeBits, int ySizeBits, int zSizeBits);
    }
}
