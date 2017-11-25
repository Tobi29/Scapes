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
import org.tobi29.scapes.block.InventoryContainer
import org.tobi29.scapes.chunk.WorldServer
import org.tobi29.scapes.chunk.terrain.Terrain
import org.tobi29.scapes.chunk.terrain.TerrainServer
import org.tobi29.scapes.engine.math.vector.Vector3d
import org.tobi29.scapes.engine.math.vector.plus
import org.tobi29.scapes.engine.utils.readOnly
import org.tobi29.scapes.engine.utils.tag.ReadWriteTagMap
import org.tobi29.scapes.engine.utils.tag.TagMap
import org.tobi29.scapes.engine.utils.tag.toMap
import org.tobi29.scapes.entity.EntityType
import org.tobi29.scapes.entity.server.EntityAbstractServer
import org.tobi29.scapes.entity.server.EntityContainerServer
import org.tobi29.scapes.entity.server.MobPlayerServer
import org.tobi29.scapes.packets.PacketUpdateInventory
import org.tobi29.scapes.vanilla.basics.util.dropItem

abstract class EntityAbstractContainerServer(type: EntityType<*, *>,
                                             world: WorldServer,
                                             pos: Vector3d,
                                             inventory: Inventory) : EntityAbstractServer(
        type, world, pos), EntityContainerServer {
    protected val inventories: InventoryContainer
    protected val viewersMut = ArrayList<MobPlayerServer>()
    override val viewers = viewersMut.readOnly()

    init {
        inventories = InventoryContainer { id ->
            world.send(PacketUpdateInventory(registry, this, id))
        }
        inventories.add("Container", inventory)
    }

    override fun inventories(): InventoryContainer {
        return inventories
    }

    override fun addViewer(player: MobPlayerServer) {
        if (!viewers.contains(player)) {
            viewersMut.add(player)
        }
    }

    override fun removeViewer(player: MobPlayerServer) {
        viewersMut.remove(player)
    }

    override fun write(map: ReadWriteTagMap) {
        super.write(map)
        map["Inventory"] = TagMap {
            inventories.forEach { id, inventory ->
                this[id] = TagMap { inventory.write(this) }
            }
        }
    }

    override fun read(map: TagMap) {
        super.read(map)
        map["Inventory"]?.toMap()?.let { inventoryTag ->
            inventories.forEach { id, inventory ->
                inventoryTag[id]?.toMap()?.let { inventory.read(it) }
            }
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
                    world.dropItem(inventory.item(i),
                            pos.now() + Vector3d(0.5, 0.5, 0.5))
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
