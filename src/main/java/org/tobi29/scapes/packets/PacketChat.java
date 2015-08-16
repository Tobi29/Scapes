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
import org.tobi29.scapes.engine.server.InvalidPacketDataException;
import org.tobi29.scapes.engine.utils.io.ReadableByteStream;
import org.tobi29.scapes.engine.utils.io.WritableByteStream;
import org.tobi29.scapes.server.connection.PlayerConnection;

import java.io.IOException;

public class PacketChat extends Packet implements PacketServer, PacketClient {
    private String text;

    public PacketChat() {
    }

    public PacketChat(String text) {
        this.text = text;
    }

    @Override
    public void sendClient(PlayerConnection player, WritableByteStream stream)
            throws IOException {
        stream.putString(text);
    }

    @Override
    public void parseClient(ClientConnection client, ReadableByteStream stream)
            throws IOException {
        text = stream.getString();
    }

    @Override
    public void runClient(ClientConnection client, WorldClient world) {
        client.game().chatHistory().addLine(text);
    }

    @Override
    public void sendServer(ClientConnection client, WritableByteStream stream)
            throws IOException {
        stream.putString(text);
    }

    @Override
    public void parseServer(PlayerConnection player, ReadableByteStream stream)
            throws IOException {
        text = stream.getString(1 << 10);
    }

    @Override
    public void runServer(PlayerConnection player, WorldServer world) {
        if (text.isEmpty() || text.length() > 64) {
            throw new InvalidPacketDataException("Invalid chat text length!");
        }
        if (text.charAt(0) == '/') {
            player.server().server().commandRegistry()
                    .get(text.substring(1), player).execute()
                    .forEach(output -> player.tell(output.toString()));
        } else {
            player.server().chat('<' + player.mob().nickname() + "> " +
                    text);
        }
    }
}
