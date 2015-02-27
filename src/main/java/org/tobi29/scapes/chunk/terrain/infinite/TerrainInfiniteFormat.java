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

package org.tobi29.scapes.chunk.terrain.infinite;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.engine.utils.io.filesystem.Directory;
import org.tobi29.scapes.engine.utils.io.filesystem.File;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.io.tag.TagStructureArchive;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.vector.Vector2i;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class TerrainInfiniteFormat {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(TerrainInfiniteFormat.class);
    private final Directory directory;
    private final Map<Vector2i, RegionFile> regions = new ConcurrentHashMap<>();
    private final TerrainInfinite terrain;
    private final ByteArrayOutputStream byteStreamOut =
            new ByteArrayOutputStream(0x2000);

    public TerrainInfiniteFormat(Directory directory, TerrainInfinite terrain) {
        this.directory = directory;
        this.terrain = terrain;
        try {
            directory.make();
        } catch (IOException e) {
            LOGGER.warn("Failed to create directory: {}", e.toString());
        }
    }

    private static String getFilename(int x, int y) {
        return Integer.toString(x, 36).toUpperCase() + '_' +
                Integer.toString(y, 36).toUpperCase();
    }

    public synchronized Optional<TagStructure> getChunkTag(int x, int y)
            throws IOException {
        Vector2i location = new Vector2i(FastMath.floor((double) x / 16),
                FastMath.floor((double) y / 16));
        RegionFile region = regions.get(location);
        if (region == null) {
            region = createRegion(location);
        }
        return region.tag.getTagStructure(
                getFilename(x - (location.intX() << 4),
                        y - (location.intY() << 4)));
    }

    public synchronized void putChunkTag(int x, int y, TagStructure tag)
            throws IOException {
        Vector2i location = new Vector2i(FastMath.floor((double) x / 16),
                FastMath.floor((double) y / 16));
        RegionFile region = regions.get(location);
        if (region == null) {
            region = createRegion(location);
        }
        region.tag.setTagStructure(getFilename(x - (location.intX() << 4),
                y - (location.intY() << 4)), tag);
    }

    public synchronized void dispose() {
        regions.keySet().forEach(this::removeRegion);
    }

    private RegionFile createRegion(Vector2i location) throws IOException {
        RegionFile region = new RegionFile(directory.getResource(
                getFilename(location.intX(), location.intY()) + ".star"),
                byteStreamOut);
        regions.keySet().stream().filter(this::checkRegionUnused)
                .collect(Collectors.toList()).forEach(this::removeRegion);
        regions.put(location, region);
        return region;
    }

    private boolean checkRegionUnused(Vector2i location) {
        int x = location.intX() << 4;
        int y = location.intY() << 4;
        for (int xx = 0; xx < 16; xx++) {
            for (int yy = 0; yy < 16; yy++) {
                if (terrain.hasChunk(x + xx, y + yy)) {
                    return false;
                }
            }
        }
        return true;
    }

    private void removeRegion(Vector2i location) {
        try {
            regions.remove(location).write();
        } catch (IOException e) {
            LOGGER.error("Error whilst saving tag-list-container", e);
        }
    }

    private static class RegionFile {
        private final File file;
        private final TagStructureArchive tag;

        public RegionFile(File file, ByteArrayOutputStream byteStreamOut)
                throws IOException {
            this.file = file;
            tag = new TagStructureArchive(byteStreamOut);
            if (file.exists()) {
                file.read(tag::read);
            }
        }

        private void write() throws IOException {
            file.write(tag::write);
        }
    }
}
