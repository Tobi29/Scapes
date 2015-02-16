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

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

public class MarkedDeflaterOutputStream extends DeflaterOutputStream {
    private final byte[] singleBuffer = new byte[1];

    public MarkedDeflaterOutputStream(OutputStream out, Deflater deflater,
            int size) {
        super(out, deflater, size, true);
        if (size > Short.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "This output stream should only be used with small buffer sizes!");
        }
    }

    @Override
    public void write(int b) throws IOException {
        singleBuffer[0] = (byte) b;
        write(singleBuffer, 0, 1);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (def.finished()) {
            throw new IOException("write beyond end of stream");
        }
        if ((off | len | off + len | b.length - (off + len)) < 0) {
            throw new IndexOutOfBoundsException(
                    "Array: " + b.length + " Offset" + off + " Length: " + len);
        } else if (len == 0) {
            return;
        }
        if (!def.finished()) {
            def.setInput(b, off, len);
            while (!def.needsInput()) {
                deflate();
            }
        }
    }

    @Override
    public void finish() throws IOException {
        if (!def.finished()) {
            def.finish();
            while (!def.finished()) {
                deflate();
            }
        }
    }

    @Override
    protected void deflate() throws IOException {
        int len = def.deflate(buf, 0, buf.length);
        if (len > Short.MAX_VALUE) {
            throw new IOException("Length was too long, unable to mark!");
        }
        if (len > 0) {
            out.write(len >> 8 & 0xFF);
            out.write(len & 0xFF);
            out.write(buf, 0, len);
        }
    }

    @Override
    public void flush() throws IOException {
        if (!def.finished()) {
            int len = def.deflate(buf, 0, buf.length, Deflater.SYNC_FLUSH);
            while (len > 0) {
                if (len > Short.MAX_VALUE) {
                    throw new IOException(
                            "Length was too long, unable to mark!");
                }
                out.write(len >> 8 & 0xFF);
                out.write(len & 0xFF);
                out.write(buf, 0, len);
                if (len < buf.length) {
                    break;
                }
                len = def.deflate(buf, 0, buf.length, Deflater.SYNC_FLUSH);
            }
        }
        out.flush();
    }
}
