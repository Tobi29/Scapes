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
import org.tobi29.scapes.block.InventoryContainer
import org.tobi29.scapes.chunk.WorldServer
import org.tobi29.scapes.chunk.terrain.TerrainServer
import org.tobi29.scapes.engine.utils.io.tag.TagStructure
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.engine.utils.math.vector.plus
import org.tobi29.scapes.engine.utils.readOnly
import org.tobi29.scapes.entity.server.EntityContainerServer
import org.tobi29.scapes.entity.server.EntityServer
import org.tobi29.scapes.entity.server.MobPlayerServer
import org.tobi29.scapes.packets.PacketUpdateInventory
import java.util.*

abstract class EntityAbstractContainerServer(world: WorldServer, pos: Vector3d,
                                             inventory: Inventory) : EntityServer(
        world, pos), EntityContainerServer {
    protected val inventories: InventoryContainer
    protected val viewersMut = ArrayList<MobPlayerServer>()
    override val viewers = viewersMut.readOnly()

    init {
        inventories = InventoryContainer { id ->
            world.send(PacketUpdateInventory(this, id))
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

    override fun write(): TagStructure {
        val tagStructure = super.write()
        val inventoryTag = tagStructure.structure("Inventory")
        inventories.forEach { id, inventory ->
            inventoryTag.setStructure(id, inventory.save())
        }
        return tagStructure
    }

    override fun read(tagStructure: TagStructure) {
        super.read(tagStructure)
        tagStructure.getStructure("Inventory")?.let { inventoryTag ->
            inventories.forEach { id, inventory ->
                inventoryTag.getStructure(id)?.let { inventory.load(it) }
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
                for (i in 0..inventory.size() - 1) {
                    world.dropItem(inventory.item(i),
                            pos.now().plus(Vector3d(0.5, 0.5, 0.5)))
                }
            }
            world.removeEntity(this)
        }
    }

    protected abstract fun isValidOn(terrain: TerrainServer,
                                     x: Int,
                                     y: Int,
                                     z: Int): Boolean
}
