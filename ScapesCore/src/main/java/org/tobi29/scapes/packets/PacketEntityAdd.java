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
package org.tobi29.scapes.packets;

import java8.util.Optional;
import org.tobi29.scapes.block.GameRegistry;
import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.client.connection.ClientConnection;
import org.tobi29.scapes.engine.utils.io.ReadableByteStream;
import org.tobi29.scapes.engine.utils.io.WritableByteStream;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.io.tag.binary.TagStructureBinary;
import org.tobi29.scapes.entity.client.EntityClient;
import org.tobi29.scapes.entity.server.EntityServer;
import org.tobi29.scapes.server.connection.PlayerConnection;

import java.io.IOException;
import java.util.UUID;

public class PacketEntityAdd extends PacketAbstract implements PacketClient {
    private UUID uuid;
    private int id;
    private TagStructure tag;

    public PacketEntityAdd() {
    }

    public PacketEntityAdd(EntityServer entity, GameRegistry registry) {
        super(entity.pos());
        uuid = entity.uuid();
        id = entity.id(registry);
        tag = entity.write();
    }

    @Override
    public void sendClient(PlayerConnection player, WritableByteStream stream)
            throws IOException {
        stream.putLong(uuid.getMostSignificantBits());
        stream.putLong(uuid.getLeastSignificantBits());
        stream.putInt(id);
        TagStructureBinary.write(tag, stream);
    }

    @Override
    public void parseClient(ClientConnection client, ReadableByteStream stream)
            throws IOException {
        uuid = new UUID(stream.getLong(), stream.getLong());
        id = stream.getInt();
        tag = new TagStructure();
        TagStructureBinary.read(tag, stream);
    }

    @Override
    public void localClient() {
        tag = tag.copy();
    }

    @Override
    public void runClient(ClientConnection client, WorldClient world) {
        if (world == null) {
            return;
        }
        Optional<EntityClient> fetch = world.entity(uuid);
        if (fetch.isPresent()) {
            EntityClient entity = fetch.get();
            entity.read(tag);
        } else {
            EntityClient entity = EntityClient.make(id, world);
            if (entity != null) {
                entity.setEntityID(uuid);
                entity.read(tag);
                client.world().terrain().addEntity(entity);
            }
        }
    }
}
