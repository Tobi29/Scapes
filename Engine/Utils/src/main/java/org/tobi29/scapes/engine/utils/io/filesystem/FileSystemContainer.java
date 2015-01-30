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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.engine.utils.Pair;
import org.tobi29.scapes.engine.utils.io.filesystem.spi.FileSystemProvider;

import java.io.IOException;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class FileSystemContainer implements Path {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(FileSystemContainer.class);
    private static final Pattern SPLIT = Pattern.compile(":");
    private final Map<String, Path> fileSystems = new ConcurrentHashMap<>();

    public static FileSystem newFileSystem(String root, String id)
            throws IOException {
        for (FileSystemProvider fileSystemProvider : ServiceLoader
                .load(FileSystemProvider.class)) {
            try {
                return fileSystemProvider.getFileFileSystem(root, id);
            } catch (ServiceConfigurationError e) {
                LOGGER.warn("Error loading file system provider: {}",
                        e.toString());
            }
        }
        throw new IOException("No file system provider found");
    }

    public static PathRootCreator newFileSystem(String root) {
        return (id, path) -> newFileSystem(root, id);
    }

    public void registerFileSystem(String id, PathRootCreator fileSystem)
            throws IOException {
        registerFileSystem(id, "", fileSystem);
    }

    public void registerFileSystem(String id, String root,
            PathRootCreator fileSystem) throws IOException {
        fileSystems.put(id, fileSystem.get(id + ':' + root, root));
    }

    public void removeFileSystem(String id) {
        fileSystems.remove(id);
    }

    public Path getFileSystem(String id) throws IOException {
        Path fileSystem = fileSystems.get(id);
        if (fileSystem == null) {
            throw new IOException("Unknown file system: " + id);
        }
        return fileSystem;
    }

    @Override
    public String getID() {
        throw new UnsupportedOperationException(
                "Cannot get ID of root container");
    }

    @Override
    public Path get(String path) throws IOException {
        Pair<String, String> location = splitPath(path);
        return getFileSystem(location.a).get(location.b);
    }

    @Override
    public Resource getResource(String path) throws IOException {
        Pair<String, String> location = splitPath(path);
        return getFileSystem(location.a).getResource(location.b);
    }

    public Directory getDirectory(String path) throws IOException {
        Path directory = get(path);
        if (directory instanceof Directory) {
            return (Directory) directory;
        }
        throw new IOException("Not a directory-based filesystem: " + path);
    }

    public File getFile(String path) throws IOException {
        Resource file = getResource(path);
        if (file instanceof File) {
            return (File) file;
        }
        throw new IOException("Not a file-based filesystem: " + path);
    }

    private Pair<String, String> splitPath(String path) throws IOException {
        String[] array = SPLIT.split(path, 2);
        if (array.length != 2) {
            throw new IOException("Invalid path: " + path);
        }
        return new Pair<>(array[0], array[1]);
    }

    @FunctionalInterface
    public interface PathRootCreator {
        PathRoot get(String id, String path) throws IOException;
    }
}
