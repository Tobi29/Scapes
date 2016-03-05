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

import java8.util.function.Consumer;
import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.chunk.WorldServer;
import org.tobi29.scapes.client.connection.ClientConnection;
import org.tobi29.scapes.client.connection.RemoteClientConnection;
import org.tobi29.scapes.server.connection.PlayerConnection;

public class PacketPingClient extends PacketPing {
    public PacketPingClient() {
    }

    public PacketPingClient(long timestamp) {
        super(timestamp);
    }

    @Override
    public void runServer(PlayerConnection player, Consumer<Consumer<WorldServer>> worldAccess) {
        player.send(new PacketPingClient(timestamp));
    }

    @Override
    public void runClient(ClientConnection client, WorldClient world) {
        if (client instanceof RemoteClientConnection) {
            ((RemoteClientConnection) client).updatePing(timestamp);
        }
    }
}
