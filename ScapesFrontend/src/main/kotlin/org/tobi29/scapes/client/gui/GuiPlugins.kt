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

import org.tobi29.graphics.decodePng
import org.tobi29.logging.KLogging
import org.tobi29.scapes.engine.GameState
import org.tobi29.scapes.engine.graphics.TextureFilter
import org.tobi29.scapes.engine.gui.*
import org.tobi29.scapes.plugins.Plugins
import org.tobi29.scapes.plugins.spi.PluginDescription

class GuiPlugins(
    state: GameState,
    previous: Gui,
    style: GuiStyle
) : GuiMenuSingle(
    state, "Plugins", "Back", previous, style
) {
    private val scrollPane: GuiComponentScrollPaneViewport

    init {
        scrollPane = pane.addVert(16.0, 5.0, -1.0, -1.0) {
            GuiComponentScrollPane(it, 70)
        }.viewport
        updatePlugins()
    }

    private fun updatePlugins() {
        scrollPane.removeAll()
        Plugins.available().sortedBy { it.first.name }.forEach { (plugin, _) ->
            scrollPane.addVert(0.0, 0.0, -1.0, 70.0) {
                Element(it, plugin)
            }
        }
    }

    private inner class Element(
        parent: GuiLayoutData,
        plugin: PluginDescription
    ) : GuiComponentGroupSlab(
        parent
    ) {
        init {
            val icon = addHori(15.0, 15.0, 40.0, -1.0) { GuiComponentIcon(it) }
            addHori(5.0, 20.0, -1.0, -1.0) { button(it, plugin.name) }

            icon.texture = engine.resources.load {
                engine.graphics.createTexture(
                    plugin.icon.readAsync { decodePng(it) },
                    minFilter = TextureFilter.LINEAR,
                    magFilter = TextureFilter.LINEAR
                )
            }
        }
    }

    companion object : KLogging()
}
