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

package org.tobi29.scapes.server.extension.base

import org.tobi29.args.CommandOption
import org.tobi29.args.get
import org.tobi29.args.getInt
import org.tobi29.args.require
import org.tobi29.io.IOException
import org.tobi29.io.MemoryViewStreamDefault
import org.tobi29.io.asString
import org.tobi29.io.tag.TagMap
import org.tobi29.io.tag.json.writeJSON
import org.tobi29.io.tag.toTag
import org.tobi29.scapes.block.inventories
import org.tobi29.scapes.inventory.Item
import org.tobi29.scapes.inventory.toTag
import org.tobi29.scapes.server.MessageLevel
import org.tobi29.scapes.server.ScapesServer
import org.tobi29.scapes.server.command.getVector3d
import org.tobi29.scapes.server.extension.ServerExtension
import org.tobi29.scapes.server.extension.event.MessageEvent
import org.tobi29.scapes.server.extension.spi.ServerExtensionProvider

class DebugCommandsExtension(server: ScapesServer) : ServerExtension(server) {

    override fun init() {
        val plugins = server.plugins
        val connection = server.connection
        val group = server.commandRegistry().group("debug")

        group.register("give", 8) {
            val playerOption = CommandOption(setOf('p'), setOf("player"),
                    listOf("name"),
                    "Player that the item will be given to").also { add(it) }
            val materialOption = CommandOption(setOf('m'), setOf("material"),
                    listOf("name"), "Material of item").also { add(it) }
            val dataOption = CommandOption(setOf('d'), setOf("data"),
                    listOf("value"), "Data value of item").also { add(it) }
            val amountOption = CommandOption(setOf('a'), setOf("amount"),
                    listOf("value"), "Amount of item in stack").also { add(it) }
            return@register { args, executor, commands ->
                val playerName = args.require(
                        playerOption) { it ?: executor.playerName() }
                val materialName = args.require(materialOption)
                val data = args.getInt(dataOption) ?: 0
                val amount = args.getInt(amountOption) ?: 1
                commands.add {
                    val player = requireGet({ connection.playerByName(it) },
                            playerName)
                    val material = requireGet({ plugins.materialResolver[it] },
                            materialName)
                    val item = Item(material,
                            mapOf("Data" to data.toTag(),
                                    "Amount" to amount.toTag()).toTag())
                    player.mob { mob ->
                        mob.inventories.modify("Container") { it.add(item) }
                    }
                }
            }
        }

        group.register("clear", 8) {
            val playerOption = CommandOption(setOf('p'), setOf("player"),
                    listOf("name"),
                    "Player whose inventory will be cleared").also { add(it) }
            return@register { args, executor, commands ->
                val playerName = args.require(
                        playerOption) { it ?: executor.playerName() }
                commands.add {
                    val player = requireGet({ connection.playerByName(it) },
                            playerName)
                    player.mob { mob ->
                        mob.inventories.modify("Container") { it.clear() }
                    }
                }
            }
        }

        group.register("tp", 8) {
            val playerOption = CommandOption(setOf('p'), setOf("player"),
                    listOf("name"), "Player who will be teleported").also {
                add(it)
            }
            val targetOption = CommandOption(setOf('t'), setOf("target"),
                    listOf("name"),
                    "Target that will be teleported to").also { add(it) }
            val worldOption = CommandOption(setOf('w'), setOf("world"),
                    listOf("name"),
                    "World that will be teleported to").also { add(it) }
            val locationOption = CommandOption(setOf('l'), setOf("location"),
                    listOf("x", "y", "z"),
                    "Location that will be teleported to").also { add(it) }
            return@register { args, executor, commands ->
                val playerName = args.require(
                        playerOption) { it ?: executor.playerName() }
                val worldName = args.get(worldOption)
                val location = args.getVector3d(locationOption)
                if (location != null) {
                    if (worldName != null) {
                        commands.add {
                            val player = requireGet(
                                    { connection.playerByName(it) }, playerName)
                            val world = requireGet({ server.world(it) },
                                    worldName)
                            player.setWorld(world, location)
                        }
                    } else {
                        commands.add {
                            val player = requireGet(
                                    { connection.playerByName(it) }, playerName)
                            player.mob({ mob -> mob.setPos(location) })
                        }
                    }
                } else {
                    val targetName = args.require(
                            targetOption) { it ?: executor.playerName() }
                    if (worldName != null) {
                        commands.add {
                            val player = requireGet(
                                    { connection.playerByName(it) }, playerName)
                            val target = requireGet(
                                    { connection.playerByName(it) }, targetName)
                            val world = requireGet({ server.world(it) },
                                    worldName)
                            target.mob { mob ->
                                player.setWorld(world, mob.getCurrentPos())
                            }
                        }
                    } else {
                        commands.add {
                            val player = requireGet(
                                    { connection.playerByName(it) }, playerName)
                            val target = requireGet(
                                    { connection.playerByName(it) }, targetName)
                            player.mob { mob ->
                                target.mob { targetMob ->
                                    if (mob.world == targetMob.world) {
                                        mob.setPos(targetMob.getCurrentPos())
                                    } else {
                                        player.setWorld(targetMob.world,
                                                targetMob.getCurrentPos())
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        group.register("item", 8) {
            val playerOption = CommandOption(setOf('p'), setOf("player"),
                    listOf("name"),
                    "Player holding the item in the left hand").also { add(it) }
            return@register { args, executor, commands ->
                val playerName = args.require(
                        playerOption) { it ?: executor.playerName() }
                commands.add {
                    val player = requireGet({ connection.playerByName(it) },
                            playerName)
                    player.mob { mob ->
                        try {
                            val stream = MemoryViewStreamDefault()
                            val item = mob.leftWeapon()
                            item.toTag().writeJSON(stream)
                            stream.flip()
                            val str = stream.asString()
                            executor.events.fire(MessageEvent(executor,
                                    MessageLevel.FEEDBACK_INFO, str, executor))
                        } catch (e: IOException) {
                            executor.events.fire(MessageEvent(executor,
                                    MessageLevel.FEEDBACK_ERROR,
                                    "Failed to serialize item: ${e.message}",
                                    executor))
                        }
                    }
                }
            }
        }
    }
}

class DebugCommandsExtensionProvider : ServerExtensionProvider {
    override val name = "Debug Commands"

    override fun create(server: ScapesServer,
                        configMap: TagMap?): ServerExtension? {
        return DebugCommandsExtension(server)
    }
}
