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
package org.tobi29.scapes.entity.client;

import java8.util.Optional;
import org.tobi29.scapes.block.GameRegistry;
import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.chunk.WorldServer;
import org.tobi29.scapes.engine.utils.ListenerOwner;
import org.tobi29.scapes.engine.utils.ListenerOwnerHandle;
import org.tobi29.scapes.engine.utils.io.tag.MultiTag;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.math.vector.MutableVector3d;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.entity.Entity;
import org.tobi29.scapes.entity.model.EntityModel;
import org.tobi29.scapes.entity.server.EntityServer;
import org.tobi29.scapes.packets.PacketEntityMetaData;

import java.util.UUID;

public class EntityClient implements Entity, MultiTag.Readable, ListenerOwner {
    protected final WorldClient world;
    protected final GameRegistry registry;
    protected final MutableVector3d pos;
    private final ListenerOwnerHandle listenerOwner;
    protected UUID uuid;
    protected TagStructure metaData = new TagStructure();

    protected EntityClient(WorldClient world, Vector3 pos) {
        this.world = world;
        registry = world.registry();
        this.pos = new MutableVector3d(pos);
        listenerOwner = new ListenerOwnerHandle(
                () -> !world.disposed() && world.hasEntity(this));
    }

    public static EntityClient make(int id, WorldClient world) {
        return world
                .registry().<WorldServer, EntityServer, WorldClient, EntityClient>getAsymSupplier(
                        "Core", "Entity").get(id).b.apply(world);
    }

    @Override
    public UUID uuid() {
        return uuid;
    }

    public Vector3 pos() {
        return pos.now();
    }

    @Override
    public double x() {
        return pos.doubleX();
    }

    @Override
    public double y() {
        return pos.doubleY();
    }

    @Override
    public double z() {
        return pos.doubleZ();
    }

    public void setEntityID(UUID uuid) {
        this.uuid = uuid;
    }

    public WorldClient world() {
        return world;
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

    public Optional<? extends EntityModel> createModel() {
        return Optional.empty();
    }

    public void processPacket(PacketEntityMetaData packet) {
        metaData.setStructure(packet.category(), packet.tagStructure());
    }

    @Override
    public ListenerOwnerHandle owner() {
        return listenerOwner;
    }

    public interface Supplier {
        EntityClient get(WorldClient world);
    }
}
