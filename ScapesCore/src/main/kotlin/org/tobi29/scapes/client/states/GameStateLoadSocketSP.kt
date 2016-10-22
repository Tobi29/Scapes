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

package org.tobi29.scapes.client.states

import mu.KLogging
import org.tobi29.scapes.client.connection.NewConnection
import org.tobi29.scapes.client.gui.desktop.GuiLoading
import org.tobi29.scapes.client.gui.touch.GuiTouchLoading
import org.tobi29.scapes.connection.Account
import org.tobi29.scapes.connection.ServerInfo
import org.tobi29.scapes.engine.Container
import org.tobi29.scapes.engine.GameState
import org.tobi29.scapes.engine.ScapesEngine
import org.tobi29.scapes.engine.graphics.Scene
import org.tobi29.scapes.engine.gui.Gui
import org.tobi29.scapes.engine.server.*
import org.tobi29.scapes.engine.utils.UnsupportedJVMException
import org.tobi29.scapes.engine.utils.io.tag.getDouble
import org.tobi29.scapes.engine.utils.math.round
import org.tobi29.scapes.server.ScapesServer
import org.tobi29.scapes.server.format.WorldSource
import org.tobi29.scapes.server.ssl.dummy.DummyKeyManagerProvider
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.channels.SocketChannel

class GameStateLoadSocketSP(private var source: WorldSource?, engine: ScapesEngine,
                            scene: Scene) : GameState(engine, scene) {
    private var step = 0
    private var server: ScapesServer? = null
    private var channel: SocketChannel? = null
    private var address: InetSocketAddress? = null
    private var client: NewConnection? = null
    private var progress: ((String) -> Unit)? = null

    override fun dispose() {
        if (server != null) {
            try {
                server!!.stop(ScapesServer.ShutdownReason.ERROR)
            } catch (e: IOException) {
                logger.error(
                        e) { "Failed to stop internal server after login error" }
            }
        }
        if (source != null) {
            try {
                source!!.close()
            } catch (e: IOException) {
                logger.error(e) { "Failed to close world source" }
            }
        }
    }

    override fun init() {
        val gui: Gui
        val valueSupplier = {
            if (step < 0)
                Double.NEGATIVE_INFINITY
            else if (step >= 6) Double.POSITIVE_INFINITY else step / 6.0
        }
        when (engine.container.formFactor()) {
            Container.FormFactor.PHONE -> {
                val progress = GuiTouchLoading(this, valueSupplier,
                        engine.guiStyle)
                this.progress = { progress.setLabel(it) }
                gui = progress
            }
            else -> {
                val progress = GuiLoading(this, valueSupplier, engine.guiStyle)
                this.progress = { progress.setLabel(it) }
                gui = progress
            }
        }
        engine.guiStack.add("20-Progress", gui)
    }

    override val isMouseGrabbed: Boolean
        get() = false

    override fun step(delta: Double) {
        try {
            when (step) {
                0 -> {
                    step++
                    progress!!("Creating server...")
                }
                1 -> {
                    val tagStructure = engine.tagStructure.structure(
                            "Scapes").structure("IntegratedServer")
                    val panorama = source!!.panorama()
                    val serverInfo: ServerInfo
                    if (panorama != null) {
                        serverInfo = ServerInfo("Local Server",
                                panorama.elements[0])
                    } else {
                        serverInfo = ServerInfo("Local Server")
                    }
                    val ssl: SSLHandle
                    try {
                        ssl = SSLProvider.sslHandle(
                                DummyKeyManagerProvider.get())
                    } catch (e: IOException) {
                        throw UnsupportedJVMException(e)
                    }

                    server = ScapesServer(source!!, tagStructure, serverInfo,
                            ssl, engine)
                    step++
                    progress!!("Starting server...")
                }
                2 -> {
                    val port = server!!.connection.start(0)
                    if (port <= 0) {
                        throw IOException(
                                "Unable to open server socket (Invalid port returned: " +
                                        port + ')')
                    }
                    address = InetSocketAddress(port)
                    channel = SocketChannel.open(address)
                    channel!!.configureBlocking(false)
                    step++
                    progress!!("Connecting to server...")
                }
                3 -> if (channel!!.finishConnect()) {
                    step++
                    progress!!("Sending request...")
                }
                4 -> {
                    val bundleChannel: PacketBundleChannel
                    // Ignore invalid certificates because local server
                    // cannot provide a valid one
                    val ssl = SSLProvider.sslHandle { certificates -> true }
                    bundleChannel = PacketBundleChannel(
                            RemoteAddress(address!!),
                            channel!!, engine.taskExecutor, ssl, true)
                    val loadingRadius = round(engine.tagStructure.getStructure(
                            "Scapes")?.getDouble("RenderDistance") ?: 0.0) + 16
                    val account = Account[engine.home.resolve(
                            "Account.properties")]
                    client = NewConnection(engine, bundleChannel, account,
                            loadingRadius)
                    step++
                }
                5 -> {
                    val status = client!!.login()
                    if (status != null) {
                        progress!!(status)
                    } else {
                        step++
                        progress!!("Loading world...")
                    }
                }
                6 -> {
                    val game = GameStateGameSP(client!!.finish(), source!!,
                            server!!, scene, engine)
                    server = null
                    source = null
                    engine.switchState(game)
                }
            }
        } catch (e: IOException) {
            logger.error(e) { "Failed to start internal server" }
            engine.switchState(
                    GameStateServerDisconnect(e.message ?: "", engine))
            step = -1
        }
    }

    companion object : KLogging()
}
