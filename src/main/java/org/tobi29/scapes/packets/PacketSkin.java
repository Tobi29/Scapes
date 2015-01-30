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
import org.tobi29.scapes.connection.ConnectionCloseException;
import org.tobi29.scapes.entity.skin.ServerSkin;
import org.tobi29.scapes.server.connection.PlayerConnection;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class PacketSkin extends Packet implements PacketServer, PacketClient {
    private byte[] array;
    private String checksum;

    public PacketSkin(GameRegistry registry) {
    }

    public PacketSkin(String checksum) {
        this.checksum = checksum;
    }

    public PacketSkin(byte[] array, String checksum) {
        this.array = array;
        this.checksum = checksum;
    }

    @Override
    public void sendClient(PlayerConnection player, DataOutputStream streamOut)
            throws IOException {
        streamOut.write(array);
        streamOut.writeUTF(checksum);
    }

    @Override
    public void parseClient(ClientConnection client, DataInputStream streamIn)
            throws IOException {
        array = new byte[64 * 64 * 4];
        streamIn.readFully(array);
        checksum = streamIn.readUTF();
    }

    @Override
    public void runClient(ClientConnection client, WorldClient world)
            throws ConnectionCloseException {
        client.getWorld().getScene().getSkinStorage().addSkin(checksum, array);
    }

    @Override
    public void sendServer(ClientConnection client, DataOutputStream streamOut)
            throws IOException {
        streamOut.writeUTF(checksum);
    }

    @Override
    public void parseServer(PlayerConnection player, DataInputStream streamIn)
            throws IOException {
        checksum = streamIn.readUTF();
    }

    @Override
    public void runServer(PlayerConnection player, WorldServer world) {
        ServerSkin skin = player.getServer().getSkin(checksum);
        if (skin != null) {
            player.send(new PacketSkin(skin.getImage(), skin.getChecksum()));
        }
    }
}
