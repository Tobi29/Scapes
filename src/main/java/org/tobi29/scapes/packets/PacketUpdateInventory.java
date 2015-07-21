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

package org.tobi29.scapes.packets;

import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.client.connection.ClientConnection;
import org.tobi29.scapes.engine.utils.io.ReadableByteStream;
import org.tobi29.scapes.engine.utils.io.WritableByteStream;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.io.tag.TagStructureBinary;
import org.tobi29.scapes.entity.client.EntityContainerClient;
import org.tobi29.scapes.entity.server.EntityContainerServer;
import org.tobi29.scapes.entity.server.EntityServer;
import org.tobi29.scapes.server.connection.PlayerConnection;

import java.io.IOException;

public class PacketUpdateInventory extends Packet implements PacketClient {
    private int entityID;
    private TagStructure tag;

    public PacketUpdateInventory() {
    }

    public PacketUpdateInventory(EntityContainerServer entity) {
        entityID = ((EntityServer) entity).entityID();
        tag = entity.inventory().save();
    }

    @Override
    public void sendClient(PlayerConnection player, WritableByteStream stream)
            throws IOException {
        stream.putInt(entityID);
        TagStructureBinary.write(tag, stream);
    }

    @Override
    public void parseClient(ClientConnection client, ReadableByteStream stream)
            throws IOException {
        entityID = stream.getInt();
        tag = new TagStructure();
        TagStructureBinary.read(tag, stream);
    }

    @Override
    public void runClient(ClientConnection client, WorldClient world) {
        if (world == null) {
            return;
        }
        world.entity(entityID).ifPresent(entity -> {
            if (entity instanceof EntityContainerClient) {
                EntityContainerClient entityContainer =
                        (EntityContainerClient) entity;
                entityContainer.inventory().load(tag);
            }
        });
    }
}
