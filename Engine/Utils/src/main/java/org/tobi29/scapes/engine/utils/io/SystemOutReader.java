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
    private final BufferedReader logReader;

    /**
     * Constructs a new reader
     *
     * @throws IOException If an I/O error occurred
     */
    public SystemOutReader() throws IOException {
        consoleStream = new PipedOutputStream();
        copyOut = new CopyOutputStream(System.out, consoleStream);
        copyErr = new CopyOutputStream(System.err, consoleStream);
        logReader = new BufferedReader(
                new InputStreamReader(new PipedInputStream(consoleStream)));
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
    public String readLine() throws IOException {
        if (logReader.ready()) {
            return logReader.readLine();
        }
        return null;
    }

    @Override
    public void close() throws IOException {
        System.setOut((PrintStream) copyOut.out1);
        System.setErr((PrintStream) copyErr.out1);
        consoleStream.close();
    }
}
