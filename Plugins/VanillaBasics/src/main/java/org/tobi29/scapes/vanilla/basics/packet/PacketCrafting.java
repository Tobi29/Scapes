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

import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.chunk.WorldServer;
import org.tobi29.scapes.client.connection.ClientConnection;
import org.tobi29.scapes.engine.utils.io.ReadableByteStream;
import org.tobi29.scapes.engine.utils.io.WritableByteStream;
import org.tobi29.scapes.packets.Packet;
import org.tobi29.scapes.packets.PacketServer;
import org.tobi29.scapes.server.connection.PlayerConnection;
import org.tobi29.scapes.vanilla.basics.VanillaBasics;
import org.tobi29.scapes.vanilla.basics.material.CraftingRecipe;

import java.io.IOException;

public class PacketCrafting extends Packet implements PacketServer {
    private int type, id;

    public PacketCrafting() {
    }

    public PacketCrafting(int type, int id) {
        this.type = type;
        this.id = id;
    }

    @Override
    public void sendServer(ClientConnection client, WritableByteStream stream)
            throws IOException {
        stream.putInt(type);
        stream.putInt(id);
    }

    @Override
    public void parseServer(PlayerConnection player, ReadableByteStream stream)
            throws IOException {
        type = stream.getInt();
        id = stream.getInt();
    }

    @Override
    public void runServer(PlayerConnection player, WorldServer world) {
        if (world == null) {
            return;
        }
        // TODO: Check if table nearby
        VanillaBasics plugin =
                (VanillaBasics) world.plugins().plugin("VanillaBasics");
        player.mob().inventories().modify("Container", inventory -> {
            CraftingRecipe recipe =
                    plugin.getCraftingRecipes().get(type).recipes().get(id);
            ItemStack result = recipe.result();
            if (inventory.canAdd(result) >= result.amount()) {
                recipe.takes(inventory).ifPresent(takes -> {
                    takes.forEach(inventory::take);
                    inventory.add(result);
                });
            }
        });
    }
}
