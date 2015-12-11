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

import java8.util.Optional;
import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.client.connection.ClientConnection;
import org.tobi29.scapes.engine.utils.io.ReadableByteStream;
import org.tobi29.scapes.engine.utils.io.WritableByteStream;
import org.tobi29.scapes.entity.client.EntityClient;
import org.tobi29.scapes.entity.client.MobLivingClient;
import org.tobi29.scapes.entity.server.MobLivingServer;
import org.tobi29.scapes.server.connection.PlayerConnection;

import java.io.IOException;

public class PacketMobDamage extends Packet implements PacketClient {
    private int entityID;
    private double health, maxHealth;

    public PacketMobDamage() {
    }

    public PacketMobDamage(MobLivingServer entity) {
        super(entity.pos());
        entityID = entity.entityID();
        health = entity.health();
        maxHealth = entity.maxHealth();
    }

    public double health() {
        return health;
    }

    public double maxHealth() {
        return maxHealth;
    }

    @Override
    public void sendClient(PlayerConnection player, WritableByteStream stream)
            throws IOException {
        stream.putInt(entityID);
        stream.putDouble(health);
        stream.putDouble(maxHealth);
    }

    @Override
    public void parseClient(ClientConnection client, ReadableByteStream stream)
            throws IOException {
        entityID = stream.getInt();
        health = stream.getDouble();
        maxHealth = stream.getDouble();
    }

    @Override
    public void runClient(ClientConnection client, WorldClient world) {
        if (world == null) {
            return;
        }
        Optional<EntityClient> fetch = world.entity(entityID);
        if (fetch.isPresent()) {
            EntityClient entity = fetch.get();
            if (entity instanceof MobLivingClient) {
                ((MobLivingClient) entity).processPacket(this);
            }
        } else {
            client.send(new PacketRequestEntity(entityID));
        }
    }
}