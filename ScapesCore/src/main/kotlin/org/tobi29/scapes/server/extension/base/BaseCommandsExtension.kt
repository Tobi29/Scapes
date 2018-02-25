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

import org.tobi29.args.CommandArgument
import org.tobi29.args.CommandOption
import org.tobi29.args.getBoolean
import org.tobi29.args.require
import org.tobi29.io.tag.TagMap
import org.tobi29.scapes.server.MessageLevel
import org.tobi29.scapes.server.ScapesServer
import org.tobi29.scapes.server.extension.ServerExtension
import org.tobi29.scapes.server.extension.event.MessageEvent
import org.tobi29.scapes.server.extension.spi.ServerExtensionProvider

class BaseCommandsExtension(server: ScapesServer) : ServerExtension(server) {

    override fun init() {
        val connection = server.connection
        val registry = server.commandRegistry()
        val serverGroup = registry.group("server")

        registry.register("say", 0) {
            val nameOption = CommandOption(setOf('n'), setOf("name"),
                    listOf("name"), "Name used for prefix").also { add(it) }
            val rawOption = CommandOption(setOf('r'), setOf("raw"),
                    "Disable prefix").also { add(it) }
            val messageArgument = CommandArgument(
                    name = "message",
                    count = 0..Integer.MAX_VALUE).also { add(it) }
            return@register { args, executor, commands ->
                val message: String
                if (args.getBoolean(rawOption)) {
                    requirePermission(executor, 8, rawOption)
                    message = args.arguments[messageArgument]?.joinToString(
                            separator = " ") ?: ""
                } else {
                    val name = args.parameters[nameOption]?.firstOrNull()?.apply {
                        requirePermission(executor, 8, nameOption)
                    } ?: executor.name()
                    message = "<$name> ${args.arguments[messageArgument]?.joinToString(
                            separator = " ") ?: ""}"
                }
                commands.add {
                    server.events.fire(
                            MessageEvent(executor, MessageLevel.CHAT, message))
                }
            }
        }

        registry.register("tell", 0) {
            val targetOption = CommandOption(setOf('t'), setOf("target"),
                    listOf("name"), "Target player").also { add(it) }
            val nameOption = CommandOption(setOf('n'), setOf("name"),
                    listOf("name"), "Name used for prefix").also { add(it) }
            val rawOption = CommandOption(setOf('r'), setOf("raw"),
                    "Disable prefix").also { add(it) }
            val messageArgument = CommandArgument(
                    name = "message",
                    count = 0..Integer.MAX_VALUE).also { add(it) }
            return@register { args, executor, commands ->
                val targetName = args.require(
                        targetOption) { it ?: executor.playerName() }
                val message: String
                if (args.getBoolean(rawOption)) {
                    requirePermission(executor, 8, rawOption)
                    message = args.arguments[messageArgument]?.joinToString(
                            separator = " ") ?: ""
                } else {
                    val name = args.parameters[nameOption]?.firstOrNull()?.apply {
                        requirePermission(executor, 8, nameOption)
                    } ?: executor.name()
                    message = "<$name> ${args.arguments[messageArgument]?.joinToString(
                            separator = " ") ?: ""}"
                }
                commands.add {
                    val target = requireGet({ connection.playerByName(it) },
                            targetName)
                    server.events.fire(
                            MessageEvent(executor, MessageLevel.CHAT, message,
                                    target))
                }
            }
        }

        serverGroup.register("stop", 10) {
            { _, _, commands ->
                commands.add {
                    server.scheduleStop(ScapesServer.ShutdownReason.STOP)
                }
            }
        }

        serverGroup.register("reload", 10) {
            { _, _, commands ->
                commands.add {
                    server.scheduleStop(ScapesServer.ShutdownReason.RELOAD)
                }
            }
        }
    }
}

class BaseCommandsExtensionProvider : ServerExtensionProvider {
    override val name = "Base Commands"

    override fun create(server: ScapesServer,
                        configMap: TagMap?): ServerExtension? {
        return BaseCommandsExtension(server)
    }
}
