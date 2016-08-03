/*
 * Copyright 2012-2016 Tobi29
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
import org.tobi29.scapes.engine.server.InvalidPacketDataException;
import org.tobi29.scapes.engine.utils.io.ReadableByteStream;
import org.tobi29.scapes.engine.utils.io.WritableByteStream;
import org.tobi29.scapes.entity.client.EntityClient;
import org.tobi29.scapes.entity.client.MobPlayerClient;
import org.tobi29.scapes.server.connection.PlayerConnection;

import java.io.IOException;

public class PacketInteraction extends PacketAbstract
        implements PacketClient, PacketServer {
    public static final byte INVENTORY_SLOT_LEFT = 0x00, INVENTORY_SLOT_RIGHT =
            0x01, OPEN_INVENTORY = 0x10, CLOSE_INVENTORY = 0x11;
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
        Optional<EntityClient> fetch = world.entity(entityID);
        if (fetch.isPresent()) {
            EntityClient entity = fetch.get();
            if (entity instanceof MobPlayerClient) {
                switch (type) {
                    case INVENTORY_SLOT_LEFT:
                        ((MobPlayerClient) entity).setInventorySelectLeft(data);
                        break;
                    case INVENTORY_SLOT_RIGHT:
                        ((MobPlayerClient) entity)
                                .setInventorySelectRight(data);
                        break;
                }
            }
        } else {
            client.send(new PacketRequestEntity(entityID));
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
    public void runServer(PlayerConnection player) {
        player.mob(mob -> {
            switch (type) {
                case INVENTORY_SLOT_LEFT:
                    if (data < 0 || data >= 10) {
                        throw new InvalidPacketDataException(
                                "Invalid slot change data!");
                    }
                    mob.setInventorySelectLeft(data);
                    mob.world().send(new PacketInteraction(mob.entityID(),
                            INVENTORY_SLOT_LEFT, data));
                    break;
                case INVENTORY_SLOT_RIGHT:
                    if (data < 0 || data >= 10) {
                        throw new InvalidPacketDataException(
                                "Invalid slot change data!");
                    }
                    mob.setInventorySelectRight(data);
                    mob.world().send(new PacketInteraction(mob.entityID(),
                            INVENTORY_SLOT_RIGHT, data));
                    break;
                case OPEN_INVENTORY:
                    player.send(new PacketOpenGui(mob));
                    break;
                case CLOSE_INVENTORY:
                    mob.inventories().modify("Hold",
                            inventory -> inventory.item(0).take()
                                    .ifPresent(mob::dropItem));
                    player.send(new PacketCloseGui());
                    break;
                default:
                    throw new InvalidPacketDataException(
                            "Invalid interaction type!");
            }
        });
    }
}
