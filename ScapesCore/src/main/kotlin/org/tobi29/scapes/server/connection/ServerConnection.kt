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

package org.tobi29.scapes.server.connection

import org.tobi29.scapes.connection.ConnectionInfo
import org.tobi29.scapes.connection.ConnectionType
import org.tobi29.scapes.engine.server.*
import org.tobi29.scapes.engine.utils.*
import org.tobi29.scapes.engine.utils.io.IOException
import org.tobi29.scapes.engine.utils.tag.TagMap
import org.tobi29.scapes.engine.utils.tag.toInt
import org.tobi29.scapes.entity.skin.ServerSkin
import org.tobi29.scapes.packets.PacketClient
import org.tobi29.scapes.plugins.Plugins
import org.tobi29.scapes.server.ControlPanel
import org.tobi29.scapes.server.ScapesServer
import org.tobi29.scapes.server.command.Executor
import org.tobi29.scapes.server.extension.event.NewConnectionEvent
import java.nio.channels.SelectionKey
import java.nio.channels.SocketChannel

class ServerConnection(val server: ScapesServer,
                       configMap: TagMap,
                       ssl: SSLHandle) : ConnectionListenWorker(
        server.connections, ConnectionInfo.header(), ssl) {
    private val mayPlayers: Int
    private val controlPassword: String?
    val plugins: Plugins
    private val playersMut = ConcurrentHashMap<String, PlayerConnection>()
    val players = playersMut.values.readOnly()
    private val playerByName = ConcurrentHashMap<String, PlayerConnection>()
    private val executors = ConcurrentHashSet<Executor>()
    private var allowsJoin = true
    private var allowsCreation = true

    init {
        plugins = server.plugins
        mayPlayers = configMap["MaxPlayers"]?.toInt() ?: 0
        controlPassword = ServerConnection.checkControlPassword(
                configMap["ControlPassword"]?.toString())
    }

    fun player(id: String): PlayerConnection? {
        return playersMut[id]
    }

    fun playerByName(name: String): PlayerConnection? {
        return playerByName[name]
    }

    fun skin(checksum: Checksum): ServerSkin? {
        for (player in playerByName.values) {
            if (player.skin().checksum == checksum) {
                return player.skin()
            }
        }
        return null
    }

    fun send(packet: PacketClient) {
        playerByName.values.forEach { player -> player.send(packet) }
    }

    fun doesAllowJoin(): Boolean {
        return allowsJoin
    }

    fun setAllowsJoin(allowsJoin: Boolean) {
        this.allowsJoin = allowsJoin
    }

    fun doesAllowCreation(): Boolean {
        return allowsCreation
    }

    fun setAllowsCreation(allowsCreation: Boolean) {
        this.allowsCreation = allowsCreation
    }

    fun addExecutor(executor: Executor) {
        executors.add(executor)
    }

    fun removeExecutor(executor: Executor) {
        executors.remove(executor)
    }

    fun addPlayer(player: PlayerConnection): String? {
        synchronized(playersMut) {
            if (playersMut.size >= mayPlayers) {
                return "Server full"
            }
            if (playersMut.containsKey(player.id())) {
                return "User already online"
            }
            if (playerByName.containsKey(player.name())) {
                return "User with same name online"
            }
            playersMut.put(player.id(), player)
            playerByName.put(player.name(), player)
            addExecutor(player)
        }
        return null
    }

    fun removePlayer(player: PlayerConnection) {
        playersMut.remove(player.id())
        playerByName.remove(player.name())
        removeExecutor(player)
    }

    override fun accept(channel: SocketChannel): String? {
        val event = NewConnectionEvent(channel)
        server.events.fire(event)
        if (!event.success) {
            return event.reason
        }
        return null
    }

    suspend override fun onConnect(worker: ConnectionWorker,
                                   channel: PacketBundleChannel,
                                   id: Byte,
                                   connection: Connection) {
        when (ConnectionType[id]) {
            ConnectionType.GET_INFO -> {
                channel.register(worker.joiner, SelectionKey.OP_READ)
                val output = channel.outputStream
                output.put(server.serverInfo.getBuffer())
                channel.queueBundle()
                channel.aClose()
            }
            ConnectionType.PLAY -> {
                RemotePlayerConnection(worker, channel, this).run(connection)
            }
            ConnectionType.CONTROL -> {
                if (controlPassword != null) {
                    val controlPanel = ControlPanel(worker, channel, this)
                    controlPanel.openHook {
                        logger.info { "Control panel accepted from $channel" }
                    }
                    addExecutor(controlPanel)
                    try {
                        controlPanel.runServer(connection) { id, mode, salt ->
                            if (id != "Control Panel") {
                                throw IOException("Invalid name: $id")
                            }
                            ControlPanelProtocol.passwordAuthentication(mode,
                                    salt, controlPassword)
                        }
                    } finally {
                        removeExecutor(controlPanel)
                    }
                }
            }
        }
    }

    companion object {
        private fun checkControlPassword(password: String?): String? {
            if (password == null || password.isEmpty()) {
                return null
            }
            return password
        }
    }
}
