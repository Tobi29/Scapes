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
import org.tobi29.scapes.client.connection.ClientConnection;
import org.tobi29.scapes.engine.utils.io.ReadableByteStream;
import org.tobi29.scapes.engine.utils.io.WritableByteStream;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.entity.MobileEntity;
import org.tobi29.scapes.server.connection.PlayerConnection;

import java.io.IOException;

public class PacketMobChangeRot extends Packet
        implements PacketServer, PacketClient {
    private int entityID;
    private float x, y, z;

    public PacketMobChangeRot() {
    }

    public PacketMobChangeRot(int entityID, Vector3 pos, float x, float y,
            float z) {
        super(pos, 0.0, false, false);
        this.entityID = entityID;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public void sendClient(PlayerConnection player, WritableByteStream stream)
            throws IOException {
        stream.putInt(entityID);
        stream.putFloat(x);
        stream.putFloat(y);
        stream.putFloat(z);
    }

    @Override
    public void parseClient(ClientConnection client, ReadableByteStream stream)
            throws IOException {
        entityID = stream.getInt();
        x = stream.getFloat();
        y = stream.getFloat();
        z = stream.getFloat();
    }

    @Override
    public void runClient(ClientConnection client, WorldClient world) {
        if (world == null) {
            return;
        }
        world.entity(entityID).ifPresent(entity -> {
            if (entity instanceof MobileEntity) {
                ((MobileEntity) entity).positionHandler()
                        .receiveRotation(x, y, z);
            }
        });
    }

    @Override
    public void sendServer(ClientConnection client, WritableByteStream stream)
            throws IOException {
        stream.putFloat(x);
        stream.putFloat(y);
        stream.putFloat(z);
    }

    @Override
    public void parseServer(PlayerConnection player, ReadableByteStream stream)
            throws IOException {
        x = stream.getFloat();
        y = stream.getFloat();
        z = stream.getFloat();
    }

    @Override
    public void runServer(PlayerConnection player, WorldServer world) {
        player.mob().positionHandler().receiveRotation(x, y, z);
    }
}
