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
import org.tobi29.scapes.client.connection.ClientConnection;
import org.tobi29.scapes.engine.utils.io.ReadableByteStream;
import org.tobi29.scapes.engine.utils.io.WritableByteStream;
import org.tobi29.scapes.packets.PacketAbstract;
import org.tobi29.scapes.packets.PacketServer;
import org.tobi29.scapes.server.connection.PlayerConnection;
import org.tobi29.scapes.vanilla.basics.VanillaBasics;
import org.tobi29.scapes.vanilla.basics.entity.client.EntityQuernClient;
import org.tobi29.scapes.vanilla.basics.entity.server.EntityQuernServer;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;

import java.io.IOException;

public class PacketQuern extends PacketAbstract implements PacketServer {
    private int entityID;

    public PacketQuern() {
    }

    public PacketQuern(EntityQuernClient quern) {
        entityID = quern.entityID();
        entityID = 0;
    }

    @Override
    public void sendServer(ClientConnection client, WritableByteStream stream)
            throws IOException {
        stream.putInt(entityID);
    }

    @Override
    public void parseServer(PlayerConnection player, ReadableByteStream stream)
            throws IOException {
        entityID = stream.getInt();
    }

    @Override
    public void runServer(PlayerConnection player) {
        player.mob(mob -> mob.world().entity(entityID)
                .filter(entity -> entity instanceof EntityQuernServer)
                .map(entity -> (EntityQuernServer) entity).ifPresent(quern -> {
                    if (quern.viewers().filter(check -> check == mob).findAny()
                            .isPresent()) {
                        VanillaBasics plugin =
                                (VanillaBasics) mob.world().plugins()
                                        .plugin("VanillaBasics");
                        VanillaMaterial materials = plugin.getMaterials();
                        quern.inventories().modify("Container", inventory -> {
                            ItemStack item = inventory.item(0);
                            if (item.material() == materials.cropDrop) {
                                item.setMaterial(materials.grain);
                                item.setAmount(item.amount() << 2);
                            }
                        });
                    }
                }));
    }
}
