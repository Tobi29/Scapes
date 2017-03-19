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

package org.tobi29.scapes.client.states

import mu.KLogging
import org.tobi29.scapes.client.ScapesClient
import org.tobi29.scapes.client.connection.NewClientConnection
import org.tobi29.scapes.client.connection.RemoteClientConnection
import org.tobi29.scapes.client.gui.GuiLoading
import org.tobi29.scapes.connection.Account
import org.tobi29.scapes.connection.ServerInfo
import org.tobi29.scapes.engine.GameState
import org.tobi29.scapes.engine.ScapesEngine
import org.tobi29.scapes.engine.graphics.Scene
import org.tobi29.scapes.engine.graphics.renderScene
import org.tobi29.scapes.engine.server.*
import org.tobi29.scapes.engine.utils.UnsupportedJVMException
import org.tobi29.scapes.engine.utils.tag.TagMap
import org.tobi29.scapes.engine.utils.tag.toDouble
import org.tobi29.scapes.engine.utils.tag.toMap
import org.tobi29.scapes.engine.utils.math.round
import org.tobi29.scapes.server.ScapesServer
import org.tobi29.scapes.server.format.WorldSource
import org.tobi29.scapes.server.ssl.dummy.DummyKeyManagerProvider
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.channels.SelectionKey

class GameStateLoadSocketSP(private var source: WorldSource?,
                            engine: ScapesEngine,
                            private val scene: Scene) : GameState(
        engine) {
    private var step = 0
    private var server: ScapesServer? = null
    private var gui: GuiLoading? = null

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
        gui = GuiLoading(this, engine.guiStyle).apply {
            engine.guiStack.add("20-Progress", this)
        }
        switchPipeline { gl ->
            renderScene(gl, scene)
        }
    }

    override val isMouseGrabbed: Boolean
        get() = false

    override fun step(delta: Double) {
        val source = source ?: return
        try {
            when (step) {
                0 -> {
                    gui?.setProgress("Creating server...", 0.0)
                    val serverConfigMap = (engine.configMap["Scapes"]?.toMap()?.get(
                            "IntegratedServer")?.toMap() ?: TagMap())
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

                    this.server = ScapesServer(source, serverConfigMap,
                            serverInfo, ssl, engine)
                    step++
                }
                1 -> {
                    gui?.setProgress("Starting server...", 0.2)
                    val server = server ?: throw IllegalStateException(
                            "Server lost too early")
                    val port = server.connection.start(0)
                    if (port <= 0) {
                        throw IOException(
                                "Unable to open server socket (Invalid port returned: $port)")
                    }
                    val address = InetSocketAddress(port)

                    step++
                    gui?.setProgress("Connecting to local server...", 0.4)

                    val fail = { e: Exception ->
                        logger.error(
                                e) { "Failed to connect to server" }
                        engine.switchState(
                                GameStateServerDisconnect(
                                        e.message ?: "", engine))
                    }

                    (engine.game as ScapesClient).connection.addConnection { worker, connection ->
                        val channel = try {
                            connect(worker, address)
                        } catch (e: Exception) {
                            fail(e)
                            return@addConnection
                        }
                        try {
                            channel.register(worker.joiner.selector,
                                    SelectionKey.OP_READ)
                            gui?.setProgress("Logging in...", 0.6)
                            val bundleChannel: PacketBundleChannel
                            val ssl = SSLProvider.sslHandle { true }
                            bundleChannel = PacketBundleChannel(
                                    RemoteAddress(address), channel,
                                    engine.taskExecutor, ssl, true)
                            val loadingRadius = round(
                                    engine.configMap["Scapes"]?.toMap()?.get(
                                            "RenderDistance")?.toDouble() ?: 0.0) + 16
                            val account = Account[engine.home.resolve(
                                    "Account.properties")]
                            val (plugins, loadingDistanceServer) = NewClientConnection.run(
                                    bundleChannel, engine, account,
                                    loadingRadius, { status ->
                                gui?.setProgress(status,
                                        0.8)
                            }) ?: return@addConnection

                            gui?.setProgress("Loading world...", 1.0)
                            val game = GameStateGameSP({ state ->
                                RemoteClientConnection(worker, state,
                                        bundleChannel, plugins,
                                        loadingDistanceServer)
                            }, scene, source, server, engine)
                            this@GameStateLoadSocketSP.server = null
                            this@GameStateLoadSocketSP.source = null
                            engine.switchState(game)
                            game.awaitInit()
                            game.client.run(connection)
                        } catch (e: Exception) {
                            fail(e)
                        } finally {
                            try {
                                channel.close()
                            } catch (e: IOException) {
                                logger.warn(
                                        e) { "Failed to close socket" }
                            }
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
