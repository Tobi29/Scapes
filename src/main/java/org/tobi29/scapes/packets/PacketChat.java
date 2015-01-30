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

import org.tobi29.scapes.block.GameRegistry;
import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.chunk.WorldServer;
import org.tobi29.scapes.client.connection.ClientConnection;
import org.tobi29.scapes.connection.InvalidPacketDataException;
import org.tobi29.scapes.server.connection.PlayerConnection;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class PacketChat extends Packet implements PacketServer, PacketClient {
    private String text;

    public PacketChat(GameRegistry registry) {
    }

    public PacketChat(String text) {
        this.text = text;
    }

    @Override
    public void sendClient(PlayerConnection player, DataOutputStream streamOut)
            throws IOException {
        streamOut.writeUTF(text);
    }

    @Override
    public void parseClient(ClientConnection client, DataInputStream streamIn)
            throws IOException {
        text = streamIn.readUTF();
    }

    @Override
    public void runClient(ClientConnection client, WorldClient world) {
        client.getWorld().getScene().chat(text);
    }

    @Override
    public void sendServer(ClientConnection client, DataOutputStream streamOut)
            throws IOException {
        streamOut.writeUTF(text);
    }

    @Override
    public void parseServer(PlayerConnection player, DataInputStream streamIn)
            throws IOException {
        text = streamIn.readUTF();
    }

    @Override
    public void runServer(PlayerConnection player, WorldServer world) {
        if (text.isEmpty() || text.length() > 64) {
            throw new InvalidPacketDataException("Invalid chat text length!");
        }
        if (text.charAt(0) == '/') {
            player.getServer().getServer().getCommandRegistry()
                    .get(text.substring(1), player).execute().forEach(
                    output -> player.send(new PacketChat(output.toString())));
        } else {
            player.getServer().send(new PacketChat(
                    '<' + player.getMob().getNickname() + "> " +
                            text));
        }
    }
}
