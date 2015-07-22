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
import org.tobi29.scapes.packets.PacketDisconnect;
import org.tobi29.scapes.server.command.Command;
import org.tobi29.scapes.server.command.CommandRegistry;
import org.tobi29.scapes.server.connection.PlayerConnection;
import org.tobi29.scapes.server.format.PlayerBans;

final class ScapesServerCommands {
    private ScapesServerCommands() {
    }

    static void register(CommandRegistry registry, ScapesServer server) {
        GameRegistry gameRegistry = server.worldFormat().plugins().registry();

        registry.register("give", 8, options -> {
            options.add("p", "player", true,
                    "Player that the item will be given to");
            options.add("m", "material", true, "Material of item");
            options.add("d", "data", true, "Data value of item");
            options.add("a", "amount", true, "Amount of item in stack");
        }, (args, executor, commands) -> {
            String playerName =
                    args.getOption('p', executor.playerName().orElse(null));
            Command.require(playerName, 'p');
            String materialName = args.getOption('m');
            Command.require(materialName, 'm');
            int data = Command.getInt(args.getOption('d', "0"));
            int amount = Command.getInt(args.getOption('a', "1"));
            commands.add(() -> {
                PlayerConnection player =
                        server.connection().playerByName(playerName);
                Command.require(player, playerName);
                Material material = gameRegistry.material(materialName);
                Command.require(material, materialName);
                ItemStack item = new ItemStack(material, data, amount);
                player.mob().inventory().add(item);
            });
        });

        registry.register("clear", 8, options -> options
                        .add("p", "player", true,
                                "Player whose inventory will be cleared"),
                (args, executor, commands) -> {
                    String playerName = args.getOption('p',
                            executor.playerName().orElse(null));
                    Command.require(playerName, 'p');
                    commands.add(() -> {
                        PlayerConnection player =
                                server.connection().playerByName(playerName);
                        Command.require(player, playerName);
                        player.mob().inventory().clear();
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
                    args.getOption('p', executor.playerName().orElse(null));
            Command.require(playerName, 'p');
            if (args.hasOption('l')) {
                String[] locationStr = args.getOptionArray('l');
                Command.require(locationStr, 'l');
                Vector3d location = Command.getVector3d(locationStr);
                commands.add(() -> {
                    PlayerConnection player =
                            server.connection().playerByName(playerName);
                    Command.require(player, playerName);
                    player.mob().setPos(location);
                });
            } else {
                String targetName =
                        args.getOption('t', executor.playerName().orElse(null));
                Command.require(playerName, 't');
                commands.add(() -> {
                    PlayerConnection player =
                            server.connection().playerByName(playerName);
                    Command.require(player, playerName);
                    PlayerConnection target =
                            server.connection().playerByName(targetName);
                    Command.require(target, targetName);
                    player.mob().setPos(target.mob().pos());
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
                    name = executor.name();
                }
                Command.require(name, 'n');
                message =
                        '<' + name + "> " + ArrayUtil.join(args.getArgs(), " ");
            }
            commands.add(() -> server.connection().chat(message));
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
                    name = executor.name();
                }
                Command.require(name, 'n');
                message =
                        '[' + name + "] " + ArrayUtil.join(args.getArgs(), " ");
            }
            commands.add(() -> {
                PlayerConnection target =
                        server.connection().playerByName(targetName);
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
                        message = ArrayUtil.join(messageArray, " ");
                    }
                    commands.add(() -> {
                        PlayerConnection player =
                                server.connection().playerByName(playerName);
                        Command.require(player, playerName);
                        player.send(new PacketDisconnect(message));
                    });
                });

        registry.register("ban", 9, options -> {
            options.add("p", "player", true, "Player to be banned");
            options.add("i", "id", true, "ID of ban");
            options.add("k", "key", false, "Add key to ban entry");
            options.add("a", "address", false, "Add address to ban entry");
            options.add("n", "nickname", false, "Add nickname to ban entry");
        }, (args, executor, commands) -> {
            String playerName = args.getOption('p');
            Command.require(playerName, 'p');
            boolean key = args.hasOption('k');
            boolean address = args.hasOption('a');
            boolean banNickname = args.hasOption('n');
            boolean banKey, banAddress;
            if (!key && !address && !banNickname) {
                banKey = true;
                banAddress = true;
            } else {
                banKey = key;
                banAddress = address;
            }
            String id = args.getOption('i', playerName);
            String reason = ArrayUtil.join(args.getArgs(), " ");
            commands.add(() -> {
                PlayerConnection player =
                        server.connection().playerByName(playerName);
                Command.require(player, playerName);
                server.worldFormat().playerBans()
                        .ban(player, id, reason, banKey, banAddress,
                                banNickname);
            });
        });

        registry.register("unban", 9,
                options -> options.add("i", "id", true, "ID of ban"),
                (args, executor, commands) -> {
                    String id = args.getOption('i');
                    Command.require(id, 'i');
                    commands.add(
                            () -> server.worldFormat().playerBans().unban(id));
                });

        registry.register("banlist", 9, options -> {
        }, (args, executor, commands) -> commands
                .add(() -> server.worldFormat().playerBans().entries()
                        .map(PlayerBans.Entry::toString)
                        .forEach(executor::tell)));

        registry.register("op", 10, options -> {
            options.add("p", "player", true,
                    "Player whose hunger values will be changed");
            options.add("l", "level", true, "Permission level (0-10)");
        }, (args, executor, commands) -> {
            String playerName =
                    args.getOption('p', executor.playerName().orElse(null));
            Command.require(playerName, 'p');
            Command.require(args, 'l');
            int permissionLevel = Command.getInt(args.getOption('l'));
            commands.add(() -> {
                PlayerConnection player =
                        server.connection().playerByName(playerName);
                Command.require(player, playerName);
                player.setPermissionLevel(permissionLevel);
            });
        });

        registry.register("stop", 10, options -> {
        }, (args, executor, commands) -> server
                .scheduleStop(ScapesServer.ShutdownReason.STOP));

        registry.register("reload", 10, options -> {
        }, (args, executor, commands) -> server
                .scheduleStop(ScapesServer.ShutdownReason.RELOAD));
    }
}
