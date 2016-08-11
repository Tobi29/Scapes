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
import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.client.connection.ClientConnection;
import org.tobi29.scapes.engine.utils.io.ReadableByteStream;
import org.tobi29.scapes.engine.utils.io.WritableByteStream;
import org.tobi29.scapes.entity.client.EntityClient;
import org.tobi29.scapes.entity.client.EntityContainerClient;
import org.tobi29.scapes.entity.client.MobPlayerClientMain;
import org.tobi29.scapes.entity.server.EntityContainerServer;
import org.tobi29.scapes.server.connection.PlayerConnection;

import java.io.IOException;
import java.util.UUID;

public class PacketOpenGui extends PacketAbstract implements PacketClient {
    private UUID uuid;

    public PacketOpenGui() {
    }

    public PacketOpenGui(EntityContainerServer entity) {
        uuid = entity.uuid();
    }

    @Override
    public void sendClient(PlayerConnection player, WritableByteStream stream)
            throws IOException {
        stream.putLong(uuid.getMostSignificantBits());
        stream.putLong(uuid.getLeastSignificantBits());
    }

    @Override
    public void parseClient(ClientConnection client, ReadableByteStream stream)
            throws IOException {
        uuid = new UUID(stream.getLong(), stream.getLong());
    }

    @Override
    public void runClient(ClientConnection client, WorldClient world) {
        if (world == null) {
            return;
        }
        Optional<EntityClient> fetch = world.entity(uuid);
        if (fetch.isPresent()) {
            EntityClient entity = fetch.get();
            if (entity instanceof EntityContainerClient) {
                MobPlayerClientMain player = client.entity();
                ((EntityContainerClient) entity).gui(player)
                        .ifPresent(player::openGui);
            }
        } else {
            client.send(new PacketRequestEntity(uuid));
        }
    }
}
