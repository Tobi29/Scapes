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
import org.tobi29.scapes.block.Inventory;
import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.chunk.WorldServer;
import org.tobi29.scapes.client.connection.ClientConnection;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.entity.client.EntityClient;
import org.tobi29.scapes.entity.client.EntityContainerClient;
import org.tobi29.scapes.entity.server.EntityContainerServer;
import org.tobi29.scapes.entity.server.MobPlayerServer;
import org.tobi29.scapes.server.connection.PlayerConnection;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class PacketInventoryInteraction extends Packet implements PacketServer {
    public static final byte LEFT = 0, RIGHT = 1;
    private int entityID, slot;
    private byte type;

    public PacketInventoryInteraction(GameRegistry registry) {
    }

    public PacketInventoryInteraction(EntityContainerClient chest, byte type,
            int slot) {
        entityID = ((EntityClient) chest).getEntityID();
        this.type = type;
        this.slot = slot;
    }

    private static void updateInventory(MobPlayerServer interactE,
            EntityContainerServer chestE) {
        chestE.getViewers().forEach(player -> {
            player.getConnection().send(new PacketUpdateInventory(chestE));
            if (interactE != chestE) {
                player.getConnection()
                        .send(new PacketUpdateInventory(interactE));
            }
        });
    }

    @Override
    public void sendServer(ClientConnection client, DataOutputStream streamOut)
            throws IOException {
        streamOut.writeInt(entityID);
        streamOut.writeInt(slot);
        streamOut.writeByte(type);
    }

    @Override
    public void parseServer(PlayerConnection player, DataInputStream streamIn)
            throws IOException {
        entityID = streamIn.readInt();
        slot = streamIn.readInt();
        type = streamIn.readByte();
    }

    @Override
    public void runServer(PlayerConnection player, WorldServer world) {
        if (world == null) {
            return;
        }
        MobPlayerServer playerE = player.getMob();
        EntityContainerServer chestE =
                (EntityContainerServer) world.getEntity(entityID);
        if (chestE != null &&
                chestE.getViewers().filter(check -> check == playerE).findAny()
                        .isPresent()) {
            synchronized (chestE) {
                Inventory chestI = chestE.getInventory(), playerI =
                        playerE.getInventory();
                switch (type) {
                    case LEFT:
                        if (playerI.getHold() == null) {
                            playerI.setHold(chestI.getItem(slot)
                                    .take(chestI.getItem(slot)));
                        } else {
                            int amount = chestI.getItem(slot)
                                    .canStack(playerI.getHold());
                            if (amount == 0) {
                                ItemStack swap = playerI.getHold();
                                playerI.setHold(chestI.getItem(slot));
                                chestI.setItem(slot, swap);
                            } else {
                                chestI.getItem(slot).stack(playerI.getHold());
                                playerI.getHold().setAmount(
                                        playerI.getHold().getAmount() - amount);
                                if (playerI.getHold().getAmount() <= 0) {
                                    playerI.setHold(null);
                                }
                            }
                        }
                        updateInventory(playerE, chestE);
                        break;
                    case RIGHT:
                        if (playerI.getHold() == null) {
                            playerI.setHold(chestI.getItem(slot)
                                    .take(chestI.getItem(slot), (int) FastMath
                                            .ceil(chestI.getItem(slot)
                                                    .getAmount() / 2.0d)));
                        } else {
                            playerI.getHold().setAmount(
                                    playerI.getHold().getAmount() -
                                            chestI.getItem(slot)
                                                    .stack(playerI.getHold(),
                                                            1));
                            if (playerI.getHold().getAmount() <= 0) {
                                playerI.setHold(null);
                            }
                        }
                        updateInventory(playerE, chestE);
                        break;
                }
            }
        }
    }
}
