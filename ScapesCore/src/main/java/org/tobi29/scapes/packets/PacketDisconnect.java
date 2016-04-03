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
import org.tobi29.scapes.client.states.GameStateServerDisconnect;
import org.tobi29.scapes.engine.server.ConnectionCloseException;
import org.tobi29.scapes.engine.server.ConnectionEndException;
import org.tobi29.scapes.engine.server.RemoteAddress;
import org.tobi29.scapes.engine.utils.io.ReadableByteStream;
import org.tobi29.scapes.engine.utils.io.WritableByteStream;
import org.tobi29.scapes.server.connection.PlayerConnection;

import java.io.IOException;

public class PacketDisconnect extends Packet implements PacketClient {
    private String reason;
    private double time;

    public PacketDisconnect() {
    }

    public PacketDisconnect(String reason, double time) {
        this.reason = reason;
        this.time = time;
    }

    @Override
    public void sendClient(PlayerConnection player, WritableByteStream stream)
            throws IOException {
        stream.putString(reason);
        stream.putDouble(time);
    }

    @Override
    public void parseClient(ClientConnection client, ReadableByteStream stream)
            throws IOException {
        reason = stream.getString();
        time = stream.getDouble();
    }

    @Override
    public void runClient(ClientConnection client, WorldClient world)
            throws ConnectionCloseException {
        Optional<RemoteAddress> address = client.address();
        if (time >= 0 && address.isPresent()) {
            client.game().engine().setState(
                    new GameStateServerDisconnect(reason, address,
                            client.game().engine(), time));
        } else {
            client.game().engine().setState(
                    new GameStateServerDisconnect(reason,
                            client.game().engine()));
        }
        throw new ConnectionEndException(reason);
    }
}
