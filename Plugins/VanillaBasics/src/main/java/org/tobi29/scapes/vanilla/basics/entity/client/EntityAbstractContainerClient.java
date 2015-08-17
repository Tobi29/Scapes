/*
 * Copyright 2012-2015 Tobi29
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

package org.tobi29.scapes.vanilla.basics.entity.client;

import org.tobi29.scapes.block.Inventory;
import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.entity.client.EntityClient;
import org.tobi29.scapes.entity.client.EntityContainerClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class EntityAbstractContainerClient extends EntityClient
        implements EntityContainerClient {
    protected final Inventory inventory;
    protected final Map<String, Inventory> inventories =
            new ConcurrentHashMap<>();

    protected EntityAbstractContainerClient(WorldClient world, Vector3 pos,
            Inventory inventory) {
        super(world, pos);
        this.inventory = inventory;
        inventories.put("Container", inventory);
    }

    @Override
    public Inventory inventory(String id) {
        return inventories.get(id);
    }

    @Override
    public void read(TagStructure tagStructure) {
        super.read(tagStructure);
        TagStructure inventoryTag = tagStructure.getStructure("Inventory");
        inventories.forEach((id, inventory) -> inventory
                .load(inventoryTag.getStructure(id)));
    }
}