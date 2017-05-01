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

import org.tobi29.scapes.client.ScapesClient
import org.tobi29.scapes.engine.GameState
import org.tobi29.scapes.engine.gui.*
import org.tobi29.scapes.engine.utils.IOException
import org.tobi29.scapes.engine.utils.io.filesystem.*
import org.tobi29.scapes.engine.utils.io.process
import org.tobi29.scapes.engine.utils.io.put
import org.tobi29.scapes.engine.utils.logging.KLogging

class GuiPlaylists(state: GameState,
                   previous: Gui,
                   style: GuiStyle) : GuiMenuDouble(
        state, "Playlists", "Add", "Back", previous, style) {
    private val scapes = engine.game as ScapesClient
    private val scrollPane: GuiComponentScrollPaneViewport
    private var playlist = ""

    init {
        val slab = row(pane)
        val day = slab.addHori(5.0, 5.0, -1.0, -1.0) { button(it, "Day") }
        val night = slab.addHori(5.0, 5.0, -1.0, -1.0) { button(it, "Night") }
        val battle = slab.addHori(5.0, 5.0, -1.0, -1.0) { button(it, "Battle") }
        selection(day, night, battle)
        scrollPane = pane.addVert(16.0, 5.0, -1.0, -1.0) {
            GuiComponentScrollPane(it, 70)
        }.viewport
        updateTitles("day")

        day.on(GuiEvent.CLICK_LEFT) { updateTitles("day") }
        night.on(GuiEvent.CLICK_LEFT) { updateTitles("night") }
        battle.on(GuiEvent.CLICK_LEFT) { updateTitles("battle") }
        save.on(GuiEvent.CLICK_LEFT) {
            try {
                val directory = scapes.home.resolve(
                        "playlists").resolve(playlist)
                scapes.dialogs.openMusicDialog { name, stream ->
                    write(directory.resolve(name)) { process(stream, put(it)) }
                    updateTitles(playlist)
                }
            } catch (e: IOException) {
                logger.warn { "Failed to import music: $e" }
            }
        }
    }

    @Synchronized
    private fun updateTitles(playlist: String) {
        scrollPane.removeAll()
        this.playlist = playlist
        try {
            val path = scapes.home.resolve("playlists").resolve(playlist)
            listRecursive(path) {
                filter {
                    isRegularFile(it) && isNotHidden(it)
                }.sorted().forEach { file ->
                    scrollPane.addVert(0.0, 0.0, -1.0, 20.0) {
                        Element(it, file)
                    }
                }
            }
        } catch (e: IOException) {
            logger.warn { "Failed to load playlist: $e" }
        }
    }

    private inner class Element(parent: GuiLayoutData,
                                path: FilePath) : GuiComponentGroupSlab(
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
            val play = addHori(2.0, 2.0, -1.0, 15.0) {
                button(it, 12, name)
            }
            val delete = addHori(2.0, 2.0, 60.0, 15.0) {
                button(it, 12, "Delete")
            }
            selection(play, delete)

            play.on(GuiEvent.CLICK_LEFT) {
                state.engine.notifications.add {
                    GuiNotificationSimple(it,
                            state.engine.graphics.textures()["Scapes:image/gui/Playlist"],
                            name)
                }
                state.engine.sounds.stop("music")
                state.engine.sounds.playMusic(read(path), "music.Playlist",
                        true, 1.0, 1.0)
            }
            delete.on(GuiEvent.CLICK_LEFT) {
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
