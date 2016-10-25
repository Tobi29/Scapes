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
import org.tobi29.scapes.client.ScapesClient
import org.tobi29.scapes.client.connection.GetInfoOutConnection
import org.tobi29.scapes.client.states.GameStateLoadMP
import org.tobi29.scapes.connection.ConnectionInfo
import org.tobi29.scapes.connection.ConnectionType
import org.tobi29.scapes.engine.GameState
import org.tobi29.scapes.engine.graphics.TextureFilter
import org.tobi29.scapes.engine.graphics.TextureWrap
import org.tobi29.scapes.engine.gui.*
import org.tobi29.scapes.engine.server.PacketBundleChannel
import org.tobi29.scapes.engine.server.RemoteAddress
import org.tobi29.scapes.engine.server.SSLProvider
import org.tobi29.scapes.engine.server.addOutConnection
import org.tobi29.scapes.engine.utils.io.tag.TagStructure
import java.util.*

class GuiTouchServerSelect(state: GameState, previous: Gui, style: GuiStyle) : GuiTouchMenuDouble(
        state, "Multiplayer", "Add", "Back", previous, style) {
    private val servers = ArrayList<TagStructure>()
    private val scrollPane: GuiComponentScrollPaneViewport

    init {
        scrollPane = pane.addVert(112.0, 10.0, 736.0, 320.0) {
            GuiComponentScrollPane(it, 70)
        }.viewport()

        save.on(GuiEvent.CLICK_LEFT) { event ->
            state.engine.guiStack.add("10-Menu",
                    GuiTouchAddServer(state, this, style))
        }

        val scapesTag = state.engine.tagStructure.structure("Scapes")
        if (scapesTag.has("Servers")) {
            scapesTag.getList("Servers")?.let { servers.addAll(it) }
        }
        updateServers()
    }


    fun updateServers() {
        scrollPane.removeAll()
        for (tagStructure in servers) {
            scrollPane.addVert(0.0, 0.0, -1.0, 90.0) {
                Element(it, tagStructure)
            }
        }
    }

    fun addServer(server: TagStructure) {
        servers.add(server)
        val scapesTag = state.engine.tagStructure.structure("Scapes")
        scapesTag.setList("Servers", servers)
    }

    private inner class Element(parent: GuiLayoutData,
                                tagStructure: TagStructure) : GuiComponentGroupSlab(
            parent) {
        val icon: GuiComponentIcon
        val label: GuiComponentTextButton
        val address: RemoteAddress

        init {
            icon = addHori(10.0, 10.0, 60.0,
                    -1.0) { parent: GuiLayoutData -> GuiComponentIcon(parent) }
            label = addHori(10.0, 10.0, -1.0, -1.0) {
                button(it, "Pinging...")
            }
            val delete = addHori(10.0, 10.0, 160.0, -1.0) {
                button(it, "Delete")
            }

            selection(label, delete)

            address = RemoteAddress(tagStructure)
            label.on(GuiEvent.CLICK_LEFT) { event ->
                state.engine.switchState(GameStateLoadMP(address, state.engine,
                        state.scene()))
            }
            delete.on(GuiEvent.CLICK_LEFT) { event ->
                servers.remove(tagStructure)
                val scapesTag = state.engine.tagStructure.structure("Scapes")
                scapesTag.setList("Servers", servers)
                scrollPane.remove(this)
            }

            (state.engine.game as ScapesClient).connection.addOutConnection(
                    address, { e ->
                label.setText(error(e))
            }) { worker, channel ->
                label.setText("Fetching info...")
                // Ignore invalid certificates because worst case
                // server name and icon get faked
                val ssl = SSLProvider.sslHandle({ certificates -> true })
                val bundleChannel = PacketBundleChannel(address, channel,
                        state.engine.taskExecutor, ssl, true)
                val output = bundleChannel.outputStream
                output.put(ConnectionInfo.header())
                output.put(ConnectionType.GET_INFO.data().toInt())
                bundleChannel.queueBundle()
                worker.addConnection {
                    val connection = GetInfoOutConnection(worker,
                            bundleChannel, { e ->
                        label.setText(error(e))
                    }) { serverInfo ->
                        label.setText(serverInfo.name)
                        val image = serverInfo.image
                        val texture = state.engine.graphics.createTexture(
                                image, 0, TextureFilter.NEAREST,
                                TextureFilter.NEAREST, TextureWrap.CLAMP,
                                TextureWrap.CLAMP)
                        icon.texture = texture
                    }
                    connection
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