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

import java8.util.Optional;
import java8.util.function.Consumer;
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

public class PacketInventoryInteraction extends Packet implements PacketServer {
    public static final byte LEFT = 0, RIGHT = 1;
    private int entityID, slot;
    private String id;
    private byte type;

    public PacketInventoryInteraction() {
    }

    public PacketInventoryInteraction(EntityContainerClient chest, byte type,
            String id, int slot) {
        entityID = ((EntityClient) chest).entityID();
        this.type = type;
        this.id = id;
        this.slot = slot;
    }

    @Override
    public void sendServer(ClientConnection client, WritableByteStream stream)
            throws IOException {
        stream.putInt(entityID);
        stream.put(type);
        stream.putString(id);
        stream.putInt(slot);
    }

    @Override
    public void parseServer(PlayerConnection player, ReadableByteStream stream)
            throws IOException {
        entityID = stream.getInt();
        type = stream.get();
        id = stream.getString();
        slot = stream.getInt();
    }

    @Override
    public void runServer(PlayerConnection player,
            Consumer<Consumer<WorldServer>> worldAccess) {
        worldAccess.accept(world -> {
            MobPlayerServer playerE = player.mob();
            world.entity(entityID)
                    .filter(entity -> entity instanceof EntityContainerServer)
                    .map(entity -> (EntityContainerServer) entity)
                    .ifPresent(chestE -> {
                        if (chestE.viewers().filter(check -> check == playerE)
                                .findAny().isPresent()) {
                            synchronized (chestE) {
                                chestE.inventories().modify(id,
                                        chestI -> playerE.inventories()
                                                .modify("Hold", playerI -> {
                                                    ItemStack hold =
                                                            playerI.item(0);
                                                    ItemStack item =
                                                            chestI.item(slot);
                                                    switch (type) {
                                                        case LEFT:
                                                            if (hold.isEmpty()) {
                                                                chestI.item(
                                                                        slot)
                                                                        .take()
                                                                        .ifPresent(
                                                                                hold::stack);
                                                            } else {
                                                                if (item.stack(
                                                                        hold) ==
                                                                        0) {
                                                                    Optional<ItemStack>
                                                                            swap =
                                                                            item.take();
                                                                    item.stack(
                                                                            hold);
                                                                    swap.ifPresent(
                                                                            hold::stack);
                                                                }
                                                            }
                                                            break;
                                                        case RIGHT:
                                                            if (hold.isEmpty()) {
                                                                item.take(
                                                                        (int) FastMath
                                                                                .ceil(item
                                                                                        .amount() /
                                                                                        2.0))
                                                                        .ifPresent(
                                                                                hold::stack);
                                                            } else {
                                                                hold.take(1)
                                                                        .ifPresent(
                                                                                item::stack);
                                                            }
                                                            break;
                                                    }
                                                }));
                            }
                        }
                    });
        });
    }
}
