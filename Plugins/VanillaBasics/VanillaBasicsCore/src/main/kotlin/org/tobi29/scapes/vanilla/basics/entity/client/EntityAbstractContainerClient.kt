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

package org.tobi29.scapes.vanilla.basics.entity.client

import org.tobi29.scapes.block.Inventory
import org.tobi29.scapes.block.InventoryContainer
import org.tobi29.scapes.chunk.WorldClient
import org.tobi29.scapes.engine.utils.io.tag.TagMap
import org.tobi29.scapes.engine.utils.io.tag.toMap
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.entity.EntityType
import org.tobi29.scapes.entity.client.EntityAbstractClient
import org.tobi29.scapes.entity.client.EntityContainerClient

abstract class EntityAbstractContainerClient(type: EntityType<*, *>,
                                             world: WorldClient,
                                             pos: Vector3d,
                                             inventory: Inventory) : EntityAbstractClient(
        type, world, pos), EntityContainerClient {
    protected val inventories: InventoryContainer

    init {
        inventories = InventoryContainer()
        inventories.add("Container", inventory)
    }

    override fun inventories(): InventoryContainer {
        return inventories
    }

    override fun read(map: TagMap) {
        super.read(map)
        map["Inventory"]?.toMap()?.let { inventoryTag ->
            inventories.forEach { id, inventory ->
                inventoryTag[id]?.toMap()?.let {
                    inventory.read(it)
                }
            }
        }
    }
}
