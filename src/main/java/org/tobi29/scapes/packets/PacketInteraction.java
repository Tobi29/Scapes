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
import org.tobi29.scapes.connection.InvalidPacketDataException;
import org.tobi29.scapes.entity.client.EntityClient;
import org.tobi29.scapes.entity.client.MobPlayerClient;
import org.tobi29.scapes.server.connection.PlayerConnection;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Optional;

public class PacketInteraction extends Packet
        implements PacketClient, PacketServer {
    public static final byte INVENTORY_SLOT_CHANGE = 0, OPEN_INVENTORY = 1,
            CLOSE_INVENTORY = 2, OPEN_STATISTICS = 3;
    private int entityID;
    private byte type, data;

    public PacketInteraction() {
    }

    public PacketInteraction(byte type) {
        this(type, (byte) 0);
    }

    public PacketInteraction(byte type, byte data) {
        this.type = type;
        this.data = data;
    }

    public PacketInteraction(int entityID, byte type, byte data) {
        this.entityID = entityID;
        this.type = type;
        this.data = data;
    }

    @Override
    public void sendClient(PlayerConnection player, DataOutputStream streamOut)
            throws IOException {
        streamOut.writeInt(entityID);
        streamOut.writeByte(type);
        streamOut.writeByte(data);
    }

    @Override
    public void parseClient(ClientConnection client, DataInputStream streamIn)
            throws IOException {
        entityID = streamIn.readInt();
        type = streamIn.readByte();
        data = streamIn.readByte();
    }

    @Override
    public void runClient(ClientConnection client, WorldClient world) {
        if (world == null) {
            return;
        }
        switch (type) {
            case INVENTORY_SLOT_CHANGE:
                EntityClient entity = world.getEntity(entityID);
                if (entity instanceof MobPlayerClient) {
                    if (data > 9) {
                        ((MobPlayerClient) entity)
                                .setInventorySelectRight(data - 10);
                    } else {
                        ((MobPlayerClient) entity).setInventorySelectLeft(data);
                    }
                }
                break;
        }
    }

    @Override
    public void sendServer(ClientConnection client, DataOutputStream streamOut)
            throws IOException {
        streamOut.writeByte(type);
        streamOut.writeByte(data);
    }

    @Override
    public void parseServer(PlayerConnection player, DataInputStream streamIn)
            throws IOException {
        type = streamIn.readByte();
        data = streamIn.readByte();
    }

    @Override
    public void runServer(PlayerConnection player, WorldServer world) {
        if (world == null) {
            return;
        }
        switch (type) {
            case INVENTORY_SLOT_CHANGE:
                if (data < 0 || data >= 20) {
                    throw new InvalidPacketDataException(
                            "Invalid slot change data!");
                }
                if (data > 9) {
                    player.getMob().setInventorySelectRight(data - 10);
                } else {
                    player.getMob().setInventorySelectLeft(data);
                }
                world.getConnection().send(new PacketInteraction(
                        player.getMob().getEntityID(), INVENTORY_SLOT_CHANGE,
                        data));
                break;
            case OPEN_INVENTORY:
                world.getConnection()
                        .send(new PacketUpdateInventory(player.getMob()));
                player.send(new PacketOpenGui(player.getMob()));
                break;
            case CLOSE_INVENTORY:
                player.getMob().getInventory().getHold().ifPresent(hold -> {
                    player.getMob().dropItem(hold);
                    player.getMob().getInventory().setHold(Optional.empty());
                });
                player.send(new PacketCloseGui());
                break;
            case OPEN_STATISTICS:
                player.send(new PacketUpdateStatistics(player.getMob()));
                break;
            default:
                throw new InvalidPacketDataException(
                        "Invalid interaction type!");
        }
    }
}
