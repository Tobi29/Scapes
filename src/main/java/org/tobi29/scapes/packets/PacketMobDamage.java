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
import org.tobi29.scapes.entity.client.EntityClient;
import org.tobi29.scapes.entity.client.MobLivingClient;
import org.tobi29.scapes.entity.server.MobLivingServer;
import org.tobi29.scapes.server.connection.PlayerConnection;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class PacketMobDamage extends Packet implements PacketClient {
    private int entityId;
    private double lives, maxLives;

    public PacketMobDamage() {
    }

    public PacketMobDamage(MobLivingServer entity) {
        super(entity.getPos());
        entityId = entity.getEntityID();
        lives = entity.getLives();
        maxLives = entity.getMaxLives();
    }

    public double getLives() {
        return lives;
    }

    public double getMaxLives() {
        return maxLives;
    }

    @Override
    public void sendClient(PlayerConnection player, DataOutputStream streamOut)
            throws IOException {
        streamOut.writeInt(entityId);
        streamOut.writeDouble(lives);
        streamOut.writeDouble(maxLives);
    }

    @Override
    public void parseClient(ClientConnection client, DataInputStream streamIn)
            throws IOException {
        entityId = streamIn.readInt();
        lives = streamIn.readDouble();
        maxLives = streamIn.readDouble();
    }

    @Override
    public void runClient(ClientConnection client, WorldClient world) {
        if (world == null) {
            return;
        }
        EntityClient entity = world.getEntity(entityId);
        if (entity instanceof MobLivingClient) {
            ((MobLivingClient) entity).processPacket(this);
        }
    }
}
