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
package org.tobi29.scapes.entity.server;

import org.tobi29.scapes.block.GameRegistry;
import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.chunk.WorldServer;
import org.tobi29.scapes.chunk.terrain.TerrainServer;
import org.tobi29.scapes.engine.utils.Streams;
import org.tobi29.scapes.engine.utils.io.tag.MultiTag;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.math.vector.MutableVector3d;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.entity.client.EntityClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EntityServer implements MultiTag.ReadAndWrite {
    protected final Map<String, SpawnListener> spawnListeners =
            new ConcurrentHashMap<>();
    protected final WorldServer world;
    protected final GameRegistry registry;
    protected final MutableVector3d pos;
    protected int entityID;
    protected TagStructure metaData = new TagStructure();

    protected EntityServer(WorldServer world, Vector3 pos) {
        this.world = world;
        registry = world.registry();
        this.pos = new MutableVector3d(pos);
    }

    public static EntityServer make(int id, WorldServer world) {
        return world
                .registry().<WorldServer, EntityServer, WorldClient, EntityClient>getAsymSupplier(
                        "Core", "Entity").get(id).a.apply(world);
    }

    public int entityID() {
        return entityID;
    }

    public void setEntityID(int entityID) {
        this.entityID = entityID;
    }

    public int id(GameRegistry registry) {
        return registry.getAsymSupplier("Core", "Entity").id(this);
    }

    public WorldServer world() {
        return world;
    }

    public Vector3 pos() {
        return pos.now();
    }

    public double x() {
        return pos.doubleX();
    }

    public double y() {
        return pos.doubleY();
    }

    public double z() {
        return pos.doubleZ();
    }

    @Override
    public TagStructure write() {
        TagStructure tag = new TagStructure();
        tag.setMultiTag("Pos", pos);
        tag.setStructure("MetaData", metaData);
        return tag;
    }

    @Override
    public void read(TagStructure tagStructure) {
        tagStructure.getMultiTag("Pos", pos);
        metaData = tagStructure.getStructure("MetaData");
    }

    public TagStructure metaData(String category) {
        return metaData.getStructure(category);
    }

    public void update(double delta) {
    }

    public void updateTile(TerrainServer terrain, int x, int y, int z) {
    }

    public void tickSkip(long oldTick, long newTick) {
    }

    public void listener(Listener listener) {
        listener("Local", listener);
    }

    public void listener(String id, Listener listener) {
        if (listener instanceof SpawnListener) {
            spawnListeners.put(id, (SpawnListener) listener);
        }
    }

    public void onSpawn() {
        world.entityListeners().forEach(listener -> listener.listen(this));
        Streams.forEach(spawnListeners.values(), SpawnListener::onSpawn);
    }

    public void onUnload() {
    }

    public interface SpawnListener extends Listener {
        void onSpawn();
    }

    public interface Listener {
    }

    public interface Supplier {
        EntityServer get(WorldServer world);
    }
}
