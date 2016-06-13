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
package org.tobi29.scapes.vanilla.basics;

import java8.util.Optional;
import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.chunk.EnvironmentServer;
import org.tobi29.scapes.chunk.WorldServer;
import org.tobi29.scapes.engine.utils.StringUtil;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.server.ScapesServer;
import org.tobi29.scapes.server.command.Command;
import org.tobi29.scapes.server.command.CommandRegistry;
import org.tobi29.scapes.server.connection.PlayerConnection;
import org.tobi29.scapes.server.connection.ServerConnection;
import org.tobi29.scapes.vanilla.basics.generator.ClimateGenerator;
import org.tobi29.scapes.vanilla.basics.generator.EnvironmentOverworldServer;
import org.tobi29.scapes.vanilla.basics.material.MetalType;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;
import org.tobi29.scapes.vanilla.basics.util.IngotUtil;
import org.tobi29.scapes.vanilla.basics.util.ToolUtil;

class VanillaBasicsRegisters {
    static void registerCommands(ScapesServer server, VanillaBasics plugin) {
        VanillaMaterial materials = plugin.getMaterials();
        CommandRegistry registry = server.commandRegistry();
        ServerConnection connection = server.connection();

        registry.register("time", 8, options -> {
            options.add("w", "world", true, "World that is targeted");
            options.add("d", "day", true, "Day that time will be set to");
            options.add("t", "time", true,
                    "Time of day that time will be set to");
            options.add("r", "relative", false,
                    "Add time instead of setting it");
        }, (args, executor, commands) -> {
            String worldName = args.requireOption('w');
            boolean relative = args.hasOption('r');
            Optional<String> dayOption = args.option('d');
            if (dayOption.isPresent()) {
                long day = Command.getLong(dayOption.get());
                commands.add(() -> {
                    WorldServer world =
                            Command.require(server::world, worldName);
                    EnvironmentServer environment = world.environment();
                    if (environment instanceof EnvironmentOverworldServer) {
                        EnvironmentOverworldServer environmentOverworld =
                                (EnvironmentOverworldServer) environment;
                        ClimateGenerator climateGenerator =
                                environmentOverworld.climate();
                        climateGenerator.setDay(day +
                                (relative ? climateGenerator.day() : 0));
                    } else {
                        throw new Command.CommandException(20,
                                "Unsupported environment");
                    }
                });
            }
            Optional<String> dayTimeOption = args.option('t');
            if (dayTimeOption.isPresent()) {
                float dayTime = Command.getFloat(dayTimeOption.get());
                commands.add(() -> {
                    WorldServer world =
                            Command.require(server::world, worldName);
                    EnvironmentServer environment = world.environment();
                    if (environment instanceof EnvironmentOverworldServer) {
                        EnvironmentOverworldServer environmentOverworld =
                                (EnvironmentOverworldServer) environment;
                        ClimateGenerator climateGenerator =
                                environmentOverworld.climate();
                        climateGenerator.setDayTime(dayTime +
                                (relative ? climateGenerator.dayTime() : 0.0f));
                    } else {
                        throw new Command.CommandException(20,
                                "Unsupported environment");
                    }
                });
            }
        });

        registry.register("hunger", 8, options -> {
            options.add("p", "player", true,
                    "Player whose hunger values will be changed");
            options.add("w", "wake", true, "Wake value (0.0-1.0)");
            options.add("s", "saturation", true, "Saturation value (0.0-1.0)");
            options.add("t", "thirst", true, "Thirst value (0.0-1.0)");
        }, (args, executor, commands) -> {
            String playerName = args.requireOption('p', executor.playerName());
            Optional<String> wakeOption = args.option('w');
            if (wakeOption.isPresent()) {
                double wake = Command.getDouble(wakeOption.get());
                commands.add(() -> {
                    PlayerConnection player =
                            Command.require(connection::playerByName,
                                    playerName);
                    player.mob(mob -> {
                        TagStructure conditionTag = mob.metaData("Vanilla")
                                .getStructure("Condition");
                        synchronized (conditionTag) {
                            conditionTag.setDouble("Wake", wake);
                        }
                    });
                });
            }
            Optional<String> saturationOption = args.option('s');
            if (saturationOption.isPresent()) {
                double saturation = Command.getDouble(saturationOption.get());
                commands.add(() -> {
                    PlayerConnection player =
                            Command.require(connection::playerByName,
                                    playerName);
                    player.mob(mob -> {
                        TagStructure conditionTag = mob.metaData("Vanilla")
                                .getStructure("Condition");
                        synchronized (conditionTag) {
                            conditionTag.setDouble("Hunger", saturation);
                        }
                    });
                });
            }
            Optional<String> thirstOption = args.option('t');
            if (thirstOption.isPresent()) {
                double thirst = Command.getDouble(thirstOption.get());
                commands.add(() -> {
                    PlayerConnection player =
                            Command.require(connection::playerByName,
                                    playerName);
                    player.mob(mob -> {
                        TagStructure conditionTag = mob.metaData("Vanilla")
                                .getStructure("Condition");
                        synchronized (conditionTag) {
                            conditionTag.setDouble("Thirst", thirst);
                        }
                    });
                });
            }
        });

        registry.register("giveingot", 8, options -> {
            options.add("p", "player", true,
                    "Player that the item will be given to");
            options.add("m", "metal", true, "Metal type");
            options.add("d", "data", true, "Data value of item");
            options.add("a", "amount", true, "Amount of item in stack");
            options.add("t", "temperature", true, "Temperature of metal");
        }, (args, executor, commands) -> {
            String playerName = args.requireOption('p', executor.playerName());
            String metal = args.requireOption('m');
            int data = Command.getInt(args.option('d', "0"));
            int amount = Command.getInt(args.option('a', "1"));
            float temperature = Command.getFloat(args.option('t', "0.0"));
            commands.add(() -> {
                PlayerConnection player =
                        Command.require(connection::playerByName, playerName);
                MetalType metalType = plugin.metalType(metal);
                ItemStack item = new ItemStack(materials.ingot, data, amount);
                IngotUtil.createIngot(item, metalType, temperature);
                player.mob(mob -> mob.inventories()
                        .modify("Container", inventory -> inventory.add(item)));
            });
        });

        registry.register("givetool", 8, options -> {
            options.add("p", "player", true,
                    "Player that the item will be given to");
            options.add("m", "metal", true, "Metal type");
            options.add("d", "data", true, "Data value of item");
            options.add("a", "amount", true, "Amount of item in stack");
            options.add("t", "temperature", true, "Temperature of metal");
            options.add("k", "kind", true, "Kind of tool");
        }, (args, executor, commands) -> {
            String playerName = args.requireOption('p', executor.playerName());
            String metal = args.requireOption('m');
            String kind = args.requireOption('k');
            int data = Command.getInt(args.option('d', "0"));
            int amount = Command.getInt(args.option('a', "1"));
            float temperature = Command.getFloat(args.option('t', "0.0"));
            commands.add(() -> {
                PlayerConnection player =
                        Command.require(connection::playerByName, playerName);
                MetalType metalType = plugin.metalType(metal);
                ItemStack item = new ItemStack(materials.ingot, data, amount);
                IngotUtil.createIngot(item, metalType, temperature);
                if (!ToolUtil.createTool(plugin, item, kind)) {
                    Command.error("Unknown tool kind: " + kind);
                }
                player.mob(mob -> mob.inventories()
                        .modify("Container", inventory -> inventory.add(item)));
            });
        });

        CommandRegistry worldGroup = registry.group("world");

        worldGroup.register("new NAME", 9, options -> {
        }, (args, executor, commands) -> {
            String name = Command.require(args.arg(0), "name");
            commands.add(() -> server.registerWorld(
                    world -> new EnvironmentOverworldServer(world, plugin),
                    name, StringUtil.hash(name, server.seed())));
        });

        worldGroup.register("remove NAME", 9, options -> {
        }, (args, executor, commands) -> {
            String name = Command.require(args.arg(0), "name");
            commands.add(() -> {
                if (!server.removeWorld(name)) {
                    Command.error("World not loaded: " + name);
                }
            });
        });

        worldGroup.register("delete NAME", 9, options -> {
        }, (args, executor, commands) -> {
            String name = Command.require(args.arg(0), "name");
            commands.add(() -> server.deleteWorld(name));
        });
    }
}
