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

import org.tobi29.args.*
import org.tobi29.io.tag.toTag
import org.tobi29.scapes.block.inventories
import org.tobi29.scapes.server.ScapesServer
import org.tobi29.scapes.vanilla.basics.entity.server.ComponentMobLivingServerCondition
import org.tobi29.scapes.vanilla.basics.material.copy
import org.tobi29.scapes.vanilla.basics.util.Alloy
import org.tobi29.scapes.vanilla.basics.util.createTool
import org.tobi29.scapes.vanilla.basics.util.id
import org.tobi29.scapes.vanilla.basics.util.toIngot
import org.tobi29.scapes.vanilla.basics.world.EnvironmentOverworldServer
import org.tobi29.stdex.longHashCode

internal fun registerCommands(
    server: ScapesServer,
    plugin: VanillaBasics
) {
    val materials = plugin.materials
    val registry = server.commandRegistry
    val connection = server.connection

    registry.register("time", 8) {
        val worldOption = commandOption(
            setOf('w'), setOf("world"), listOf("value"),
            "World that is targeted"
        )
        val dayOption = commandOption(
            setOf('d'), setOf("day"), listOf("value"),
            "Day that time will be set to"
        )
        val timeOption = commandOption(
            setOf('t'), setOf("time"), listOf("value"),
            "Time of day that time will be set to"
        )
        val relativeOption = commandFlag(
            setOf('r'), setOf("relative"),
            "Add time instead of setting it"
        )
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
                            day + if (relative) climateGenerator.day() else 0
                        )
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
        val playerOption = commandOption(
            setOf('p'), setOf("player"), listOf("name"),
            "Player whose hunger values will be changed"
        )
        val wakeOption = commandOption(
            setOf('w'), setOf("wake"), listOf("value"),
            "Wake value (0.0-1.0)"
        )
        val saturationOption = commandOption(
            setOf('s'), setOf("saturation"), listOf("value"),
            "Saturation value (0.0-1.0)"
        )
        val thirstOption = commandOption(
            setOf('t'), setOf("thirst"), listOf("value"),
            "Thirst value (0.0-1.0)"
        )
        return@register { args, executor, commands ->
            val playerName = args.require(
                playerOption
            ) { it ?: executor.playerName() }
            args.getDouble(wakeOption)?.let { wake ->
                commands.add {
                    val player = requireGet(
                        { connection.playerByName(it) },
                        playerName
                    )
                    player.mob { mob ->
                        mob.getOrNull(
                            ComponentMobLivingServerCondition.COMPONENT
                        )?.let {
                            it.wake = wake
                        }
                    }
                }
            }
            args.getDouble(saturationOption)?.let { saturation ->
                commands.add {
                    val player = requireGet(
                        { connection.playerByName(it) },
                        playerName
                    )
                    player.mob { mob ->
                        mob.getOrNull(
                            ComponentMobLivingServerCondition.COMPONENT
                        )?.let {
                            it.hunger = saturation
                        }
                    }
                }
            }
            args.getDouble(thirstOption)?.let { thirst ->
                commands.add {
                    val player = requireGet(
                        { connection.playerByName(it) },
                        playerName
                    )
                    player.mob { mob ->
                        mob.getOrNull(
                            ComponentMobLivingServerCondition.COMPONENT
                        )?.let {
                            it.thirst = thirst
                        }
                    }
                }
            }
        }
    }

    registry.register("giveingot", 8) {
        val playerOption = commandOption(
            setOf('p'), setOf("player"), listOf("name"),
            "Player that the item will be given to"
        )
        val metalOption = commandOption(
            setOf('m'), setOf("metal"), listOf("name"),
            "Metal type"
        )
        val dataOption = commandOption(
            setOf('d'), setOf("data"), listOf("value"),
            "Data value of item"
        )
        val temperatureOption = commandOption(
            setOf('t'), setOf("temperature"), listOf("value"),
            "Temperature of metal"
        )
        return@register { args, executor, commands ->
            val playerName = args.require(
                playerOption
            ) { it ?: executor.playerName() }
            val metal = args.require(metalOption)
            val data = args.getInt(dataOption) ?: 0
            val temperature = args.getDouble(temperatureOption) ?: 0.0
            commands.add {
                val player = requireGet(
                    { connection.playerByName(it) },
                    playerName
                )
                val alloyType = requireGet({ plugin.alloyType(metal) }, metal)
                val ingot = alloyType.ingredients.toIngot(plugin)
                val item = ingot.copy(
                    temperature = temperature,
                    metaData = (ingot.metaData +
                            ("Data" to data.toTag())).toTag()
                )
                player.mob { mob ->
                    mob.inventories.modify("Container") { it.add(item) }
                }
            }
        }
    }

    registry.register("givetool", 8) {
        val playerOption = commandOption(
            setOf('p'), setOf("player"), listOf("name"),
            "Player that the item will be given to"
        )
        val metalOption = commandOption(
            setOf('m'), setOf("metal"), listOf("name"),
            "Metal type"
        )
        val dataOption = commandOption(
            setOf('d'), setOf("data"), listOf("value"),
            "Data value of item"
        )
        val temperatureOption = commandOption(
            setOf('t'), setOf("temperature"), listOf("value"),
            "Temperature of metal"
        )
        val kindOption = commandOption(
            setOf('k'), setOf("kind"), listOf("value"),
            "Kind of tool"
        )
        return@register { args, executor, commands ->
            val playerName = args.require(
                playerOption
            ) { it ?: executor.playerName() }
            val metal = args.require(metalOption)
            val kind = args.require(kindOption)
            val data = args.getInt(dataOption) ?: 0
            val temperature = args.getDouble(temperatureOption) ?: 0.0
            commands.add {
                val player = requireGet(
                    { connection.playerByName(it) },
                    playerName
                )
                val alloyType = requireGet({ plugin.alloyType(metal) }, metal)
                val kindId = id(kind)
                if (kindId < 0) error("Unknown tool kind: " + kind)
                val tool = createTool(
                    plugin, kindId,
                    Alloy(alloyType.ingredients), temperature
                )
                val item = tool.copy(
                    metaData = (tool.metaData +
                            ("Data" to data.toTag())).toTag()
                )
                player.mob { mob ->
                    mob.inventories.modify("Container") {
                        it.add(item)
                    }
                }
            }
        }
    }

    val worldGroup = registry.group("world")

    worldGroup.register("new", 9) {
        val worldArgument = commandArgument(
            "world", 0..Integer.MAX_VALUE
        )
        return@register { args, _, commands ->
            args.arguments[worldArgument]?.forEach {
                commands.add {
                    server.registerWorld(
                        { plugin.createEnvironment(it) }, it,
                        it.longHashCode(server.seed)
                    )
                }
            }
        }
    }

    worldGroup.register("remove", 9) {
        val worldArgument = commandArgument(
            "world", 0..Integer.MAX_VALUE
        )
        return@register { args, _, commands ->
            args.arguments[worldArgument]?.forEach {
                commands.add {
                    if (!server.removeWorld(it)) {
                        error("World not loaded: $it")
                    }
                }
            }
        }
    }

    worldGroup.register("delete", 9) {
        val worldArgument = commandArgument(
            "world", 0..Integer.MAX_VALUE
        )
        return@register { args, _, commands ->
            args.arguments[worldArgument]?.forEach {
                commands.add { server.deleteWorld(it) }
            }
        }
    }
}
