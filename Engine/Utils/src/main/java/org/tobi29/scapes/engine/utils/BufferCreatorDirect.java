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

package org.tobi29.scapes.engine.utils;

import java.nio.*;

/**
 * Utility class for creating direct buffers
 */
public final class BufferCreatorDirect {
    private BufferCreatorDirect() {
    }

    /**
     * Creates a direct {@code ByteBuffer} with native byte-order
     *
     * @param size Capacity of the buffer
     * @return A direct {@code ByteBuffer} with native byte-order, usable for native calls
     */
    public static ByteBuffer byteBuffer(int size) {
        return ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
    }

    /**
     * Creates a direct {@code ShortBuffer} with native byte-order
     *
     * @param size Capacity of the buffer
     * @return A direct {@code ShortBuffer} with native byte-order, usable for native calls
     */
    public static ShortBuffer shortBuffer(int size) {
        return byteBuffer(size << 1).asShortBuffer();
    }

    /**
     * Creates a direct {@code IntBuffer} with native byte-order
     *
     * @param size Capacity of the buffer
     * @return A direct {@code IntBuffer} with native byte-order, usable for native calls
     */
    public static IntBuffer intBuffer(int size) {
        return byteBuffer(size << 2).asIntBuffer();
    }

    /**
     * Creates a direct {@code LongBuffer} with native byte-order
     *
     * @param size Capacity of the buffer
     * @return A direct {@code LongBuffer} with native byte-order, usable for native calls
     */
    public static LongBuffer longBuffer(int size) {
        return byteBuffer(size << 3).asLongBuffer();
    }

    /**
     * Creates a direct {@code FloatBuffer} with native byte-order
     *
     * @param size Capacity of the buffer
     * @return A direct {@code FloatBuffer} with native byte-order, usable for native calls
     */
    public static FloatBuffer floatBuffer(int size) {
        return byteBuffer(size << 2).asFloatBuffer();
    }

    /**
     * Creates a direct {@code DoubleBuffer} with native byte-order
     *
     * @param size Capacity of the buffer
     * @return A direct {@code DoubleBuffer} with native byte-order, usable for native calls
     */
    public static DoubleBuffer doubleBuffer(int size) {
        return byteBuffer(size << 3).asDoubleBuffer();
    }

    public static ByteBuffer wrap(byte... array) {
        ByteBuffer buffer = byteBuffer(array.length);
        buffer.put(array);
        buffer.rewind();
        return buffer;
    }

    public static ShortBuffer wrap(short... array) {
        ShortBuffer buffer = shortBuffer(array.length);
        buffer.put(array);
        buffer.rewind();
        return buffer;
    }

    public static IntBuffer wrap(int... array) {
        IntBuffer buffer = intBuffer(array.length);
        buffer.put(array);
        buffer.rewind();
        return buffer;
    }

    public static LongBuffer wrap(long... array) {
        LongBuffer buffer = longBuffer(array.length);
        buffer.put(array);
        buffer.rewind();
        return buffer;
    }

    public static FloatBuffer wrap(float... array) {
        FloatBuffer buffer = floatBuffer(array.length);
        buffer.put(array);
        buffer.rewind();
        return buffer;
    }

    public static DoubleBuffer wrap(double... array) {
        DoubleBuffer buffer = doubleBuffer(array.length);
        buffer.put(array);
        buffer.rewind();
        return buffer;
    }

    public static ByteBuffer wrap(ByteBuffer array) {
        ByteBuffer buffer = byteBuffer(array.remaining());
        buffer.put(array);
        buffer.rewind();
        return buffer;
    }

    public static ShortBuffer wrap(ShortBuffer array) {
        ShortBuffer buffer = shortBuffer(array.remaining());
        buffer.put(array);
        buffer.rewind();
        return buffer;
    }

    public static IntBuffer wrap(IntBuffer array) {
        IntBuffer buffer = intBuffer(array.remaining());
        buffer.put(array);
        buffer.rewind();
        return buffer;
    }

    public static LongBuffer wrap(LongBuffer array) {
        LongBuffer buffer = longBuffer(array.remaining());
        buffer.put(array);
        buffer.rewind();
        return buffer;
    }

    public static FloatBuffer wrap(FloatBuffer array) {
        FloatBuffer buffer = floatBuffer(array.remaining());
        buffer.put(array);
        buffer.rewind();
        return buffer;
    }

    public static DoubleBuffer wrap(DoubleBuffer array) {
        DoubleBuffer buffer = doubleBuffer(array.remaining());
        buffer.put(array);
        buffer.rewind();
        return buffer;
    }
}
