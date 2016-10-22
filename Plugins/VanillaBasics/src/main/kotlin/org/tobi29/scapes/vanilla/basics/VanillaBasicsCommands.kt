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

package org.tobi29.scapes.vanilla.basics

import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.engine.utils.hash
import org.tobi29.scapes.engine.utils.io.tag.setDouble
import org.tobi29.scapes.server.ScapesServer
import org.tobi29.scapes.server.command.*
import org.tobi29.scapes.vanilla.basics.generator.EnvironmentOverworldServer
import org.tobi29.scapes.vanilla.basics.util.createIngot
import org.tobi29.scapes.vanilla.basics.util.createTool

internal fun registerCommands(server: ScapesServer,
                              plugin: VanillaBasics) {
    val materials = plugin.materials
    val registry = server.commandRegistry()
    val connection = server.connection

    registry.register("time", 8, { options ->
        options.add("w", "world", true, "World that is targeted")
        options.add("d", "day", true, "Day that time will be set to")
        options.add("t", "time", true,
                "Time of day that time will be set to")
        options.add("r", "relative", false,
                "Add time instead of setting it")
    }) { args, executor, commands ->
        val worldName = args.requireOption('w')
        val relative = args.hasOption('r')
        val dayOption = args.option('d')
        if (dayOption != null) {
            val day = getLong(dayOption)
            commands.add {
                val world = requireGet({ server.world(it) }, worldName)
                val environment = world.environment
                if (environment is EnvironmentOverworldServer) {
                    val climateGenerator = environment.climate()
                    climateGenerator.setDay(
                            day + if (relative) climateGenerator.day() else 0)
                } else {
                    throw Command.CommandException(20,
                            "Unsupported environment")
                }
            }
        }
        val dayTimeOption = args.option('t')
        if (dayTimeOption != null) {
            val dayTime = getDouble(dayTimeOption)
            commands.add {
                val world = requireGet({ server.world(it) }, worldName)
                val environment = world.environment
                if (environment is EnvironmentOverworldServer) {
                    val climateGenerator = environment.climate()
                    val newTime: Double
                    if (relative) {
                        newTime = dayTime + climateGenerator.dayTime()
                    } else {
                        newTime = dayTime
                    }
                    climateGenerator.setDayTime(newTime)
                } else {
                    throw Command.CommandException(20,
                            "Unsupported environment")
                }
            }
        }
    }

    registry.register("hunger", 8, { options ->
        options.add("p", "player", true,
                "Player whose hunger values will be changed")
        options.add("w", "wake", true, "Wake value (0.0-1.0)")
        options.add("s", "saturation", true, "Saturation value (0.0-1.0)")
        options.add("t", "thirst", true, "Thirst value (0.0-1.0)")
    }) { args, executor, commands ->
        val playerName = args.requireOption('p', executor.playerName())
        val wakeOption = args.option('w')
        if (wakeOption != null) {
            val wake = getDouble(wakeOption)
            commands.add {
                val player = requireGet({ connection.playerByName(it) },
                        playerName)
                player.mob { mob ->
                    val conditionTag = mob.metaData("Vanilla").structure(
                            "Condition")
                    synchronized(conditionTag) {
                        conditionTag.setDouble("Wake", wake)
                    }
                }
            }
        }
        val saturationOption = args.option('s')
        if (saturationOption != null) {
            val saturation = getDouble(saturationOption)
            commands.add {
                val player = requireGet({ connection.playerByName(it) },
                        playerName)
                player.mob { mob ->
                    val conditionTag = mob.metaData("Vanilla").structure(
                            "Condition")
                    synchronized(conditionTag) {
                        conditionTag.setDouble("Hunger", saturation)
                    }
                }
            }
        }
        val thirstOption = args.option('t')
        if (thirstOption != null) {
            val thirst = getDouble(thirstOption)
            commands.add {
                val player = requireGet({ connection.playerByName(it) },
                        playerName)
                player.mob { mob ->
                    val conditionTag = mob.metaData("Vanilla").structure(
                            "Condition")
                    synchronized(conditionTag) {
                        conditionTag.setDouble("Thirst", thirst)
                    }
                }
            }
        }
    }

    registry.register("giveingot", 8, { options ->
        options.add("p", "player", true,
                "Player that the item will be given to")
        options.add("m", "metal", true, "Metal type")
        options.add("d", "data", true, "Data value of item")
        options.add("a", "amount", true, "Amount of item in stack")
        options.add("t", "temperature", true, "Temperature of metal")
    }) { args, executor, commands ->
        val playerName = args.requireOption('p', executor.playerName())
        val metal = args.requireOption('m')
        val data = getInt(args.option('d', "0"))
        val amount = getInt(args.option('a', "1"))
        val temperature = getFloat(args.option('t', "0.0"))
        commands.add {
            val player = requireGet({ connection.playerByName(it) }, playerName)
            val metalType = plugin.metalType(metal)
            val item = ItemStack(materials.ingot, data, amount)
            createIngot(item, metalType, temperature)
            player.mob { mob ->
                mob.inventories().modify("Container") { it.add(item) }
            }
        }
    }

    registry.register("givetool", 8, { options ->
        options.add("p", "player", true,
                "Player that the item will be given to")
        options.add("m", "metal", true, "Metal type")
        options.add("d", "data", true, "Data value of item")
        options.add("a", "amount", true, "Amount of item in stack")
        options.add("t", "temperature", true, "Temperature of metal")
        options.add("k", "kind", true, "Kind of tool")
    }) { args, executor, commands ->
        val playerName = args.requireOption('p', executor.playerName())
        val metal = args.requireOption('m')
        val kind = args.requireOption('k')
        val data = getInt(args.option('d', "0"))
        val amount = getInt(args.option('a', "1"))
        val temperature = getFloat(args.option('t', "0.0"))
        commands.add {
            val player = requireGet({ connection.playerByName(it) }, playerName)
            val metalType = plugin.metalType(metal)
            val item = ItemStack(materials.ingot, data, amount)
            createIngot(item, metalType, temperature)
            if (!createTool(plugin, item, kind)) {
                error("Unknown tool kind: " + kind)
            }
            player.mob { mob ->
                mob.inventories().modify("Container") { it.add(item) }
            }
        }
    }

    val worldGroup = registry.group("world")

    worldGroup.register("new NAME", 9, {}) { args, executor, commands ->

        val name = require(args.arg(0), "name")
        commands.add({
            server.registerWorld(
                    { world -> EnvironmentOverworldServer(world, plugin) },
                    name, hash(name, server.seed))
        })
    }

    worldGroup.register("remove NAME", 9, {}) { args, executor, commands ->
        val name = require(args.arg(0), "name")
        commands.add({
            if (!server.removeWorld(name)) {
                error("World not loaded: " + name)
            }
        })
    }

    worldGroup.register("delete NAME", 9, {}) { args, executor, commands ->
        val name = require(args.arg(0), "name")
        commands.add({ server.deleteWorld(name) })
    }
}
