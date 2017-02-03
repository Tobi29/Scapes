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

package scapes.plugin.tobi29.vanilla.basics.entity.server

import org.tobi29.scapes.block.Inventory
import org.tobi29.scapes.chunk.WorldServer
import org.tobi29.scapes.chunk.terrain.TerrainServer
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import scapes.plugin.tobi29.vanilla.basics.VanillaBasics

class EntityAnvilServer constructor(world: WorldServer,
                                    pos: Vector3d = Vector3d.ZERO) : EntityAbstractContainerServer(
        world, pos, Inventory(world.registry, 2)) {

    public override fun isValidOn(terrain: TerrainServer,
                                  x: Int,
                                  y: Int,
                                  z: Int): Boolean {
        val plugin = terrain.world.plugins.plugin(
                "VanillaBasics") as VanillaBasics
        val materials = plugin.materials
        return terrain.type(x, y, z) === materials.anvil
    }
}
