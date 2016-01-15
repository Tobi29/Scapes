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
import org.tobi29.scapes.block.Material;
import org.tobi29.scapes.chunk.WorldServer;
import org.tobi29.scapes.client.connection.ClientConnection;
import org.tobi29.scapes.engine.utils.io.ReadableByteStream;
import org.tobi29.scapes.engine.utils.io.WritableByteStream;
import org.tobi29.scapes.entity.server.EntityServer;
import org.tobi29.scapes.entity.server.MobPlayerServer;
import org.tobi29.scapes.packets.Packet;
import org.tobi29.scapes.packets.PacketEntityMetaData;
import org.tobi29.scapes.packets.PacketServer;
import org.tobi29.scapes.server.connection.PlayerConnection;
import org.tobi29.scapes.vanilla.basics.VanillaBasics;
import org.tobi29.scapes.vanilla.basics.entity.client.EntityResearchTableClient;
import org.tobi29.scapes.vanilla.basics.entity.server.EntityResearchTableServer;
import org.tobi29.scapes.vanilla.basics.material.item.ItemResearch;

import java.io.IOException;

public class PacketResearch extends Packet implements PacketServer {
    private int entityID;

    public PacketResearch() {
    }

    public PacketResearch(EntityResearchTableClient researchTable) {
        entityID = researchTable.entityID();
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
    public void runServer(PlayerConnection player, WorldServer world) {
        if (world == null) {
            return;
        }
        EntityServer entity = world.entity(entityID);
        if (entity instanceof EntityResearchTableServer) {
            EntityResearchTableServer researchTable =
                    (EntityResearchTableServer) entity;
            MobPlayerServer playerE = player.mob();
            if (researchTable.viewers().filter(check -> check == playerE)
                    .findAny().isPresent()) {
                VanillaBasics plugin =
                        (VanillaBasics) world.plugins().plugin("VanillaBasics");
                synchronized (researchTable) {
                    ItemStack item =
                            researchTable.inventory("Container").item(0);
                    Material material = item.material();
                    if (material instanceof ItemResearch) {
                        for (String identifier : ((ItemResearch) material)
                                .identifiers(item)) {
                            player.mob().metaData("Vanilla")
                                    .getStructure("Research")
                                    .getStructure("Items")
                                    .setBoolean(identifier, true);
                        }
                    } else {
                        player.mob().metaData("Vanilla")
                                .getStructure("Research").getStructure("Items")
                                .setBoolean(
                                        Integer.toHexString(material.itemID()),
                                        true);
                    }
                    plugin.researchRecipes().forEach(recipe -> {
                        if (!player.mob().metaData("Vanilla")
                                .getStructure("Research")
                                .getStructure("Finished")
                                .getBoolean(recipe.name())) {
                            if (!recipe.items()
                                    .filter(requirement -> !player.mob()
                                            .metaData("Vanilla")
                                            .getStructure("Research")
                                            .getStructure("Items")
                                            .getBoolean(requirement)).findAny()
                                    .isPresent()) {
                                player.mob().metaData("Vanilla")
                                        .getStructure("Research")
                                        .getStructure("Finished")
                                        .setBoolean(recipe.name(), true);
                                player.mob().world()
                                        .send(new PacketEntityMetaData(
                                                player.mob(), "Vanilla"));
                                player.send(new PacketNotification("Research",
                                        recipe.text()));
                            }
                        }
                    });
                }
            }
        }
    }
}
