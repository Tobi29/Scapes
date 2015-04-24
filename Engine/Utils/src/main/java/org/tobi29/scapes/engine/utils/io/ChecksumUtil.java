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

import org.tobi29.scapes.engine.utils.ArrayUtil;
import org.tobi29.scapes.engine.utils.UnsupportedJVMException;
import org.tobi29.scapes.engine.utils.io.filesystem.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for creating checksums
 */
public final class ChecksumUtil {
    private ChecksumUtil() {
    }

    /**
     * Creates a SHA1 checksum from the given {@code File}
     *
     * @param file File that will be read to create the checksum
     * @return A {@code String} that represents a hexadecimal encoding of the
     * checksum
     * @throws IOException If an I/O error occurred
     */
    public static String getChecksum(Resource file) throws IOException {
        return getChecksum(file, ChecksumAlgorithm.SHA256);
    }

    /**
     * Creates a checksum from the given file
     *
     * @param file      File that will be read to create the checksum
     * @param algorithm The algorithm that will be used to create the checksum
     * @return A {@code String} that represents a hexadecimal encoding of the
     * checksum
     * @throws IOException If an I/O error occurred
     */
    public static String getChecksum(Resource file, ChecksumAlgorithm algorithm)
            throws IOException {
        return ArrayUtil.toHexadecimal(createChecksum(file, algorithm));
    }

    public static String getChecksum(InputStream streamIn) throws IOException {
        return getChecksum(streamIn, ChecksumAlgorithm.SHA256);
    }

    public static String getChecksum(InputStream streamIn,
            ChecksumAlgorithm algorithm) throws IOException {
        return ArrayUtil.toHexadecimal(createChecksum(streamIn, algorithm));
    }

    /**
     * Creates a SHA1 checksum from the given {@code String}
     *
     * @param value String that will be used to create the checksum
     * @return A {@code String} that represents a hexadecimal encoding of the
     * checksum
     */
    public static String getChecksum(String value) {
        return getChecksum(value, ChecksumAlgorithm.SHA256);
    }

    /**
     * Creates a checksum from the given {@code String}
     *
     * @param value     String that will be used to create the checksum
     * @param algorithm The algorithm that will be used to create the checksum
     * @return A {@code String} that represents a hexadecimal encoding of the
     * checksum
     */
    public static String getChecksum(String value,
            ChecksumAlgorithm algorithm) {
        return ArrayUtil.toHexadecimal(createChecksum(value, algorithm));
    }

    /**
     * Creates a SHA1 checksum from the given {@code byte[]}
     *
     * @param array Byte array that will be used to create the checksum
     * @return A {@code String} that represents a hexadecimal encoding of the
     * checksum
     */
    public static String getChecksum(byte... array) {
        return getChecksum(array, ChecksumAlgorithm.SHA256);
    }

    /**
     * Creates a checksum from the given {@code byte[]}
     *
     * @param array     Byte array that will be used to create the checksum
     * @param algorithm The algorithm that will be used to create the checksum
     * @return A {@code String} that represents a hexadecimal encoding of the
     * checksum
     */
    public static String getChecksum(byte[] array,
            ChecksumAlgorithm algorithm) {
        return ArrayUtil.toHexadecimal(createChecksum(array, algorithm));
    }

    /**
     * Creates a SHA1 checksum from the given {@code String}
     *
     * @param value String that will be used to create the checksum
     * @return A {@code byte[]} containing the checksum
     */
    public static byte[] createChecksum(String value) {
        return createChecksum(value, ChecksumAlgorithm.SHA256);
    }

    /**
     * Creates a checksum from the given {@code String}
     *
     * @param value     String that will be used to create the checksum
     * @param algorithm The algorithm that will be used to create the checksum
     * @return A {@code byte[]} containing the checksum
     */
    public static byte[] createChecksum(String value,
            ChecksumAlgorithm algorithm) {
        return createChecksum(value.getBytes(StandardCharsets.UTF_8),
                algorithm);
    }

    /**
     * Creates a SHA1 checksum from the given {@code File}
     *
     * @param file File that will be read to create the checksum
     * @return A {@code byte[]} containing the checksum
     * @throws IOException If an I/O error occurred
     */
    public static byte[] createChecksum(Resource file) throws IOException {
        return createChecksum(file, ChecksumAlgorithm.SHA256);
    }

    /**
     * Creates a checksum from the given {@code File}
     *
     * @param file      File that will be read to create the checksum
     * @param algorithm The algorithm that will be used to create the checksum
     * @return A {@code byte[]} containing the checksum
     * @throws IOException If an I/O error occurred
     */
    public static byte[] createChecksum(Resource file,
            ChecksumAlgorithm algorithm) throws IOException {
        return createChecksum(file.read(), algorithm);
    }

    public static byte[] createChecksum(InputStream streamIn)
            throws IOException {
        return createChecksum(streamIn, ChecksumAlgorithm.SHA256);
    }

    public static byte[] createChecksum(InputStream streamIn,
            ChecksumAlgorithm algorithm) throws IOException {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance(algorithm.getName());
            ProcessStream.process(streamIn, digest::update);
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new UnsupportedJVMException(e);
        }
    }

    /**
     * Creates a SHA1 checksum from the given {@code byte[]}
     *
     * @param array Byte array that will be used to create the checksum
     * @return A {@code byte[]} containing the checksum
     */
    public static byte[] createChecksum(byte... array) {
        return createChecksum(array, ChecksumAlgorithm.SHA256);
    }

    /**
     * Creates a checksum from the given {@code byte[]}
     *
     * @param array     Byte array that will be used to create the checksum
     * @param algorithm The algorithm that will be used to create the checksum
     * @return A {@code byte[]} containing the checksum
     */
    public static byte[] createChecksum(byte[] array,
            ChecksumAlgorithm algorithm) {
        try {
            MessageDigest complete =
                    MessageDigest.getInstance(algorithm.getName());
            complete.update(array);
            return complete.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new UnsupportedJVMException(e);
        }
    }

    /**
     * Encodes the given checksum into hexadecimal
     *
     * @param array The checksum that will be encoded
     * @return A {@code String} that represents a hexadecimal encoding of the
     * checksum
     */
    public static String encodeChecksum(byte... array) {
        StringBuilder result = new StringBuilder(array.length);
        for (byte aB : array) {
            result.append(
                    Integer.toString((aB & 0xff) + 0x100, 16).substring(1));
        }
        return result.toString();
    }

    /**
     * Enum containing available checksum algorithms
     */
    public enum ChecksumAlgorithm {
        SHA256("SHA-256"),
        SHA1("SHA1"),
        @Deprecated
        MD5("MD5");
        private final String name;

        ChecksumAlgorithm(String name) {
            this.name = name;
        }

        /**
         * Gives the name of the algorithm to be used for creating checksums
         *
         * @return A {@code String} representing the algorithm name
         */
        public String getName() {
            return name;
        }
    }
}
