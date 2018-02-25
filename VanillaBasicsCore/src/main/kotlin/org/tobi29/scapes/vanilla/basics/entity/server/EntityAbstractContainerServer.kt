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

import org.tobi29.scapes.block.InventoryContainer
import org.tobi29.scapes.chunk.WorldServer
import org.tobi29.scapes.chunk.terrain.Terrain
import org.tobi29.scapes.chunk.terrain.TerrainServer
import org.tobi29.math.vector.Vector3d
import org.tobi29.math.vector.plus
import org.tobi29.scapes.entity.EntityType
import org.tobi29.scapes.entity.ListenerToken
import org.tobi29.scapes.entity.server.EntityAbstractServer
import org.tobi29.scapes.packets.PacketUpdateInventory
import org.tobi29.scapes.vanilla.basics.util.dropItem

abstract class EntityAbstractContainerServer(
        type: EntityType<*, *>,
        world: WorldServer,
        pos: Vector3d
) : EntityAbstractServer(type, world, pos) {
    protected val inventories get() = this[InventoryContainer.COMPONENT]

    init {
        this[InventoryContainer.ON_UPDATE][CONTAINER_UPDATE_LISTENER_TOKEN] = { id ->
            world.send(PacketUpdateInventory(registry, this, id))
        }
    }

    override fun updateTile(terrain: TerrainServer,
                            x: Int,
                            y: Int,
                            z: Int,
                            data: Int) {
        val world = terrain.world
        if (!isValidOn(terrain, x, y, z)) {
            inventories.forEach { inventory ->
                for (i in 0 until inventory.size()) {
                    inventory[i]?.let {
                        world.dropItem(it, pos.now() + Vector3d(0.5, 0.5, 0.5))
                    }
                }
            }
            world.removeEntity(this)
        }
    }

    protected abstract fun isValidOn(terrain: Terrain,
                                     x: Int,
                                     y: Int,
                                     z: Int): Boolean
}

private val CONTAINER_UPDATE_LISTENER_TOKEN = ListenerToken(
        "VanillaBasics:ContainerUpdate")
