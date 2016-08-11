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
package org.tobi29.scapes.entity;

import java8.util.Optional;
import java8.util.function.Consumer;
import java8.util.stream.Stream;
import org.tobi29.scapes.engine.utils.math.AABB;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.Frustum;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;

import java.util.UUID;

public interface EntityContainer<E extends Entity> {
    int MAX_ENTITY_SIZE = 16;

    boolean addEntity(E entity);

    boolean removeEntity(E entity);

    default boolean hasEntity(UUID uuid) {
        return entity(uuid).isPresent();
    }

    boolean hasEntity(E entity);

    Optional<E> entity(UUID uuid);

    void entities(Consumer<Stream<? extends E>> consumer);

    void entities(int x, int y, int z, Consumer<Stream<? extends E>> consumer);

    default void entities(int minX, int minY, int minZ, int maxX, int maxY,
            int maxZ, Consumer<Stream<? extends E>> consumer) {
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

    default void entities(Vector3 pos, double range,
            Consumer<Stream<? extends E>> consumer) {
        int minX = FastMath.floor(pos.doubleX() - range);
        int minY = FastMath.floor(pos.doubleY() - range);
        int minZ = FastMath.floor(pos.doubleZ() - range);
        int maxX = (int) FastMath.ceil(pos.doubleX() + range);
        int maxY = (int) FastMath.ceil(pos.doubleY() + range);
        int maxZ = (int) FastMath.ceil(pos.doubleZ() + range);
        double rangeSqr = range * range;
        entitiesAtLeast(minX, minY, minZ, maxX, maxY, maxZ, stream -> consumer
                .accept(stream.filter(entity ->
                        FastMath.pointDistanceSqr(pos, entity.pos()) <=
                                rangeSqr)));
    }

    default void entities(AABB aabb, Consumer<Stream<? extends E>> consumer) {
        int minX = FastMath.floor(aabb.minX) - MAX_ENTITY_SIZE;
        int minY = FastMath.floor(aabb.minY) - MAX_ENTITY_SIZE;
        int minZ = FastMath.floor(aabb.minZ) - MAX_ENTITY_SIZE;
        int maxX = (int) FastMath.ceil(aabb.maxX) + MAX_ENTITY_SIZE;
        int maxY = (int) FastMath.ceil(aabb.maxY) + MAX_ENTITY_SIZE;
        int maxZ = (int) FastMath.ceil(aabb.maxZ) + MAX_ENTITY_SIZE;
        entitiesAtLeast(minX, minY, minZ, maxX, maxY, maxZ, stream -> consumer
                .accept(stream.filter(entity -> aabb.overlay(entity.aabb()))));
    }

    default void entities(Frustum frustum,
            Consumer<Stream<? extends E>> consumer) {
        double x = frustum.x();
        double y = frustum.y();
        double z = frustum.z();
        double range = frustum.range();
        int minX = FastMath.floor(x - range);
        int minY = FastMath.floor(y - range);
        int minZ = FastMath.floor(z - range);
        int maxX = (int) FastMath.ceil(x + range);
        int maxY = (int) FastMath.ceil(y + range);
        int maxZ = (int) FastMath.ceil(z + range);
        entitiesAtLeast(minX, minY, minZ, maxX, maxY, maxZ, stream -> consumer
                .accept(stream
                        .filter(entity -> frustum.inView(entity.aabb()) > 0)));
    }

    void entitiesAtLeast(int minX, int minY, int minZ, int maxX, int maxY,
            int maxZ, Consumer<Stream<? extends E>> consumer);

    void entityAdded(E entity);

    void entityRemoved(E entity);
}
