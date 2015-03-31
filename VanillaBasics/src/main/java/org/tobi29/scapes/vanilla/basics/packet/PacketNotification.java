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

package org.tobi29.scapes.vanilla.basics.packet;

import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.client.connection.ClientConnection;
import org.tobi29.scapes.client.gui.GuiInGameMessage;
import org.tobi29.scapes.packets.Packet;
import org.tobi29.scapes.packets.PacketClient;
import org.tobi29.scapes.server.connection.PlayerConnection;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class PacketNotification extends Packet implements PacketClient {
    private String title, text;

    public PacketNotification() {
    }

    public PacketNotification(String title, String text) {
        this.title = title;
        this.text = text;
    }

    @Override
    public void sendClient(PlayerConnection player, DataOutputStream streamOut)
            throws IOException {
        streamOut.writeUTF(title);
        streamOut.writeUTF(text);
    }

    @Override
    public void parseClient(ClientConnection client, DataInputStream streamIn)
            throws IOException {
        title = streamIn.readUTF();
        text = streamIn.readUTF();
    }

    @Override
    public void runClient(ClientConnection client, WorldClient world) {
        client.getEntity()
                .openGui(new GuiInGameMessage(client.getGame(), title, text));
    }
}
