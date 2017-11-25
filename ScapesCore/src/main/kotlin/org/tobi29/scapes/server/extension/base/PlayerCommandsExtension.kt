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

import org.tobi29.scapes.engine.args.CommandArgument
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

        group.register("list", 9) {
            val matchArgument = CommandArgument(
                    name = "match",
                    count = 0..1).also { add(it) }
            ;{ args, executor, commands ->
            val exp = args.arguments[matchArgument]?.firstOrNull() ?: "*"
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

        group.register("add", 9) {
            val playerArgument = CommandArgument(
                    name = "player-id",
                    count = 0..Integer.MAX_VALUE).also { add(it) }
            ;{ args, _, commands ->
            args.arguments[playerArgument]?.forEach {
                commands.add { server.addPlayer(it) }
            }
        }
        }

        group.register("remove", 9) {
            val playerArgument = CommandArgument(
                    name = "player-id",
                    count = 0..Integer.MAX_VALUE).also { add(it) }
            ;{ args, _, commands ->
            args.arguments[playerArgument]?.forEach {
                commands.add { server.removePlayer(it) }
            }
        }
        }

        group.register("kick", 9) {
            val messageOption = CommandOption(setOf('m'), setOf("message"),
                    listOf("message"),
                    "Message to display when kicked").also { add(it) }
            val playerArgument = CommandArgument(
                    name = "player",
                    count = 0..Integer.MAX_VALUE).also { add(it) }
            ;{ args, _, commands ->
            args.arguments[playerArgument]?.forEach {
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

        group.register("op", 9) {
            val levelOption = CommandOption(setOf('l'), setOf("level"),
                    listOf("value"),
                    "Permission level (0-10)").also { add(it) }
            val playerArgument = CommandArgument(
                    name = "player",
                    count = 0..Integer.MAX_VALUE).also { add(it) }
            ;{ args, _, commands ->
            args.arguments[playerArgument]?.forEach {
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
