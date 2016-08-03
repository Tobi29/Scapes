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

package org.tobi29.scapes.server.extension.base;

import java8.util.Optional;
import org.tobi29.scapes.block.GameRegistry;
import org.tobi29.scapes.block.Inventory;
import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.block.Material;
import org.tobi29.scapes.chunk.WorldServer;
import org.tobi29.scapes.engine.utils.Streams;
import org.tobi29.scapes.engine.utils.io.ByteBufferStream;
import org.tobi29.scapes.engine.utils.io.ProcessStream;
import org.tobi29.scapes.engine.utils.io.tag.json.TagStructureJSON;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;
import org.tobi29.scapes.server.MessageLevel;
import org.tobi29.scapes.server.ScapesServer;
import org.tobi29.scapes.server.command.Command;
import org.tobi29.scapes.server.command.CommandRegistry;
import org.tobi29.scapes.server.connection.PlayerConnection;
import org.tobi29.scapes.server.connection.ServerConnection;
import org.tobi29.scapes.server.extension.ServerExtension;
import org.tobi29.scapes.server.extension.ServerExtensions;

import java.io.IOException;

public class DebugCommandsExtension extends ServerExtension {
    public DebugCommandsExtension(ScapesServer server) {
        super(server);
    }

    @Override
    public void init(ServerExtensions.Registrar registrar) {
        GameRegistry gameRegistry = server.plugins().registry();
        ServerConnection connection = server.connection();
        CommandRegistry group = server.commandRegistry().group("debug");

        group.register("give", 8, options -> {
            options.add("p", "player", true,
                    "Player that the item will be given to");
            options.add("m", "item", true, "Material of item");
            options.add("d", "data", true, "Data value of item");
            options.add("a", "amount", true, "Amount of item in stack");
        }, (args, executor, commands) -> {
            String playerName = args.requireOption('p', executor.playerName());
            String materialName = args.requireOption('m');
            int data = Command.getInt(args.option('d', "0"));
            int amount = Command.getInt(args.option('a', "1"));
            commands.add(() -> {
                PlayerConnection player =
                        Command.require(connection::playerByName, playerName);
                Material material =
                        Command.require(gameRegistry::material, materialName);
                ItemStack item = new ItemStack(material, data, amount);
                player.mob(mob -> mob.inventories()
                        .modify("Container", inventory -> inventory.add(item)));
            });
        });

        group.register("clear", 8, options -> {
        }, (args, executor, commands) -> {
            String[] playerNames = args.args();
            Streams.forEach(playerNames, playerName -> commands.add(() -> {
                PlayerConnection player =
                        Command.require(connection::playerByName, playerName);
                player.mob(mob -> mob.inventories()
                        .modify("Container", Inventory::clear));
            }));
        });

        group.register("tp", 8, options -> {
            options.add("p", "player", true, "Player who will be teleported");
            options.add("t", "target", true,
                    "Target that the player will be teleported to");
            options.add("w", "world", true,
                    "World that the player will be teleported to");
            options.add("l", "location", 3,
                    "Target that the player will be teleported to");
        }, (args, executor, commands) -> {
            String playerName = args.requireOption('p', executor.playerName());
            Optional<String> worldOption = args.option('w');
            Optional<String[]> locationOption = args.optionArray('l');
            if (locationOption.isPresent()) {
                Vector3d location = Command.getVector3d(locationOption.get());
                if (worldOption.isPresent()) {
                    commands.add(() -> {
                        PlayerConnection player =
                                Command.require(connection::playerByName,
                                        playerName);
                        WorldServer world = Command.require(server::world,
                                worldOption.get());
                        player.setWorld(world, location);
                    });
                } else {
                    commands.add(() -> {
                        PlayerConnection player =
                                Command.require(connection::playerByName,
                                        playerName);
                        player.mob(mob -> mob.setPos(location));
                    });
                }
            } else {
                String targetName =
                        args.requireOption('t', executor.playerName());
                if (worldOption.isPresent()) {
                    commands.add(() -> {
                        PlayerConnection player =
                                Command.require(connection::playerByName,
                                        playerName);
                        PlayerConnection target =
                                Command.require(connection::playerByName,
                                        targetName);
                        WorldServer world = Command.require(server::world,
                                worldOption.get());
                        target.mob(mob -> player.setWorld(world, mob.pos()));
                    });
                } else {
                    commands.add(() -> {
                        PlayerConnection player =
                                Command.require(connection::playerByName,
                                        playerName);
                        PlayerConnection target =
                                Command.require(connection::playerByName,
                                        targetName);
                        player.mob(mob -> target.mob(targetMob -> {
                            if (mob.world() == targetMob.world()) {
                                mob.setPos(targetMob.pos());
                            } else {
                                player.setWorld(targetMob.world(),
                                        targetMob.pos());
                            }
                        }));
                    });
                }
            }
        });

        group.register("item", 8, options -> options.add("p", "player", true,
                "Player holding the item in the left hand to debug"),
                (args, executor, commands) -> {
                    String playerName =
                            args.requireOption('p', executor.playerName());
                    commands.add(() -> {
                        PlayerConnection player =
                                Command.require(connection::playerByName,
                                        playerName);
                        player.mob(mob -> {
                            try {
                                ByteBufferStream stream =
                                        new ByteBufferStream();
                                TagStructureJSON
                                        .write(mob.leftWeapon().save(), stream);
                                stream.buffer().flip();
                                String str = ProcessStream.process(stream,
                                        ProcessStream.asString());
                                executor.message(str,
                                        MessageLevel.FEEDBACK_INFO);
                            } catch (IOException e) {
                                executor.message("Failed to serialize item",
                                        MessageLevel.FEEDBACK_ERROR);
                            }
                        });
                    });
                });
    }
}
