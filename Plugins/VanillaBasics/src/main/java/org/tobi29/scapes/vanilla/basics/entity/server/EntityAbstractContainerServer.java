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
package org.tobi29.scapes.vanilla.basics.entity.server;

import java8.util.stream.Stream;
import org.tobi29.scapes.block.Inventory;
import org.tobi29.scapes.block.InventoryContainer;
import org.tobi29.scapes.chunk.WorldServer;
import org.tobi29.scapes.chunk.terrain.TerrainServer;
import org.tobi29.scapes.engine.utils.Streams;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;
import org.tobi29.scapes.entity.server.EntityContainerServer;
import org.tobi29.scapes.entity.server.EntityServer;
import org.tobi29.scapes.entity.server.MobPlayerServer;
import org.tobi29.scapes.packets.PacketUpdateInventory;

import java.util.ArrayList;
import java.util.List;

public abstract class EntityAbstractContainerServer extends EntityServer
        implements EntityContainerServer {
    protected final InventoryContainer inventories;
    protected final List<MobPlayerServer> viewers = new ArrayList<>();

    protected EntityAbstractContainerServer(WorldServer world, Vector3 pos,
            Inventory inventory) {
        super(world, pos);
        inventories = new InventoryContainer(
                id -> world.send(new PacketUpdateInventory(this, id)));
        inventories.add("Container", inventory);
    }

    @Override
    public InventoryContainer inventories() {
        return inventories;
    }

    @Override
    public void addViewer(MobPlayerServer player) {
        if (!viewers.contains(player)) {
            viewers.add(player);
        }
    }

    @Override
    public Stream<MobPlayerServer> viewers() {
        return Streams.of(viewers);
    }

    @Override
    public void removeViewer(MobPlayerServer player) {
        viewers.remove(player);
    }

    @Override
    public TagStructure write() {
        TagStructure tagStructure = super.write();
        TagStructure inventoryTag = tagStructure.getStructure("Inventory");
        inventories.forEach((id, inventory) -> inventoryTag
                .setStructure(id, inventory.save()));
        return tagStructure;
    }

    @Override
    public void read(TagStructure tagStructure) {
        super.read(tagStructure);
        TagStructure inventoryTag = tagStructure.getStructure("Inventory");
        inventories.forEach((id, inventory) -> inventory
                .load(inventoryTag.getStructure(id)));
    }

    @Override
    public void updateTile(TerrainServer terrain, int x, int y, int z) {
        WorldServer world = terrain.world();
        if (!isValidOn(terrain, x, y, z)) {
            inventories.forEach(inventory -> {
                for (int i = 0; i < inventory.size(); i++) {
                    world.dropItem(inventory.item(i),
                            pos.now().plus(new Vector3d(0.5, 0.5, 0.5)));
                }
            });
            world.removeEntity(this);
        }
    }

    protected abstract boolean isValidOn(TerrainServer terrain, int x, int y,
            int z);
}
