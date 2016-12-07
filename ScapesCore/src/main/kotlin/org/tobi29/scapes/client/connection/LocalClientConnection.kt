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

package org.tobi29.scapes.client.connection

import org.tobi29.scapes.client.states.GameStateGameMP
import org.tobi29.scapes.client.states.GameStateMenu
import org.tobi29.scapes.client.states.GameStateServerDisconnect
import org.tobi29.scapes.connection.Account
import org.tobi29.scapes.engine.server.ConnectionEndException
import org.tobi29.scapes.engine.server.ConnectionWorker
import org.tobi29.scapes.engine.server.RemoteAddress
import org.tobi29.scapes.packets.PacketServer
import org.tobi29.scapes.plugins.Plugins
import org.tobi29.scapes.server.connection.LocalPlayerConnection
import java.io.IOException

class LocalClientConnection(private val worker: ConnectionWorker,
                            game: GameStateGameMP,
                            private val player: LocalPlayerConnection,
                            plugins: Plugins,
                            loadingDistance: Int,
                            private val account: Account) : ClientConnection(
        game, plugins, loadingDistance) {
    override fun tick(worker: ConnectionWorker) {
        try {
            while (player.queueClient.isNotEmpty()) {
                val packet = player.queueClient.poll()
                packet.localClient()
                packet.runClient(this)
            }
        } catch (e: ConnectionEndException) {
            logger.info { "Closed client connection: $e" }
        } catch (e: IOException) {
            logger.info { "Lost connection: $e" }
            game.engine.switchState(GameStateServerDisconnect(e.message ?: "",
                    game.engine))
        }
    }

    override val isClosed: Boolean
        get() = player.isClosed

    override fun close() {
    }

    override fun start() {
        synchronized(player) {
            if (player.isClosed) {
                return
            }
            try {
                player.start(this, worker, account)
                if (!player.server.addConnection { player }) {
                    throw IOException("Failed to add client to server")
                }
            } catch (e: IOException) {
                player.error(e)
            }
        }
    }

    override fun stop() {
        if (!player.isClosed) {
            player.stop()
            game.engine.switchState(GameStateMenu(game.engine))
        }
    }

    override fun task(runnable: () -> Unit) {
        synchronized(player) {
            if (player.isClosed) {
                return
            }
            try {
                runnable()
            } catch (e: IOException) {
                player.error(e)
            }

        }
    }

    override fun transmit(packet: PacketServer) {
        player.receiveServer(packet)
    }

    override fun address(): RemoteAddress? {
        return null
    }
}
