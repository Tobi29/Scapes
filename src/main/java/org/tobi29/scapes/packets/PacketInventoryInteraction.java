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

import org.tobi29.scapes.block.Inventory;
import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.chunk.WorldServer;
import org.tobi29.scapes.client.connection.ClientConnection;
import org.tobi29.scapes.engine.utils.io.ReadableByteStream;
import org.tobi29.scapes.engine.utils.io.WritableByteStream;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.entity.client.EntityClient;
import org.tobi29.scapes.entity.client.EntityContainerClient;
import org.tobi29.scapes.entity.server.EntityContainerServer;
import org.tobi29.scapes.entity.server.MobPlayerServer;
import org.tobi29.scapes.server.connection.PlayerConnection;

import java.io.IOException;
import java.util.Optional;

public class PacketInventoryInteraction extends Packet implements PacketServer {
    public static final byte LEFT = 0, RIGHT = 1;
    private int entityID, slot;
    private byte type;

    public PacketInventoryInteraction() {
    }

    public PacketInventoryInteraction(EntityContainerClient chest, byte type,
            int slot) {
        entityID = ((EntityClient) chest).entityID();
        this.type = type;
        this.slot = slot;
    }

    private static void updateInventory(MobPlayerServer interactE,
            EntityContainerServer chestE) {
        chestE.viewers().forEach(player -> {
            player.connection().send(new PacketUpdateInventory(chestE));
            if (interactE != chestE) {
                player.connection()
                        .send(new PacketUpdateInventory(interactE));
            }
        });
    }

    @Override
    public void sendServer(ClientConnection client, WritableByteStream stream)
            throws IOException {
        stream.putInt(entityID);
        stream.putInt(slot);
        stream.put(type);
    }

    @Override
    public void parseServer(PlayerConnection player, ReadableByteStream stream)
            throws IOException {
        entityID = stream.getInt();
        slot = stream.getInt();
        type = stream.get();
    }

    @Override
    public void runServer(PlayerConnection player, WorldServer world) {
        if (world == null) {
            return;
        }
        MobPlayerServer playerE = player.mob();
        EntityContainerServer chestE =
                (EntityContainerServer) world.entity(entityID);
        if (chestE != null &&
                chestE.viewers().filter(check -> check == playerE).findAny()
                        .isPresent()) {
            synchronized (chestE) {
                Inventory chestI = chestE.inventory(), playerI =
                        playerE.inventory();
                switch (type) {
                    case LEFT:
                        Optional<ItemStack> hold = playerI.hold();
                        if (hold.isPresent()) {
                            ItemStack item = hold.get();
                            int amount = chestI.item(slot).canStack(item);
                            if (amount == 0) {
                                playerI.setHold(chestI.item(slot));
                                chestI.setItem(slot, item);
                            } else {
                                chestI.item(slot).stack(item);
                                item.setAmount(item.amount() - amount);
                                if (item.amount() <= 0) {
                                    playerI.setHold(Optional.empty());
                                }
                            }
                        } else {
                            playerI.setHold(
                                    chestI.item(slot).take(chestI.item(slot)));
                        }
                        updateInventory(playerE, chestE);
                        break;
                    case RIGHT:
                        hold = playerI.hold();
                        if (hold.isPresent()) {
                            ItemStack item = hold.get();
                            item.setAmount(item.amount() -
                                    chestI.item(slot).stack(item, 1));
                            if (item.amount() <= 0) {
                                playerI.setHold(Optional.empty());
                            }
                        } else {
                            playerI.setHold(chestI.item(slot)
                                    .take(chestI.item(slot), (int) FastMath
                                            .ceil(chestI.item(slot).amount() /
                                                    2.0)));
                        }
                        updateInventory(playerE, chestE);
                        break;
                }
            }
        }
    }
}
