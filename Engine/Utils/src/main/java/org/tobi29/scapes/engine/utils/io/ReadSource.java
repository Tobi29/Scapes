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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

@FunctionalInterface
public interface ReadSource {
    InputStream read() throws IOException;

    default void read(StreamReaderConsumer reader) throws IOException {
        try (InputStream streamIn = read()) {
            reader.read(streamIn);
        }
    }

    default <T> T readAndReturn(StreamReaderFunction<T> reader)
            throws IOException {
        try (InputStream streamIn = read()) {
            return reader.read(streamIn);
        }
    }

    default BufferedReader reader() throws IOException {
        return new BufferedReader(new InputStreamReader(read()));
    }

    @FunctionalInterface
    interface StreamReaderConsumer {
        void read(InputStream streamIn) throws IOException;
    }

    @FunctionalInterface
    interface StreamReaderFunction<T> {
        T read(InputStream streamIn) throws IOException;
    }
}
