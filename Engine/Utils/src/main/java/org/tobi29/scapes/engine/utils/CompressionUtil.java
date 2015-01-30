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

import org.tobi29.scapes.engine.utils.io.ProcessStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterOutputStream;

/**
 * Utility class for compressing and decompressing data
 */
public final class CompressionUtil {
    private CompressionUtil() {
    }

    /**
     * Compresses the given array using Java's {@code DeflaterOutputStream} with
     * default {@code Deflater}
     *
     * @param array The {@code byte[]} to be compressed
     * @return A {@code byte[]} containing output data
     * @throws IOException If an I/O Error occurred
     */
    public static byte[] compress(byte... array) throws IOException {
        ByteArrayOutputStream streamOut =
                new ByteArrayOutputStream(array.length >> 2);
        compress(array, array.length, streamOut);
        return streamOut.toByteArray();
    }

    public static void compress(byte[] array, int length,
            OutputStream streamOut) throws IOException {
        Deflater deflater = new Deflater();
        compress(array, length, streamOut, deflater);
        deflater.end();
    }

    public static void compress(byte[] array, int length,
            OutputStream streamOut, Deflater deflater) throws IOException {
        try (DeflaterOutputStream deflaterStreamOut = new DeflaterOutputStream(
                streamOut, deflater)) {
            deflaterStreamOut.write(array, 0, length);
            deflaterStreamOut.finish();
        }
    }

    public static ByteBuffer compress(ByteBuffer buffer) throws IOException {
        Deflater deflater = new Deflater();
        ByteBuffer output = compress(buffer,
                length -> BufferCreator.byteBuffer(length + 1024), deflater);
        deflater.end();
        return output;
    }

    public static ByteBuffer compress(ByteBuffer buffer, Deflater deflater)
            throws IOException {
        return compress(buffer,
                length -> BufferCreator.byteBuffer(length + 1024), deflater);
    }

    public static ByteBuffer compress(ByteBuffer buffer,
            ByteBufferOutputStream.BufferSupplier supplier, Deflater deflater)
            throws IOException {
        ByteBufferOutputStream streamOut = new ByteBufferOutputStream(supplier);
        compress(buffer, streamOut, deflater);
        return streamOut.getBuffer();
    }

    public static void compress(ByteBuffer buffer, OutputStream streamOut,
            Deflater deflater) throws IOException {
        ByteBufferInputStream streamIn = new ByteBufferInputStream(buffer);
        compress(streamIn, streamOut, deflater);
    }

    public static void compress(InputStream streamIn, OutputStream streamOut,
            Deflater deflater) throws IOException {
        try (DeflaterOutputStream deflaterStreamOut = new DeflaterOutputStream(
                streamOut, deflater)) {
            ProcessStream.process(streamIn, deflaterStreamOut::write);
            deflaterStreamOut.finish();
        }
    }

    /**
     * Decompresses the given array using Java's {@code InflaterOutputStream}
     * with default {@code Inflater}
     *
     * @param array The {@code byte[]} to be decompressed
     * @return A {@code byte[]} containing output data
     * @throws IOException If an I/O Error occurred
     */
    public static byte[] decompress(byte... array) throws IOException {
        ByteArrayOutputStream streamOut =
                new ByteArrayOutputStream(array.length << 2);
        decompress(array, array.length, streamOut);
        return streamOut.toByteArray();
    }

    public static void decompress(byte[] array, int length,
            OutputStream streamOut) throws IOException {
        Inflater inflater = new Inflater();
        decompress(array, length, streamOut, inflater);
        inflater.end();
    }

    public static void decompress(byte[] array, int length,
            OutputStream streamOut, Inflater inflater) throws IOException {
        try (InflaterOutputStream inflaterStreamOut = new InflaterOutputStream(
                streamOut, inflater)) {
            inflaterStreamOut.write(array, 0, length);
            inflaterStreamOut.finish();
        }
    }

    public static ByteBuffer decompress(ByteBuffer buffer) throws IOException {
        Inflater inflater = new Inflater();
        ByteBuffer output = decompress(buffer, inflater);
        inflater.end();
        return output;
    }

    public static ByteBuffer decompress(ByteBuffer buffer, Inflater inflater)
            throws IOException {
        return decompress(buffer,
                length -> BufferCreator.byteBuffer(length + 1024), inflater);
    }

    public static ByteBuffer decompress(ByteBuffer buffer,
            ByteBufferOutputStream.BufferSupplier supplier, Inflater inflater)
            throws IOException {
        ByteBufferOutputStream streamOut = new ByteBufferOutputStream(supplier);
        decompress(buffer, streamOut, inflater);
        return streamOut.getBuffer();
    }

    public static void decompress(ByteBuffer buffer, OutputStream streamOut,
            Inflater inflater) throws IOException {
        decompress(new ByteBufferInputStream(buffer), streamOut, inflater);
    }

    public static void decompress(InputStream streamIn, OutputStream streamOut,
            Inflater inflater) throws IOException {
        try (InflaterOutputStream inflaterStreamOut = new InflaterOutputStream(
                streamOut, inflater)) {
            ProcessStream.process(streamIn, inflaterStreamOut::write);
            inflaterStreamOut.finish();
        }
    }
}
