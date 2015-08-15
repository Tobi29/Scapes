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
import org.tobi29.scapes.engine.utils.io.ReadableByteStream;
import org.tobi29.scapes.engine.utils.io.WritableByteStream;
import org.tobi29.scapes.entity.client.EntityClient;
import org.tobi29.scapes.entity.client.MobPlayerClient;
import org.tobi29.scapes.server.connection.PlayerConnection;

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
    public void sendClient(PlayerConnection player, WritableByteStream stream)
            throws IOException {
        stream.putInt(entityID);
        stream.put(type);
        stream.put(data);
    }

    @Override
    public void parseClient(ClientConnection client, ReadableByteStream stream)
            throws IOException {
        entityID = stream.getInt();
        type = stream.get();
        data = stream.get();
    }

    @Override
    public void runClient(ClientConnection client, WorldClient world) {
        if (world == null) {
            return;
        }
        switch (type) {
            case INVENTORY_SLOT_CHANGE:
                Optional<EntityClient> fetch = world.entity(entityID);
                if (fetch.isPresent()) {
                    EntityClient entity = fetch.get();
                    if (entity instanceof MobPlayerClient) {
                        if (data > 9) {
                            ((MobPlayerClient) entity)
                                    .setInventorySelectRight(data - 10);
                        } else {
                            ((MobPlayerClient) entity)
                                    .setInventorySelectLeft(data);
                        }
                    }
                } else {
                    client.send(new PacketRequestEntity(entityID));
                }
                break;
        }
    }

    @Override
    public void sendServer(ClientConnection client, WritableByteStream stream)
            throws IOException {
        stream.put(type);
        stream.put(data);
    }

    @Override
    public void parseServer(PlayerConnection player, ReadableByteStream stream)
            throws IOException {
        type = stream.get();
        data = stream.get();
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
                    player.mob().setInventorySelectRight(data - 10);
                } else {
                    player.mob().setInventorySelectLeft(data);
                }
                world.connection()
                        .send(new PacketInteraction(player.mob().entityID(),
                                INVENTORY_SLOT_CHANGE, data));
                break;
            case OPEN_INVENTORY:
                player.send(new PacketOpenGui(player.mob()));
                break;
            case CLOSE_INVENTORY:
                player.mob().inventory("Hold").item(0).take()
                        .ifPresent(drop -> {
                            player.mob().dropItem(drop);
                            world.connection().send(new PacketUpdateInventory(
                                    player.mob(), "Hold"));
                        });
                player.send(new PacketCloseGui());
                break;
            case OPEN_STATISTICS:
                player.send(new PacketUpdateStatistics(player.mob()));
                break;
            default:
                throw new InvalidPacketDataException(
                        "Invalid interaction type!");
        }
    }
}
