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

import org.tobi29.scapes.block.GameRegistry;
import org.tobi29.scapes.block.Inventory;
import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.chunk.WorldServer;
import org.tobi29.scapes.client.connection.ClientConnection;
import org.tobi29.scapes.entity.server.EntityServer;
import org.tobi29.scapes.entity.server.MobPlayerServer;
import org.tobi29.scapes.packets.Packet;
import org.tobi29.scapes.packets.PacketServer;
import org.tobi29.scapes.packets.PacketUpdateInventory;
import org.tobi29.scapes.server.connection.PlayerConnection;
import org.tobi29.scapes.vanilla.basics.VanillaBasics;
import org.tobi29.scapes.vanilla.basics.entity.client.EntityQuernClient;
import org.tobi29.scapes.vanilla.basics.entity.server.EntityQuernServer;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class PacketQuern extends Packet implements PacketServer {
    private int entityID;

    public PacketQuern(GameRegistry registry) {
    }

    public PacketQuern(EntityQuernClient quern) {
        entityID = quern.getEntityID();
        entityID = 0;
    }

    @Override
    public void sendServer(ClientConnection client, DataOutputStream streamOut)
            throws IOException {
        streamOut.writeInt(entityID);
    }

    @Override
    public void parseServer(PlayerConnection player, DataInputStream streamIn)
            throws IOException {
        entityID = streamIn.readInt();
    }

    @Override
    public void runServer(PlayerConnection player, WorldServer world) {
        if (world == null) {
            return;
        }
        EntityServer entity = world.getEntity(entityID);
        if (entity instanceof EntityQuernServer) {
            EntityQuernServer quern = (EntityQuernServer) entity;
            MobPlayerServer playerE = player.getMob();
            if (quern.getViewers().filter(check -> check == playerE).findAny()
                    .isPresent()) {
                VanillaBasics plugin = (VanillaBasics) world.getPlugins()
                        .getPlugin("VanillaBasics");
                VanillaMaterial materials = plugin.getMaterials();
                synchronized (quern) {
                    Inventory inventory = quern.getInventory();
                    ItemStack item = inventory.getItem(0);
                    if (item.getMaterial() == materials.cropDrop) {
                        item.setMaterial(materials.grain);
                        item.setAmount(item.getAmount() << 2);
                        Packet packet = new PacketUpdateInventory(quern);
                        quern.getViewers().map(MobPlayerServer::getConnection)
                                .forEach(connection -> connection.send(packet));
                    }
                }
            }
        }
    }
}
