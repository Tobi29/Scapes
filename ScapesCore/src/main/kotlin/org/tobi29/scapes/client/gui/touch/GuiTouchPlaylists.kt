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
import org.tobi29.scapes.engine.GameState
import org.tobi29.scapes.engine.gui.*
import org.tobi29.scapes.engine.input.FileType
import org.tobi29.scapes.engine.utils.io.filesystem.*
import org.tobi29.scapes.engine.utils.io.process
import org.tobi29.scapes.engine.utils.io.put
import org.tobi29.scapes.engine.utils.stream
import java.io.IOException

class GuiTouchPlaylists(state: GameState, previous: Gui, style: GuiStyle) : GuiTouchMenuDouble(
        state, "Playlists", "Add", "Back", previous, style) {
    private val scrollPane: GuiComponentScrollPaneViewport
    private var playlist: String = ""

    init {
        val slab = row(pane)
        val day = slab.addHori(10.0, 10.0, -1.0, -1.0) { button(it, "Day") }
        val night = slab.addHori(10.0, 10.0, -1.0, -1.0) { button(it, "Night") }
        val battle = slab.addHori(10.0, 10.0, -1.0, -1.0) {
            button(it, "Battle")
        }
        selection(day, night, battle)
        scrollPane = pane.addVert(112.0, 10.0, 736.0, 250.0) {
            GuiComponentScrollPane(it, 60)
        }.viewport
        updateTitles("day")

        day.on(GuiEvent.CLICK_LEFT) { event -> updateTitles("day") }
        night.on(GuiEvent.CLICK_LEFT) { event -> updateTitles("night") }
        battle.on(GuiEvent.CLICK_LEFT) { event -> updateTitles("battle") }
        save.on(GuiEvent.CLICK_LEFT) { event ->
            try {
                val directory = state.engine.home.resolve(
                        "playlists").resolve(playlist)
                state.engine.container.openFileDialog(FileType.MUSIC,
                        "Import music", true) { name, input ->
                    write(directory.resolve(name)) { output ->
                        process(input, put(output))
                    }
                }
                updateTitles(playlist)
            } catch (e: IOException) {
                logger.warn { "Failed to import music: $e" }
            }
        }
    }

    private fun updateTitles(playlist: String) {
        scrollPane.removeAll()
        this.playlist = playlist
        try {
            val path = state.engine.home.resolve("playlists").resolve(playlist)
            val files = listRecursive(path,
                    { isRegularFile(it) && isNotHidden(it) })
            files.stream().sorted().forEach { file ->
                scrollPane.addVert(0.0, 0.0, -1.0, 40.0) {
                    Element(it, file)
                }
            }
        } catch (e: IOException) {
            logger.warn { "Failed to load playlist: $e" }
        }
    }

    private inner class Element(parent: GuiLayoutData, path: FilePath) : GuiComponentGroupSlab(
            parent) {
        init {
            val fileName = path.fileName.toString()
            val index = fileName.lastIndexOf('.')
            val name: String
            if (index == -1) {
                name = fileName
            } else {
                name = fileName.substring(0, index)
            }
            val play = addHori(10.0, 5.0, -1.0, 30.0) {
                button(it, 24, name)
            }
            val delete = addHori(10.0, 5.0, 110.0, 30.0) {
                button(it, 24, "Delete")
            }
            selection(play, delete)

            play.on(GuiEvent.CLICK_LEFT) { event ->
                state.engine.notifications.add() {
                    GuiNotificationSimple(it,
                            state.engine.graphics.textures()["Scapes:image/gui/Playlist"],
                            name)
                }
                state.engine.sounds.stop("music")
                state.engine.sounds.playMusic(read(path), "music.Playlist",
                        1.0f, 1.0f, true)
            }
            delete.on(GuiEvent.CLICK_LEFT) { event ->
                try {
                    delete(path)
                    scrollPane.remove(this)
                } catch (e: IOException) {
                    logger.warn { "Failed to delete music: $e" }
                }
            }
        }
    }

    companion object : KLogging()
}
