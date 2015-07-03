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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for creating checksums
 */
public final class ChecksumUtil {
    private ChecksumUtil() {
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
     * Creates a checksum from the given {@code byte[]}
     *
     * @param array Byte array that will be used to create the checksum
     * @return A {@code byte[]} containing the checksum
     */
    public static byte[] createChecksum(byte[] array) {
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

    public static byte[] createChecksum(ByteBuffer buffer) {
        return createChecksum(buffer, ChecksumAlgorithm.SHA256);
    }

    public static byte[] createChecksum(ByteBuffer buffer,
            ChecksumAlgorithm algorithm) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance(algorithm.getName());
            digest.update(buffer);
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new UnsupportedJVMException(e);
        }
    }

    public static byte[] createChecksum(ReadableByteStream input)
            throws IOException {
        return createChecksum(input, ChecksumAlgorithm.SHA256);
    }

    public static byte[] createChecksum(ReadableByteStream input,
            ChecksumAlgorithm algorithm) throws IOException {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance(algorithm.getName());
            ProcessStream.process(input, digest::update);
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new UnsupportedJVMException(e);
        }
    }

    /**
     * Enum containing available checksum algorithms
     */
    public enum ChecksumAlgorithm {
        SHA256("SHA-256"),
        SHA1("SHA1"),
        @Deprecated MD5("MD5");
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
