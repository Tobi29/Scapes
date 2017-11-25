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

package org.tobi29.scapes.vanilla.basics.entity.server

import org.tobi29.scapes.block.Inventory
import org.tobi29.scapes.chunk.WorldServer
import org.tobi29.scapes.chunk.terrain.Terrain
import org.tobi29.scapes.engine.math.vector.Vector3d
import org.tobi29.scapes.entity.EntityType
import org.tobi29.scapes.vanilla.basics.VanillaBasics

class EntityFurnaceServer(type: EntityType<*, *>,
                          world: WorldServer) : EntityAbstractFurnaceServer(
        type, world, Vector3d.ZERO, Inventory(world.plugins, 8), 4, 3, 800.0,
        1.001, 3.0, 0) {

    public override fun isValidOn(terrain: Terrain,
                                  x: Int,
                                  y: Int,
                                  z: Int): Boolean {
        val plugin = world.plugins.plugin("VanillaBasics") as VanillaBasics
        val materials = plugin.materials
        return terrain.type(x, y, z) == materials.furnace
    }
}
