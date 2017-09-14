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

import kotlinx.coroutines.experimental.CoroutineName
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.yield
import org.tobi29.scapes.client.states.GameStateGameMP
import org.tobi29.scapes.client.states.GameStateServerDisconnect
import org.tobi29.scapes.engine.server.*
import org.tobi29.scapes.engine.utils.ConcurrentLinkedQueue
import org.tobi29.scapes.engine.utils.io.IOException
import org.tobi29.scapes.engine.utils.logging.KLogging
import org.tobi29.scapes.engine.utils.task.Timer
import org.tobi29.scapes.engine.utils.task.loop
import org.tobi29.scapes.packets.PacketAbstract
import org.tobi29.scapes.packets.PacketPingClient
import org.tobi29.scapes.packets.PacketServer
import org.tobi29.scapes.plugins.Plugins
import java.util.concurrent.TimeUnit

class RemoteClientConnection(private val worker: ConnectionWorker,
                             game: GameStateGameMP,
                             private val address: RemoteAddress,
                             private val channel: PacketBundleChannel,
                             private val rateChannel: SSLChannel,
                             plugins: Plugins,
                             loadingDistance: Int) : ClientConnection(game,
        plugins, loadingDistance) {
    private val sendQueue = ConcurrentLinkedQueue<PacketServer>()
    private var isClosed = false
    private var close = false
    private var pingHandler: (Long) -> Unit = {}

    override fun start() {
        launch(game.engine.taskExecutor + CoroutineName("Connection-Rate")) {
            Timer().apply { init() }.loop(Timer.toDiff(1.0),
                    { delay(it, TimeUnit.NANOSECONDS) }) {
                if (isClosed) return@loop false

                send(PacketPingClient(plugins.registry,
                        System.currentTimeMillis()))
                downloadDebug.setValue(rateChannel.inputRate / 128.0)
                uploadDebug.setValue(rateChannel.outputRate / 128.0)

                true
            }
        }
    }

    override fun stop() {
        close = true
    }

    override suspend fun run(connection: Connection) {
        pingHandler = { connection.increaseTimeout(10000L - it) }
        try {
            while (!connection.shouldClose && !close) {
                while (!sendQueue.isEmpty()) {
                    val packet = sendQueue.poll() ?: continue
                    val output = channel.outputStream
                    val pos = output.position()
                    output.putShort(packet.type.id.toShort())
                    packet.sendServer(this@RemoteClientConnection, output)
                    val size = output.position() - pos
                    profilerSent.packet(packet, size.toLong())
                }
                if (channel.bundleSize() > 0) {
                    channel.queueBundle()
                }
                loop@ while (true) {
                    when (channel.process()) {
                        PacketBundleChannel.FetchResult.CLOSED -> return
                        PacketBundleChannel.FetchResult.YIELD -> break@loop
                        PacketBundleChannel.FetchResult.BUNDLE -> {
                            val bundle = channel.inputStream
                            while (bundle.hasRemaining()) {
                                val packet = PacketAbstract.make(
                                        plugins.registry,
                                        bundle.getShort().toInt()).createClient()
                                val pos = bundle.position()
                                packet.parseClient(
                                        this@RemoteClientConnection,
                                        bundle)
                                val size = bundle.position() - pos
                                profilerReceived.packet(packet,
                                        size.toLong())
                                packet.runClient(
                                        this@RemoteClientConnection)
                            }
                        }
                    }
                }
                yield()
            }
        } catch (e: ConnectionEndException) {
            logger.info { "Closed client connection: $e" }
        } catch (e: IOException) {
            logger.info { "Lost connection: $e" }
            game.engine.switchState(
                    GameStateServerDisconnect(e.message ?: "",
                            game.engine))
        } finally {
            isClosed = true
        }
    }

    override fun send(packet: PacketServer) {
        sendQueue.add(packet)
        if (packet.isImmediate) {
            worker.wake()
        }
    }

    override fun address() = address

    fun updatePing(ping: Long) {
        pingHandler(ping)
        pingDebug.setValue(ping)
    }

    internal enum class State {
        WAIT,
        OPEN,
        CLOSED
    }

    companion object : KLogging()
}
