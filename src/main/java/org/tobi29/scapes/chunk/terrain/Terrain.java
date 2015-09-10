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

import org.tobi29.scapes.block.AABBElement;
import org.tobi29.scapes.block.BlockType;
import org.tobi29.scapes.engine.utils.Pool;
import org.tobi29.scapes.engine.utils.math.PointerPane;

public interface Terrain {
    void sunLight(int x, int y, int z, int light);

    void blockLight(int x, int y, int z, int light);

    BlockType type(int x, int y, int z);

    int data(int x, int y, int z);

    int light(int x, int y, int z);

    int sunLight(int x, int y, int z);

    int blockLight(int x, int y, int z);

    int sunLightReduction(int x, int y);

    int highestBlockZAt(int x, int y);

    int highestTerrainBlockZAt(int x, int y);

    boolean isBlockLoaded(int x, int y, int z);

    boolean isBlockTicking(int x, int y, int z);

    Pool<AABBElement> collisions(int minX, int minY, int minZ, int maxX,
            int maxY, int maxZ);

    Pool<PointerPane> pointerPanes(int x, int y, int z, int range);
}
