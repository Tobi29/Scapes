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

package org.tobi29.scapes.chunk.terrain.infinite

import org.tobi29.scapes.entity.Entity

inline fun <E : Entity, R> TerrainInfinite<E>.chunk(x: Int,
                                                    y: Int,
                                                    consumer: (TerrainInfiniteChunk<E>) -> R): R? {
    chunk(x, y)?.let { consumer(it)?.let { return it } }
    return null
}

inline fun <R> TerrainInfiniteClient.chunkC(x: Int,
                                            y: Int,
                                            consumer: (TerrainInfiniteChunkClient) -> R): R? {
    chunk(x, y)?.let { consumer(it)?.let { return it } }
    return null
}

inline fun <R> TerrainInfiniteServer.chunkS(x: Int,
                                            y: Int,
                                            consumer: (TerrainInfiniteChunkServer) -> R): R? {
    chunk(x, y)?.let { consumer(it)?.let { return it } }
    return null
}
