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
import org.tobi29.scapes.client.ScapesClient
import org.tobi29.scapes.client.connection.NewClientConnection
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

class GameStateLoadSocketSP(private var source: WorldSource?, engine: ScapesEngine,
                            scene: Scene) : GameState(engine, scene) {
    private var step = 0
    private var server: ScapesServer? = null
    private var progress: ((String, Double) -> Unit)? = null

    override fun dispose() {
        try {
            server?.stop(ScapesServer.ShutdownReason.ERROR)
        } catch (e: IOException) {
            logger.error(
                    e) { "Failed to stop internal server after login error" }
        }
        try {
            source?.close()
        } catch (e: IOException) {
            logger.error(e) { "Failed to close world source" }
        }
    }

    override fun init() {
        val gui: Gui
        var progressvalue = 0.0
        when (engine.container.formFactor()) {
            Container.FormFactor.PHONE -> {
                val progress = GuiTouchLoading(this, { progressvalue },
                        engine.guiStyle)
                this.progress = { status, value ->
                    progress.setLabel(status)
                    progressvalue = value
                }
                gui = progress
            }
            else -> {
                val progress = GuiLoading(this, { progressvalue },
                        engine.guiStyle)
                this.progress = { status, value ->
                    progress.setLabel(status)
                    progressvalue = value
                }
                gui = progress
            }
        }
        progress?.invoke("Creating server...", 0.0)
        engine.guiStack.add("20-Progress", gui)
    }

    override val isMouseGrabbed: Boolean
        get() = false

    override fun step(delta: Double) {
        val source = source ?: return
        try {
            when (step) {
                0 -> {
                    val tagStructure = engine.tagStructure.structure(
                            "Scapes").structure("IntegratedServer")
                    val panorama = source.panorama()
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

                    this.server = ScapesServer(source, tagStructure, serverInfo,
                            ssl, engine)
                    step++
                    progress?.invoke("Starting server...", 0.2)
                }
                1 -> {
                    val server = server ?: throw IllegalStateException(
                            "Server lost too early")
                    val port = server.connection.start(0)
                    if (port <= 0) {
                        throw IOException(
                                "Unable to open server socket (Invalid port returned: $port)")
                    }
                    val address = InetSocketAddress(port)

                    step++
                    progress?.invoke("Connecting to local server...", 0.4)

                    (engine.game as ScapesClient).connection.addOutConnection(
                            address, { e ->
                        GameStateLoadMP.logger.error(
                                e) { "Failed to connect to server" }
                        engine.switchState(
                                GameStateServerDisconnect(
                                        e.message ?: "", engine))
                    }) { worker, channel ->
                        progress?.invoke("Logging in...", 0.6)
                        val bundleChannel: PacketBundleChannel
                        val ssl = SSLProvider.sslHandle { certificates -> true }
                        bundleChannel = PacketBundleChannel(
                                RemoteAddress(address), channel,
                                engine.taskExecutor, ssl, true)
                        val loadingRadius = round(
                                engine.tagStructure.getStructure(
                                        "Scapes")?.getDouble(
                                        "RenderDistance") ?: 0.0) + 16
                        val account = Account[engine.home.resolve(
                                "Account.properties")]
                        worker.addConnection {
                            val connection = NewClientConnection(worker, engine,
                                    bundleChannel, account, loadingRadius,
                                    { status ->
                                        progress?.invoke(status, 0.8)
                                    }, { e ->
                                GameStateLoadMP.logger.error(
                                        e) { "Failed to log into server" }
                                engine.switchState(
                                        GameStateServerDisconnect(
                                                e.message ?: "", engine))
                            }) { init ->
                                progress?.invoke("Loading world...", 1.0)
                                val game = GameStateGameSP(init, source,
                                        server, scene, engine)
                                this.server = null
                                this.source = null
                                engine.switchState(game)
                            }
                            connection
                        }
                    }
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
