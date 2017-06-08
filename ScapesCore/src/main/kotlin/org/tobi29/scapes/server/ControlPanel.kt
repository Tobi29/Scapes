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

package org.tobi29.scapes.server

import org.tobi29.scapes.engine.server.ConnectionWorker
import org.tobi29.scapes.engine.server.ControlPanelProtocol
import org.tobi29.scapes.engine.server.PacketBundleChannel
import org.tobi29.scapes.engine.utils.CPUUtil
import org.tobi29.scapes.engine.utils.ListenerRegistrar
import org.tobi29.scapes.engine.utils.logging.KLogging
import org.tobi29.scapes.engine.utils.tag.*
import org.tobi29.scapes.engine.utils.toReplace
import org.tobi29.scapes.server.command.Executor
import org.tobi29.scapes.server.connection.ServerConnection
import org.tobi29.scapes.server.extension.event.MessageEvent

class ControlPanel(worker: ConnectionWorker,
                   channel: PacketBundleChannel,
                   private val connection: ServerConnection) : ControlPanelProtocol(
        worker, channel, connection.server.events), Executor {

    init {
        initCommands()
    }

    override fun ListenerRegistrar.listeners() {
        listen<MessageEvent> { event ->
            val style: String
            when (event.level) {
                MessageLevel.SERVER_ERROR,
                MessageLevel.FEEDBACK_ERROR -> style = "color: red;"
                else -> style = ""
            }
            val html = "<span style=\"$style\">${ControlPanel.Companion.ESCAPE(
                    event.message)}</span>"
            send("Message", TagMap { this["Message"] = html.toTag() })
        }
    }

    private fun initCommands() {
        addCommand("Ping") { payload ->
            send("Pong", payload)
        }
        addCommand("Command") { payload ->
            payload["Command"]?.toString()?.let { command ->
                connection.server.commandRegistry()[command, this].execute().forEach { output ->
                    events.fire(
                            MessageEvent(this, MessageLevel.FEEDBACK_ERROR,
                                    output.toString(), this))
                }
            }
            payload["Commands"]?.toList()?.asSequence()?.mapNotNull(
                    Tag::toString)?.forEach { command ->
                connection.server.commandRegistry()[command, this].execute().forEach { output ->
                    events.fire(
                            MessageEvent(this, MessageLevel.FEEDBACK_ERROR,
                                    output.toString(), this))
                }
            }
        }
        val cpuReader = CPUUtil.reader()
        val cpuSupplier = cpuReader?.let { { it.totalCPU() } } ?: { Double.NaN }
        val runtime = Runtime.getRuntime()
        addCommand("Stats") {
            val cpu = cpuSupplier()
            val memory = runtime.totalMemory() - runtime.freeMemory()
            send("Stats", TagMap {
                this["CPU"] = cpu.toTag()
                this["Memory"] = memory.toTag()
            })
        }
        addCommand("Players-List") {
            send("Players-List", TagMap {
                this["Players"] = TagList {
                    connection.players.asSequence().map { it.name() }.map {
                        TagMap { this["Name"] = it.toTag() }
                    }.forEach { add(it) }
                }
            })
        }
    }

    override fun playerName(): String? {
        return null
    }

    override fun name(): String {
        return "Control Panel"
    }

    override fun permissionLevel(): Int {
        return 10
    }

    companion object : KLogging() {
        private val ESCAPE = listOf(
                "&" to "&amp",
                ">" to "&gt;",
                "<" to "&lt;",
                "'" to "&apos;",
                "\"" to "&quot;").toReplace()
    }
}
