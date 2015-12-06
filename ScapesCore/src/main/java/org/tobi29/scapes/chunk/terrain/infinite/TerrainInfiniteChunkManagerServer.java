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

import java8.util.Optional;
import org.tobi29.scapes.engine.utils.math.vector.Vector2i;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TerrainInfiniteChunkManagerServer
        implements TerrainInfiniteChunkManager {
    private final Map<Vector2i, TerrainInfiniteChunkServer> chunks =
            new ConcurrentHashMap<>();
    private TerrainInfiniteChunkServer lastLookup;

    public void add(TerrainInfiniteChunkServer chunk) {
        chunks.put(chunk.pos(), chunk);
    }

    public TerrainInfiniteChunkServer remove(int x, int y) {
        TerrainInfiniteChunkServer chunk = chunks.remove(new Vector2i(x, y));
        if (chunk == lastLookup) {
            lastLookup = null;
        }
        return chunk;
    }

    @Override
    public Optional<TerrainInfiniteChunkServer> get(int x, int y) {
        TerrainInfiniteChunkServer lastChunk = lastLookup;
        if (lastChunk != null) {
            if (lastChunk.x() == x && lastChunk.y() == y) {
                return lastChunk.optional();
            }
        }
        TerrainInfiniteChunkServer chunk = chunks.get(new Vector2i(x, y));
        lastChunk = chunk;
        if (chunk == null) {
            return Optional.empty();
        } else {
            return chunk.optional();
        }
    }

    @Override
    public boolean has(int x, int y) {
        return chunks.containsKey(new Vector2i(x, y));
    }

    @Override
    public Collection<TerrainInfiniteChunkServer> iterator() {
        return chunks.values();
    }

    public int chunks() {
        return chunks.size();
    }
}
