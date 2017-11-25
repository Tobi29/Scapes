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

import kotlinx.coroutines.experimental.launch
import org.tobi29.scapes.Debug
import org.tobi29.scapes.client.SaveStorage
import org.tobi29.scapes.client.ScapesClient
import org.tobi29.scapes.client.states.GameStateLoadSP
import org.tobi29.scapes.client.states.GameStateLoadSocketSP
import org.tobi29.scapes.client.states.scenes.SceneMenu
import org.tobi29.scapes.engine.GameState
import org.tobi29.scapes.engine.graphics.TextureFilter
import org.tobi29.scapes.engine.graphics.TextureWrap
import org.tobi29.scapes.engine.gui.*
import org.tobi29.scapes.engine.resource.Resource
import org.tobi29.scapes.engine.utils.io.IOException
import org.tobi29.scapes.engine.utils.io.use
import org.tobi29.scapes.engine.utils.logging.KLogging
import org.tobi29.scapes.plugins.Plugins

class GuiSaveSelect(state: GameState,
                    previous: Gui,
                    private val scene: SceneMenu,
                    style: GuiStyle) : GuiMenuDouble(state, "Singleplayer",
        "Create", "Back", previous, style) {
    private val saves: SaveStorage
    private val scrollPane: GuiComponentScrollPaneViewport

    init {
        val scapes = engine[ScapesClient.COMPONENT]
        saves = scapes.saves

        scrollPane = pane.addVert(16.0, 5.0, -1.0, -1.0) {
            GuiComponentScrollPane(it, 70)
        }.viewport
        updateSaves()

        save.on(GuiEvent.CLICK_LEFT) { event ->
            launch(engine.taskExecutor) {
                try {
                    val path = scapes.home.resolve("plugins")
                    val plugins = Plugins.installed(path)
                    val worldTypes = plugins.asSequence()
                            .filter { plugin -> "WorldType" == plugin.parent() }
                            .sortedBy { it.name() }.toList()
                    if (worldTypes.isEmpty()) {
                        state.engine.guiStack.swap(this@GuiSaveSelect,
                                GuiMessage(state, this@GuiSaveSelect, "Error",
                                        NO_WORLD_TYPE, style))
                    } else {
                        state.engine.guiStack.swap(this@GuiSaveSelect,
                                GuiCreateWorld(state, this@GuiSaveSelect,
                                        worldTypes, plugins, style))
                    }
                } catch (e: IOException) {
                    logger.warn { "Failed to read plugins: $e" }
                }
            }
        }
    }

    fun updateSaves() {
        try {
            scrollPane.removeAll()
            saves.list().sorted().forEach { file ->
                scrollPane.addVert(0.0, 0.0, -1.0, 70.0) {
                    Element(it, file)
                }
            }
        } catch (e: IOException) {
            logger.warn { "Failed to read saves: $e" }
        }
    }

    private inner class Element(parent: GuiLayoutData,
                                name: String) : GuiComponentGroupSlab(
            parent) {
        init {
            val icon = addHori(15.0, 15.0, 40.0,
                    -1.0) { parent: GuiLayoutData -> GuiComponentIcon(parent) }
            val label = addHori(5.0, 20.0, -1.0, -1.0) { button(it, name) }
            val delete = addHori(5.0, 20.0, 80.0, -1.0) {
                button(it, "Delete")
            }

            label.on(GuiEvent.CLICK_LEFT) { event ->
                try {
                    if (Debug.socketSingleplayer()) {
                        state.engine.switchState(
                                GameStateLoadSocketSP(saves[name],
                                        state.engine, scene))
                    } else {
                        state.engine.switchState(GameStateLoadSP(saves[name],
                                state.engine, scene))
                    }
                } catch (e: IOException) {
                    logger.warn { "Failed to open save: $e" }
                }
            }
            label.on(GuiEvent.HOVER_ENTER) { event ->
                try {
                    saves[name].use { source -> scene.changeBackground(source) }
                } catch (e: IOException) {
                }
            }
            delete.on(GuiEvent.CLICK_LEFT) { event ->
                try {
                    saves.delete(name)
                    scrollPane.remove(this)
                } catch (e: IOException) {
                    logger.warn { "Failed to delete save: $e" }
                }
            }
            try {
                saves[name].use { source ->
                    val panorama = source.panorama()
                    if (panorama != null) {
                        val image = panorama.elements[0]
                        val texture = state.engine.graphics.createTexture(image,
                                4,
                                TextureFilter.LINEAR,
                                TextureFilter.LINEAR, TextureWrap.CLAMP,
                                TextureWrap.CLAMP)
                        icon.texture = Resource(texture)
                    }
                }
            } catch (e: IOException) {
                logger.warn(e) { "Failed to load save icon" }
            }
        }
    }

    companion object : KLogging() {
        private val NO_WORLD_TYPE = "No plugin found that that can\n" + "be used to create a save."
    }
}
