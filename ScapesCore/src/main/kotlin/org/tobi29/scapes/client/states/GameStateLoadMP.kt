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
import org.tobi29.scapes.client.gui.desktop.GuiCertificateWarning
import org.tobi29.scapes.client.gui.desktop.GuiLoading
import org.tobi29.scapes.client.gui.touch.GuiTouchLoading
import org.tobi29.scapes.connection.Account
import org.tobi29.scapes.engine.Container
import org.tobi29.scapes.engine.GameState
import org.tobi29.scapes.engine.ScapesEngine
import org.tobi29.scapes.engine.graphics.Scene
import org.tobi29.scapes.engine.gui.Gui
import org.tobi29.scapes.engine.server.AddressResolver
import org.tobi29.scapes.engine.server.PacketBundleChannel
import org.tobi29.scapes.engine.server.RemoteAddress
import org.tobi29.scapes.engine.server.SSLProvider
import org.tobi29.scapes.engine.utils.io.tag.getDouble
import org.tobi29.scapes.engine.utils.math.round
import org.tobi29.scapes.engine.utils.task.Joiner
import java.io.IOException
import java.nio.channels.SocketChannel
import java.util.concurrent.atomic.AtomicBoolean

class GameStateLoadMP(private val address: RemoteAddress, engine: ScapesEngine,
                      scene: Scene) : GameState(engine, scene) {
    private var step = 0
    private var channel: SocketChannel? = null
    private var client: NewConnection? = null
    private var progress: ((String) -> Unit)? = null
    private var gui: Gui? = null

    override fun init() {
        val valueSupplier = {
            if (step < 0)
                Double.NEGATIVE_INFINITY
            else if (step >= 5) Double.POSITIVE_INFINITY else step / 5.0
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
        engine.guiStack.add("20-Progress", gui!!)
    }

    override val isMouseGrabbed: Boolean
        get() = false

    override fun step(delta: Double) {
        try {
            when (step) {
                0 -> {
                    step++
                    progress!!("Connecting to server...")
                }
                1 -> {
                    val socketAddress = AddressResolver.resolve(address,
                            engine.taskExecutor)
                    if (socketAddress != null) {
                        channel = SocketChannel.open()
                        channel!!.configureBlocking(false)
                        channel!!.connect(socketAddress)
                        step++
                    }
                }
                2 -> if (channel!!.finishConnect()) {
                    step++
                    progress!!("Sending request...")
                }
                3 -> {
                    val bundleChannel: PacketBundleChannel
                    val ssl = SSLProvider.sslHandle { certificates ->
                        engine.guiStack.remove(gui!!)
                        try {
                            for (certificate in certificates) {
                                val result = AtomicBoolean()
                                val joinable = Joiner.BasicJoinable()
                                val warning = GuiCertificateWarning(this,
                                        certificate, { value ->
                                    result.set(value)
                                    joinable.join()
                                }, gui!!.style)
                                engine.guiStack.add("10-Menu", warning)
                                joinable.joiner.join { warning.isValid }
                                engine.guiStack.remove(warning)
                                if (!result.get()) {
                                    return@sslHandle false
                                }
                            }
                            return@sslHandle true
                        } finally {
                            engine.guiStack.add("20-Progress", gui!!)
                        }
                    }
                    bundleChannel = PacketBundleChannel(address, channel!!,
                            engine.taskExecutor, ssl, true)
                    val loadingRadius = round(engine.tagStructure.getStructure(
                            "Scapes")?.getDouble("RenderDistance") ?: 0.0) + 16
                    val account = Account[engine.home.resolve(
                            "Account.properties")]
                    client = NewConnection(engine, bundleChannel, account,
                            loadingRadius)
                    step++
                }
                4 -> {
                    val status = client!!.login()
                    if (status != null) {
                        progress!!(status)
                    } else {
                        step++
                        progress!!("Loading world...")
                    }
                }
                5 -> {
                    val game = GameStateGameMP(client!!.finish(), scene,
                            engine)
                    engine.switchState(game)
                }
            }
        } catch (e: IOException) {
            logger.error(e) { "Failed to connect to server" }
            engine.switchState(
                    GameStateServerDisconnect(e.message ?: "", engine))
            step = -1
        }

    }

    companion object : KLogging()
}
