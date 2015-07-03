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

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.function.IntFunction;

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
     * @param source    {@code ReadSource} to read from
     * @param processor {@code StreamProcessor} to process the stream data
     * @throws IOException Thrown when an I/O error occurs
     */
    public static <E> E processSource(ReadSource source,
            StreamProcessor<E> processor) throws IOException {
        return source.readReturn(stream -> process(stream, processor, 1024));
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
            StreamProcessorIO<E> processor) throws IOException {
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
            StreamProcessorIO<E> processor, int bufferSize) throws IOException {
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

    public static <E> E process(ReadableByteStream input,
            StreamProcessor<E> processor) throws IOException {
        return process(input, processor, 1024);
    }

    public static <E> E process(ReadableByteStream input,
            StreamProcessor<E> processor, int bufferSize) throws IOException {
        ByteBuffer buffer = BufferCreator.byteBuffer(bufferSize);
        boolean available = true;
        while (available) {
            available = input.getSome(buffer);
            buffer.flip();
            processor.process(buffer);
            buffer.clear();
        }
        return processor.result();
    }

    public static StreamProcessor<byte[]> asArray() {
        return new StreamProcessor<byte[]>() {
            private final ByteBufferStream stream = new ByteBufferStream();

            @Override
            public void process(ByteBuffer buffer) throws IOException {
                stream.put(buffer);
            }

            @Override
            public byte[] result() {
                stream.buffer().flip();
                byte[] array = new byte[stream.buffer().remaining()];
                stream.buffer().get(array);
                return array;
            }
        };
    }

    public static StreamProcessor<ByteBuffer> asBuffer() {
        return asBuffer(capacity -> BufferCreator.byteBuffer(capacity + 8192));
    }

    public static StreamProcessor<ByteBuffer> asBuffer(
            IntFunction<ByteBuffer> supplier) {
        return new StreamProcessor<ByteBuffer>() {
            private final ByteBufferStream stream =
                    new ByteBufferStream(supplier);

            @Override
            public void process(ByteBuffer buffer) throws IOException {
                stream.put(buffer);
            }

            @Override
            public ByteBuffer result() {
                return stream.buffer();
            }
        };
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
            private final ByteBufferStream stream = new ByteBufferStream(
                    capacity -> ByteBuffer.wrap(new byte[capacity + 1024]));

            @Override
            public void process(ByteBuffer buffer) throws IOException {
                stream.put(buffer);
            }

            @Override
            public String result() {
                return new String(stream.buffer().array(), 0,
                        stream.buffer().position(), charset);
            }
        };
    }

    /**
     * Functional interface to process data
     */
    @FunctionalInterface
    public interface StreamProcessorIO<E> {
        void process(byte[] buffer, int offset, int length) throws IOException;

        @SuppressWarnings("ReturnOfNull")
        default E result() {
            return null;
        }
    }

    @FunctionalInterface
    public interface StreamProcessor<E> {
        void process(ByteBuffer buffer) throws IOException;

        @SuppressWarnings("ReturnOfNull")
        default E result() {
            return null;
        }
    }
}
