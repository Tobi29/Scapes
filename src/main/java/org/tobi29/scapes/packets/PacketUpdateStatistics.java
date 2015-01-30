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
import org.tobi29.scapes.client.connection.ClientConnection;
import org.tobi29.scapes.client.gui.GuiStatistics;
import org.tobi29.scapes.entity.server.MobPlayerServer;
import org.tobi29.scapes.server.connection.PlayerConnection;
import org.tobi29.scapes.server.format.PlayerStatistics;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PacketUpdateStatistics extends Packet implements PacketClient {
    private List<PlayerStatistics.StatisticMaterial> statisticMaterials;

    public PacketUpdateStatistics(GameRegistry registry) {
    }

    public PacketUpdateStatistics(MobPlayerServer player) {
        statisticMaterials =
                player.getConnection().getStatistics().getStatisticMaterials();
    }

    @Override
    public void sendClient(PlayerConnection player, DataOutputStream streamOut)
            throws IOException {
        streamOut.writeInt(statisticMaterials.size());
        for (PlayerStatistics.StatisticMaterial statisticMaterial : statisticMaterials) {
            streamOut.writeInt(statisticMaterial.getType().getItemID());
            streamOut.writeShort(statisticMaterial.getData());
            streamOut.writeInt(statisticMaterial.getBreakAmount());
            streamOut.writeInt(statisticMaterial.getPlaceAmount());
            streamOut.writeInt(statisticMaterial.getCraftAmount());
        }
    }

    @Override
    public void parseClient(ClientConnection client, DataInputStream streamIn)
            throws IOException {
        int length = streamIn.readInt();
        statisticMaterials = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            statisticMaterials.add(new PlayerStatistics.StatisticMaterial(
                    client.getPlugins().getRegistry()
                            .getMaterial(streamIn.readInt()),
                    streamIn.readShort(), streamIn.readInt(),
                    streamIn.readInt(), streamIn.readInt()));
        }
    }

    @Override
    public void runClient(ClientConnection client, WorldClient world) {
        client.getEntity().openGui(
                new GuiStatistics(client.getGame(), statisticMaterials));
    }
}
