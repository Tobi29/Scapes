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

import org.tobi29.scapes.block.Inventory;
import org.tobi29.scapes.chunk.WorldServer;
import org.tobi29.scapes.chunk.terrain.TerrainServer;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;
import org.tobi29.scapes.entity.server.EntityContainerServer;
import org.tobi29.scapes.entity.server.EntityServer;
import org.tobi29.scapes.entity.server.MobPlayerServer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public abstract class EntityAbstractContainerServer extends EntityServer
        implements EntityContainerServer {
    protected final Inventory inventory;
    protected final List<MobPlayerServer> viewers = new ArrayList<>();

    protected EntityAbstractContainerServer(WorldServer world, Vector3 pos,
            Inventory inventory) {
        super(world, pos);
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public void addViewer(MobPlayerServer player) {
        if (!viewers.contains(player)) {
            viewers.add(player);
        }
    }

    @Override
    public Stream<MobPlayerServer> getViewers() {
        return viewers.stream();
    }

    @Override
    public void removeViewer(MobPlayerServer player) {
        viewers.remove(player);
    }

    @Override
    public TagStructure write() {
        TagStructure tag = super.write();
        tag.setStructure("Inventory", inventory.save());
        return tag;
    }

    @Override
    public void read(TagStructure tagStructure) {
        super.read(tagStructure);
        inventory.load(tagStructure.getStructure("Inventory"));
    }

    @Override
    public void updateTile(TerrainServer terrain, int x, int y, int z) {
        WorldServer world = terrain.getWorld();
        if (!isValidOn(terrain, x, y, z)) {
            synchronized (this) {
                for (int i = 0; i < inventory.getSize(); i++) {
                    world.dropItem(inventory.getItem(i),
                            pos.now().plus(new Vector3d(0.5, 0.5, 0.5)));
                }
            }
            world.deleteEntity(this);
        }
    }

    protected abstract boolean isValidOn(TerrainServer terrain, int x, int y,
            int z);
}
