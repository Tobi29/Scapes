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
import org.tobi29.scapes.client.connection.NewClientConnection
import org.tobi29.scapes.client.connection.RemoteClientConnection
import org.tobi29.scapes.client.gui.GuiCertificateWarning
import org.tobi29.scapes.client.gui.GuiLoading
import org.tobi29.scapes.connection.Account
import org.tobi29.scapes.engine.GameState
import org.tobi29.scapes.engine.ScapesEngine
import org.tobi29.scapes.engine.graphics.Scene
import org.tobi29.scapes.engine.graphics.renderScene
import org.tobi29.server.*
import org.tobi29.io.IOException
import org.tobi29.io.toChannel
import org.tobi29.logging.KLogging
import org.tobi29.scapes.entity.skin.ClientSkinStorage
import java.nio.channels.SelectionKey
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.roundToInt

class GameStateLoadMP(private val address: RemoteAddress,
                      engine: ScapesEngine,
                      private val scene: Scene) : GameState(engine) {
    private val gui: GuiLoading = GuiLoading(engine.guiStyle)

    override fun init() {
        gui.setProgress("Connecting...", 0.0)
        engine.guiStack.add("20-Progress", gui)
        switchPipeline { gl ->
            renderScene(gl, scene)
        }

        val fail = { e: Exception ->
            GameStateLoadSocketSP.logger.error(
                    e) { "Failed to connect to server" }
            engine.switchState(
                    GameStateServerDisconnect(e.message ?: "", engine))
        }

        engine[ConnectionManager.COMPONENT].addConnection { worker, connection ->
            val ssl = SSLHandle()
            try {
                connect(worker, connection, ssl)
            } catch (e: SSLCertificateException) {
                val certificates = e.certificates
                engine.guiStack.remove(gui)
                try {
                    for (certificate in certificates) {
                        val result = AtomicReference<Boolean?>()
                        val warning = GuiCertificateWarning(
                                this@GameStateLoadMP, certificate,
                                { value -> result.set(value) }, gui.style)
                        engine.guiStack.add("10-Menu", warning)
                        while (warning.isVisible) {
                            val ignore = result.get() ?: continue
                            if (ignore) {
                                break
                            } else {
                                fail(e)
                                return@addConnection
                            }
                        }
                        engine.guiStack.remove(warning)
                    }
                } finally {
                    engine.guiStack.add("20-Progress", gui)
                }
                val sslForce = SSLHandle.fromCertificates(certificates)
                connect(worker, connection, sslForce)
            } catch (e: IOException) {
                fail(e)
            }
        }
    }

    override fun dispose() {
        engine.guiStack.remove(gui)
    }

    private suspend fun connect(worker: ConnectionWorker,
                                connection: Connection,
                                ssl: SSLHandle) {
        val scapes = engine[ScapesClient.COMPONENT]

        val channel = connect(worker, address)
        try {
            channel.register(worker.selector, SelectionKey.OP_READ)
            gui.setProgress("Logging in...", 0.6)
            val secureChannel = ssl.newSSLChannel(address,
                    channel.toChannel(), engine.taskExecutor, true)
            val bundleChannel = PacketBundleChannel(secureChannel)
            val loadingRadius = (scapes.renderDistance).roundToInt() + 16
            val account = Account[scapes.home.resolve("Account.properties")]
            val (plugins, loadingDistanceServer) = NewClientConnection.run(
                    bundleChannel, engine, account, loadingRadius,
                    { status ->
                        gui.setProgress(status, 0.66)
                    }) ?: return@connect
            gui.setProgress("Loading world...", 1.0)
            val skinStorage = ClientSkinStorage(engine,
                    engine.graphics.textures["Scapes:image/entity/mob/Player"].getAsync())
            val game = GameStateGameMP({ state ->
                RemoteClientConnection(worker, state, address, bundleChannel,
                        secureChannel, plugins, loadingDistanceServer,
                        skinStorage)
            }, scene, engine)
            engine.switchState(game)
            game.awaitInit()
            game.client.run(connection)
            bundleChannel.flushAsync()
            secureChannel.requestClose()
            bundleChannel.finishAsync()
            secureChannel.finishAsync()
            logger.info { "Closed client connection!" }
            engine.switchState(GameStateMenu(engine))
        } finally {
            try {
                channel.close()
            } catch (e: IOException) {
                logger.warn(e) { "Failed to close socket" }
            }
        }
    }

    override val isMouseGrabbed: Boolean
        get() = false

    override fun step(delta: Double) {
    }

    companion object : KLogging()
}
