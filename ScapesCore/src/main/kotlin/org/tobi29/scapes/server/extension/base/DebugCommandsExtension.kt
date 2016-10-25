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

package org.tobi29.scapes.server.extension.base

import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.engine.utils.io.ByteBufferStream
import org.tobi29.scapes.engine.utils.io.asString
import org.tobi29.scapes.engine.utils.io.process
import org.tobi29.scapes.engine.utils.io.tag.json.TagStructureJSON
import org.tobi29.scapes.server.MessageLevel
import org.tobi29.scapes.server.ScapesServer
import org.tobi29.scapes.server.command.getInt
import org.tobi29.scapes.server.command.getVector3d
import org.tobi29.scapes.server.command.requireGet
import org.tobi29.scapes.server.command.requireOption
import org.tobi29.scapes.server.extension.ServerExtension
import org.tobi29.scapes.server.extension.event.MessageEvent
import java.io.IOException

class DebugCommandsExtension(server: ScapesServer) : ServerExtension(server) {

    override fun init() {
        val gameRegistry = server.plugins.registry()
        val connection = server.connection
        val group = server.commandRegistry().group("debug")

        group.register("give", 8, { options ->
            options.add("p", "player", true,
                    "Player that the item will be given to")
            options.add("m", "item", true, "Material of item")
            options.add("d", "data", true, "Data value of item")
            options.add("a", "amount", true, "Amount of item in stack")
        }, { args, executor, commands ->
            val playerName = args.requireOption('p', executor.playerName())
            val materialName = args.requireOption('m')
            val data = getInt(args.option('d', "0"))
            val amount = getInt(args.option('a', "1"))
            commands.add({
                val player = requireGet({ connection.playerByName(it) },
                        playerName)
                val material = requireGet({ gameRegistry.material(it) },
                        materialName)
                val item = ItemStack(material, data, amount)
                player.mob({ mob ->
                    mob.inventories().modify("Container",
                            { inventory -> inventory.add(item) })
                })
            })
        })

        group.register("clear", 8, { options -> }, { args, executor, commands ->
            val playerNames = args.args()
            playerNames.forEach { playerName ->
                commands.add({
                    val player = requireGet({ connection.playerByName(it) },
                            playerName)
                    player.mob({ mob ->
                        mob.inventories().modify("Container", { it.clear() })
                    })
                })
            }
        })

        group.register("tp", 8, { options ->
            options.add("p", "player", true, "Player who will be teleported")
            options.add("t", "target", true,
                    "Target that the player will be teleported to")
            options.add("w", "world", true,
                    "World that the player will be teleported to")
            options.add("l", "location", 3,
                    "Target that the player will be teleported to")
        }, { args, executor, commands ->
            val playerName = args.requireOption('p', executor.playerName())
            val worldOption = args.option('w')
            val locationOption = args.optionArray('l')
            if (locationOption != null) {
                val location = getVector3d(locationOption)
                if (worldOption != null) {
                    commands.add({
                        val player = requireGet({ connection.playerByName(it) },
                                playerName)
                        val world = requireGet({ server.world(it) },
                                worldOption)
                        player.setWorld(world, location)
                    })
                } else {
                    commands.add({
                        val player = requireGet({ connection.playerByName(it) },
                                playerName)
                        player.mob({ mob -> mob.setPos(location) })
                    })
                }
            } else {
                val targetName = args.requireOption('t', executor.playerName())
                if (worldOption != null) {
                    commands.add({
                        val player = requireGet({ connection.playerByName(it) },
                                playerName)
                        val target = requireGet({ connection.playerByName(it) },
                                targetName)
                        val world = requireGet({ server.world(it) },
                                worldOption)
                        target.mob({ mob ->
                            player.setWorld(world, mob.getCurrentPos())
                        })
                    })
                } else {
                    commands.add({
                        val player = requireGet({ connection.playerByName(it) },
                                playerName)
                        val target = requireGet({ connection.playerByName(it) },
                                targetName)
                        player.mob({ mob ->
                            target.mob({ targetMob ->
                                if (mob.world == targetMob.world) {
                                    mob.setPos(targetMob.getCurrentPos())
                                } else {
                                    player.setWorld(targetMob.world,
                                            targetMob.getCurrentPos())
                                }
                            })
                        })
                    })
                }
            }
        })

        group.register("item", 8, { options ->
            options.add("p", "player", true,
                    "Player holding the item in the left hand to debug")
        },
                { args, executor, commands ->
                    val playerName = args.requireOption('p',
                            executor.playerName())
                    commands.add({
                        val player = requireGet({ connection.playerByName(it) },
                                playerName)
                        player.mob({ mob ->
                            try {
                                val stream = ByteBufferStream()
                                TagStructureJSON.write(mob.leftWeapon().save(),
                                        stream)
                                stream.buffer().flip()
                                val str = process(stream, asString())
                                executor.events.fireLocal(
                                        MessageEvent(executor,
                                                MessageLevel.FEEDBACK_INFO,
                                                str))
                            } catch (e: IOException) {
                                executor.events.fireLocal(
                                        MessageEvent(executor,
                                                MessageLevel.FEEDBACK_ERROR,
                                                "Failed to serialize item: ${e.message}"))
                            }
                        })
                    })
                })
    }
}