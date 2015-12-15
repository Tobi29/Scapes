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

import java8.util.Objects;
import java8.util.Optional;
import java8.util.stream.Collectors;
import org.tobi29.scapes.engine.utils.Streams;
import org.tobi29.scapes.engine.utils.math.FastMath;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;

public class TerrainInfiniteChunkManagerClient
        implements TerrainInfiniteChunkManager {
    private final int radius, size;
    private final TerrainInfiniteChunkClient[] array;
    private final AtomicLong lock = new AtomicLong();
    private int x, y;

    public TerrainInfiniteChunkManagerClient(int radius) {
        this.radius = radius;
        size = (radius << 1) + 1;
        array = new TerrainInfiniteChunkClient[size * size];
    }

    public synchronized void add(TerrainInfiniteChunkClient chunk) {
        int xx = chunk.x() - x;
        int yy = chunk.y() - y;
        if (xx >= 0 && xx < size && yy >= 0 && yy < size) {
            int i = yy * size + xx;
            array[i] = null;
            array[i] = chunk;
        }
    }

    public synchronized Optional<TerrainInfiniteChunkClient> remove(int x,
            int y) {
        int xx = x - this.x;
        int yy = y - this.y;
        if (xx >= 0 && xx < size && yy >= 0 && yy < size) {
            int i = yy * size + xx;
            TerrainInfiniteChunkClient chunk = array[i];
            if (chunk != null) {
                array[i] = null;
                return Optional.of(chunk);
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<TerrainInfiniteChunkClient> get(int x, int y) {
        long stamp = lock.get();
        int xx = x - this.x;
        int yy = y - this.y;
        if (xx >= 0 && xx < size && yy >= 0 && yy < size) {
            int i = yy * size + xx;
            TerrainInfiniteChunkClient value = array[i];
            long validate = lock.get();
            if (stamp == validate && (validate & 1) == 0) {
                if (value == null) {
                    return Optional.empty();
                } else {
                    return value.optional();
                }
            }
        }
        synchronized (this) {
            xx = x - this.x;
            yy = y - this.y;
            if (xx >= 0 && xx < size && yy >= 0 && yy < size) {
                int i = yy * size + xx;
                TerrainInfiniteChunkClient value = array[i];
                if (value == null) {
                    return Optional.empty();
                } else {
                    return value.optional();
                }
            } else {
                return Optional.empty();
            }
        }
    }

    @Override
    public boolean has(int x, int y) {
        return get(x, y) != null;
    }

    @Override
    public Collection<TerrainInfiniteChunkClient> iterator() {
        return Streams.of(array).filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    protected boolean setCenter(int x, int y) {
        x -= radius;
        y -= radius;
        if (x != this.x || y != this.y) {
            synchronized (this) {
                lock.incrementAndGet();
                int xDiff = this.x - x;
                int xDiffAbs = FastMath.abs(xDiff);
                if (xDiffAbs > 0) {
                    if (xDiffAbs > size) {
                        clear();
                    } else {
                        for (int i = 0; i < xDiff; i++) {
                            shiftXPositive();
                        }
                        xDiff = -xDiff;
                        for (int i = 0; i < xDiff; i++) {
                            shiftXNegative();
                        }
                    }
                    this.x = x;
                }
                int yDiff = this.y - y;
                int yDiffAbs = FastMath.abs(yDiff);
                if (yDiffAbs > 0) {
                    if (yDiffAbs > size) {
                        clear();
                    } else {
                        for (int i = 0; i < yDiff; i++) {
                            shiftYPositive();
                        }
                        yDiff = -yDiff;
                        for (int i = 0; i < yDiff; i++) {
                            shiftYNegative();
                        }
                    }
                    this.y = y;
                }
                lock.incrementAndGet();
            }
            return true;
        }
        return false;
    }

    private void clear() {
        Arrays.fill(array, null);
    }

    private void shiftXPositive() {
        System.arraycopy(array, 0, array, 1, array.length - 1);
        for (int i = 0; i < array.length; i += size) {
            array[i] = null;
        }
    }

    private void shiftXNegative() {
        System.arraycopy(array, 1, array, 0, array.length - 1);
        for (int i = size - 1; i < array.length; i += size) {
            array[i] = null;
        }
    }

    private void shiftYPositive() {
        System.arraycopy(array, 0, array, size, array.length - size);
        for (int i = 0; i < size; i++) {
            array[i] = null;
        }
    }

    private void shiftYNegative() {
        System.arraycopy(array, size, array, 0, array.length - size);
        for (int i = array.length - size; i < array.length; i++) {
            array[i] = null;
        }
    }
}
