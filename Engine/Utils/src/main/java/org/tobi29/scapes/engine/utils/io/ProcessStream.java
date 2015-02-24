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
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Utility class to read an entire stream and process it as a byte array.
 */
public final class ProcessStream {
    private ProcessStream() {
    }

    /**
     * Processes the entire stream and invokes the processor with the read data.
     * The stream will be closed after the stream ended.
     *
     * @param source      {@code ReadSource} to read from
     * @param destination {@code WriteDestination} to process the stream data
     * @throws IOException Thrown when an I/O error occurs
     */
    public static <E> E processSourceAndDestination(ReadSource source,
            WriteDestination destination) throws IOException {
        return processDestination(source.read(), destination);
    }

    /**
     * Processes the entire stream and invokes the processor with the read data.
     * The stream will be closed after the stream ended.
     *
     * @param streamIn    {@code InputStream} to read from
     * @param destination {@code WriteDestination} to process the stream data
     * @throws IOException Thrown when an I/O error occurs
     */
    public static <E> E processDestination(InputStream streamIn,
            WriteDestination destination) throws IOException {
        return destination.writeAndReturn(
                streamOut -> process(streamIn, streamOut::write, 1024));
    }

    /**
     * Processes the entire stream and invokes the processor with the read data.
     * The stream will be closed after the stream ended.
     *
     * @param source    {@code ReadSource} to read from
     * @param processor {@code StreamProcessor} to process the stream data
     * @throws IOException Thrown when an I/O error occurs
     */
    public static <E> E processSource(ReadSource source,
            StreamProcessor<E> processor) throws IOException {
        return process(source.read(), processor, 1024);
    }

    /**
     * Processes the entire stream and invokes the processor with the read data.
     * The stream will be closed after the stream ended.
     *
     * @param streamIn  {@code InputStream} to read from
     * @param processor {@code StreamProcessor} to process the stream data
     * @throws IOException Thrown when an I/O error occurs
     */
    public static <E> E process(InputStream streamIn,
            StreamProcessor<E> processor) throws IOException {
        return process(streamIn, processor, 1024);
    }

    /**
     * Processes the entire stream and invokes the processor with the read data.
     * The stream will be closed after the stream ended.
     *
     * @param streamIn   {@code InputStream} to read from
     * @param processor  {@code StreamProcessor} tp process the stream data
     * @param bufferSize Size of the buffer to store the data in
     * @throws IOException Thrown when an I/O error occurs
     */
    public static <E> E process(InputStream streamIn,
            StreamProcessor<E> processor, int bufferSize) throws IOException {
        try {
            byte[] buffer = new byte[bufferSize];
            int read = streamIn.read(buffer);
            while (read != -1) {
                if (read > 0) {
                    processor.process(buffer, 0, read);
                }
                read = streamIn.read(buffer);
            }
        } finally {
            streamIn.close();
        }
        return processor.result();
    }

    /**
     * A default {@code StreamProcessor} that encoded the input into a {@code String}
     *
     * @return UTF8 encoded {@code String}
     */
    public static StreamProcessor<String> asString() {
        return asString(StandardCharsets.UTF_8);
    }

    /**
     * A default {@code StreamProcessor} that encoded the input into a {@code String}
     *
     * @param charset Encoding charset for data
     * @return Encoded {@code String}
     */
    public static StreamProcessor<String> asString(Charset charset) {
        return new StreamProcessor<String>() {
            private final StringBuilder collector = new StringBuilder(16);

            @Override
            public void process(byte[] buffer, int offset, int length) {
                collector.append(new String(buffer, offset, length, charset));
            }

            @Override
            public String result() {
                return collector.toString();
            }
        };
    }

    /**
     * Functional interface to process data
     */
    @FunctionalInterface
    public interface StreamProcessor<E> {
        void process(byte[] buffer, int offset, int length) throws IOException;

        @SuppressWarnings("ReturnOfNull")
        default E result() {
            return null;
        }
    }
}
