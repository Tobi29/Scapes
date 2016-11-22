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
package org.tobi29.scapes.client.gui.desktop

import mu.KLogging
import org.tobi29.scapes.engine.GameState
import org.tobi29.scapes.engine.graphics.TextureFilter
import org.tobi29.scapes.engine.graphics.TextureWrap
import org.tobi29.scapes.engine.gui.*
import org.tobi29.scapes.engine.input.FileType
import org.tobi29.scapes.engine.utils.graphics.decodePNG
import org.tobi29.scapes.engine.utils.io.BufferedReadChannelStream
import org.tobi29.scapes.engine.utils.io.filesystem.*
import org.tobi29.scapes.engine.utils.io.process
import org.tobi29.scapes.engine.utils.io.put
import org.tobi29.scapes.engine.utils.io.use
import org.tobi29.scapes.engine.utils.stream
import org.tobi29.scapes.plugins.PluginFile
import org.tobi29.scapes.plugins.Plugins
import java.io.IOException
import java.nio.channels.Channels

class GuiPlugins(state: GameState, previous: Gui, style: GuiStyle) : GuiMenu(
        state, "Plugins", previous, style) {
    private val path: FilePath
    private val scrollPane: GuiComponentScrollPaneViewport

    init {
        path = state.engine.home.resolve("plugins")
        scrollPane = pane.addVert(16.0, 5.0, -1.0, -1.0) {
            GuiComponentScrollPane(it, 70)
        }.viewport
        val add = addControl(60) { button(it, "Add") }
        updatePlugins()

        selection(-1, add)

        add.on(GuiEvent.CLICK_LEFT) { event ->
            try {
                state.engine.container.openFileDialog(
                        FileType("*.jar", "Jar Archive"),
                        "Import plugin", true) { name, input ->
                    val temp = createTempFile("Plugin", ".jar")
                    write(temp) { output ->
                        process(input, put(output))
                    }
                    val newPlugin = PluginFile(temp)
                    move(temp, path.resolve(
                            newPlugin.id() + ".jar"))
                    updatePlugins()
                }
            } catch (e: IOException) {
                logger.warn { "Failed to import plugin: $e" }
            }
        }
    }

    private fun updatePlugins() {
        try {
            scrollPane.removeAll()
            Plugins.installed(path).stream().sorted { plugin1, plugin2 ->
                plugin1.name().compareTo(plugin2.name())
            }.forEach { file ->
                scrollPane.addVert(0.0, 0.0, -1.0, 70.0) {
                    Element(it, file)
                }
            }
        } catch (e: IOException) {
            logger.warn { "Failed to read plugins: $e" }
        }

    }

    private inner class Element(parent: GuiLayoutData, plugin: PluginFile) : GuiComponentGroupSlab(
            parent) {
        init {
            val icon = addHori(15.0, 15.0, 40.0,
                    -1.0) { parent: GuiLayoutData -> GuiComponentIcon(parent) }
            val label = addHori(5.0, 20.0, -1.0, -1.0) {
                button(it, "Invalid plugin")
            }
            val delete = addHori(5.0, 20.0, 80.0, -1.0) {
                button(it, "Delete")
            }

            selection(delete)

            if (plugin.file() != null) {
                delete.on(GuiEvent.CLICK_LEFT) { event ->
                    try {
                        plugin.file()?.let(::delete)
                        scrollPane.remove(this)
                    } catch (e: IOException) {
                        logger.warn { "Failed to delete plugin: $e" }
                    }
                }
                try {
                    label.setText(plugin.name())
                    plugin.file()?.let {
                        zipFile(it).use { zip ->
                            val stream = BufferedReadChannelStream(
                                    Channels.newChannel(zip.getInputStream(
                                            zip.getEntry("Icon.png"))))
                            val image = decodePNG(stream) {
                                state.engine.allocate(it)
                            }
                            val texture = state.engine.graphics.createTexture(
                                    image, 0, TextureFilter.LINEAR,
                                    TextureFilter.LINEAR, TextureWrap.CLAMP,
                                    TextureWrap.CLAMP)
                            icon.texture = texture
                        }
                    }
                } catch (e: IOException) {
                }
            }
        }
    }

    companion object : KLogging()
}
