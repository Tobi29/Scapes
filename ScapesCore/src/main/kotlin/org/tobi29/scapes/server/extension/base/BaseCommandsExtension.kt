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

import org.tobi29.scapes.engine.utils.join
import org.tobi29.scapes.server.MessageLevel
import org.tobi29.scapes.server.ScapesServer
import org.tobi29.scapes.server.command.requireGet
import org.tobi29.scapes.server.command.requireOption
import org.tobi29.scapes.server.command.requirePermission
import org.tobi29.scapes.server.extension.ServerExtension
import org.tobi29.scapes.server.extension.event.MessageEvent

class BaseCommandsExtension(server: ScapesServer) : ServerExtension(server) {

    override fun init() {
        val connection = server.connection
        val registry = server.commandRegistry()
        val serverGroup = registry.group("server")

        registry.register("say MESSAGE...", 0, {
            add("n", "name", true, "Name used for prefix")
            add("r", "raw", false, "Disable prefix")
        }) { args, executor, commands ->
            val message: String
            if (args.hasOption('r')) {
                requirePermission(executor, 8, 'r')
                message = join(args.args, delimiter = " ")
            } else {
                val name: String
                val nameOption = args.option('n')
                if (nameOption != null) {
                    requirePermission(executor, 8, 'n')
                    name = nameOption
                } else {
                    name = executor.name()
                }
                message = "<$name> ${join(args.args, delimiter = " ")}"
            }
            commands.add {
                server.events.fireLocal(
                        MessageEvent(executor, MessageLevel.CHAT, message))
            }
        }

        registry.register("tell MESSAGE...", 0, {
            add("t", "target", true, "Target player")
            add("n", "name", true, "Name used for prefix")
            add("r", "raw", false, "Disable prefix")
        }) { args, executor, commands ->
            val targetName = args.requireOption('t', executor.playerName())
            val message: String
            if (args.hasOption('r')) {
                requirePermission(executor, 8, 'r')
                message = join(args.args, delimiter = " ")
            } else {
                val name: String
                val nameOption = args.option('n')
                if (nameOption != null) {
                    requirePermission(executor, 8, 'n')
                    name = nameOption
                } else {
                    name = executor.name()
                }
                message = "[$name] ${join(args.args, delimiter = " ")}"
            }
            commands.add {
                val target = requireGet({ connection.playerByName(it) },
                        targetName)
                target.events.fireLocal(
                        MessageEvent(executor, MessageLevel.CHAT, message))
            }
        }

        serverGroup.register("stop", 10, {}) { args, executor, commands ->
            server.scheduleStop(ScapesServer.ShutdownReason.STOP)
        }

        serverGroup.register("reload", 10, {}) { args, executor, commands ->
            server.scheduleStop(ScapesServer.ShutdownReason.RELOAD)
        }
    }
}
