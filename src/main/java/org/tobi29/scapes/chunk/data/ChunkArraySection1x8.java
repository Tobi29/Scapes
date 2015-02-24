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

import java.util.Optional;

public class ChunkArraySection1x8 implements ChunkArraySection {
    private final int xSizeBits, ySizeBits, size;
    private byte[] data;
    private short defaultValue;
    private boolean changed;

    public ChunkArraySection1x8(int xSizeBits, int ySizeBits, int zSizeBits) {
        this.xSizeBits = xSizeBits;
        this.ySizeBits = ySizeBits;
        size = 1 << xSizeBits + ySizeBits + zSizeBits;
    }

    @Override
    public int getData(int x, int y, int z, int offset) {
        return getData(((z << ySizeBits | y) << xSizeBits | x) + offset);
    }

    @Override
    public int getData(int offset) {
        byte[] data = this.data;
        if (data == null) {
            return defaultValue;
        }
        return data[offset];
    }

    @Override
    public void setData(int x, int y, int z, int offset, int value) {
        setData(((z << ySizeBits | y) << xSizeBits | x) + offset, value);
    }

    @Override
    public synchronized void setData(int offset, int value) {
        byte[] data = this.data;
        if (data == null) {
            if (value == defaultValue) {
                return;
            }
            byte[] newData = new byte[size];
            for (int i = 0; i < size; i++) {
                newData[i] = (byte) defaultValue;
            }
            data = newData;
            data[offset] = (byte) value;
            this.data = data;
        } else {
            data[offset] = (byte) value;
            changed = true;
        }
    }

    @Override
    public boolean isEmpty() {
        return data == null && defaultValue == 0;
    }

    @Override
    public synchronized boolean compress() {
        if (data == null || !changed) {
            return data == null;
        }
        boolean flag = true;
        for (int i = 1; i < data.length && flag; i++) {
            if (data[i] != data[0]) {
                flag = false;
            }
        }
        if (flag) {
            defaultValue = data[0];
            data = null;
            changed = false;
            return true;
        }
        changed = false;
        return false;
    }

    @Override
    public synchronized Optional<TagStructure> save() {
        TagStructure tag = new TagStructure();
        if (data == null) {
            if (defaultValue == 0) {
                return Optional.empty();
            } else {
                tag.setShort("Default", defaultValue);
            }
        } else {
            tag.setByteArray("Array", data);
        }
        return Optional.of(tag);
    }

    @Override
    public synchronized void load(TagStructure tag) {
        if (tag == null) {
            defaultValue = 0;
            data = null;
        } else {
            if (tag.has("Array")) {
                data = tag.getByteArray("Array");
                defaultValue = 1;
            } else if (tag.has("Default")) {
                defaultValue = tag.getShort("Default");
                data = null;
            }
        }
    }
}
