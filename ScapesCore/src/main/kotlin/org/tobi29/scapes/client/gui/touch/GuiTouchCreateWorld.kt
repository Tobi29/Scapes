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
import org.tobi29.scapes.client.gui.desktop.GuiMessage
import org.tobi29.scapes.engine.GameState
import org.tobi29.scapes.engine.gui.GuiComponentText
import org.tobi29.scapes.engine.gui.GuiComponentTextField
import org.tobi29.scapes.engine.gui.GuiEvent
import org.tobi29.scapes.engine.gui.GuiStyle
import org.tobi29.scapes.engine.utils.hash
import org.tobi29.scapes.engine.utils.io.filesystem.FilePath
import org.tobi29.scapes.engine.utils.io.use
import org.tobi29.scapes.plugins.PluginFile
import java.io.IOException
import java.util.*

class GuiTouchCreateWorld(state: GameState, previous: GuiTouchSaveSelect,
                          worldType: PluginFile, style: GuiStyle) : GuiTouchMenuDouble(
        state, "New World", previous, style) {

    init {
        val game = state.engine.game as ScapesClient
        val saves = game.saves()
        pane.addVert(112.0, 10.0, -1.0, 36.0) {
            GuiComponentText(it, "Name:")
        }
        val name = row(pane) { GuiComponentTextField(it, 36, "New World") }
        pane.addVert(112.0, 10.0, -1.0, 36.0) {
            GuiComponentText(it, "Seed:")
        }
        val seed = row(pane) { GuiComponentTextField(it, 36, "") }

        selection(name)
        selection(seed)

        save.on(GuiEvent.CLICK_LEFT) { event ->
            if (name.text().isEmpty()) {
                name.setText("New World")
            }
            try {
                val saveName = name.text()
                if (saves.exists(saveName)) {
                    state.engine.guiStack.swap(this,
                            GuiMessage(state, this, "Error", SAVE_EXISTS,
                                    style))
                    return@on
                }
                val randomSeed = if (seed.text().isEmpty()) {
                    Random().nextLong()
                } else {
                    try {
                        seed.text().toLong()
                    } catch (e: NumberFormatException) {
                        hash(seed.text())
                    }
                }
                val worldTypeFile = worldType.file()
                val pluginFiles: List<FilePath>
                if (worldTypeFile == null) {
                    pluginFiles = Collections.emptyList()
                } else {
                    pluginFiles = listOf(worldTypeFile)
                }
                saves[saveName].use { source ->
                    source.init(randomSeed, pluginFiles)
                }
                previous.updateSaves()
                state.engine.guiStack.swap(this, previous)
            } catch (e: IOException) {
                logger.error { "Failed to create world: $e" }
            }
        }
    }

    companion object : KLogging() {
        private val SAVE_EXISTS = "This save already exists!\n" +
                "Please choose a different name."
    }
}
