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
import org.tobi29.scapes.packets.PacketServer;
import org.tobi29.scapes.server.connection.PlayerConnection;
import org.tobi29.scapes.vanilla.basics.VanillaBasics;
import org.tobi29.scapes.vanilla.basics.entity.client.EntityAnvilClient;
import org.tobi29.scapes.vanilla.basics.entity.server.EntityAnvilServer;
import org.tobi29.scapes.vanilla.basics.material.item.ItemHeatable;
import org.tobi29.scapes.vanilla.basics.material.item.ItemIngot;
import org.tobi29.scapes.vanilla.basics.material.item.ItemOreChunk;
import org.tobi29.scapes.vanilla.basics.util.ToolUtil;

import java.io.IOException;

public class PacketAnvil extends Packet implements PacketServer {
    private int entityID, id;

    public PacketAnvil() {
    }

    public PacketAnvil(EntityAnvilClient anvil, int id) {
        entityID = anvil.entityID();
        this.id = id;
    }

    @Override
    public void sendServer(ClientConnection client, WritableByteStream stream)
            throws IOException {
        stream.putInt(entityID);
        stream.putInt(id);
    }

    @Override
    public void parseServer(PlayerConnection player, ReadableByteStream stream)
            throws IOException {
        entityID = stream.getInt();
        id = stream.getInt();
    }

    @Override
    public void runServer(PlayerConnection player, WorldServer world) {
        if (world == null) {
            return;
        }
        EntityServer entity = world.entity(entityID);
        if (entity instanceof EntityAnvilServer) {
            EntityAnvilServer anvil = (EntityAnvilServer) entity;
            MobPlayerServer playerE = player.mob();
            if (anvil.viewers().filter(check -> check == playerE).findAny()
                    .isPresent()) {
                VanillaBasics plugin =
                        (VanillaBasics) world.plugins().plugin("VanillaBasics");
                anvil.inventories().modify("Container", anvilI -> {
                    if (!"Hammer".equals(anvilI.item(1).material()
                            .toolType(anvilI.item(1)))) {
                        return;
                    }
                    world.playSound("VanillaBasics:sound/blocks/Metal.ogg",
                            anvil);
                    ItemStack ingredient = anvilI.item(0);
                    Material type = ingredient.material();
                    if (type instanceof ItemIngot) {
                        float meltingPoint =
                                ((ItemHeatable) type).meltingPoint(ingredient);
                        float temperature =
                                ((ItemHeatable) type).temperature(ingredient);
                        if (temperature >= meltingPoint ||
                                temperature < meltingPoint * 0.7f) {
                            return;
                        }
                        if (id == 0) {
                            ingredient.setData(1);
                        } else {
                            if (ingredient.data() == 0) {
                                return;
                            }
                            ingredient.setData(0);
                            ToolUtil.createTool(plugin, ingredient, id);
                        }
                    } else if (type instanceof ItemOreChunk) {
                        if (id == 0 && ingredient.data() == 8) {
                            float meltingPoint = ((ItemHeatable) type)
                                    .meltingPoint(ingredient);
                            float temperature = ((ItemHeatable) type)
                                    .temperature(ingredient);
                            if (temperature >= meltingPoint ||
                                    temperature < meltingPoint * 0.7f) {
                                return;
                            }
                            ingredient.setData(9);
                        }
                    }
                });
            }
        }
    }
}
