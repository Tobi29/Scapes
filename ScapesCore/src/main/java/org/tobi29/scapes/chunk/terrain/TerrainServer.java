/*
 * Copyright 2012-2016 Tobi29
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

import java8.util.Optional;
import java8.util.function.Consumer;
import java8.util.stream.Stream;
import org.tobi29.scapes.block.BlockType;
import org.tobi29.scapes.block.Update;
import org.tobi29.scapes.chunk.MobSpawner;
import org.tobi29.scapes.chunk.WorldServer;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.entity.server.EntityServer;
import org.tobi29.scapes.entity.server.MobPlayerServer;

import java.util.Collection;

public interface TerrainServer extends Terrain {
    WorldServer world();

    void update(double delta, Collection<MobSpawner> spawners);

    void queue(BlockChanges blockChanges);

    void addDelayedUpdate(Update update);

    boolean hasDelayedUpdate(int x, int y, int z,
            Class<? extends Update> clazz);

    boolean isBlockSendable(MobPlayerServer player, int x, int y, int z,
            boolean chunkContent);

    void chunks2D(Consumer<TerrainChunk2D> consumer);

    default void chunks3D(Consumer<TerrainChunk3D> consumer) {
        chunks2D(consumer::accept);
    }

    boolean addEntity(EntityServer entity);

    boolean removeEntity(EntityServer entity);

    Optional<EntityServer> entity(int id);

    void entities(Consumer<Stream<EntityServer>> consumer);

    void entities(int x, int y, int z, Consumer<Stream<EntityServer>> consumer);

    default void entities(int minX, int minY, int minZ, int maxX, int maxY,
            int maxZ, Consumer<Stream<EntityServer>> consumer) {
        entitiesAtLeast(minX, minY, minZ, maxX, maxY, maxZ,
                stream -> consumer.accept(stream.filter(entity -> {
                    int pos = FastMath.floor(entity.x());
                    return pos >= minX && pos <= maxX;
                }).filter(entity -> {
                    int pos = FastMath.floor(entity.y());
                    return pos >= minY && pos <= maxY;
                }).filter(entity -> {
                    int pos = FastMath.floor(entity.z());
                    return pos >= minZ && pos <= maxZ;
                })));
    }

    void entitiesAtLeast(int minX, int minY, int minZ, int maxX, int maxY,
            int maxZ, Consumer<Stream<EntityServer>> consumer);

    void dispose();

    interface BlockChanges {
        void run(TerrainMutable handler);
    }

    interface TerrainMutable extends TerrainServer {
        void type(int x, int y, int z, BlockType type);

        void data(int x, int y, int z, int data);

        void typeData(int x, int y, int z, BlockType block, int data);
    }
}
