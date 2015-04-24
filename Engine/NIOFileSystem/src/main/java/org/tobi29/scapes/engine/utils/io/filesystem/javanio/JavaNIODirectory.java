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

package org.tobi29.scapes.engine.utils.io.filesystem.javanio;

import org.tobi29.scapes.engine.utils.io.filesystem.Directory;
import org.tobi29.scapes.engine.utils.io.filesystem.File;
import org.tobi29.scapes.engine.utils.io.filesystem.FileAttributes;
import org.tobi29.scapes.engine.utils.platform.PlatformDialogs;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JavaNIODirectory implements Directory {
    final Path path;
    final String id;

    public JavaNIODirectory(Path path, String id) throws IOException {
        if (Files.exists(path) && !Files.isDirectory(path)) {
            throw new IOException("File is no directory: " + path);
        }
        this.path = path;
        this.id = id;
    }

    @Override
    public String getName() {
        return path.getFileName().toString();
    }

    @Override
    public boolean exists() {
        return Files.exists(path);
    }

    @Override
    public void make() throws IOException {
        Files.createDirectories(path);
    }

    @Override
    public void move(Directory directory) throws IOException {
        Files.move(path, directory.getFile().toPath());
    }

    @Override
    public void copy(Directory directory) throws IOException {
        Path dest = directory.getFile().toPath();
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir,
                    BasicFileAttributes attrs) throws IOException {
                Files.createDirectories(dest.resolve(path.relativize(dir)));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file,
                    BasicFileAttributes attrs) throws IOException {
                Files.copy(file, dest.resolve(path.relativize(file)));
                return FileVisitResult.CONTINUE;
            }
        });
    }

    @Override
    public void delete() throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file,
                    BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                    throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    @Override
    public FileAttributes getAttributes() {
        return new JavaNIOAttributes(path);
    }

    @Override
    public Directory get(String path) throws IOException {
        if (path.isEmpty()) {
            return this;
        }
        return new JavaNIODirectory(this.path.resolve(path), id + path + '/');
    }

    @Override
    public File getResource(String path) throws IOException {
        return new JavaNIOFile(this.path.resolve(path), id + path);
    }

    @Override
    public List<File> listFiles(FileFilter filter) throws IOException {
        List<File> files = new ArrayList<>();
        for (Path file : Files.newDirectoryStream(path)) {
            if (Files.isRegularFile(file) && !Files.isHidden(file)) {
                JavaNIOFile virtualFile = new JavaNIOFile(file,
                        id + file.getFileName().toString());
                boolean valid = false;
                try {
                    valid = filter.accept(virtualFile);
                } catch (IOException e) {
                }
                if (valid) {
                    files.add(virtualFile);
                }
            }
        }
        return files;
    }

    @Override
    public List<Directory> listDirectories(DirectoryFilter filter)
            throws IOException {
        List<Directory> directories = new ArrayList<>();
        for (Path file : Files.newDirectoryStream(path)) {
            if (Files.isDirectory(file) && !Files.isHidden(file)) {
                JavaNIODirectory virtualDirectory = new JavaNIODirectory(file,
                        id + file.getFileName().toString() + '/');
                boolean valid = false;
                try {
                    valid = filter.accept(virtualDirectory);
                } catch (IOException e) {
                }
                if (valid) {
                    directories.add(virtualDirectory);
                }
            }
        }
        return directories;
    }

    @Override
    public boolean importFromUser(PlatformDialogs.Extension[] extensions,
            String title, boolean multiple, PlatformDialogs dialogs)
            throws IOException {
        java.io.File[] files = dialogs.openFileDialog(extensions, title, true);
        for (java.io.File file : files) {
            Files.copy(file.toPath(), path.resolve(file.getName()));
        }
        return files.length > 0;
    }

    @Override
    public java.io.File getFile() {
        return path.toFile();
    }

    @Override
    public String getID() {
        return id;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Directory &&
                Objects.equals(((Directory) obj).getID(), id);
    }
}
