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

import org.tobi29.scapes.client.ScapesClient
import org.tobi29.scapes.client.connection.LocalClientConnection
import org.tobi29.scapes.client.gui.GuiLoading
import org.tobi29.scapes.connection.Account
import org.tobi29.scapes.connection.ServerInfo
import org.tobi29.scapes.engine.GameState
import org.tobi29.scapes.engine.ScapesEngine
import org.tobi29.scapes.engine.graphics.Scene
import org.tobi29.scapes.engine.graphics.renderScene
import org.tobi29.scapes.engine.server.SSLHandle
import org.tobi29.scapes.engine.server.SSLProvider
import org.tobi29.scapes.engine.utils.UnsupportedJVMException
import org.tobi29.scapes.engine.utils.io.IOException
import org.tobi29.scapes.engine.utils.logging.KLogging
import org.tobi29.scapes.engine.utils.math.round
import org.tobi29.scapes.engine.utils.tag.TagMap
import org.tobi29.scapes.engine.utils.tag.toMap
import org.tobi29.scapes.server.ScapesServer
import org.tobi29.scapes.server.connection.LocalPlayerConnection
import org.tobi29.scapes.server.format.WorldSource
import org.tobi29.scapes.server.ssl.dummy.DummyKeyManagerProvider

class GameStateLoadSP(private var source: WorldSource?,
                      engine: ScapesEngine,
                      private val scene: Scene) : GameState(engine) {
    private val scapes = engine.game as ScapesClient
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
                    val serverConfigMap =
                            scapes.configMap["IntegratedServer"]?.toMap() ?: TagMap()
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

                    server = ScapesServer(source, serverConfigMap, serverInfo,
                            ssl, engine.taskExecutor)
                    step++
                }
                1 -> {
                    gui?.setProgress("Starting server...", 0.5)
                    val server = server ?: throw IllegalStateException(
                            "Server lost too early")
                    val loadingRadius = round(scapes.renderDistance) + 16
                    val account = Account[scapes.home.resolve(
                            "Account.properties")]
                    gui?.setProgress("Loading world...", 1.0)

                    server.connections.addConnection { worker, connection ->
                        val player = LocalPlayerConnection(worker,
                                server.connection, loadingRadius)
                        scapes.connection.addConnection { worker, connection ->
                            val game = GameStateGameSP({
                                LocalClientConnection(worker, it,
                                        player, server.plugins, loadingRadius,
                                        account)
                            }, scene, source, server, engine)
                            this@GameStateLoadSP.server = null
                            this@GameStateLoadSP.source = null
                            engine.switchState(game)
                            game.awaitInit()
                            game.client.run(connection)
                        }
                        player.run(connection)
                    }
                    step++
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
