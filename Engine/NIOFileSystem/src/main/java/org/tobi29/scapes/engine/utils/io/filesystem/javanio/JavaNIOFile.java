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

import org.tobi29.scapes.engine.utils.io.filesystem.File;
import org.tobi29.scapes.engine.utils.io.filesystem.FileAttributes;
import org.tobi29.scapes.engine.utils.io.filesystem.FileContents;
import org.tobi29.scapes.engine.utils.platform.PlatformDialogs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import java.util.zip.ZipFile;

public class JavaNIOFile implements File {
    final Path path;
    final String id;

    public JavaNIOFile(Path path, String id) throws IOException {
        if (Files.exists(path) && !Files.isRegularFile(path)) {
            throw new IOException("File is a directory: " + path);
        }
        this.path = path;
        this.id = id;
    }

    @Override
    public String getName() {
        return path.getFileName().toString();
    }

    @Override
    public void move(File file) throws IOException {
        Files.move(path, Paths.get(file.getURI()));
    }

    @Override
    public void copy(File file) throws IOException {
        Files.copy(path, Paths.get(file.getURI()));
    }

    @Override
    public void delete() throws IOException {
        Files.delete(path);
    }

    @Override
    public FileAttributes getAttributes() {
        return new JavaNIOAttributes(path);
    }

    @Override
    public boolean exportToUser(PlatformDialogs.Extension[] extensions,
            String title, PlatformDialogs dialogs) throws IOException {
        Optional<java.io.File> file = dialogs.saveFileDialog(extensions, title);
        if (file.isPresent()) {
            Files.copy(path, file.get().toPath());
            return true;
        }
        return false;
    }

    @Override
    public boolean importFromUser(PlatformDialogs.Extension[] extensions,
            String title, PlatformDialogs dialogs) throws IOException {
        java.io.File[] files = dialogs.openFileDialog(extensions, title, false);
        for (java.io.File file : files) {
            Files.copy(file.toPath(), path);
        }
        return files.length > 0;
    }

    @Override
    public FileContents contents() throws IOException {
        return new JavaNIOFileContents(path);
    }

    @Override
    public ZipFile readZIP() throws IOException {
        return new ZipFile(path.toFile());
    }

    @Override
    public URI getURI() {
        return path.toUri();
    }

    @Override
    public OutputStream write() throws IOException {
        return Files.newOutputStream(path);
    }

    @Override
    public String getID() {
        return id;
    }

    @Override
    public boolean exists() {
        return Files.exists(path);
    }

    @Override
    public URL getURL() throws IOException {
        return path.toUri().toURL();
    }

    @Override
    public InputStream read() throws IOException {
        return Files.newInputStream(path);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof File && Objects.equals(((File) obj).getID(), id);
    }
}
