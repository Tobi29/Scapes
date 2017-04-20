/*
 * Copyright 2012-2017 Tobi29
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
import org.tobi29.scapes.engine.args.*
import org.tobi29.scapes.engine.utils.hash
import org.tobi29.scapes.engine.utils.tag.set
import org.tobi29.scapes.engine.utils.tag.syncMapMut
import org.tobi29.scapes.server.ScapesServer
import org.tobi29.scapes.vanilla.basics.util.createIngot
import org.tobi29.scapes.vanilla.basics.util.createTool
import org.tobi29.scapes.vanilla.basics.world.EnvironmentOverworldServer

internal fun registerCommands(server: ScapesServer,
                              plugin: VanillaBasics) {
    val materials = plugin.materials
    val registry = server.commandRegistry()
    val connection = server.connection

    registry.register("time", 8) {
        val worldOption = CommandOption(setOf('w'), setOf("world"), 1,
                "World that is targeted").also { add(it) }
        val dayOption = CommandOption(setOf('d'), setOf("day"), 1,
                "Day that time will be set to").also { add(it) }
        val timeOption = CommandOption(setOf('t'), setOf("time"), 1,
                "Time of day that time will be set to").also { add(it) }
        val relativeOption = CommandOption(setOf('r'), setOf("relative"),
                "Add time instead of setting it").also { add(it) }
        return@register { args, executor, commands ->
            val worldName = args.require(worldOption)
            val relative = args.getBoolean(relativeOption)
            val day = args.getLong(dayOption)
            if (day != null) {
                commands.add {
                    val world = requireGet({ server.world(it) }, worldName)
                    val environment = world.environment
                    if (environment is EnvironmentOverworldServer) {
                        val climateGenerator = environment.climate()
                        climateGenerator.setDay(
                                day + if (relative) climateGenerator.day() else 0)
                    } else {
                        error("Unsupported environment")
                    }
                }
            }
            val dayTime = args.getDouble(timeOption)
            if (dayTime != null) {
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
                        error("Unsupported environment")
                    }
                }
            }
        }
    }

    registry.register("hunger", 8) {
        val playerOption = CommandOption(setOf('p'), setOf("player"), 1,
                "Player whose hunger values will be changed").also { add(it) }
        val wakeOption = CommandOption(setOf('w'), setOf("wake"), 1,
                "Wake value (0.0-1.0)").also { add(it) }
        val saturationOption = CommandOption(setOf('s'), setOf("saturation"), 1,
                "Saturation value (0.0-1.0)").also { add(it) }
        val thirstOption = CommandOption(setOf('t'), setOf("thirst"), 1,
                "Thirst value (0.0-1.0)").also { add(it) }
        return@register { args, executor, commands ->
            val playerName = args.require(
                    playerOption) { it ?: executor.playerName() }
            args.getDouble(wakeOption)?.let { wake ->
                commands.add {
                    val player = requireGet({ connection.playerByName(it) },
                            playerName)
                    player.mob { mob ->
                        mob.metaData("Vanilla").syncMapMut(
                                "Condition") { conditionTag ->
                            conditionTag["Wake"] = wake
                        }
                    }
                }
            }
            args.getDouble(saturationOption)?.let { saturation ->
                commands.add {
                    val player = requireGet({ connection.playerByName(it) },
                            playerName)
                    player.mob { mob ->
                        mob.metaData("Vanilla").syncMapMut(
                                "Condition") { conditionTag ->
                            conditionTag["Hunger"] = saturation
                        }
                    }
                }
            }
            args.getDouble(thirstOption)?.let { thirst ->
                commands.add {
                    val player = requireGet({ connection.playerByName(it) },
                            playerName)
                    player.mob { mob ->
                        mob.metaData("Vanilla").syncMapMut(
                                "Condition") { conditionTag ->
                            conditionTag["Thirst"] = thirst
                        }
                    }
                }
            }
        }
    }

    registry.register("giveingot", 8) {
        val playerOption = CommandOption(setOf('p'), setOf("player"), 1,
                "Player that the item will be given to").also { add(it) }
        val metalOption = CommandOption(setOf('m'), setOf("metal"), 1,
                "Metal type").also { add(it) }
        val dataOption = CommandOption(setOf('d'), setOf("data"), 1,
                "Data value of item").also { add(it) }
        val amountOption = CommandOption(setOf('a'), setOf("amount"), 1,
                "Amount of item in stack").also { add(it) }
        val temperatureOption = CommandOption(setOf('t'), setOf("temperature"),
                1, "Temperature of metal").also { add(it) }
        return@register { args, executor, commands ->
            val playerName = args.require(
                    playerOption) { it ?: executor.playerName() }
            val metal = args.require(metalOption)
            val data = args.getInt(dataOption) ?: 0
            val amount = args.getInt(amountOption) ?: 1
            val temperature = args.getDouble(temperatureOption) ?: 0.0
            commands.add {
                val player = requireGet({ connection.playerByName(it) },
                        playerName)
                val alloyType = requireGet({ plugin.alloyType(metal) }, metal)
                val item = ItemStack(materials.ingot, data, amount)
                createIngot(item, alloyType)
                item.metaData("Vanilla")["Temperature"] = temperature
                player.mob { mob ->
                    mob.inventories().modify("Container") { it.add(item) }
                }
            }
        }
    }

    registry.register("givetool", 8) {
        val playerOption = CommandOption(setOf('p'), setOf("player"), 1,
                "Player that the item will be given to").also { add(it) }
        val metalOption = CommandOption(setOf('m'), setOf("metal"), 1,
                "Metal type").also { add(it) }
        val dataOption = CommandOption(setOf('d'), setOf("data"), 1,
                "Data value of item").also { add(it) }
        val amountOption = CommandOption(setOf('a'), setOf("amount"), 1,
                "Amount of item in stack").also { add(it) }
        val temperatureOption = CommandOption(setOf('t'), setOf("temperature"),
                1, "Temperature of metal").also { add(it) }
        val kindOption = CommandOption(setOf('k'), setOf("kind"), 1,
                "Kind of tool").also { add(it) }
        return@register { args, executor, commands ->
            val playerName = args.require(
                    playerOption) { it ?: executor.playerName() }
            val metal = args.require(metalOption)
            val kind = args.require(kindOption)
            val data = args.getInt(dataOption) ?: 0
            val amount = args.getInt(amountOption) ?: 1
            val temperature = args.getDouble(temperatureOption) ?: 0.0
            commands.add {
                val player = requireGet({ connection.playerByName(it) },
                        playerName)
                val alloyType = requireGet({ plugin.alloyType(metal) }, metal)
                val item = ItemStack(materials.ingot, data, amount)
                createIngot(item, alloyType)
                item.metaData("Vanilla")["Temperature"] = temperature
                if (!createTool(plugin, item, kind)) {
                    error("Unknown tool kind: " + kind)
                }
                player.mob { mob ->
                    mob.inventories().modify("Container") { it.add(item) }
                }
            }
        }
    }

    val worldGroup = registry.group("world")

    worldGroup.register("new NAME...", 9) {
        return@register { args, _, commands ->
            args.arguments.forEach {
                commands.add {
                    server.registerWorld({ plugin.createEnvironment(it) }, it,
                            hash(it, server.seed))
                }
            }
        }
    }

    worldGroup.register("remove NAME...", 9) {
        return@register { args, _, commands ->
            args.arguments.forEach {
                commands.add {
                    if (!server.removeWorld(it)) {
                        error("World not loaded: $it")
                    }
                }
            }
        }
    }

    worldGroup.register("delete NAME...", 9) {
        return@register { args, _, commands ->
            args.arguments.forEach {
                commands.add { server.deleteWorld(it) }
            }
        }
    }
}
