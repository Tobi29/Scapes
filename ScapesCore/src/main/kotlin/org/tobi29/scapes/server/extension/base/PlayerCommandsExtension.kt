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

import org.tobi29.scapes.engine.args.CommandOption
import org.tobi29.scapes.engine.args.get
import org.tobi29.scapes.engine.args.requireInt
import org.tobi29.scapes.engine.utils.tag.TagMap
import org.tobi29.scapes.engine.utils.wildcard
import org.tobi29.scapes.server.MessageLevel
import org.tobi29.scapes.server.ScapesServer
import org.tobi29.scapes.server.extension.ServerExtension
import org.tobi29.scapes.server.extension.event.MessageEvent
import org.tobi29.scapes.server.extension.spi.ServerExtensionProvider

class PlayerCommandsExtension(server: ScapesServer) : ServerExtension(server) {

    override fun init() {
        val group = server.commandRegistry().group("players")
        val connection = server.connection

        group.register("list [MATCH]", 9) {
            { args, executor, commands ->
                val exp = args.arguments.firstOrNull() ?: "*"
                val pattern = wildcard(exp)
                commands.add {
                    server.connection.players.asSequence().map { it.name() }.filter { nickname ->
                        pattern.matches(nickname)
                    }.forEach { message ->
                        executor.events.fire(MessageEvent(executor,
                                MessageLevel.FEEDBACK_INFO, message, executor))
                    }
                }
            }
        }

        group.register("add PLAYER-ID...", 9) {
            { args, _, commands ->
                args.arguments.forEach {
                    commands.add { server.addPlayer(it) }
                }
            }
        }

        group.register("remove PLAYER-ID...", 9) {
            { args, _, commands ->
                args.arguments.forEach {
                    commands.add { server.removePlayer(it) }
                }
            }
        }

        group.register("kick PLAYER-NAME...", 9) {
            val messageOption = CommandOption(setOf('m'), setOf("message"), 1,
                    "Message to display when kicked").also { add(it) }
            return@register { args, _, commands ->
                args.arguments.forEach {
                    commands.add {
                        val player = requireGet({ connection.playerByName(it) },
                                it)
                        val message = args.get(
                                messageOption) ?: "Kick by an Admin!"
                        player.disconnect(message)
                    }
                }
            }
        }

        group.register("op -l LEVEL PLAYER-NAME...", 9) {
            val levelOption = CommandOption(setOf('l'), setOf("level"), 1,
                    "Permission level (0-10)").also { add(it) }
            return@register { args, _, commands ->
                args.arguments.forEach {
                    commands.add {
                        val player = requireGet({ connection.playerByName(it) },
                                it)
                        val level = args.requireInt(levelOption)
                        player.permissionLevel = level
                    }
                }
            }
        }
    }
}

class PlayerCommandsExtensionProvider : ServerExtensionProvider {
    override val name = "Player Commands"

    override fun create(server: ScapesServer,
                        configMap: TagMap?): ServerExtension? {
        return PlayerCommandsExtension(server)
    }
}
