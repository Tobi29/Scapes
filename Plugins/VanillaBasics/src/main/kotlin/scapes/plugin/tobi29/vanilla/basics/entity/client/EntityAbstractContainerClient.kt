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

package scapes.plugin.tobi29.vanilla.basics.entity.client

import org.tobi29.scapes.block.Inventory
import org.tobi29.scapes.block.InventoryContainer
import org.tobi29.scapes.chunk.WorldClient
import org.tobi29.scapes.engine.utils.io.tag.TagStructure
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.entity.client.EntityClient
import org.tobi29.scapes.entity.client.EntityContainerClient

abstract class EntityAbstractContainerClient(world: WorldClient,
                                             pos: Vector3d,
                                             inventory: Inventory) : EntityClient(
        world, pos), EntityContainerClient {
    protected val inventories: InventoryContainer

    init {
        inventories = InventoryContainer()
        inventories.add("Container", inventory)
    }

    override fun inventories(): InventoryContainer {
        return inventories
    }

    override fun read(tagStructure: TagStructure) {
        super.read(tagStructure)
        tagStructure.getStructure("Inventory")?.let { inventoryTag ->
            inventories.forEach { id, inventory ->
                inventoryTag.getStructure(id)?.let { inventory.load(it) }
            }
        }
    }
}
