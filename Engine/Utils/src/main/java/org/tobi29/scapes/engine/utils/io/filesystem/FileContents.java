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

package org.tobi29.scapes.engine.utils.io.filesystem;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface FileContents extends AutoCloseable {
    boolean open() throws IOException;

    int read(ByteBuffer buffer, long position) throws IOException;

    default void readAll(ByteBuffer buffer, long position) throws IOException {
        long offset = 0;
        while (buffer.hasRemaining()) {
            int read = read(buffer, position + offset);
            if (read == -1) {
                throw new IOException("End of file");
            }
            offset += read;
        }
    }

    int write(ByteBuffer buffer, long position) throws IOException;

    default void writeAll(ByteBuffer buffer, long position) throws IOException {
        long offset = 0;
        while (buffer.hasRemaining()) {
            int write = write(buffer, position + offset);
            if (write == -1) {
                throw new IOException("End of file");
            }
            offset += write;
        }
    }

    long getSize() throws IOException;

    void truncate(long position, long size) throws IOException;

    @Override
    void close() throws IOException;
}
