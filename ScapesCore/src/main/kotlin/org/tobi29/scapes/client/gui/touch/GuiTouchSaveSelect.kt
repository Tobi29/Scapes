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

import java8.util.stream.Collectors
import mu.KLogging
import org.tobi29.scapes.Debug
import org.tobi29.scapes.client.SaveStorage
import org.tobi29.scapes.client.ScapesClient
import org.tobi29.scapes.client.gui.desktop.GuiMessage
import org.tobi29.scapes.client.states.GameStateLoadSP
import org.tobi29.scapes.client.states.GameStateLoadSocketSP
import org.tobi29.scapes.client.states.scenes.SceneMenu
import org.tobi29.scapes.engine.GameState
import org.tobi29.scapes.engine.graphics.TextureFilter
import org.tobi29.scapes.engine.graphics.TextureWrap
import org.tobi29.scapes.engine.gui.*
import org.tobi29.scapes.engine.utils.io.use
import org.tobi29.scapes.engine.utils.stream
import org.tobi29.scapes.plugins.PluginFile
import org.tobi29.scapes.plugins.Plugins
import java.io.IOException

class GuiTouchSaveSelect(state: GameState, previous: Gui, private val scene: SceneMenu,
                         style: GuiStyle) : GuiTouchMenuDouble(state,
        "Singleplayer", "Create", "Back", previous, style) {
    private val saves: SaveStorage
    private val scrollPane: GuiComponentScrollPaneViewport

    init {
        val game = state.engine.game as ScapesClient
        saves = game.saves()
        scrollPane = pane.addVert(112.0, 10.0, 736.0, 320.0
        ) { GuiComponentScrollPane(it, 70) }.viewport()

        save.on(GuiEvent.CLICK_LEFT) { event ->
            try {
                val path = state.engine.home.resolve("plugins")
                val plugins = Plugins.installed(path)
                val worldTypes = plugins.stream().filter { plugin -> "WorldType" == plugin.parent() }.collect(
                        Collectors.toList<PluginFile>())
                if (worldTypes.isEmpty()) {
                    state.engine.guiStack.swap(this,
                            GuiMessage(state, this, "Error", NO_WORLD_TYPE,
                                    style))
                } else {
                    state.engine.guiStack.swap(this,
                            GuiTouchCreateWorld(state, this,
                                    worldTypes[0], style))
                }
            } catch (e: IOException) {
                logger.warn { "Failed to read plugins: $e" }
            }
        }
        updateSaves()
    }

    fun updateSaves() {
        try {
            scrollPane.removeAll()
            saves.list().sorted().forEach { file ->
                scrollPane.addVert(0.0, 0.0, -1.0, 80.0) {
                    Element(it, file)
                }
            }
        } catch (e: IOException) {
            logger.warn { "Failed to read saves:$e" }
        }
    }

    private inner class Element(parent: GuiLayoutData, name: String) : GuiComponentGroupSlab(
            parent) {
        init {
            val icon = addHori(10.0, 10.0, 60.0,
                    -1.0) { parent: GuiLayoutData -> GuiComponentIcon(parent) }
            val label = addHori(10.0, 10.0, -1.0, -1.0) { button(it, name) }
            val delete = addHori(10.0, 10.0, 160.0, -1.0) {
                button(it, "Delete")
            }

            selection(label, delete)

            label.on(GuiEvent.CLICK_LEFT) { event ->
                scene.setSpeed(0.0f)
                try {
                    if (Debug.socketSingleplayer()) {
                        state.engine.switchState(
                                GameStateLoadSocketSP(saves[name],
                                        state.engine, state.scene()))
                    } else {
                        state.engine.switchState(GameStateLoadSP(saves[name],
                                state.engine, state.scene()))
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
                        icon.texture = texture
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