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
package org.tobi29.scapes.server.format.basic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.engine.utils.Pair;
import org.tobi29.scapes.engine.utils.io.ByteBufferStream;
import org.tobi29.scapes.engine.utils.io.filesystem.FileUtil;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.io.tag.TagStructureArchive;
import org.tobi29.scapes.engine.utils.math.vector.Vector2i;
import org.tobi29.scapes.server.format.TerrainInfiniteFormat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class BasicTerrainInfiniteFormat implements TerrainInfiniteFormat {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(BasicTerrainInfiniteFormat.class);
    private final Path path;
    private final Map<Vector2i, RegionFile> regions = new ConcurrentHashMap<>();
    private final ByteBufferStream byteStream = new ByteBufferStream(),
            compressionStream = new ByteBufferStream();

    public BasicTerrainInfiniteFormat(Path path) {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(
                    new RuntimePermission("scapes.terrainInfiniteFormat"));
        }
        this.path = path;
    }

    private static String filename(int x, int y) {
        return Integer.toString(x, 36).toUpperCase() + '_' +
                Integer.toString(y, 36).toUpperCase();
    }

    @Override
    public synchronized List<Optional<TagStructure>> chunkTags(
            List<Vector2i> chunks) {
        List<Optional<TagStructure>> tagStructures = new ArrayList<>();
        for (Vector2i chunk : chunks) {
            try {
                tagStructures.add(chunkTag(chunk.intX(), chunk.intY()));
            } catch (IOException e) {
                tagStructures.add(Optional.empty());
            }
        }
        return tagStructures;
    }

    @Override
    public synchronized void putChunkTags(
            List<Pair<Vector2i, TagStructure>> chunks) throws IOException {
        for (Pair<Vector2i, TagStructure> chunk : chunks) {
            putChunkTag(chunk.a.intX(), chunk.a.intY(), chunk.b);
        }
    }

    @Override
    public synchronized void dispose() {
        regions.keySet().forEach(this::removeRegion);
    }

    private Optional<TagStructure> chunkTag(int x, int y) throws IOException {
        Vector2i location = new Vector2i(x >> 4, y >> 4);
        RegionFile region = regions.get(location);
        if (region == null) {
            region = createRegion(location);
        }
        region.lastUse = System.currentTimeMillis();
        return region.tag.getTagStructure(filename(x - (location.intX() << 4),
                y - (location.intY() << 4)));
    }

    private void putChunkTag(int x, int y, TagStructure tag)
            throws IOException {
        Vector2i location = new Vector2i(x >> 4, y >> 4);
        RegionFile region = regions.get(location);
        if (region == null) {
            region = createRegion(location);
        }
        region.lastUse = System.currentTimeMillis();
        region.tag.setTagStructure(filename(x - (location.intX() << 4),
                y - (location.intY() << 4)), tag);
    }

    private RegionFile createRegion(Vector2i location) {
        RegionFile region = AccessController.doPrivileged(
                (PrivilegedAction<RegionFile>) () -> new RegionFile(
                        path.resolve(
                                filename(location.intX(), location.intY()) +
                                        ".star")));
        long time = System.currentTimeMillis();
        regions.entrySet().stream()
                .filter(entry -> time - entry.getValue().lastUse > 1000)
                .map(Map.Entry::getKey).collect(Collectors.toList())
                .forEach(this::removeRegion);
        regions.put(location, region);
        return region;
    }

    private void removeRegion(Vector2i location) {
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            regions.remove(location).write();
            return null;
        });
    }

    private class RegionFile {
        private final Path path;
        private final TagStructureArchive tag;
        private long lastUse;

        private RegionFile(Path path) {
            this.path = path;
            tag = new TagStructureArchive(byteStream, compressionStream);
            if (Files.exists(path)) {
                try {
                    FileUtil.read(path, tag::read);
                } catch (IOException e) {
                    LOGGER.error("Error whilst loading tag-list-container", e);
                }
            }
        }

        private void write() {
            try {
                FileUtil.write(path, tag::write);
            } catch (IOException e) {
                LOGGER.error("Error whilst saving tag-list-container", e);
            }
        }
    }
}
