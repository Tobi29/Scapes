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

import org.tobi29.math.vector.Vector3d
import org.tobi29.scapes.block.copy
import org.tobi29.scapes.block.data
import org.tobi29.scapes.chunk.WorldServer
import org.tobi29.scapes.chunk.terrain.Terrain
import org.tobi29.scapes.entity.EntityType
import org.tobi29.scapes.inventory.*
import org.tobi29.scapes.vanilla.basics.VanillaBasics
import org.tobi29.scapes.vanilla.basics.material.item.ItemIngot
import org.tobi29.scapes.vanilla.basics.material.meltingPoint
import org.tobi29.scapes.vanilla.basics.material.temperature
import org.tobi29.stdex.math.floorToInt

class EntityForgeServer(
        type: EntityType<*, *>,
        world: WorldServer
) : EntityAbstractFurnaceServer(type, world, Vector3d.ZERO, 4, 3,
        Double.POSITIVE_INFINITY, 1.006, 5.0, 50, { inventory, item ->
    if (item.amount == 1) {
        item.kind<ItemIngot>()?.let { item ->
            if (item.temperature >= item.meltingPoint && item.data == 1) {
                val plugin = world.plugins.plugin<VanillaBasics>()
                val materials = plugin.materials
                val (remaining, taken) = inventory[8].takeAll(
                        Item(materials.mold))
                if (!taken.isEmpty()) {
                    inventory[8] = remaining
                    item.copy(data = 0)
                } else item
            } else item
        } ?: item
    } else item
}) {
    init {
        inventories.add("Container", 9)
    }

    override fun update(delta: Double) {
        super.update(delta)
        val plugin = world.plugins.plugin<VanillaBasics>()
        val materials = plugin.materials
        val xx = pos.x.floorToInt()
        val yy = pos.y.floorToInt()
        val zz = pos.z.floorToInt()
        val blockOff = materials.forge.block(0)
        val blockOn = materials.forge.block(1)
        if (temperature > 80.0) {
            if (world.terrain.block(xx, yy, zz) == blockOff) {
                world.terrain.modify(xx, yy, zz) { handle ->
                    if (handle.block(xx, yy, zz) == blockOff) {
                        handle.block(xx, yy, zz, blockOn)
                    }
                }
            }
        } else if (world.terrain.block(xx, yy, zz) == blockOn) {
            world.terrain.modify(xx, yy, zz) { handle ->
                if (handle.block(xx, yy, zz) == blockOn) {
                    handle.block(xx, yy, zz, blockOff)
                }
            }
        }
    }

    override fun isValidOn(terrain: Terrain,
                           x: Int,
                           y: Int,
                           z: Int): Boolean {
        val plugin = world.plugins.plugin<VanillaBasics>()
        val materials = plugin.materials
        return terrain.type(x, y, z) == materials.forge
    }
}
