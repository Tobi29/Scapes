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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

public class MarkedInflaterInputStream extends InflaterInputStream {
    public MarkedInflaterInputStream(InputStream in, Inflater inflater,
            int size) {
        super(in, inflater, size);
    }

    @Override
    protected void fill() throws IOException {
        int byte1 = in.read();
        int byte2 = in.read();
        if ((byte1 | byte2) < 0) {
            throw new EOFException("End of stream when reading mark");
        }
        len = (short) (byte1 << 8 | byte2);
        if (buf.length < len) {
            buf = new byte[len];
        }
        int n = 0;
        while (n < len) {
            int count = in.read(buf, n, len - n);
            if (count < 0) {
                throw new EOFException("End of stream when reading buffer");
            }
            n += count;
        }
        inf.setInput(buf, 0, len);
    }
}
