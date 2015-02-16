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

import org.apache.tika.Tika;
import org.tobi29.scapes.engine.utils.io.filesystem.FileAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;

public class JavaNIOAttributes implements FileAttributes {
    private static final Tika TIKA = new Tika();
    final Path path;

    public JavaNIOAttributes(Path path) {
        this.path = path;
    }

    @Override
    public Instant getCreationTime() throws IOException {
        return Files.readAttributes(path, BasicFileAttributes.class)
                .creationTime().toInstant();
    }

    @Override
    public Instant getLastModifiedTime() throws IOException {
        return Files.getLastModifiedTime(path).toInstant();
    }

    @Override
    public void setLastModifiedTime(Instant time) throws IOException {
        Files.setLastModifiedTime(path, FileTime.from(time));
    }

    @Override
    public String getMIMEType() throws IOException {
        String mime = Files.probeContentType(path);
        if (mime == null) {
            mime = TIKA.detect(path.toFile());
        }
        return mime;
    }

    @Override
    public long getSize() throws IOException {
        return Files.size(path);
    }
}
