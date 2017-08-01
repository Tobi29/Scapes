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

import kotlinx.coroutines.experimental.yield
import org.tobi29.scapes.client.ScapesClient
import org.tobi29.scapes.client.connection.LocalClientConnection
import org.tobi29.scapes.connection.Account
import org.tobi29.scapes.engine.server.Connection
import org.tobi29.scapes.engine.server.ConnectionCloseException
import org.tobi29.scapes.engine.server.ConnectionWorker
import org.tobi29.scapes.engine.server.InvalidPacketDataException
import org.tobi29.scapes.engine.utils.Algorithm
import org.tobi29.scapes.engine.utils.ConcurrentLinkedQueue
import org.tobi29.scapes.engine.utils.graphics.decodePNG
import org.tobi29.scapes.engine.utils.io.IOException
import org.tobi29.scapes.engine.utils.io.checksum
import org.tobi29.scapes.engine.utils.io.filesystem.exists
import org.tobi29.scapes.engine.utils.io.filesystem.read
import org.tobi29.scapes.entity.skin.ServerSkin
import org.tobi29.scapes.packets.PacketClient
import org.tobi29.scapes.packets.PacketDisconnect
import org.tobi29.scapes.packets.PacketDisconnectSelf
import org.tobi29.scapes.packets.PacketServer
import org.tobi29.scapes.server.MessageLevel
import org.tobi29.scapes.server.extension.event.MessageEvent
import org.tobi29.scapes.server.extension.event.PlayerJoinEvent
import org.tobi29.scapes.server.extension.event.PlayerLeaveEvent

class LocalPlayerConnection(private val worker: ConnectionWorker,
                            server: ServerConnection,
                            loadingDistance: Int) : PlayerConnection(
        server) {
    private val queue = ConcurrentLinkedQueue<PacketServer>()
    internal val queueClient = ConcurrentLinkedQueue<PacketClient>()
    private var workerClient: ConnectionWorker? = null

    init {
        loadingRadius = loadingDistance
    }

    internal fun receiveServer(packet: PacketServer) {
        queue.add(packet)
        worker.joiner.wake()
    }

    internal fun receiveClient(packet: PacketClient) {
        queueClient.add(packet)
        workerClient?.joiner?.wake()
    }

    fun start(client: LocalClientConnection,
              workerClient: ConnectionWorker,
              account: Account): String? {
        this.workerClient = workerClient
        val engine = client.game.engine
        val scapes = engine.component(ScapesClient.COMPONENT)
        val path = scapes.home.resolve("Skin.png")
        val image = if (exists(path)) {
            read(path) { decodePNG(it) }
        } else {
            engine.files["Scapes:image/entity/mob/Player.png"].read {
                decodePNG(it)
            }
        }
        if (image.width != 64 || image.height != 64) {
            throw ConnectionCloseException("Invalid skin!")
        }
        nickname = account.nickname()
        skin = ServerSkin(image)
        id = checksum(account.keyPair().public.encoded,
                Algorithm.SHA1).toString()

        val response = server.addPlayer(this)
        if (response != null) {
            return response
        }
        added = true
        setWorld()
        workerClient.joiner.wake()
        return null
    }

    suspend fun run(connection: Connection) {
        try {
            events.fire(PlayerJoinEvent(this@LocalPlayerConnection))
            events.fire(
                    MessageEvent(this@LocalPlayerConnection,
                            MessageLevel.SERVER_INFO,
                            "Player connected: $id ($nickname) locally"))
            while (!connection.shouldClose) {
                connection.increaseTimeout(10000)
                while (queue.isNotEmpty()) {
                    val packet = queue.poll() ?: continue
                    packet.localServer()
                    packet.runServer(this@LocalPlayerConnection)
                }
                yield()
            }
        } catch (e: ConnectionCloseException) {
            events.fire(
                    MessageEvent(this@LocalPlayerConnection,
                            MessageLevel.SERVER_INFO,
                            "Disconnecting player: $nickname"))
        } catch (e: InvalidPacketDataException) {
            events.fire(
                    MessageEvent(this@LocalPlayerConnection,
                            MessageLevel.SERVER_INFO,
                            "Disconnecting player: $nickname"))
        } catch (e: IOException) {
            events.fire(
                    MessageEvent(this@LocalPlayerConnection,
                            MessageLevel.SERVER_INFO,
                            "Player disconnected: $nickname ($e)"))
        } finally {
            events.fire(PlayerLeaveEvent(this@LocalPlayerConnection))
            isClosed = true
            if (added) {
                server.removePlayer(this@LocalPlayerConnection)
                added = false
            }
            close()
        }
    }

    fun error(e: Exception) {
        events.fire(
                MessageEvent(this, MessageLevel.SERVER_INFO,
                        "Player disconnected: $nickname ($e)"))
    }

    override fun transmit(packet: PacketClient) {
        receiveClient(packet)
        if (packet.isImmediate) {
            worker.joiner.wake()
        }
    }

    override fun disconnect(reason: String,
                            time: Double) {
        removeEntity()
        receiveClient(PacketDisconnect(registry, reason, time))
        receiveServer(PacketDisconnectSelf(registry, reason))
    }
}
