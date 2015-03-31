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
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.io.tag.TagStructureBinary;
import org.tobi29.scapes.entity.client.EntityClient;
import org.tobi29.scapes.entity.server.EntityServer;
import org.tobi29.scapes.server.connection.PlayerConnection;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class PacketEntityMetaData extends Packet implements PacketClient {
    private int entityID;
    private String category;
    private TagStructure tag;

    public PacketEntityMetaData() {
    }

    public PacketEntityMetaData(EntityServer entity, String category) {
        super(entity.getPos());
        entityID = entity.getEntityID();
        this.category = category;
        tag = entity.getMetaData(category);
    }

    @Override
    public void sendClient(PlayerConnection player, DataOutputStream streamOut)
            throws IOException {
        streamOut.writeInt(entityID);
        streamOut.writeUTF(category);
        TagStructureBinary.write(tag, streamOut);
    }

    @Override
    public void parseClient(ClientConnection client, DataInputStream streamIn)
            throws IOException {
        entityID = streamIn.readInt();
        category = streamIn.readUTF();
        tag = new TagStructure();
        TagStructureBinary.read(tag, streamIn);
    }

    @Override
    public void runClient(ClientConnection client, WorldClient world) {
        if (world == null) {
            return;
        }
        EntityClient entity = world.getEntity(entityID);
        if (entity != null) {
            entity.processPacket(this);
        }
    }

    public String getCategory() {
        return category;
    }

    public TagStructure getTag() {
        return tag;
    }
}
