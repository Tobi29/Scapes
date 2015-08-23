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
import org.tobi29.scapes.engine.utils.io.ReadableByteStream;
import org.tobi29.scapes.engine.utils.io.WritableByteStream;
import org.tobi29.scapes.entity.client.MobPlayerClientMain;
import org.tobi29.scapes.packets.Packet;
import org.tobi29.scapes.packets.PacketClient;
import org.tobi29.scapes.server.connection.PlayerConnection;
import org.tobi29.scapes.vanilla.basics.entity.client.MobPlayerClientMainVB;
import org.tobi29.scapes.vanilla.basics.gui.GuiCrafting;

import java.io.IOException;

public class PacketOpenCrafting extends Packet implements PacketClient {
    @Override
    public void sendClient(PlayerConnection player, WritableByteStream stream)
            throws IOException {
    }

    @Override
    public void parseClient(ClientConnection client, ReadableByteStream stream)
            throws IOException {
    }

    @Override
    public void runClient(ClientConnection client, WorldClient world) {
        MobPlayerClientMain player = client.entity();
        if (player instanceof MobPlayerClientMainVB) {
            player.openGui(
                    new GuiCrafting(true, (MobPlayerClientMainVB) player));
        }
    }
}
