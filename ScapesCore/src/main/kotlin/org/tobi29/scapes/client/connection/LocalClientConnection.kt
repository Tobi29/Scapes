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

package org.tobi29.scapes.client.connection

import kotlinx.coroutines.experimental.yield
import org.tobi29.scapes.client.states.GameStateGameMP
import org.tobi29.scapes.client.states.GameStateMenu
import org.tobi29.scapes.client.states.GameStateServerDisconnect
import org.tobi29.scapes.connection.Account
import org.tobi29.server.Connection
import org.tobi29.server.ConnectionEndException
import org.tobi29.server.ConnectionWorker
import org.tobi29.server.RemoteAddress
import org.tobi29.io.IOException
import org.tobi29.scapes.entity.skin.ClientSkinStorage
import org.tobi29.scapes.packets.PacketDisconnectSelf
import org.tobi29.scapes.packets.PacketServer
import org.tobi29.scapes.plugins.Plugins
import org.tobi29.scapes.server.connection.LocalPlayerConnection

class LocalClientConnection(
        private val worker: ConnectionWorker,
        game: GameStateGameMP,
        private val player: LocalPlayerConnection,
        plugins: Plugins,
        loadingDistance: Int,
        private val account: Account,
        skinStorage: ClientSkinStorage
) : ClientConnection(game, plugins, loadingDistance, skinStorage) {

    override suspend fun start() {
        try {
            player.start(this, worker, account)
        } catch (e: IOException) {
            player.error(e)
        }
    }

    override fun stop() {
        player.receiveServer(
                PacketDisconnectSelf(plugins.registry, "Disconnected"))
    }

    override suspend fun run(connection: Connection) {
        try {
            while (!player.isClosed) {
                connection.increaseTimeout(10000)
                if (connection.shouldClose) {
                    player.receiveServer(
                            PacketDisconnectSelf(plugins.registry,
                                    "Disconnected"))
                    break
                }
                while (!player.queueClient.isEmpty()) {
                    val packet = player.queueClient.poll() ?: continue
                    packet.localClient()
                    packet.runClient(this@LocalClientConnection)
                }
                yield()
            }
            game.engine.switchState(GameStateMenu(game.engine))
        } catch (e: ConnectionEndException) {
            logger.info { "Closed client connection: $e" }
        } catch (e: IOException) {
            logger.info { "Lost connection: $e" }
            game.engine.switchState(
                    GameStateServerDisconnect(e.message ?: "",
                            game.engine))
        }
    }

    override fun send(packet: PacketServer) {
        player.receiveServer(packet)
    }

    override fun address(): RemoteAddress? {
        return null
    }
}
