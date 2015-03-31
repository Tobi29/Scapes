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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.engine.utils.ArrayUtil;
import org.tobi29.scapes.engine.utils.UnsupportedJVMException;
import org.tobi29.scapes.engine.utils.io.filesystem.Directory;
import org.tobi29.scapes.engine.utils.io.filesystem.File;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Utility class for managing data in a file cache
 */
public class FileCache {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(FileCache.class);
    private final Directory root, temp;
    private final Duration time;

    /**
     * Creates a new {@code FileCache}
     *
     * @param root The root directory that the cache will be saved into, will be created if it doesn't exist
     */
    public FileCache(Directory root, Directory temp) throws IOException {
        this(root, temp, Duration.ofDays(16));
    }

    /**
     * Creates a new {@code FileCache}
     *
     * @param root The root directory that the cache will be saved into, will be created if it doesn't exist
     * @param time Time until a file will be treated as old and is deleted on {@linkplain #check()}
     */
    public FileCache(Directory root, Directory temp, Duration time)
            throws IOException {
        root.make();
        temp.make();
        this.root = root;
        this.temp = temp;
        this.time = time;
    }

    /**
     * Reads the given {@code InputStream} and write its data into a file in the cache
     *
     * @param streamIn {@code InputStream} that will be read until it ends and will be closed
     * @param type     The type of data that will be stored, to organize the cache
     * @return A {@code FileCacheLocation} to later access the stored data, containing the checksum of the written file
     * @throws IOException If an I/O error occurred
     */
    public synchronized Location store(InputStream streamIn, String type)
            throws IOException {
        Directory parent = root.get(type);
        parent.make();
        File write = temp.getResource(UUID.randomUUID().toString());
        try (OutputStream streamOut = write.write()) {
            MessageDigest digest = MessageDigest
                    .getInstance(ChecksumUtil.ChecksumAlgorithm.SHA1.getName());
            ProcessStream.process(streamIn, (buffer, offset, length) -> {
                digest.update(buffer, offset, length);
                streamOut.write(buffer, offset, length);
            });
            byte[] array = digest.digest();
            String name = ArrayUtil.toHexadecimal(array);
            write.move(getFile(type, name));
            return new Location(type, array);
        } catch (NoSuchAlgorithmException e) {
            throw new UnsupportedJVMException(e);
        }
    }

    /**
     * Gives the {@code File} from the {@code FileCacheLocation} in this cache
     *
     * @param location The location that will be looked up
     * @return A {@code File} pointing at the file in cache or null if the cache doesn't contain a matching file
     */
    public synchronized Optional<File> retrieve(Location location) throws IOException {
        String name = ArrayUtil.toHexadecimal(location.array);
        File file = getFile(location.type, name);
        if (file.exists()) {
            file.getAttributes().setLastModifiedTime(Instant.now());
            return Optional.of(file);
        }
        return Optional.empty();
    }

    /**
     * Deletes the give location from the cache, does nothing if no matching file is found
     *
     * @param location The location that will be deleted
     */
    public synchronized void delete(Location location) throws IOException {
        String name = ArrayUtil.toHexadecimal(location.array);
        File file = getFile(location.type, name);
        if (file != null) {
            file.delete();
        }
    }

    /**
     * Deletes all files of the given type from the cache
     *
     * @param type The name of the type that will be removed
     */
    public synchronized void delete(String type) throws IOException {
        root.get(type).delete();
    }

    /**
     * Checks the entire cache and deletes files that don't match their checksum name or are old
     */
    public synchronized void check() throws IOException {
        Instant currentTime = Instant.now().minus(time);
        for (Directory type : root.listDirectories()) {
            for (File file : type.listFiles()) {
                if (file.getAttributes().getLastModifiedTime()
                        .isBefore(currentTime)) {
                    file.delete();
                    LOGGER.debug("Deleted old cache entry: {}", file);
                }
            }
        }
    }

    private File getFile(String type, String name) throws IOException {
        return root.getResource(type + '/' + name);
    }

    /**
     * A location in the cache
     */
    public static class Location {
        private final String type;
        private final byte[] array;

        /**
         * Constructs a new location of the given type and the checksum
         *
         * @param type     Type of the location
         * @param checksum Array containing the checksum name
         */
        public Location(String type, byte[] checksum) {
            this.type = type;
            array = checksum;
        }
    }
}
