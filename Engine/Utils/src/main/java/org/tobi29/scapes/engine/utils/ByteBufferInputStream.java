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

import org.tobi29.scapes.engine.utils.math.FastMath;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class ByteBufferInputStream extends InputStream {
    private final ByteBuffer buffer;

    public ByteBufferInputStream(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    @Override
    public int read() throws IOException {
        if (!buffer.hasRemaining()) {
            return -1;
        }
        int value = buffer.get();
        if (value < 0) {
            value += 256;
        }
        return value;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        len = FastMath.min(len, buffer.remaining());
        if (len == 0) {
            return -1;
        }
        buffer.get(b, off, len);
        return len;
    }

    @Override
    public long skip(long n) throws IOException {
        int skip = (int) FastMath.min(n, buffer.remaining());
        buffer.position(buffer.position() + skip);
        return skip;
    }

    @Override
    public int available() throws IOException {
        return buffer.remaining();
    }
}
