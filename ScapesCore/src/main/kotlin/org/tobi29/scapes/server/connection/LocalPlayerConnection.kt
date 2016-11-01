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

import org.tobi29.scapes.client.connection.LocalClientConnection
import org.tobi29.scapes.client.states.GameStateGameMP
import org.tobi29.scapes.connection.Account
import org.tobi29.scapes.engine.server.ConnectionCloseException
import org.tobi29.scapes.engine.server.ConnectionWorker
import org.tobi29.scapes.engine.utils.BufferCreator
import org.tobi29.scapes.engine.utils.graphics.Image
import org.tobi29.scapes.engine.utils.graphics.decodePNG
import org.tobi29.scapes.engine.utils.io.Algorithm
import org.tobi29.scapes.engine.utils.io.checksum
import org.tobi29.scapes.engine.utils.io.filesystem.exists
import org.tobi29.scapes.engine.utils.io.filesystem.read
import org.tobi29.scapes.entity.skin.ServerSkin
import org.tobi29.scapes.packets.PacketClient
import org.tobi29.scapes.packets.PacketDisconnect
import org.tobi29.scapes.packets.PacketServer
import org.tobi29.scapes.server.MessageLevel
import org.tobi29.scapes.server.extension.event.MessageEvent
import java.io.IOException
import java.util.concurrent.atomic.AtomicReference

class LocalPlayerConnection(server: ServerConnection, game: GameStateGameMP,
                            loadingDistance: Int, account: Account) : PlayerConnection(
        server) {
    private val client: LocalClientConnection
    private var state = State.OPEN

    init {
        loadingRadius = loadingDistance
        client = LocalClientConnection(game, this, server.plugins,
                loadingDistance, account)
    }

    @Throws(IOException::class)
    fun start(account: Account): String? {
        val engine = client.game.engine
        val path = engine.home.resolve("Skin.png")
        val image: Image
        if (exists(path)) {
            image = read(path) { decodePNG(it) { BufferCreator.bytes(it) } }
        } else {
            val reference = AtomicReference<Image>()
            engine.files["Scapes:image/entity/mob/Player.png"].read({ stream ->
                val defaultImage = decodePNG(stream) { BufferCreator.bytes(it) }
                reference.set(defaultImage)
            })
            image = reference.get()
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
        return null
    }

    fun stop() {
        error(ConnectionCloseException("Disconnected"))
    }

    fun error(e: Exception) {
        server.events.fireLocal(
                MessageEvent(this, MessageLevel.SERVER_INFO,
                        "Player disconnected: $nickname ($e)"))
        state = State.CLOSED
    }

    fun client(): LocalClientConnection {
        return client
    }

    @Synchronized fun receive(packet: PacketServer) {
        if (state == State.CLOSED) {
            return
        }
        try {
            packet.localServer()
            packet.runServer(this)
        } catch (e: ConnectionCloseException) {
            error(e)
        }

    }

    override fun send(packet: PacketClient) {
        if (state == State.CLOSED) {
            return
        }
        try {
            sendPacket(packet)
        } catch (e: IOException) {
            error(e)
        }
    }

    @Synchronized override fun transmit(packet: PacketClient) {
        if (state == State.CLOSED) {
            return
        }
        try {
            client.receive(packet)
        } catch (e: IOException) {
            error(e)
        }

    }

    @Synchronized override fun close() {
        super.close()
        state = State.CLOSED
    }

    override fun disconnect(reason: String,
                            time: Double) {
        removeEntity()
        transmit(PacketDisconnect(reason, time))
        error(ConnectionCloseException(reason))
    }

    override fun tick(worker: ConnectionWorker) {
    }

    override val isClosed: Boolean
        get() = state == State.CLOSED


    internal enum class State {
        OPEN,
        CLOSED
    }
}
