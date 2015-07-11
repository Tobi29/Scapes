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
import org.tobi29.scapes.chunk.WorldServer;
import org.tobi29.scapes.chunk.terrain.infinite.TerrainInfiniteClient;
import org.tobi29.scapes.client.connection.ClientConnection;
import org.tobi29.scapes.engine.utils.graphics.Cam;
import org.tobi29.scapes.engine.utils.io.ReadableByteStream;
import org.tobi29.scapes.engine.utils.io.WritableByteStream;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.io.tag.TagStructureBinary;
import org.tobi29.scapes.entity.server.MobPlayerServer;
import org.tobi29.scapes.server.connection.PlayerConnection;

import java.io.IOException;

public class PacketSetWorld extends Packet implements PacketClient {
    private TagStructure tag;
    private long seed;
    private String name;
    private int entityID;

    public PacketSetWorld() {
    }

    public PacketSetWorld(WorldServer world, MobPlayerServer player) {
        tag = player.write();
        seed = world.getSeed();
        name = world.getName();
        entityID = player.getEntityID();
    }

    @Override
    public void sendClient(PlayerConnection player, WritableByteStream stream)
            throws IOException {
        TagStructureBinary.write(tag, stream);
        stream.putLong(seed);
        stream.putString(name);
        stream.putInt(entityID);
    }

    @Override
    public void parseClient(ClientConnection client, ReadableByteStream stream)
            throws IOException {
        tag = new TagStructure();
        TagStructureBinary.read(tag, stream);
        seed = stream.getLong();
        name = stream.getString();
        entityID = stream.getInt();
    }

    @Override
    public void runClient(ClientConnection client, WorldClient world) {
        client.changeWorld(new WorldClient(client,
                new Cam(0.01f, client.getLoadingRadius()), seed, name,
                newWorld -> new TerrainInfiniteClient(newWorld,
                        client.getLoadingRadius() >> 4, 512,
                        client.getGame().getEngine().taskExecutor()), tag,
                entityID));
    }
}
