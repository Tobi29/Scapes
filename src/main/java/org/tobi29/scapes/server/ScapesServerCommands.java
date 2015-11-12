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
package org.tobi29.scapes.server;

import org.tobi29.scapes.block.GameRegistry;
import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.block.Material;
import org.tobi29.scapes.chunk.WorldServer;
import org.tobi29.scapes.engine.utils.ArrayUtil;
import org.tobi29.scapes.engine.utils.StringUtil;
import org.tobi29.scapes.engine.utils.io.ByteBufferStream;
import org.tobi29.scapes.engine.utils.io.ProcessStream;
import org.tobi29.scapes.engine.utils.io.tag.TagStructureJSON;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;
import org.tobi29.scapes.entity.server.MobPlayerServer;
import org.tobi29.scapes.packets.PacketDisconnect;
import org.tobi29.scapes.packets.PacketUpdateInventory;
import org.tobi29.scapes.server.command.Command;
import org.tobi29.scapes.server.command.CommandRegistry;
import org.tobi29.scapes.server.connection.PlayerConnection;
import org.tobi29.scapes.server.connection.ServerConnection;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Pattern;

final class ScapesServerCommands {
    private ScapesServerCommands() {
    }

    static void register(CommandRegistry registry, ScapesServer server) {
        GameRegistry gameRegistry = server.worldFormat().plugins().registry();
        ServerConnection connection = server.connection();

        registry.register("give", 8, options -> {
            options.add("p", "player", true,
                    "Player that the item will be given to");
            options.add("m", "material", true, "Material of item");
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
                player.mob().inventory("Container").add(item);
                player.mob().world()
                        .send(new PacketUpdateInventory(player.mob(),
                                "Container"));
            });
        });

        registry.register("clear", 8, options -> {
        }, (args, executor, commands) -> {
            String[] playerNames = args.args();
            Arrays.stream(playerNames)
                    .forEach(playerName -> commands.add(() -> {
                        PlayerConnection player =
                                Command.require(connection::playerByName,
                                        playerName);
                        player.mob().inventory("Container").clear();
                        player.mob().world()
                                .send(new PacketUpdateInventory(player.mob(),
                                        "Container"));
                    }));
        });

        registry.register("tp", 8, options -> {
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
                        WorldServer world =
                                Command.require(server.worldFormat()::world,
                                        worldOption.get());
                        player.setWorld(world, location);
                    });
                } else {
                    commands.add(() -> {
                        PlayerConnection player =
                                Command.require(connection::playerByName,
                                        playerName);
                        player.mob().setPos(location);
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
                        WorldServer world =
                                Command.require(server.worldFormat()::world,
                                        worldOption.get());
                        MobPlayerServer targetMob = target.mob();
                        player.setWorld(world, targetMob.pos());
                    });
                } else {
                    commands.add(() -> {
                        PlayerConnection player =
                                Command.require(connection::playerByName,
                                        playerName);
                        PlayerConnection target =
                                Command.require(connection::playerByName,
                                        targetName);
                        MobPlayerServer playerMob = target.mob();
                        MobPlayerServer targetMob = target.mob();
                        if (playerMob.world() == targetMob.world()) {
                            playerMob.setPos(targetMob.pos());
                        } else {
                            player.setWorld(targetMob.world(), targetMob.pos());
                        }
                    });
                }
            }
        });

        registry.register("say MESSAGE...", 0, options -> {
            options.add("n", "name", true, "Name used for prefix");
            options.add("r", "raw", false, "Disable prefix");
        }, (args, executor, commands) -> {
            String message;
            if (args.hasOption('r')) {
                Command.requirePermission(executor, 8, 'r');
                message = ArrayUtil.join(args.args(), " ");
            } else {
                String name;
                Optional<String> nameOption = args.option('n');
                if (nameOption.isPresent()) {
                    Command.requirePermission(executor, 8, 'n');
                    name = nameOption.get();
                } else {
                    name = executor.name();
                }
                message = '<' + name + "> " + ArrayUtil.join(args.args(), " ");
            }
            commands.add(() -> server.connection()
                    .message(message, MessageLevel.CHAT));
        });

        registry.register("tell MESSAGE...", 0, options -> {
            options.add("t", "target", true, "Target player");
            options.add("n", "name", true, "Name used for prefix");
            options.add("r", "raw", false, "Disable prefix");
        }, (args, executor, commands) -> {
            String targetName = args.requireOption('t', executor.playerName());
            String message;
            if (args.hasOption('r')) {
                Command.requirePermission(executor, 8, 'r');
                message = ArrayUtil.join(args.args(), " ");
            } else {
                String name;
                Optional<String> nameOption = args.option('n');
                if (nameOption.isPresent()) {
                    Command.requirePermission(executor, 8, 'n');
                    name = nameOption.get();
                } else {
                    name = executor.name();
                }
                message = '[' + name + "] " + ArrayUtil.join(args.args(), " ");
            }
            commands.add(() -> {
                PlayerConnection target =
                        Command.require(connection::playerByName, targetName);
                target.message(message, MessageLevel.CHAT);
            });
        });

        CommandRegistry playersGroup = registry.group("players");

        playersGroup.register("list [MATCH]", 9, options -> {
        }, (args, executor, commands) -> {
            String exp = args.arg(0).orElse("*");
            Pattern pattern = StringUtil.wildcard(exp);
            commands.add(() -> server.connection().players()
                    .map(PlayerConnection::nickname)
                    .filter(nickname -> pattern.matcher(nickname).matches())
                    .forEach(message -> executor
                            .message(message, MessageLevel.FEEDBACK_INFO)));
        });

        playersGroup.register("add PLAYER-ID...", 9, options -> {
        }, (args, executor, commands) -> {
            String[] ids = args.args();
            Arrays.stream(ids).forEach(id -> commands
                    .add(() -> server.worldFormat().playerData().add(id)));
        });

        playersGroup.register("remove PLAYER-ID...", 9, options -> {
        }, (args, executor, commands) -> {
            String[] ids = args.args();
            Arrays.stream(ids).forEach(id -> commands
                    .add(() -> server.worldFormat().playerData().remove(id)));
        });

        playersGroup.register("kick PLAYER-NAME...", 9, options -> {
        }, (args, executor, commands) -> {
            String[] playerNames = args.args();
            String message = "Kick by an Admin!";
            Arrays.stream(playerNames)
                    .forEach(playerName -> commands.add(() -> {
                        PlayerConnection player =
                                Command.require(connection::playerByName,
                                        playerName);
                        player.send(new PacketDisconnect(message));
                    }));
        });

        playersGroup.register("op -l LEVEL PLAYER-NAME...", 10,
                options -> options
                        .add("l", "level", true, "Permission level (0-10)"),
                (args, executor, commands) -> {
                    String[] playerNames = args.args();
                    int permissionLevel =
                            Command.getInt(args.requireOption('l'));
                    Arrays.stream(playerNames)
                            .forEach(playerName -> commands.add(() -> {
                                PlayerConnection player = Command.require(
                                        connection::playerByName, playerName);
                                player.setPermissionLevel(permissionLevel);
                            }));
                });

        CommandRegistry serverGroup = registry.group("server");

        serverGroup.register("stop", 10, options -> {
        }, (args, executor, commands) -> server
                .scheduleStop(ScapesServer.ShutdownReason.STOP));

        serverGroup.register("reload", 10, options -> {
        }, (args, executor, commands) -> server
                .scheduleStop(ScapesServer.ShutdownReason.RELOAD));

        CommandRegistry debugGroup = registry.group("debug");

        debugGroup.register("item", 8, options -> options
                        .add("p", "player", true,
                                "Player holding the item in the left hand to debug"),
                (args, executor, commands) -> {
                    String playerName =
                            args.requireOption('p', executor.playerName());
                    commands.add(() -> {
                        PlayerConnection player =
                                Command.require(connection::playerByName,
                                        playerName);
                        try {
                            ByteBufferStream stream = new ByteBufferStream();
                            TagStructureJSON
                                    .write(player.mob().leftWeapon().save(),
                                            stream);
                            stream.buffer().flip();
                            String str = ProcessStream
                                    .process(stream, ProcessStream.asString());
                            executor.message(str, MessageLevel.FEEDBACK_INFO);
                        } catch (IOException e) {
                            executor.message("Failed to serialize item",
                                    MessageLevel.FEEDBACK_ERROR);
                        }
                    });
                });
    }
}
