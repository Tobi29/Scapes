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
import org.tobi29.scapes.engine.server.addOutConnection
import org.tobi29.scapes.engine.utils.io.tag.getDouble
import org.tobi29.scapes.engine.utils.math.round
import org.tobi29.scapes.engine.utils.task.Joiner
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

        (engine.game as ScapesClient).connection.addOutConnection(address,
                { e ->
                    logger.error(e) { "Failed to connect to server" }
                    engine.switchState(
                            GameStateServerDisconnect(e.message ?: "", engine))
                }) { worker, channel ->
            gui?.setProgress("Logging in...", 0.3)
            val bundleChannel: PacketBundleChannel
            val ssl = SSLProvider.sslHandle { certificates ->
                gui?.let { gui ->
                    engine.guiStack.remove(gui)
                    try {
                        for (certificate in certificates) {
                            val result = AtomicBoolean()
                            val joinable = Joiner.BasicJoinable()
                            val warning = GuiCertificateWarning(this,
                                    certificate, { value ->
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
            bundleChannel = PacketBundleChannel(address, channel,
                    engine.taskExecutor, ssl, true)
            val loadingRadius = round(engine.tagStructure.getStructure(
                    "Scapes")?.getDouble("RenderDistance") ?: 0.0) + 16
            val account = Account[engine.home.resolve(
                    "Account.properties")]
            worker.addConnection {
                val connection = NewClientConnection(worker, engine,
                        bundleChannel, account, loadingRadius, { status ->
                    gui?.setProgress(status, 0.66)
                }, { e ->
                    logger.error(e) { "Failed to connect to server" }
                    engine.switchState(
                            GameStateServerDisconnect(e.message ?: "", engine))
                }) { init ->
                    gui?.setProgress("Loading world...", 1.0)
                    val game = GameStateGameMP(init, scene, engine)
                    engine.switchState(game)
                }
                connection
            }
        }
    }

    override val isMouseGrabbed: Boolean
        get() = false

    override fun step(delta: Double) {
    }

    companion object : KLogging()
}
