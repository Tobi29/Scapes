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

package org.tobi29.scapes.client.gui.touch

import mu.KLogging
import org.tobi29.scapes.client.states.GameStateLoadMP
import org.tobi29.scapes.connection.ConnectionInfo
import org.tobi29.scapes.connection.ConnectionType
import org.tobi29.scapes.connection.ServerInfo
import org.tobi29.scapes.engine.GameState
import org.tobi29.scapes.engine.graphics.TextureFilter
import org.tobi29.scapes.engine.graphics.TextureWrap
import org.tobi29.scapes.engine.gui.*
import org.tobi29.scapes.engine.server.*
import org.tobi29.scapes.engine.utils.BufferCreator
import org.tobi29.scapes.engine.utils.io.tag.TagStructure
import java.io.IOException
import java.nio.channels.SocketChannel
import java.util.*

class GuiTouchServerSelect(state: GameState, previous: Gui, style: GuiStyle) : GuiTouchMenuDouble(
        state, "Multiplayer", "Add", "Back", previous, style) {
    private val servers = ArrayList<TagStructure>()
    private val elements = ArrayList<Element>()
    private val scrollPane: GuiComponentScrollPaneViewport

    init {
        scrollPane = pane.addVert(112.0, 10.0, 736.0, 320.0
        ) { GuiComponentScrollPane(it, 70) }.viewport()

        save.on(GuiEvent.CLICK_LEFT) { event ->
            state.engine.guiStack.add("10-Menu",
                    GuiTouchAddServer(state, this, style))
        }
        val scapesTag = state.engine.tagStructure.structure("Scapes")
        if (scapesTag.has("Servers")) {
            scapesTag.getList("Servers")?.let { servers.addAll(it) }
        }
        updateServers()
        on(GuiAction.BACK) { this.disposeServers() }
    }

    private fun disposeServers() {
        for (element in elements) {
            if (element.channel != null) {
                try {
                    element.channel!!.close()
                } catch (e: IOException) {
                    logger.warn { "Failed to close server info socket: $e" }
                }
            }
            scrollPane.remove(element)
        }
        elements.clear()
    }

    fun updateServers() {
        disposeServers()
        for (tagStructure in servers) {
            val element = scrollPane.addVert(0.0, 0.0, -1.0,
                    80.0) { Element(it, tagStructure) }
            elements.add(element)
        }
    }

    public override fun updateComponent(delta: Double) {
        elements.forEach { it.checkConnection() }
    }

    fun addServer(server: TagStructure) {
        servers.add(server)
        val scapesTag = state.engine.tagStructure.structure("Scapes")
        scapesTag.setList("Servers", servers)
    }

    private inner class Element(parent: GuiLayoutData, tagStructure: TagStructure) : GuiComponentGroupSlab(
            parent) {
        val icon: GuiComponentIcon
        val label: GuiComponentTextButton
        val address: RemoteAddress
        var channel: SocketChannel? = null
        var bundleChannel: PacketBundleChannel? = null
        var readState = 0

        init {
            icon = addHori(10.0, 10.0, 60.0,
                    -1.0) { parent: GuiLayoutData -> GuiComponentIcon(parent) }
            label = addHori(10.0, 10.0, -1.0, -1.0) {
                button(it, "Pinging...")
            }
            val delete = addHori(10.0, 10.0, 160.0, -1.0) {
                button(it, "Delete")
            }

            address = RemoteAddress(tagStructure)
            label.on(GuiEvent.CLICK_LEFT) { event ->
                state.engine.switchState(GameStateLoadMP(address, state.engine,
                        state.scene()))
            }
            delete.on(GuiEvent.CLICK_LEFT) { event ->
                servers.remove(tagStructure)
                val scapesTag = state.engine.tagStructure.structure("Scapes")
                scapesTag.setList("Servers", servers)
                elements.remove(this)
                scrollPane.remove(this)
            }
        }

        fun checkConnection() {
            try {
                when (readState) {
                    0 -> {
                        val socketAddress = AddressResolver.resolve(address,
                                state.engine.taskExecutor)
                        if (socketAddress != null) {
                            channel = SocketChannel.open()
                            channel!!.configureBlocking(false)
                            channel!!.connect(socketAddress)
                            readState++
                        }
                    }
                    1 -> if (channel!!.finishConnect()) {
                        // Ignore invalid certificates because worst case
                        // server name and icon get faked
                        val ssl = SSLProvider.sslHandle({ true })
                        bundleChannel = PacketBundleChannel(address, channel!!,
                                state.engine.taskExecutor, ssl,
                                true)
                        val output = bundleChannel!!.outputStream
                        output.put(ConnectionInfo.header())
                        output.put(ConnectionType.GET_INFO.data().toInt())
                        bundleChannel!!.queueBundle()
                        readState++
                    }
                    2 -> if (bundleChannel!!.process({ bundle ->
                        if (readState != 2) {
                            return@process true
                        }
                        val infoBuffer = BufferCreator.bytes(bundle.remaining())
                        bundle[infoBuffer]
                        infoBuffer.flip()
                        val serverInfo = ServerInfo(infoBuffer)
                        label.setText(serverInfo.name)
                        val image = serverInfo.image
                        val texture = state.engine.graphics.createTexture(image,
                                0,
                                TextureFilter.NEAREST,
                                TextureFilter.NEAREST,
                                TextureWrap.CLAMP,
                                TextureWrap.CLAMP)
                        icon.texture = texture
                        bundleChannel!!.requestClose()
                        readState++
                        true
                    })) {
                        readState = -1
                    }
                }
            } catch (e: IOException) {
                logger.info { "Failed to fetch server info: $e" }
                readState = -1
                label.setText(error(e))
            }

            if (readState == -1) {
                readState = -2
                try {
                    channel!!.close()
                } catch (e: IOException) {
                    logger.warn { "Failed to close server info socket: $e" }
                }

            }
        }
    }

    companion object : KLogging() {
        private fun error(e: Throwable): String {
            val message = e.message
            if (message != null) {
                return message
            }
            return e.javaClass.simpleName
        }
    }
}
