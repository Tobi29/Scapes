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
package org.tobi29.scapes.chunk.terrain;

import java8.util.function.Supplier;
import java8.util.stream.Stream;
import org.tobi29.scapes.engine.utils.Streams;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TerrainRenderInfo {
    private final Map<String, InfoLayer> layers = new ConcurrentHashMap<>();

    public TerrainRenderInfo(
            Stream<Map.Entry<String, Supplier<InfoLayer>>> layers) {
        layers.forEach(entry -> this.layers
                .put(entry.getKey(), entry.getValue().get()));
    }

    public void init(int x, int y, int z, int xSize, int ySize, int zSize) {
        Streams.forEach(layers.values(),
                layer -> layer.init(x, y, z, xSize, ySize, zSize));
    }

    @SuppressWarnings("unchecked")
    public <E> E get(String name) {
        return (E) layers.get(name);
    }

    public interface InfoLayer {
        void init(int x, int y, int z, int xSize, int ySize, int zSize);
    }
}
