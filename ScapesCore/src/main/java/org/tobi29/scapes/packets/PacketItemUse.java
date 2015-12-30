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

import org.tobi29.scapes.block.BlockType;
import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.chunk.WorldServer;
import org.tobi29.scapes.client.connection.ClientConnection;
import org.tobi29.scapes.engine.server.InvalidPacketDataException;
import org.tobi29.scapes.engine.utils.io.ReadableByteStream;
import org.tobi29.scapes.engine.utils.io.WritableByteStream;
import org.tobi29.scapes.engine.utils.math.Face;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.PointerPane;
import org.tobi29.scapes.engine.utils.math.vector.*;
import org.tobi29.scapes.entity.server.EntityBlockBreakServer;
import org.tobi29.scapes.entity.server.EntityServer;
import org.tobi29.scapes.server.connection.PlayerConnection;

import java.io.IOException;
import java.util.List;

public class PacketItemUse extends Packet implements PacketServer {
    private double strength;
    private boolean side;
    private Vector2 direction;

    public PacketItemUse() {
    }

    public PacketItemUse(double strength, boolean side, Vector2 direction) {
        this.strength = strength;
        this.side = side;
        this.direction = direction;
    }

    @Override
    public void sendServer(ClientConnection client, WritableByteStream stream)
            throws IOException {
        stream.putDouble(strength);
        stream.putBoolean(side);
        stream.putDouble(direction.doubleX());
        stream.putDouble(direction.doubleY());
    }

    @Override
    public void parseServer(PlayerConnection player, ReadableByteStream stream)
            throws IOException {
        strength = stream.getDouble();
        side = stream.getBoolean();
        direction = new Vector2d(stream.getDouble(), stream.getDouble());
    }

    @Override
    public void runServer(PlayerConnection player, WorldServer world) {
        if (world == null) {
            return;
        }
        if (strength > 1.0 || strength < 0.0) {
            throw new InvalidPacketDataException("Invalid item use strength!");
        }
        if (FastMath.abs(direction.doubleX()) > 180.0 ||
                FastMath.abs(direction.doubleY()) > 90.0) {
            throw new InvalidPacketDataException("Invalid direction!");
        }
        System.out.println(direction.doubleX() + "/" + direction.doubleY());
        ItemStack item;
        if (side) {
            player.mob().attackLeft(strength * strength, direction);
            item = player.mob().leftWeapon();
        } else {
            player.mob().attackRight(strength * strength, direction);
            item = player.mob().rightWeapon();
        }
        item.material().click(player.mob(), item);
        player.mob().onPunch(strength);
        PointerPane pane = player.mob().selectedBlock(direction);
        if (pane != null) {
            Vector3 block = new Vector3i(pane.x, pane.y, pane.z);
            Face face = pane.face;
            double br = item.material()
                    .click(player.mob(), item, world.getTerrain(), block.intX(),
                            block.intY(), block.intZ(), face);
            boolean flag = false;
            if (strength < 0.6) {
                flag = world.getTerrain()
                        .type(block.intX(), block.intY(), block.intZ())
                        .click(world.getTerrain(), block.intX(), block.intY(),
                                block.intZ(), face, player.mob());
            }
            if (!flag && br > 0.0 && strength > 0.0) {
                world.taskExecutor().addTask(() -> {
                            world.getTerrain().queue(handler -> {
                                BlockType type =
                                        handler.type(block.intX(), block.intY(),
                                                block.intZ());
                                int data = handler.data(block.intX(), block.intY(),
                                        block.intZ());
                                double punch = br / type.resistance(item, data) *
                                        strength * strength;
                                if (punch > 0) {
                                    world.playSound(type.breakSound(item, data),
                                            new Vector3d(block.intX() + 0.5,
                                                    block.intY() + 0.5,
                                                    block.intZ() + 0.5), Vector3d.ZERO);
                                    EntityBlockBreakServer entityBreak = null;
                                    for (EntityServer entity : world
                                            .entities(block.intX(), block.intY(),
                                                    block.intZ())) {
                                        if (entity instanceof EntityBlockBreakServer) {
                                            entityBreak =
                                                    (EntityBlockBreakServer) entity;
                                        }
                                    }
                                    if (entityBreak == null) {
                                        entityBreak = new EntityBlockBreakServer(world,
                                                new Vector3d(block.intX() + 0.5,
                                                        block.intY() + 0.5,
                                                        block.intZ() + 0.5));
                                        entityBreak.onSpawn();
                                        world.addEntity(entityBreak);
                                    }
                                    if (entityBreak.punch(world, punch)) {
                                        if (type.destroy(handler, block.intX(),
                                                block.intY(), block.intZ(), face,
                                                player.mob(), item)) {
                                            List<ItemStack> drops =
                                                    type.drops(item, data);
                                            world.dropItems(drops, block.intX(),
                                                    block.intY(), block.intZ());
                                            handler.typeData(block.intX(), block.intY(),
                                                    block.intZ(), handler.world().air(),
                                                    0);
                                        }
                                    }
                                }
                            });
                            return -1;
                        }, "Block-Break", (long) (item.material().hitWait(item) * 0.23),
                        false);
            }
        }
        world.send(new PacketUpdateInventory(player.mob(), "Container"));
    }
}
