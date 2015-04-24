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

import java.io.*;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Utility class for reading log from {@code System.out} and {@code System.err}
 * <p>
 * <b>Warning:</b> The internal piped stream can caused severe blocking when the
 * output is not read fast enough, or can even cause deadlocks if the reading
 * thread itself writes to {@code System.out} or {@code System.err}
 */
public class SystemOutReader implements Closeable {
    private final CopyOutputStream copyOut, copyErr;
    private final PipedOutputStream consoleStream;
    private final InputStream logReader;
    private final Queue<String> lineQueue = new ConcurrentLinkedQueue<>();
    private byte[] buffer = new byte[1024];
    private int i;

    /**
     * Constructs a new reader
     *
     * @throws IOException If an I/O error occurred
     */
    public SystemOutReader() throws IOException {
        consoleStream = new PipedOutputStream();
        copyOut = new CopyOutputStream(System.out, consoleStream);
        copyErr = new CopyOutputStream(System.err, consoleStream);
        logReader = new PipedInputStream(consoleStream);
        System.setOut(new PrintStream(copyOut));
        System.setErr(new PrintStream(copyErr));
    }

    /**
     * Reads next line
     *
     * @return A {@code String} containing a single line or {@code null} if no
     * line was available yet
     * @throws IOException If an I/O error occurred
     */
    public Optional<String> readLine() throws IOException {
        if (lineQueue.isEmpty() && logReader.available() > 0) {
            if (i >= buffer.length) {
                byte[] newBuffer = new byte[buffer.length + 1024];
                System.arraycopy(buffer, 0, newBuffer, 0, i);
                buffer = newBuffer;
            }
            int length = logReader.read(buffer, i, buffer.length - i);
            if (length > 0) {
                int j = 0;
                while (j < length) {
                    int ij = i + j;
                    char c = (char) buffer[ij];
                    if (c == '\n') {
                        lineQueue.add(new String(buffer, 0, ij));
                        System.arraycopy(buffer, ij + 1, buffer, 0,
                                length - j - 1);
                        length -= j + 1;
                        i = 0;
                        j = 0;
                    } else {
                        j++;
                    }
                }
                i += length;
            }
        }
        return Optional.ofNullable(lineQueue.poll());
    }

    @Override
    public void close() throws IOException {
        System.setOut((PrintStream) copyOut.out1);
        System.setErr((PrintStream) copyErr.out1);
        consoleStream.close();
    }
}
