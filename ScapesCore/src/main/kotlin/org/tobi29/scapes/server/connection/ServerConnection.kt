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

package org.tobi29.scapes.server.connection

import java8.util.stream.Stream
import org.tobi29.scapes.connection.ConnectionInfo
import org.tobi29.scapes.connection.ConnectionType
import org.tobi29.scapes.connection.GetInfoConnection
import org.tobi29.scapes.engine.server.*
import org.tobi29.scapes.engine.utils.Checksum
import org.tobi29.scapes.engine.utils.io.tag.TagStructure
import org.tobi29.scapes.engine.utils.io.tag.getInt
import org.tobi29.scapes.engine.utils.stream
import org.tobi29.scapes.entity.skin.ServerSkin
import org.tobi29.scapes.packets.PacketClient
import org.tobi29.scapes.plugins.Plugins
import org.tobi29.scapes.server.ControlPanel
import org.tobi29.scapes.server.ScapesServer
import org.tobi29.scapes.server.command.Executor
import org.tobi29.scapes.server.extension.event.NewConnectionEvent
import java.io.IOException
import java.nio.channels.SocketChannel
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class ServerConnection(val server: ScapesServer,
                       tagStructure: TagStructure,
                       ssl: SSLHandle) : AbstractServerConnection(
        server.taskExecutor(), ConnectionInfo.header(), ssl, 10) {
    val events = server.events
    private val mayPlayers: Int
    private val controlPassword: String?
    val plugins: Plugins
    private val players = ConcurrentHashMap<String, PlayerConnection>()
    private val playerByName = ConcurrentHashMap<String, PlayerConnection>()
    private val executors = Collections.newSetFromMap<Executor>(
            ConcurrentHashMap<Executor, Boolean>())
    private var allowsJoin = true
    private var allowsCreation = true

    init {
        plugins = server.plugins
        mayPlayers = tagStructure.getInt("MaxPlayers") ?: 0
        controlPassword = ServerConnection.checkControlPassword(
                tagStructure.getString("ControlPassword"))
    }

    fun player(id: String): PlayerConnection? {
        return players[id]
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

    fun players(): Stream<PlayerConnection> {
        return playerByName.values.stream()
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
        synchronized(players) {
            if (players.size >= mayPlayers) {
                return "Server full"
            }
            if (players.containsKey(player.id())) {
                return "User already online"
            }
            if (playerByName.containsKey(player.name())) {
                return "User with same name online"
            }
            players.put(player.id(), player)
            playerByName.put(player.name(), player)
            addExecutor(player)
        }
        return null
    }

    fun removePlayer(player: PlayerConnection) {
        players.remove(player.id())
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

    override fun newConnection(worker: ConnectionWorker,
                               channel: PacketBundleChannel,
                               id: Byte): Connection? {
        when (ConnectionType[id]) {
            ConnectionType.GET_INFO -> return GetInfoConnection(worker, channel,
                    server.serverInfo)
            ConnectionType.PLAY -> return RemotePlayerConnection(worker,
                    channel, this)
            ConnectionType.CONTROL -> {
                if (controlPassword != null) {
                    val controlPanel = ControlPanel(worker, channel,
                            this) { id, mode, salt ->
                        if (id != "Control Panel") {
                            throw IOException("Invalid name: $id")
                        }
                        ControlPanelProtocol.passwordAuthentication(mode, salt,
                                controlPassword)
                    }
                    controlPanel.openHook {
                        ControlPanel.logger.info { "Control panel accepted from $channel" }
                    }
                    addExecutor(controlPanel)
                    controlPanel.closeHook { removeExecutor(controlPanel) }
                    return controlPanel
                }
            }
        }
        return null
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
