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

package org.tobi29.scapes.client.gui

import mu.KLogging
import org.tobi29.scapes.client.ScapesClient
import org.tobi29.scapes.client.states.GameStateLoadMP
import org.tobi29.scapes.client.states.GameStateLoadSocketSP
import org.tobi29.scapes.client.states.scenes.SceneMenu
import org.tobi29.scapes.connection.ConnectionInfo
import org.tobi29.scapes.connection.ConnectionType
import org.tobi29.scapes.connection.ServerInfo
import org.tobi29.scapes.engine.GameState
import org.tobi29.scapes.engine.graphics.TextureFilter
import org.tobi29.scapes.engine.graphics.TextureWrap
import org.tobi29.scapes.engine.gui.*
import org.tobi29.scapes.engine.resource.Resource
import org.tobi29.scapes.engine.server.*
import org.tobi29.scapes.engine.utils.ByteBuffer
import org.tobi29.scapes.engine.utils.io.tag.TagStructure
import org.tobi29.scapes.engine.utils.io.tag.getListStructure
import java.io.IOException
import java.nio.channels.SelectionKey
import java.util.*

class GuiServerSelect(state: GameState,
                      previous: Gui,
                      private val scene: SceneMenu,
                      style: GuiStyle) : GuiMenu(state, "Multiplayer", previous,
        style) {
    private val servers = ArrayList<TagStructure>()
    private val scrollPane: GuiComponentScrollPaneViewport

    init {
        scrollPane = pane.addVert(16.0, 5.0, -1.0, -1.0) {
            GuiComponentScrollPane(it, 70)
        }.viewport
        val add = addControl(60) { button(it, "Add") }

        selection(-1, add)

        add.on(GuiEvent.CLICK_LEFT) { event ->
            state.engine.guiStack.add("10-Menu",
                    GuiAddServer(state, this, style))
        }

        val scapesTag = state.engine.tagStructure.structure("Scapes")
        scapesTag.getListStructure("Servers") { servers.add(it) }
        updateServers()
    }

    fun updateServers() {
        scrollPane.removeAll()
        for (tagStructure in servers) {
            scrollPane.addVert(0.0, 0.0, -1.0, 70.0) {
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
            icon = addHori(15.0, 15.0, 40.0,
                    -1.0) { parent: GuiLayoutData -> GuiComponentIcon(parent) }
            label = addHori(5.0, 20.0, -1.0, -1.0) {
                button(it, "Pinging...")
            }
            val delete = addHori(5.0, 20.0, 80.0, -1.0) {
                button(it, "Delete")
            }

            selection(label, delete)

            address = RemoteAddress(tagStructure)
            label.on(GuiEvent.CLICK_LEFT) { event ->
                state.engine.switchState(GameStateLoadMP(address, state.engine,
                        scene))
            }
            delete.on(GuiEvent.CLICK_LEFT) { event ->
                servers.remove(tagStructure)
                val scapesTag = state.engine.tagStructure.structure("Scapes")
                scapesTag.setList("Servers", servers)
                scrollPane.remove(this)
            }

            val fail = { e: Exception ->
                label.setText(error(e))
            }
            (state.engine.game as ScapesClient).connection.addConnection { worker, connection ->
                val channel = try {
                    connect(worker, address)
                } catch (e: Exception) {
                    fail(e)
                    return@addConnection
                }
                try {
                    channel.register(worker.joiner.selector,
                            SelectionKey.OP_READ)
                    label.setText("Fetching info...")
                    // Ignore invalid certificates because worst case
                    // server name and icon get faked
                    val ssl = SSLProvider.sslHandle { true }
                    val bundleChannel = PacketBundleChannel(address,
                            channel,
                            state.engine.taskExecutor, ssl, true)
                    val output = bundleChannel.outputStream
                    output.put(ConnectionInfo.header())
                    output.put(ConnectionType.GET_INFO.data().toInt())
                    bundleChannel.queueBundle()
                    if (bundleChannel.receive()) {
                        return@addConnection
                    }
                    val infoBuffer = ByteBuffer(
                            bundleChannel.inputStream.remaining())
                    bundleChannel.inputStream[infoBuffer]
                    infoBuffer.flip()
                    val serverInfo = ServerInfo(infoBuffer)
                    label.setText(serverInfo.name)
                    val image = serverInfo.image
                    val texture = state.engine.graphics.createTexture(
                            image, 0, TextureFilter.NEAREST,
                            TextureFilter.NEAREST,
                            TextureWrap.CLAMP,
                            TextureWrap.CLAMP)
                    icon.texture = Resource(texture)
                    bundleChannel.aClose()
                } catch (e: Exception) {
                    label.setText(error(e))
                } finally {
                    try {
                        channel.close()
                    } catch (e: IOException) {
                        GameStateLoadSocketSP.logger.warn(
                                e) { "Failed to close socket" }
                    }
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
