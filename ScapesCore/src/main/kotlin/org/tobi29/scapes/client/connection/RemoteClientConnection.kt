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

import mu.KLogging
import org.tobi29.scapes.client.states.GameStateGameMP
import org.tobi29.scapes.client.states.GameStateMenu
import org.tobi29.scapes.client.states.GameStateServerDisconnect
import org.tobi29.scapes.engine.server.ConnectionEndException
import org.tobi29.scapes.engine.server.PacketBundleChannel
import org.tobi29.scapes.engine.server.RemoteAddress
import org.tobi29.scapes.engine.utils.mapNotNull
import org.tobi29.scapes.engine.utils.task.Joiner
import org.tobi29.scapes.packets.PacketAbstract
import org.tobi29.scapes.packets.PacketClient
import org.tobi29.scapes.packets.PacketPingClient
import org.tobi29.scapes.packets.PacketServer
import org.tobi29.scapes.plugins.Plugins
import java.io.IOException
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.util.concurrent.ConcurrentLinkedQueue

class RemoteClientConnection @Throws(IOException::class)
constructor(game: GameStateGameMP,
            private val channel: PacketBundleChannel, plugins: Plugins, loadingDistance: Int) : ClientConnection(
        game, plugins, loadingDistance) {
    private val selector: Selector
    private val sendQueue = ConcurrentLinkedQueue<() -> Unit>()
    private var joiner: Joiner? = null
    private var state = State.OPEN

    init {
        selector = Selector.open()
        this.channel.register(selector, SelectionKey.OP_READ)
    }

    fun run(joiner: Joiner.Joinable) {
        try {
            while (state == State.OPEN) {
                while (!sendQueue.isEmpty()) {
                    sendQueue.poll()()
                }
                if (channel.bundleSize() > 0) {
                    channel.queueBundle()
                }
                if (channel.process({ bundle ->
                    while (bundle.hasRemaining()) {
                        val packet = PacketAbstract.make(plugins.registry(),
                                bundle.short) as PacketClient
                        val pos = bundle.position()
                        packet.parseClient(this, bundle)
                        val size = bundle.position() - pos
                        profilerReceived.packet(packet, size.toLong())
                        packet.runClient(this)
                    }
                    true
                })) {
                    break
                }
                try {
                    selector.select(10)
                    selector.selectedKeys().clear()
                } catch (e: IOException) {
                    logger.warn { "Error when waiting for events: $e" }
                }

            }
            logger.info { "Closed client connection!" }
            game.engine.switchState(GameStateMenu(game.engine))
        } catch (e: ConnectionEndException) {
            logger.info { "Closed client connection: $e" }
        } catch (e: IOException) {
            logger.info { "Lost connection: $e" }
            game.engine.switchState(GameStateServerDisconnect(e.message ?: "",
                    game.engine))
        }

        state = State.CLOSED
        try {
            channel.close()
            selector.close()
        } catch (e: IOException) {
            logger.error { "Error closing socket: $e" }
        }

    }

    override fun start() {
        joiner = game.engine.taskExecutor.runThread({ run(it) },
                "Client-Connection")
        game.engine.taskExecutor.addTask({
            send(PacketPingClient(System.currentTimeMillis()))
            downloadDebug.setValue(channel.inputRate / 128.0)
            uploadDebug.setValue(channel.outputRate / 128.0)
            if (state == State.CLOSED) -1 else 1000
        }, "Connection-Rate", 1000)
    }

    override fun stop() {
        channel.requestClose()
        joiner!!.join()
    }

    override fun task(runnable: () -> Unit) {
        sendQueue.add(runnable)
        selector.wakeup()
    }

    override fun transmit(packet: PacketServer) {
        val output = channel.outputStream
        val pos = output.position()
        output.putShort(packet.id(plugins.registry()).toInt())
        packet.sendServer(this, output)
        val size = output.position() - pos
        profilerSent.packet(packet, size.toLong())
    }

    override fun address(): RemoteAddress? {
        return channel.remoteAddress?.mapNotNull(::RemoteAddress)
    }

    fun updatePing(ping: Long) {
        pingDebug.setValue(System.currentTimeMillis() - ping)
    }

    internal enum class State {
        OPEN,
        CLOSED
    }

    companion object : KLogging()
}