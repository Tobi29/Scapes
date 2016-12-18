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

package scapes.plugin.tobi29.vanilla.basics.entity.server

import org.tobi29.scapes.block.Inventory
import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.chunk.WorldServer
import org.tobi29.scapes.chunk.terrain.TerrainServer
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.packets.PacketEntityChange
import scapes.plugin.tobi29.vanilla.basics.VanillaBasics
import scapes.plugin.tobi29.vanilla.basics.material.item.ItemIngot

class EntityForgeServer(world: WorldServer, pos: Vector3d = Vector3d.ZERO) : EntityAbstractFurnaceServer(
        world, pos, Inventory(world.registry, 9), 4, 3,
        Float.POSITIVE_INFINITY, 1.006f, 10, 50) {

    override fun update(delta: Double) {
        super.update(delta)
        val plugin = world.plugins.plugin("VanillaBasics") as VanillaBasics
        val materials = plugin.materials
        inventories.modify("Container") { inventory ->
            val max = items + fuel.size + 1
            for (i in fuel.size + 1..max - 1) {
                val item = inventory.item(i)
                if (item.amount() == 1) {
                    val type = item.material()
                    if (type is ItemIngot) {
                        if (type.temperature(item) >= type.meltingPoint(
                                item) && item.data() == 1) {
                            if (inventory.item(8).take(
                                    ItemStack(materials.mold, 1)) != null) {
                                item.setData(0)
                                world.send(PacketEntityChange(this))
                            }
                        }
                    }
                }
            }
        }
        val xx = pos.intX()
        val yy = pos.intY()
        val zz = pos.intZ()
        val blockOff = materials.forge.block(0)
        val blockOn = materials.forge.block(1)
        if (temperature > 10) {
            if (world.terrain.block(xx, yy, zz) == blockOff) {
                world.terrain.queue { handle ->
                    if (handle.block(xx, yy, zz) == blockOff) {
                        handle.block(xx, yy, zz, blockOn)
                    }
                }
            }
        } else if (world.terrain.block(xx, yy, zz) == blockOn) {
            world.terrain.queue { handle ->
                if (handle.block(xx, yy, zz) == blockOn) {
                    handle.block(xx, yy, zz, blockOff)
                }
            }
        }
    }

    override fun isValidOn(terrain: TerrainServer,
                           x: Int,
                           y: Int,
                           z: Int): Boolean {
        val plugin = terrain.world.plugins.plugin(
                "VanillaBasics") as VanillaBasics
        val materials = plugin.materials
        return terrain.type(x, y, z) === materials.forge
    }
}
