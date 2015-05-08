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
import org.tobi29.scapes.engine.utils.io.filesystem.Directory;
import org.tobi29.scapes.engine.utils.io.filesystem.File;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.io.tag.TagStructureBinary;
import org.tobi29.scapes.server.ScapesServer;

import java.io.IOException;
import java.util.Optional;

public class PlayerData {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(PlayerData.class);
    private final ScapesServer server;
    private final Directory directory;

    public PlayerData(ScapesServer server, Directory directory)
            throws IOException {
        this.server = server;
        this.directory = directory;
        directory.make();
    }

    public Optional<TagStructure> load(String id) {
        try {
            File file = directory.getResource(id + ".stag");
            if (file.exists()) {
                TagStructure tagStructure =
                        file.readAndReturn(TagStructureBinary::read);
                return Optional.of(tagStructure);
            }
        } catch (IOException e) {
            LOGGER.error("Error reading player data: {}", e.toString());
        }
        return Optional.empty();
    }

    public void save(TagStructure tagStructure, String id) {
        try {
            File file = directory.getResource(id + ".stag");
            file.write(streamOut -> TagStructureBinary
                    .write(tagStructure, streamOut));
        } catch (IOException e) {
            LOGGER.error("Error writing player data: {}", e.toString());
        }
    }

    public boolean playerExists(String id) {
        try {
            return directory.getResource(id + ".stag").exists();
        } catch (IOException e) {
            LOGGER.error("Error checking player data: {}", e.toString());
        }
        return false;
    }
}
