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

package org.tobi29.scapes.server

import java8.util.stream.Collectors
import mu.KLogging
import org.tobi29.scapes.engine.server.ControlPanelProtocol
import org.tobi29.scapes.engine.server.PacketBundleChannel
import org.tobi29.scapes.engine.utils.CPUUtil
import org.tobi29.scapes.engine.utils.io.tag.TagStructure
import org.tobi29.scapes.engine.utils.io.tag.setDouble
import org.tobi29.scapes.engine.utils.io.tag.setLong
import org.tobi29.scapes.engine.utils.io.tag.structure
import org.tobi29.scapes.engine.utils.replace
import org.tobi29.scapes.server.command.Executor
import org.tobi29.scapes.server.connection.ServerConnection
import org.tobi29.scapes.server.extension.event.MessageEvent
import javax.crypto.Cipher

class ControlPanel(channel: PacketBundleChannel,
                   private val connection: ServerConnection,
                   authentication: (String, Int, ByteArray) -> Cipher?) : ControlPanelProtocol(
        channel, connection.events, authentication), Executor {


    init {
        initCommands()
        connection.events.listenerGlobal<MessageEvent>(this) { event ->
            val style: String
            when (event.level) {
                MessageLevel.SERVER_ERROR, MessageLevel.FEEDBACK_ERROR -> style = "color: red;"
                else -> style = ""
            }
            val html = "<span style=\"$style\">${ESCAPE(event.message)}</span>"
            send("Message", structure { setString("Message", html) })
        }
    }

    private fun initCommands() {
        addCommand("Ping") { payload ->
            send("Pong", payload)
        }
        addCommand("Command") { payload ->
            payload.getString("Command")?.let { command ->
                connection.server.commandRegistry()[command, this].execute().forEach { output ->
                    events.fireLocal(
                            MessageEvent(this, MessageLevel.FEEDBACK_ERROR,
                                    output.toString()))
                }
            }
            payload.getList("Commands")?.forEach {
                payload.getString("Command")?.let { command ->
                    connection.server.commandRegistry()[command, this].execute().forEach { output ->
                        events.fireLocal(
                                MessageEvent(this, MessageLevel.FEEDBACK_ERROR,
                                        output.toString()))
                    }
                }
            }
        }
        val cpuReader = CPUUtil.reader()
        val cpuSupplier = cpuReader?.let { { it.totalCPU() } } ?: { Double.NaN }
        val runtime = Runtime.getRuntime()
        addCommand("Stats") {
            val cpu = cpuSupplier()
            val memory = runtime.totalMemory() - runtime.freeMemory()
            send("Stats", structure {
                setDouble("CPU", cpu)
                setLong("Memory", memory)
            })
        }
        addCommand("Players-List") {
            val list = connection.players().map { it.name() }.map {
                structure { setString("Name", it) }
            }.collect(Collectors.toList<TagStructure>())
            send("Players-List",
                    structure { setList("Players", list) })
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
        private val ESCAPE = replace("&", "&amp;", ">", "&gt;", "<",
                "&lt;", "'", "&apos;", "\"", "&quot;")
    }
}
