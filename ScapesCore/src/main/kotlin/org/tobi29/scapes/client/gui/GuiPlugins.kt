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
import org.tobi29.scapes.engine.GameState
import org.tobi29.scapes.engine.graphics.TextureFilter
import org.tobi29.scapes.engine.graphics.TextureWrap
import org.tobi29.scapes.engine.gui.*
import org.tobi29.scapes.engine.input.FileType
import org.tobi29.scapes.engine.resource.Resource
import org.tobi29.scapes.engine.utils.graphics.decodePNG
import org.tobi29.scapes.engine.utils.io.BufferedReadChannelStream
import org.tobi29.scapes.engine.utils.io.filesystem.*
import org.tobi29.scapes.engine.utils.io.process
import org.tobi29.scapes.engine.utils.io.put
import org.tobi29.scapes.engine.utils.use
import org.tobi29.scapes.plugins.PluginFile
import org.tobi29.scapes.plugins.Plugins
import java.io.IOException
import java.nio.channels.Channels

class GuiPlugins(state: GameState,
                 previous: Gui,
                 style: GuiStyle) : GuiMenuDouble(
        state, "Plugins", "Add", "Back", previous, style) {
    private val path: FilePath
    private val scrollPane: GuiComponentScrollPaneViewport

    init {
        path = state.engine.home.resolve("plugins")
        scrollPane = pane.addVert(16.0, 5.0, -1.0, -1.0) {
            GuiComponentScrollPane(it, 70)
        }.viewport
        updatePlugins()

        save.on(GuiEvent.CLICK_LEFT) { event ->
            try {
                state.engine.container.openFileDialog(
                        FileType("*.jar", "Jar Archive"),
                        "Import plugin", true) { name, input ->
                    val temp = createTempFile("Plugin", ".jar")
                    write(temp) { output ->
                        process(input, put(output))
                    }
                    val newPlugin = PluginFile(temp)
                    move(temp, path.resolve(newPlugin.id() + ".jar"))
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
            Plugins.installed(
                    path).asSequence().sortedBy { it.name() }.forEach { file ->
                scrollPane.addVert(0.0, 0.0, -1.0, 70.0) {
                    Element(it, file)
                }
            }
        } catch (e: IOException) {
            logger.warn { "Failed to read plugins: $e" }
        }

    }

    private inner class Element(parent: GuiLayoutData,
                                plugin: PluginFile) : GuiComponentGroupSlab(
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
                                            zip.getEntry(
                                                    "scapes/plugin/Icon.png"))))
                            val image = decodePNG(stream) {
                                state.engine.allocate(it)
                            }
                            val texture = state.engine.graphics.createTexture(
                                    image, 0, TextureFilter.LINEAR,
                                    TextureFilter.LINEAR, TextureWrap.CLAMP,
                                    TextureWrap.CLAMP)
                            icon.texture = Resource(texture)
                        }
                    }
                } catch (e: IOException) {
                }
            }
        }
    }

    companion object : KLogging()
}
