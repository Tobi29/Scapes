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
import org.tobi29.scapes.client.gui.GuiCertificateWarning
import org.tobi29.scapes.client.gui.GuiLoading
import org.tobi29.scapes.connection.Account
import org.tobi29.scapes.engine.GameState
import org.tobi29.scapes.engine.ScapesEngine
import org.tobi29.scapes.engine.graphics.Scene
import org.tobi29.scapes.engine.graphics.renderScene
import org.tobi29.scapes.engine.server.PacketBundleChannel
import org.tobi29.scapes.engine.server.RemoteAddress
import org.tobi29.scapes.engine.server.SSLProvider
import org.tobi29.scapes.engine.server.connect
import org.tobi29.scapes.engine.utils.tag.toDouble
import org.tobi29.scapes.engine.utils.tag.toMap
import org.tobi29.scapes.engine.utils.math.round
import org.tobi29.scapes.engine.utils.task.Joiner
import java.io.IOException
import java.nio.channels.SelectionKey
import java.util.concurrent.atomic.AtomicBoolean

class GameStateLoadMP(private val address: RemoteAddress,
                      engine: ScapesEngine,
                      private val scene: Scene) : GameState(engine) {
    private var gui: GuiLoading? = null

    override fun init() {
        gui = GuiLoading(this, engine.guiStyle).apply {
            setProgress("Connecting...", 0.0)
            engine.guiStack.add("20-Progress", this)
        }
        switchPipeline { gl ->
            renderScene(gl, scene)
        }

        val fail = { e: Exception ->
            GameStateLoadSocketSP.logger.error(
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
                val ssl = SSLProvider.sslHandle { certificates ->
                    gui?.let { gui ->
                        engine.guiStack.remove(gui)
                        try {
                            for (certificate in certificates) {
                                val result = AtomicBoolean()
                                val joinable = Joiner.BasicJoinable()
                                val warning = GuiCertificateWarning(
                                        this@GameStateLoadMP, certificate,
                                        { value ->
                                            result.set(value)
                                            joinable.join()
                                        }, gui.style)
                                engine.guiStack.add("10-Menu", warning)
                                joinable.joiner.join { warning.isValid }
                                engine.guiStack.remove(warning)
                                if (!result.get()) {
                                    return@sslHandle false
                                }
                            }
                            return@sslHandle true
                        } finally {
                            engine.guiStack.add("20-Progress", gui)
                        }
                    }
                    return@sslHandle false
                }
                bundleChannel = PacketBundleChannel(
                        address, channel,
                        engine.taskExecutor, ssl, true)
                val loadingRadius = round(
                        engine.configMap["Scapes"]?.toMap()?.get(
                                "RenderDistance")?.toDouble() ?: 0.0) + 16
                val account = Account[engine.home.resolve(
                        "Account.properties")]
                val (plugins, loadingDistanceServer) = NewClientConnection.run(
                        bundleChannel, engine, account, loadingRadius,
                        { status ->
                            gui?.setProgress(status, 0.66)
                        }) ?: return@addConnection
                gui?.setProgress("Loading world...", 1.0)
                val game = GameStateGameMP({ state ->
                    RemoteClientConnection(worker, state, bundleChannel,
                            plugins, loadingDistanceServer)
                }, scene, engine)
                engine.switchState(game)
                game.awaitInit()
                game.client.run(connection)
            } catch (e: Exception) {
                fail(e)
            } finally {
                try {
                    channel.close()
                } catch (e: IOException) {
                    logger.warn(e) { "Failed to close socket" }
                }
            }
        }
    }

    override val isMouseGrabbed: Boolean
        get() = false

    override fun step(delta: Double) {
    }

    companion object : KLogging()
}
