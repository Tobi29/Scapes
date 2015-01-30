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
    void setSunLight(int x, int y, int z, int light);

    void setBlockLight(int x, int y, int z, int light);

    BlockType getBlockType(int x, int y, int z);

    int getBlockData(int x, int y, int z);

    int getLight(int x, int y, int z);

    int getSunLight(int x, int y, int z);

    int getBlockLight(int x, int y, int z);

    int getHighestBlockZAt(int x, int y);

    int getHighestTerrainBlockZAt(int x, int y);

    boolean isBlockAvailable(int x, int y, int z);

    boolean isBlockLoaded(int x, int y, int z);

    boolean isBlockTicking(int x, int y, int z);

    Pool<AABBElement> getCollisions(int minX, int minY, int minZ, int maxX,
            int maxY, int maxZ);

    Pool<PointerPane> getPointerPanes(int x, int y, int z, int range);

    void dispose();
}
