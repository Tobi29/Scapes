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

import org.tobi29.logging.KLogging
import org.tobi29.scapes.client.ScapesClient
import org.tobi29.scapes.client.states.GameStateLoadMP
import org.tobi29.scapes.client.states.scenes.SceneMenu
import org.tobi29.scapes.connection.ConnectionInfo
import org.tobi29.scapes.connection.ConnectionType
import org.tobi29.scapes.connection.ServerInfo
import org.tobi29.scapes.engine.GameState
import org.tobi29.scapes.engine.graphics.TextureFilter
import org.tobi29.scapes.engine.graphics.TextureWrap
import org.tobi29.scapes.engine.gui.*
import org.tobi29.scapes.engine.resource.Resource
import org.tobi29.server.*
import org.tobi29.io.IOException
import org.tobi29.io.toChannel
import org.tobi29.io.view
import org.tobi29.io.tag.MutableTagList
import org.tobi29.io.tag.TagMap
import org.tobi29.io.tag.listMut
import org.tobi29.io.tag.toMap
import java.nio.channels.SelectionKey

class GuiServerSelect(state: GameState,
                      previous: Gui,
                      private val scene: SceneMenu,
                      style: GuiStyle) : GuiMenuDouble(state, "Multiplayer",
        "Add", "Back", previous, style) {
    private val servers: MutableTagList
    private val scrollPane: GuiComponentScrollPaneViewport

    init {
        val scapes = engine[ScapesClient.COMPONENT]
        servers = scapes.configMap.listMut("Servers")

        scrollPane = pane.addVert(16.0, 5.0, -1.0, -1.0) {
            GuiComponentScrollPane(it, 70)
        }.viewport

        save.on(GuiEvent.CLICK_LEFT) { event ->
            state.engine.guiStack.add("10-Menu",
                    GuiAddServer(state, this, style))
        }
        updateServers()
    }

    fun updateServers() {
        scrollPane.removeAll()
        servers.asSequence().mapNotNull { it.toMap() }.forEach { tagMap ->
            scrollPane.addVert(0.0, 0.0, -1.0, 70.0) {
                Element(it, tagMap)
            }
        }
    }

    fun addServer(server: TagMap) {
        servers.add(server)
    }

    private inner class Element(parent: GuiLayoutData,
                                tagMap: TagMap) : GuiComponentGroupSlab(
            parent) {
        val icon: GuiComponentIcon
        val label: GuiComponentTextButton
        val address: RemoteAddress

        init {
            val scapes = engine[ScapesClient.COMPONENT]
            icon = addHori(15.0, 15.0, 40.0,
                    -1.0) { parent: GuiLayoutData -> GuiComponentIcon(parent) }
            label = addHori(5.0, 20.0, -1.0, -1.0) {
                button(it, "Pinging...")
            }
            val delete = addHori(5.0, 20.0, 80.0, -1.0) {
                button(it, "Delete")
            }

            address = RemoteAddress(tagMap)
            label.on(GuiEvent.CLICK_LEFT) { event ->
                state.engine.switchState(GameStateLoadMP(address, state.engine,
                        scene))
            }
            delete.on(GuiEvent.CLICK_LEFT) { event ->
                servers.remove(tagMap)
                scrollPane.remove(this)
            }

            val fail = { e: Exception ->
                label.setText(error(e))
            }

            val connectionManager = engine[ConnectionManager.COMPONENT]
            connectionManager.addConnection { worker, connection ->
                val channel = try {
                    connect(worker, address)
                } catch (e: Exception) {
                    fail(e)
                    return@addConnection
                }
                try {
                    channel.register(worker.selector, SelectionKey.OP_READ)
                    label.setText("Fetching info...")
                    // Ignore invalid certificates because worst case
                    // server name and icon get faked
                    val ssl = SSLHandle.insecure()
                    val secureChannel = ssl.newSSLChannel(address,
                            channel.toChannel(), state.engine.taskExecutor,
                            true)
                    val bundleChannel = PacketBundleChannel(secureChannel)
                    val output = bundleChannel.outputStream
                    output.put(ConnectionInfo.header().view)
                    output.put(ConnectionType.GET_INFO.data())
                    bundleChannel.queueBundle()
                    if (bundleChannel.receive()) {
                        return@addConnection
                    }
                    val infoBuffer = ByteArray(
                            bundleChannel.inputStream.remaining()).view
                    bundleChannel.inputStream.get(infoBuffer)
                    val serverInfo = ServerInfo(infoBuffer)
                    label.setText(serverInfo.name)
                    val image = serverInfo.image
                    val texture = state.engine.graphics.createTexture(
                            image, 0, TextureFilter.NEAREST,
                            TextureFilter.NEAREST,
                            TextureWrap.CLAMP,
                            TextureWrap.CLAMP)
                    icon.texture = Resource(texture)
                    bundleChannel.flushAsync()
                    secureChannel.requestClose()
                    bundleChannel.finishAsync()
                    secureChannel.finishAsync()
                } catch (e: Exception) {
                    label.setText(error(e))
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

    companion object : KLogging() {
        private fun error(e: Throwable): String {
            val message = e.message
            if (message != null) {
                return message
            }
            return e::class.java.simpleName
        }
    }
}
