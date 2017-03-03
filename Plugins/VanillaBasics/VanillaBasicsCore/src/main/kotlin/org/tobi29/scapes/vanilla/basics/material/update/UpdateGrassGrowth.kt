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
import org.tobi29.scapes.block.GameRegistry
import org.tobi29.scapes.block.Update
import org.tobi29.scapes.block.UpdateType
import org.tobi29.scapes.chunk.terrain.TerrainServer
import org.tobi29.scapes.vanilla.basics.VanillaBasics
import org.tobi29.scapes.vanilla.basics.world.EnvironmentOverworldServer

class UpdateGrassGrowth(type: UpdateType) : Update(type) {
    constructor(registry: GameRegistry) : this(
            of(registry, "vanilla.basics.update.GrassGrowth"))

    override fun run(terrain: TerrainServer.TerrainMutable) {
        val plugin = terrain.world.plugins.plugin(
                "VanillaBasics") as VanillaBasics
        val materials = plugin.materials
        val type = terrain.type(x, y, z)
        if (type === materials.grass) {
            val environment = terrain.world.environment as EnvironmentOverworldServer
            val climateGenerator = environment.climate()
            val humidity = climateGenerator.humidity(x, y, z)
            if (humidity < 0.2) {
                terrain.type(x, y, z, materials.dirt)
            }
        } else if (type === materials.dirt) {
            val environment = terrain.world.environment as EnvironmentOverworldServer
            val climateGenerator = environment.climate()
            val humidity = climateGenerator.humidity(x, y, z)
            if (humidity > 0.2 && (terrain.blockLight(x, y,
                    z + 1) > 8 || terrain.sunLight(x, y, z + 1) > 8) &&
                    terrain.type(x, y, z + 1).isTransparent(terrain, x, y,
                            z + 1)) {
                terrain.typeData(x, y, z, materials.grass, 0.toShort().toInt())
            }
        }
    }

    override fun isValidOn(type: BlockType,
                           terrain: TerrainServer): Boolean {
        val plugin = terrain.world.plugins.plugin(
                "VanillaBasics") as VanillaBasics
        val materials = plugin.materials
        return type === materials.grass || type === materials.dirt
    }
}
