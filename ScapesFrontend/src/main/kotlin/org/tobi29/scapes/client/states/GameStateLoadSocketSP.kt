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

import org.tobi29.io.IOException
import org.tobi29.io.tag.TagMap
import org.tobi29.io.tag.toMap
import org.tobi29.io.toChannel
import org.tobi29.logging.KLogging
import org.tobi29.scapes.chunk.WorldClient
import org.tobi29.scapes.client.Playlist
import org.tobi29.scapes.client.ScapesClient
import org.tobi29.scapes.client.connection.NewClientConnection
import org.tobi29.scapes.client.connection.RemoteClientConnection
import org.tobi29.scapes.client.gui.GuiLoading
import org.tobi29.scapes.client.states.scenes.SceneScapesVoxelWorld
import org.tobi29.scapes.connection.Account
import org.tobi29.scapes.connection.ServerInfo
import org.tobi29.scapes.engine.GameState
import org.tobi29.scapes.engine.ScapesEngine
import org.tobi29.scapes.engine.graphics.Scene
import org.tobi29.scapes.engine.graphics.renderScene
import org.tobi29.scapes.entity.skin.ClientSkinStorage
import org.tobi29.scapes.server.ScapesServer
import org.tobi29.scapes.server.ScapesServerExecutor
import org.tobi29.scapes.server.format.WorldSource
import org.tobi29.scapes.server.ssl.dummy.DummyKeyManagerProvider
import org.tobi29.server.*
import java.net.InetSocketAddress
import java.nio.channels.SelectionKey
import kotlin.math.roundToInt

class GameStateLoadSocketSP(
    private var source: WorldSource?,
    engine: ScapesEngine,
    private val scene: Scene
) : GameState(
    engine
) {
    private val scapes = engine[ScapesClient.COMPONENT]
    private var step = 0
    private var server: ScapesServer? = null
    private val gui: GuiLoading = GuiLoading(engine.guiStyle)

    override fun init() {
        engine.guiStack.add("20-Progress", gui)
        switchPipeline { gl ->
            renderScene(gl, scene)
        }
    }

    override fun dispose() {
        engine.guiStack.remove(gui)
        try {
            server?.stop(ScapesServer.ShutdownReason.ERROR)
        } catch (e: IOException) {
            logger.error(
                e
            ) { "Failed to stop internal server after login error" }
        }
        try {
            source?.close()
        } catch (e: IOException) {
            logger.error(e) { "Failed to close world source" }
        }
    }

    override val isMouseGrabbed: Boolean
        get() = false

    override fun step(delta: Double) {
        val source = source ?: return
        val onError =
            { reason: String, address: RemoteAddress?, reconnect: Double? ->
                engine.switchState(
                    GameStateServerDisconnect(
                        reason, address, reconnect, engine
                    )
                )
            }
        try {
            when (step) {
                0 -> {
                    gui.setProgress("Creating server...", 0.0)
                    val serverConfigMap =
                        scapes.configMap["IntegratedServer"]?.toMap()
                                ?: TagMap()
                    val panorama = source.panorama()
                    val serverInfo: ServerInfo
                    if (panorama != null) {
                        serverInfo = ServerInfo(
                            "Local Server",
                            panorama.elements[0]
                        )
                    } else {
                        serverInfo = ServerInfo("Local Server")
                    }
                    val ssl = SSLHandle(DummyKeyManagerProvider.get())
                    this.server = ScapesServer(
                        source, serverConfigMap,
                        serverInfo, ssl, engine.taskExecutor,
                        engine[ScapesServerExecutor.COMPONENT]
                    )
                    step++
                }
                1 -> {
                    gui.setProgress("Starting server...", 0.2)
                    val server = server ?: throw IllegalStateException(
                        "Server lost too early"
                    )
                    val port = server.connection.start(0)
                    if (port <= 0) {
                        throw IOException(
                            "Unable to open server socket (Invalid port returned: $port)"
                        )
                    }
                    val address = InetSocketAddress(port)

                    step++
                    gui.setProgress("Connecting to local server...", 0.4)

                    engine[ConnectionManager.COMPONENT].addConnection { worker, connection ->
                        val channel = try {
                            connect(worker, address)
                        } catch (e: Exception) {
                            onError(
                                e.message ?: e::class.java.simpleName,
                                null, null
                            )
                            return@addConnection
                        }
                        try {
                            channel.register(
                                worker.selector,
                                SelectionKey.OP_READ
                            )
                            gui.setProgress("Logging in...", 0.6)
                            val ssl = SSLHandle.insecure()
                            val secureChannel = ssl.newSSLChannel(
                                RemoteAddress(address), channel.toChannel(),
                                engine.taskExecutor, true
                            )
                            val bundleChannel = PacketBundleChannel(
                                secureChannel
                            )
                            val loadingRadius =
                                (scapes.renderDistance).roundToInt() + 16
                            val account = Account[scapes.home.resolve(
                                "Account.properties"
                            )]
                            val skin = scapes.home.resolve("Skin.png")
                            val (plugins, loadingDistanceServer) = NewClientConnection.run(
                                bundleChannel, engine, account, loadingRadius,
                                skin, { status ->
                                    gui.setProgress(status, 0.8)
                                }) ?: return@addConnection

                            gui.setProgress("Loading world...", 1.0)
                            val skinStorage = ClientSkinStorage(
                                engine,
                                engine.graphics.textures["Scapes:image/entity/mob/Player"].getAsync()
                            )
                            val config = WorldClient.Config(
                                sceneConfig = SceneScapesVoxelWorld.Config(
                                    resolutionMultiplier = scapes.resolutionMultiplier,
                                    animations = scapes.animations,
                                    fxaa = scapes.fxaa,
                                    bloom = scapes.bloom,
                                    autoExposure = scapes.autoExposure
                                )
                            )
                            val playlist = Playlist(
                                scapes.home.resolve("playlists"), engine
                            )
                            val onClose = {
                                engine.switchState(
                                    GameStateMenu(engine)
                                )
                            }
                            val game = GameStateGameSP(
                                { state ->
                                    RemoteClientConnection(
                                        worker,
                                        state,
                                        RemoteAddress(address),
                                        bundleChannel,
                                        secureChannel,
                                        plugins,
                                        loadingDistanceServer,
                                        skinStorage,
                                        onError
                                    )
                                },
                                config, playlist, scene, source, server,
                                onClose, onError, engine
                            )
                            this@GameStateLoadSocketSP.server = null
                            this@GameStateLoadSocketSP.source = null
                            engine.switchState(game)
                            game.awaitInit()
                            game.client.run(connection)
                            bundleChannel.flushAsync()
                            secureChannel.requestClose()
                            bundleChannel.finishAsync()
                            secureChannel.finishAsync()
                            logger.info { "Closed client connection!" }
                            engine.switchState(GameStateMenu(engine))
                        } catch (e: Exception) {
                            onError(
                                e.message ?: e::class.java.simpleName,
                                null, null
                            )
                        } finally {
                            try {
                                channel.close()
                            } catch (e: IOException) {
                                logger.warn(e) { "Failed to close socket" }
                            }
                        }
                    }
                }
            }
        } catch (e: IOException) {
            logger.error(e) { "Failed to start internal server" }
            onError(
                e.message ?: e::class.java.simpleName,
                null, null
            )
            step = -1
        }
    }

    companion object : KLogging()
}
