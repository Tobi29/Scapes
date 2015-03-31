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

import org.tobi29.scapes.block.CraftingRecipe;
import org.tobi29.scapes.block.Inventory;
import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.chunk.WorldServer;
import org.tobi29.scapes.client.connection.ClientConnection;
import org.tobi29.scapes.server.connection.PlayerConnection;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class PacketCrafting extends Packet implements PacketServer {
    private int type, id;
    private boolean table;

    public PacketCrafting() {
    }

    public PacketCrafting(int type, int id, boolean table) {
        this.type = type;
        this.id = id;
        this.table = table;
    }

    @Override
    public void sendServer(ClientConnection client, DataOutputStream streamOut)
            throws IOException {
        streamOut.writeInt(type);
        streamOut.writeInt(id);
        streamOut.writeBoolean(table);
    }

    @Override
    public void parseServer(PlayerConnection player, DataInputStream streamIn)
            throws IOException {
        type = streamIn.readInt();
        id = streamIn.readInt();
        table = streamIn.readBoolean();
    }

    @Override
    public void runServer(PlayerConnection player, WorldServer world) {
        if (world == null) {
            return;
        }
        Inventory inventory = player.getMob().getInventory();
        CraftingRecipe recipe =
                world.getPlugins().getRegistry().getCraftingRecipes(table)
                        .get(type).getRecipes().get(id);
        ItemStack result = recipe.getResult();
        if (inventory.canAdd(result) >= result.getAmount()) {
            recipe.getTakes(inventory).ifPresent(takes -> {
                takes.forEach(inventory::take);
                inventory.add(result);
                for (int i = 0; i < recipe.getResult().getAmount(); i++) {
                    player.getStatistics()
                            .blockCraft(result.getMaterial(), result.getData());
                }
                player.send(new PacketUpdateInventory(player.getMob()));
            });
        }
    }
}
