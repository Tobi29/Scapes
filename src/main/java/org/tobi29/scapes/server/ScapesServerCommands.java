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
import org.tobi29.scapes.engine.utils.ArrayUtil;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;
import org.tobi29.scapes.packets.PacketChat;
import org.tobi29.scapes.packets.PacketDisconnect;
import org.tobi29.scapes.server.command.Command;
import org.tobi29.scapes.server.command.CommandRegistry;
import org.tobi29.scapes.server.connection.PlayerConnection;

final class ScapesServerCommands {
    private ScapesServerCommands() {
    }

    static void register(CommandRegistry registry, ScapesServer server) {
        GameRegistry gameRegistry =
                server.getWorldFormat().getPlugins().getRegistry();

        registry.register("give", 8, options -> {
            options.add("p", "player", true,
                    "Player that the item will be given to");
            options.add("m", "material", true, "Material of item");
            options.add("d", "data", true, "Data value of item");
            options.add("a", "amount", true, "Amount of item in stack");
        }, (args, executor, commands) -> {
            String playerName =
                    args.getOption('p', executor.getPlayerName().orElse(null));
            Command.require(playerName, 'p');
            String materialName = args.getOption('m');
            Command.require(materialName, 'm');
            int data = Command.getInt(args.getOption('d', "0"));
            int amount = Command.getInt(args.getOption('a', "1"));
            commands.add(() -> {
                PlayerConnection player =
                        server.getConnection().getPlayerByName(playerName);
                Command.require(player, playerName);
                Material material = gameRegistry.getMaterial(materialName);
                Command.require(material, materialName);
                ItemStack item = new ItemStack(material, data, amount);
                player.getMob().getInventory().add(item);
            });
        });

        registry.register("clear", 8, options -> options
                        .add("p", "player", true,
                                "Player whose inventory will be cleared"),
                (args, executor, commands) -> {
                    String playerName = args.getOption('p',
                            executor.getPlayerName().orElse(null));
                    Command.require(playerName, 'p');
                    commands.add(() -> {
                        PlayerConnection player = server.getConnection()
                                .getPlayerByName(playerName);
                        Command.require(player, playerName);
                        player.getMob().getInventory().clear();
                    });
                });

        registry.register("tp", 8, options -> {
            options.add("p", "player", true, "Player who will be teleported");
            options.add("t", "target", true,
                    "Target that the player will be teleported to");
            options.add("l", "location", 3,
                    "Target that the player will be teleported to");
        }, (args, executor, commands) -> {
            String playerName =
                    args.getOption('p', executor.getPlayerName().orElse(null));
            Command.require(playerName, 'p');
            if (args.hasOption('l')) {
                String[] locationStr = args.getOptionArray('l');
                Command.require(locationStr, 'l');
                Vector3d location = Command.getVector3d(locationStr);
                commands.add(() -> {
                    PlayerConnection player =
                            server.getConnection().getPlayerByName(playerName);
                    Command.require(player, playerName);
                    player.getMob().setPos(location);
                });
            } else {
                String targetName = args.getOption('t',
                        executor.getPlayerName().orElse(null));
                Command.require(playerName, 't');
                commands.add(() -> {
                    PlayerConnection player =
                            server.getConnection().getPlayerByName(playerName);
                    Command.require(player, playerName);
                    PlayerConnection target =
                            server.getConnection().getPlayerByName(targetName);
                    Command.require(target, targetName);
                    player.getMob().setPos(target.getMob().getPos());
                });
            }
        });

        registry.register("say", 0, options -> {
            options.add("n", "name", true, "Name used for prefix");
            options.add("r", "raw", false, "Disable prefix");
        }, (args, executor, commands) -> {
            String message;
            if (args.hasOption('r')) {
                Command.requirePermission(executor, 8, 'r');
                message = ArrayUtil.join(args.getArgs(), " ");
            } else {
                String name;
                if (args.hasOption('n')) {
                    Command.requirePermission(executor, 8, 'n');
                    name = args.getOption('n');
                } else {
                    name = executor.getName();
                }
                Command.require(name, 'n');
                message =
                        '<' + name + "> " + ArrayUtil.join(args.getArgs(), " ");
            }
            commands.add(
                    () -> server.getConnection().send(new PacketChat(message)));
        });

        registry.register("tell", 0, options -> {
            options.add("t", "target", true, "Target player");
            options.add("n", "name", true, "Name used for prefix");
            options.add("r", "raw", false, "Disable prefix");
        }, (args, executor, commands) -> {
            String targetName = args.getOption('t');
            Command.require(targetName, 't');
            String message;
            if (args.hasOption('r')) {
                Command.requirePermission(executor, 8, 'r');
                message = ArrayUtil.join(args.getArgs(), " ");
            } else {
                String name;
                if (args.hasOption('n')) {
                    Command.requirePermission(executor, 8, 'n');
                    name = args.getOption('n');
                } else {
                    name = executor.getName();
                }
                Command.require(name, 'n');
                message =
                        '[' + name + "] " + ArrayUtil.join(args.getArgs(), " ");
            }
            commands.add(() -> {
                PlayerConnection target =
                        server.getConnection().getPlayerByName(targetName);
                Command.require(target, targetName);
                target.tell(message);
            });
        });

        registry.register("kick", 9, options -> options
                        .add("p", "player", true, "Player to be kicked"),
                (args, executor, commands) -> {
                    String playerName = args.getOption('p');
                    Command.require(playerName, 'p');
                    String message;
                    String[] messageArray = args.getArgs();
                    if (messageArray.length == 0) {
                        message = "Kick by an Admin!";
                    } else {
                        message = ArrayUtil.join(args.getArgs(), " ");
                    }
                    commands.add(() -> {
                        PlayerConnection player = server.getConnection()
                                .getPlayerByName(playerName);
                        Command.require(player, playerName);
                        player.send(new PacketDisconnect(message));
                    });
                });

        registry.register("op", 10, options -> {
            options.add("p", "player", true,
                    "Player whose hunger values will be changed");
            options.add("l", "level", true, "Permission level (0-10)");
        }, (args, executor, commands) -> {
            String playerName =
                    args.getOption('p', executor.getPlayerName().orElse(null));
            Command.require(playerName, 'p');
            Command.require(args, 'l');
            int permissionLevel = Command.getInt(args.getOption('l'));
            commands.add(() -> {
                PlayerConnection player =
                        server.getConnection().getPlayerByName(playerName);
                Command.require(player, playerName);
                player.setPermissionLevel(permissionLevel);
            });
        });

        registry.register("stop", 10, options -> {
        }, (args, executor, commands) -> server.getTaskExecutor().runTask(
                joiner -> server.stop(ScapesServer.ShutdownReason.STOP),
                "Server-Stop"));

        registry.register("reload", 10, options -> {
        }, (args, executor, commands) -> server.getTaskExecutor().runTask(
                joiner -> server.stop(ScapesServer.ShutdownReason.RELOAD),
                "Server-Reload"));
    }
}
