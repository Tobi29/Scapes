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

package org.tobi29.scapes.vanilla.basics.packet;

import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.block.Material;
import org.tobi29.scapes.client.connection.ClientConnection;
import org.tobi29.scapes.engine.utils.io.ReadableByteStream;
import org.tobi29.scapes.engine.utils.io.WritableByteStream;
import org.tobi29.scapes.packets.PacketAbstract;
import org.tobi29.scapes.packets.PacketEntityMetaData;
import org.tobi29.scapes.packets.PacketServer;
import org.tobi29.scapes.server.connection.PlayerConnection;
import org.tobi29.scapes.vanilla.basics.VanillaBasics;
import org.tobi29.scapes.vanilla.basics.entity.client.EntityResearchTableClient;
import org.tobi29.scapes.vanilla.basics.entity.server.EntityResearchTableServer;
import org.tobi29.scapes.vanilla.basics.material.item.ItemResearch;

import java.io.IOException;

public class PacketResearch extends PacketAbstract implements PacketServer {
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
    public void runServer(PlayerConnection player) {
        player.mob(mob -> mob.world().entity(entityID)
                .filter(entity -> entity instanceof EntityResearchTableServer)
                .map(entity -> (EntityResearchTableServer) entity)
                .ifPresent(researchTable -> {
                    if (researchTable.viewers().filter(check -> check == mob)
                            .findAny().isPresent()) {
                        VanillaBasics plugin =
                                (VanillaBasics) mob.world().plugins()
                                        .plugin("VanillaBasics");
                        researchTable.inventories()
                                .modify("Container", researchTableI -> {
                                    ItemStack item = researchTableI.item(0);
                                    Material material = item.material();
                                    if (material instanceof ItemResearch) {
                                        for (String identifier : ((ItemResearch) material)
                                                .identifiers(item)) {
                                            mob.metaData("Vanilla")
                                                    .getStructure("Research")
                                                    .getStructure("Items")
                                                    .setBoolean(identifier,
                                                            true);
                                        }
                                    } else {
                                        mob.metaData("Vanilla")
                                                .getStructure("Research")
                                                .getStructure("Items")
                                                .setBoolean(Integer.toHexString(
                                                        material.itemID()),
                                                        true);
                                    }
                                    plugin.researchRecipes().forEach(recipe -> {
                                        if (!mob.metaData("Vanilla")
                                                .getStructure("Research")
                                                .getStructure("Finished")
                                                .getBoolean(recipe.name())) {
                                            if (!recipe.items()
                                                    .filter(requirement -> !mob
                                                            .metaData("Vanilla")
                                                            .getStructure(
                                                                    "Research")
                                                            .getStructure(
                                                                    "Items")
                                                            .getBoolean(
                                                                    requirement))
                                                    .findAny().isPresent()) {
                                                mob.metaData("Vanilla")
                                                        .getStructure(
                                                                "Research")
                                                        .getStructure(
                                                                "Finished")
                                                        .setBoolean(
                                                                recipe.name(),
                                                                true);
                                                mob.world()
                                                        .send(new PacketEntityMetaData(
                                                                mob,
                                                                "Vanilla"));
                                                player.send(
                                                        new PacketNotification(
                                                                "Research",
                                                                recipe.text()));
                                            }
                                        }
                                    });
                                });
                    }
                }));
    }
}
