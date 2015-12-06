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
import org.tobi29.scapes.chunk.WorldServer;
import org.tobi29.scapes.chunk.terrain.TerrainServer;
import org.tobi29.scapes.engine.utils.Pair;
import org.tobi29.scapes.engine.utils.Streams;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;
import org.tobi29.scapes.entity.server.EntityContainerServer;
import org.tobi29.scapes.entity.server.EntityServer;
import org.tobi29.scapes.entity.server.MobPlayerServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class EntityAbstractContainerServer extends EntityServer
        implements EntityContainerServer {
    protected final Inventory inventory;
    protected final Map<String, Inventory> inventories =
            new ConcurrentHashMap<>();
    protected final List<MobPlayerServer> viewers = new ArrayList<>();

    protected EntityAbstractContainerServer(WorldServer world, Vector3 pos,
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
    public Stream<Pair<String, Inventory>> inventories() {
        return Streams.of(inventories.entrySet())
                .map(entry -> new Pair<>(entry.getKey(), entry.getValue()));
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
        Streams.of(inventories.entrySet()).forEach(entry -> inventoryTag
                .setStructure(entry.getKey(), entry.getValue().save()));
        return tagStructure;
    }

    @Override
    public void read(TagStructure tagStructure) {
        super.read(tagStructure);
        TagStructure inventoryTag = tagStructure.getStructure("Inventory");
        Streams.of(inventories.entrySet()).forEach(entry -> entry.getValue()
                .load(inventoryTag.getStructure(entry.getKey())));
    }

    @Override
    public void updateTile(TerrainServer terrain, int x, int y, int z) {
        WorldServer world = terrain.world();
        if (!isValidOn(terrain, x, y, z)) {
            synchronized (this) {
                for (int i = 0; i < inventory.size(); i++) {
                    world.dropItem(inventory.item(i),
                            pos.now().plus(new Vector3d(0.5, 0.5, 0.5)));
                }
            }
            world.deleteEntity(this);
        }
    }

    protected abstract boolean isValidOn(TerrainServer terrain, int x, int y,
            int z);
}
