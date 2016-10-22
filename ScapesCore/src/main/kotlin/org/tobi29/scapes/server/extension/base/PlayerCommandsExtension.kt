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

import org.tobi29.scapes.engine.utils.wildcard
import org.tobi29.scapes.server.MessageLevel
import org.tobi29.scapes.server.ScapesServer
import org.tobi29.scapes.server.command.getInt
import org.tobi29.scapes.server.command.requireGet
import org.tobi29.scapes.server.command.requireOption
import org.tobi29.scapes.server.extension.ServerExtension
import org.tobi29.scapes.server.extension.event.MessageEvent

class PlayerCommandsExtension(server: ScapesServer) : ServerExtension(server) {

    override fun init() {
        val group = server.commandRegistry().group("players")
        val connection = server.connection

        group.register("list [MATCH]", 9,
                { options -> }) { args, executor, commands ->
            val exp = args.arg(0) ?: "*"
            val pattern = wildcard(exp)
            commands.add({
                server.connection.players().map { it.name() }.filter { nickname ->
                    pattern.matcher(nickname).matches()
                }.forEach { message ->
                    executor.events.fireLocal(
                            MessageEvent(executor, MessageLevel.FEEDBACK_INFO,
                                    message))
                }
            })
        }

        group.register("add PLAYER-ID...", 9,
                { options -> }) { args, executor, commands ->
            args.args().forEach { id -> commands.add({ server.addPlayer(id) }) }
        }

        group.register("remove PLAYER-ID...", 9,
                { options -> }) { args, executor, commands ->
            args.args().forEach { id ->
                commands.add({ server.removePlayer(id) })
            }
        }

        group.register("kick PLAYER-NAME...", 9,
                { options -> }) { args, executor, commands ->
            val message = "Kick by an Admin!"
            args.args().forEach { playerName ->
                commands.add({
                    val player = requireGet({ connection.playerByName(it) },
                            playerName)
                    player.disconnect(message)
                })
            }
        }

        group.register("op -l LEVEL PLAYER-NAME...", 10, { options ->
            options.add("l", "level", true, "Permission level (0-10)")
        }) { args, executor, commands ->
            val playerNames = args.args()
            val permissionLevel = getInt(
                    args.requireOption('l'))
            playerNames.forEach { playerName ->
                commands.add({
                    val player = requireGet({ connection.playerByName(it) },
                            playerName)
                    player.permissionLevel = permissionLevel
                })
            }
        }
    }
}
