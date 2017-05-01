/*
 * Copyright 2012-2017 Tobi29
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

package org.tobi29.scapes.terrain.infinite

import org.tobi29.scapes.terrain.TerrainBaseMutable
import org.tobi29.scapes.terrain.TerrainLock
import org.tobi29.scapes.terrain.VoxelType

open class TerrainInfiniteMutableSection<B : VoxelType, C : TerrainInfiniteBaseChunk<B>, T : TerrainInfiniteBase<B, C>> : TerrainInfiniteSection<B, C, T>(), TerrainBaseMutable<B>, TerrainLock {
    override fun type(x: Int,
                      y: Int,
                      z: Int,
                      type: B) {
        chunkFor(x, y, z)?.blockTypeG(x, y, z, type)
    }

    override fun data(x: Int,
                      y: Int,
                      z: Int,
                      data: Int) {
        chunkFor(x, y, z)?.dataG(x, y, z, data)
    }

    override fun typeData(x: Int,
                          y: Int,
                          z: Int,
                          type: B,
                          data: Int) {
        chunkFor(x, y, z)?.typeDataG(x, y, z, type, data)
    }
}
