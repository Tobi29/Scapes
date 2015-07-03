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
import java.util.concurrent.atomic.AtomicLong;

public class ChunkArraySection1x16 implements ChunkArraySection {
    private final int xSizeBits, ySizeBits, size;
    private final AtomicLong lock = new AtomicLong();
    private byte[] data;
    private short defaultValue;
    private boolean changed;

    public ChunkArraySection1x16(int xSizeBits, int ySizeBits, int zSizeBits) {
        this.xSizeBits = xSizeBits;
        this.ySizeBits = ySizeBits;
        size = 2 << xSizeBits + ySizeBits + zSizeBits;
    }

    @Override
    public int data(int x, int y, int z, int offset) {
        return data(((z << ySizeBits | y) << xSizeBits | x) + offset);
    }

    @Override
    public int data(int offset) {
        byte[] data = this.data;
        if (data == null) {
            return defaultValue;
        }
        offset <<= 1;
        long stamp = lock.get();
        short value = (short) ((data[offset] << 8) + (data[offset + 1] & 0xFF));
        long validate = lock.get();
        if (stamp == validate && (validate & 1) == 0) {
            return value;
        }
        synchronized (this) {
            return (short) ((data[offset] << 8) + (data[offset + 1] & 0xFF));
        }
    }

    @Override
    public void data(int x, int y, int z, int offset, int value) {
        data(((z << ySizeBits | y) << xSizeBits | x) + offset, value);
    }

    @Override
    public synchronized void data(int offset, int value) {
        offset <<= 1;
        byte[] data = this.data;
        if (data == null) {
            if (value == defaultValue) {
                return;
            }
            byte[] newData = new byte[size];
            byte value1 = (byte) (defaultValue >> 8), value2 =
                    (byte) defaultValue;
            for (int i = 0; i < size; i += 2) {
                newData[i] = value1;
                newData[i + 1] = value2;
            }
            data = newData;
            data[offset] = (byte) (value >> 8);
            data[offset + 1] = (byte) value;
            this.data = data;
            changed = true;
        } else {
            lock.incrementAndGet();
            data[offset] = (byte) (value >> 8);
            data[offset + 1] = (byte) value;
            lock.incrementAndGet();
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
        for (int i = 2; i < size; i += 2) {
            if (data[i] != data[0] || data[i + 1] != data[1]) {
                flag = false;
                break;
            }
        }
        if (flag) {
            defaultValue = (short) (data[0] << 8 | data[1]);
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
