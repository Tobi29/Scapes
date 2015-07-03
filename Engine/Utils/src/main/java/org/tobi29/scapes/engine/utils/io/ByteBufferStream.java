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

package org.tobi29.scapes.engine.utils.io;

import org.tobi29.scapes.engine.utils.BufferCreator;
import org.tobi29.scapes.engine.utils.math.FastMath;

import java.nio.ByteBuffer;
import java.util.function.IntFunction;

public class ByteBufferStream
        implements RandomWritableByteStream, RandomReadableByteStream {
    private final IntFunction<ByteBuffer> supplier;
    private ByteBuffer buffer;

    public ByteBufferStream() {
        this(capacity -> BufferCreator.byteBuffer(capacity + 8192));
    }

    public ByteBufferStream(IntFunction<ByteBuffer> supplier) {
        this(supplier, supplier.apply(0));
    }

    public ByteBufferStream(ByteBuffer buffer) {
        this(length -> BufferCreator.byteBuffer(length + 8192), buffer);
    }

    public ByteBufferStream(IntFunction<ByteBuffer> supplier,
            ByteBuffer buffer) {
        this.buffer = buffer;
        this.supplier = supplier;
    }

    public ByteBuffer buffer() {
        return buffer;
    }

    @Override
    public int position() {
        return buffer.position();
    }

    @Override
    public ByteBufferStream position(int pos) {
        buffer.position(pos);
        return this;
    }

    @Override
    public ByteBufferStream put(ByteBuffer buffer, int len) {
        ensure(len);
        int limit = buffer.limit();
        buffer.limit(buffer.position() + len);
        this.buffer.put(buffer);
        buffer.limit(limit);
        return this;
    }

    @Override
    public ByteBufferStream put(int b) {
        ensure(1);
        buffer.put((byte) b);
        return this;
    }

    @Override
    public ByteBufferStream putShort(int value) {
        ensure(2);
        buffer.putShort((short) value);
        return this;
    }

    @Override
    public ByteBufferStream putInt(int value) {
        ensure(4);
        buffer.putInt(value);
        return this;
    }

    @Override
    public ByteBufferStream putLong(long value) {
        ensure(8);
        buffer.putLong(value);
        return this;
    }

    @Override
    public ByteBufferStream putFloat(float value) {
        ensure(4);
        buffer.putFloat(value);
        return this;
    }

    @Override
    public ByteBufferStream putDouble(double value) {
        ensure(8);
        buffer.putDouble(value);
        return this;
    }

    private void ensure(int len) {
        while (len > buffer.remaining()) {
            ByteBuffer newBuffer = supplier.apply(buffer.capacity());
            buffer.flip();
            newBuffer.put(buffer);
            buffer = newBuffer;
        }
    }

    @Override
    public int limit() {
        return buffer.limit();
    }

    @Override
    public ReadableByteStream limit(int limit) {
        buffer.limit(limit);
        return this;
    }

    @Override
    public int remaining() {
        return buffer.remaining();
    }

    @Override
    public ReadableByteStream get(ByteBuffer buffer, int len) {
        int limit = this.buffer.limit();
        this.buffer.limit(this.buffer.position() + len);
        buffer.put(this.buffer);
        this.buffer.limit(limit);
        return this;
    }

    @Override
    public boolean getSome(ByteBuffer buffer, int len) {
        len = FastMath.min(len, this.buffer.remaining());
        int limit = this.buffer.limit();
        this.buffer.limit(this.buffer.position() + len);
        buffer.put(this.buffer);
        this.buffer.limit(limit);
        return this.buffer.remaining() > 0;
    }

    @Override
    public byte get() {
        return buffer.get();
    }

    @Override
    public short getShort() {
        return buffer.getShort();
    }

    @Override
    public int getInt() {
        return buffer.getInt();
    }

    @Override
    public long getLong() {
        return buffer.getLong();
    }

    @Override
    public float getFloat() {
        return buffer.getFloat();
    }

    @Override
    public double getDouble() {
        return buffer.getDouble();
    }
}