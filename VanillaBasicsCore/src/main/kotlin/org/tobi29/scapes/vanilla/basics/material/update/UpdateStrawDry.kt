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

package org.tobi29.scapes.vanilla.basics.material.update

import org.tobi29.scapes.block.BlockType
import org.tobi29.scapes.block.Registries
import org.tobi29.scapes.block.Update
import org.tobi29.scapes.block.UpdateType
import org.tobi29.scapes.chunk.terrain.TerrainServer
import org.tobi29.scapes.chunk.terrain.modify
import org.tobi29.scapes.vanilla.basics.VanillaBasics

class UpdateStrawDry(type: UpdateType) : Update(type) {
    constructor(registry: Registries) : this(
            of(registry, "vanilla.basics.update.StrawDry"))

    override fun run(terrain: TerrainServer) {
        val world = terrain.world
        val plugin = world.plugins.plugin("VanillaBasics") as VanillaBasics
        val materials = plugin.materials
        terrain.modify(materials.straw, x, y, z) { terrain ->
            val block = terrain.block(x, y, z)
            if (terrain.data(block) == 0) {
                terrain.data(x, y, z, 1)
            }
        }
    }

    override fun isValidOn(type: BlockType,
                           terrain: TerrainServer): Boolean {
        val plugin = terrain.world.plugins.plugin(
                "VanillaBasics") as VanillaBasics
        val materials = plugin.materials
        return type == materials.straw
    }
}
