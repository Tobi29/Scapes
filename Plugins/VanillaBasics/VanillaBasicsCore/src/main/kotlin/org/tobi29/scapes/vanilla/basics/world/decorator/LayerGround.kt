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

package org.tobi29.scapes.vanilla.basics.world.decorator

import org.tobi29.scapes.block.BlockType
import org.tobi29.scapes.chunk.terrain.TerrainServer
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial
import java.util.*

class LayerGround(private val material: BlockType,
                  private val data: (TerrainServer.TerrainMutable, Int, Int, Int, Random) -> Int,
                  private val chance: Int,
                  private val check: (TerrainServer.TerrainMutable, Int, Int, Int) -> Boolean) : BiomeDecorator.Layer {

    override fun decorate(terrain: TerrainServer.TerrainMutable,
                          x: Int,
                          y: Int,
                          materials: VanillaMaterial,
                          random: Random) {
        if (random.nextInt(chance) == 0) {
            val z = terrain.highestTerrainBlockZAt(x, y)
            if (check(terrain, x, y, z)) {
                terrain.typeData(x, y, z, material,
                        data(terrain, x, y, z, random))
            }
        }
    }
}