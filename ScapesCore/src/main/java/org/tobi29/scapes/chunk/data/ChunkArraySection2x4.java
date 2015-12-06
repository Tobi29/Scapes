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
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;

import java.util.Arrays;

public class ChunkArraySection2x4 implements ChunkArraySection {
    private final int xSizeBits, ySizeBits, size;
    private byte[] data;
    private byte defaultValue;
    private boolean changed;

    public ChunkArraySection2x4(int xSizeBits, int ySizeBits, int zSizeBits) {
        this.xSizeBits = xSizeBits;
        this.ySizeBits = ySizeBits;
        size = 1 << xSizeBits + ySizeBits + zSizeBits;
    }

    @Override
    public int data(int x, int y, int z, int offset) {
        return data((((z << ySizeBits | y) << xSizeBits | x) << 1) + offset);
    }

    @Override
    public int data(int offset) {
        byte[] data = this.data;
        if ((offset & 1) == 0) {
            if (data == null) {
                return (byte) (defaultValue & 0xF);
            }
            return (byte) (data[offset >> 1] & 0xF);
        } else {
            if (data == null) {
                return (byte) ((defaultValue & 0xF0) >>> 4);
            }
            return (byte) ((data[offset >> 1] & 0xF0) >>> 4);
        }
    }

    @Override
    public void data(int x, int y, int z, int offset, int value) {
        data((((z << ySizeBits | y) << xSizeBits | x) << 1) + offset, value);
    }

    @Override
    public synchronized void data(int offset, int value) {
        byte[] data = this.data;
        if ((offset & 1) == 0) {
            offset >>= 1;
            if (data == null) {
                if (value == (defaultValue & 0xF)) {
                    return;
                }
                data = new byte[size];
                Arrays.fill(data, defaultValue);
                data[offset] = (byte) (data[offset] & 0xF0 | value);
                this.data = data;
                changed = true;
            } else {
                data[offset] = (byte) (data[offset] & 0xF0 | value);
                changed = true;
            }
        } else {
            offset >>= 1;
            value <<= 4;
            if (data == null) {
                if (value == (defaultValue & 0xF0)) {
                    return;
                }
                data = new byte[size];
                Arrays.fill(data, defaultValue);
                data[offset] = (byte) (data[offset] & 0xF | value);
                this.data = data;
                changed = true;
            } else {
                data[offset] = (byte) (data[offset] & 0xF | value);
                changed = true;
            }
        }
    }

    @Override
    public void dataUnsafe(int x, int y, int z, int offset, int value) {
        dataUnsafe((((z << ySizeBits | y) << xSizeBits | x) << 1) + offset, value);
    }

    @Override
    public void dataUnsafe(int offset, int value) {
        byte[] data = this.data;
        if ((offset & 1) == 0) {
            offset >>= 1;
            if (data == null) {
                if (value == (defaultValue & 0xF)) {
                    return;
                }
                data = new byte[size];
                Arrays.fill(data, defaultValue);
                data[offset] = (byte) (data[offset] & 0xF0 | value);
                this.data = data;
                changed = true;
            } else {
                data[offset] = (byte) (data[offset] & 0xF0 | value);
                changed = true;
            }
        } else {
            offset >>= 1;
            value <<= 4;
            if (data == null) {
                if (value == (defaultValue & 0xF0)) {
                    return;
                }
                data = new byte[size];
                Arrays.fill(data, defaultValue);
                data[offset] = (byte) (data[offset] & 0xF | value);
                this.data = data;
                changed = true;
            } else {
                data[offset] = (byte) (data[offset] & 0xF | value);
                changed = true;
            }
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
        for (int i = 1; i < data.length; i++) {
            if (data[i] != data[0]) {
                flag = false;
                break;
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
            if (defaultValue == 0xF) {
                return Optional.empty();
            } else {
                tag.setByte("Default", defaultValue);
            }
        } else {
            tag.setByteArray("Array", data);
        }
        return Optional.of(tag);
    }

    @Override
    public synchronized void load(TagStructure tag) {
        if (tag == null) {
            defaultValue = 0xF;
            data = null;
        } else {
            if (tag.has("Array")) {
                data = tag.getByteArray("Array");
                defaultValue = 0xF;
            } else if (tag.has("Default")) {
                defaultValue = tag.getByte("Default");
                data = null;
            }
        }
    }
}
