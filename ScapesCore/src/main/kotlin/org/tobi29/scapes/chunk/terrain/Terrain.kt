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
package org.tobi29.scapes.chunk.terrain

import org.tobi29.scapes.block.AABBElement
import org.tobi29.scapes.block.BlockType
import org.tobi29.scapes.engine.utils.Pool
import org.tobi29.scapes.engine.utils.math.PointerPane

interface Terrain {
    val air: BlockType

    fun sunLight(x: Int,
                 y: Int,
                 z: Int,
                 light: Int)

    fun blockLight(x: Int,
                   y: Int,
                   z: Int,
                   light: Int)

    fun block(x: Int,
              y: Int,
              z: Int): Long

    fun type(x: Int,
             y: Int,
             z: Int): BlockType

    fun light(x: Int,
              y: Int,
              z: Int): Int

    fun sunLight(x: Int,
                 y: Int,
                 z: Int): Int

    fun blockLight(x: Int,
                   y: Int,
                   z: Int): Int

    fun sunLightReduction(x: Int,
                          y: Int): Int

    fun highestBlockZAt(x: Int,
                        y: Int): Int

    fun highestTerrainBlockZAt(x: Int,
                               y: Int): Int

    fun isBlockLoaded(x: Int,
                      y: Int,
                      z: Int): Boolean

    fun isBlockTicking(x: Int,
                       y: Int,
                       z: Int): Boolean

    fun collisions(minX: Int,
                   minY: Int,
                   minZ: Int,
                   maxX: Int,
                   maxY: Int,
                   maxZ: Int,
                   pool: Pool<AABBElement>)

    fun pointerPanes(x: Int,
                     y: Int,
                     z: Int,
                     range: Int,
                     pool: Pool<PointerPane>)

    fun type(block: Long): BlockType

    fun data(block: Long): Int {
        return (block and 0xFFFFFFFF).toInt()
    }
}
