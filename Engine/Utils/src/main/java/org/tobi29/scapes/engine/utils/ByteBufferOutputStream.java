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

import java.io.OutputStream;
import java.nio.ByteBuffer;

public class ByteBufferOutputStream extends OutputStream {
    private final BufferSupplier supplier;
    private ByteBuffer buffer;

    public ByteBufferOutputStream(BufferSupplier supplier) {
        this.supplier = supplier;
        buffer = supplier.bytes(0);
    }

    @Override
    public void write(int b) {
        if (!buffer.hasRemaining()) {
            ByteBuffer newBuffer = supplier.bytes(buffer.position() + 1);
            buffer.flip();
            newBuffer.put(buffer);
            buffer = newBuffer;
        }
        buffer.put((byte) b);
    }

    @Override
    public void write(byte[] b, int off, int len) {
        if (buffer.remaining() < len) {
            ByteBuffer newBuffer = supplier.bytes(buffer.position() + len);
            buffer.flip();
            newBuffer.put(buffer);
            buffer = newBuffer;
        }
        buffer.put(b, off, len);
    }

    public ByteBuffer getBuffer() {
        ByteBuffer output = buffer.duplicate();
        output.flip();
        return output;
    }

    public int size() {
        return buffer.position();
    }

    public void reset() {
        buffer.rewind();
    }

    @FunctionalInterface
    public interface BufferSupplier {
        ByteBuffer bytes(int size);
    }
}
