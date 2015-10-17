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
package org.tobi29.scapes.server.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.engine.utils.io.filesystem.FileUtil;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.io.tag.TagStructureBinary;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class PlayerData {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(PlayerData.class);
    private final Path path;

    public PlayerData(Path path) throws IOException {
        this.path = path;
        Files.createDirectories(path);
    }

    public synchronized TagStructure load(String id) {
        try {
            Path file = path.resolve(id + ".stag");
            if (Files.exists(file)) {
                return FileUtil.readReturn(file, TagStructureBinary::read);
            }
        } catch (IOException e) {
            LOGGER.error("Error reading player data: {}", e.toString());
        }
        return new TagStructure();
    }

    public synchronized void save(TagStructure tagStructure, String id) {
        try {
            Path file = path.resolve(id + ".stag");
            FileUtil.write(file,
                    stream -> TagStructureBinary.write(tagStructure, stream));
        } catch (IOException e) {
            LOGGER.error("Error writing player data: {}", e.toString());
        }
    }

    public synchronized void add(String id) {
        save(load(id), id);
    }

    public synchronized void remove(String id) {
        try {
            Files.deleteIfExists(path.resolve(id + ".stag"));
        } catch (IOException e) {
            LOGGER.error("Error writing player data: {}", e.toString());
        }
    }

    public boolean playerExists(String id) {
        return Files.exists(path.resolve(id + ".stag"));
    }
}
