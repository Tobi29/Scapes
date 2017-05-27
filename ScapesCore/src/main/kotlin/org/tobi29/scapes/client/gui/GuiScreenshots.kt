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
import org.tobi29.scapes.engine.utils.graphics.decodePNG
import org.tobi29.scapes.engine.utils.io.IOException
import org.tobi29.scapes.engine.utils.io.filesystem.*
import org.tobi29.scapes.engine.utils.logging.KLogging

class GuiScreenshots(state: GameState,
                     previous: Gui,
                     style: GuiStyle) : GuiMenuSingle(
        state, "Screenshots", previous, style) {
    private val scapes = engine.game as ScapesClient
    private val scrollPane: GuiComponentScrollPaneViewport

    init {
        scrollPane = pane.addVert(16.0, 5.0, -1.0, -1.0) {
            GuiComponentScrollPane(it, 70)
        }.viewport
        try {
            val path = scapes.home.resolve("screenshots")
            val files = listRecursive(path,
                    { isRegularFile(it) && isNotHidden(it) })
            files.asSequence().sorted().forEach { file ->
                scrollPane.addVert(0.0, 0.0, -1.0, 70.0) {
                    Element(it, file, this)
                }
            }
        } catch (e: IOException) {
            logger.warn { "Failed to read screenshots: $e" }
        }
    }

    private inner class Element(parent: GuiLayoutData,
                                path: FilePath,
                                gui: GuiScreenshots) : GuiComponentGroupSlabHeavy(
            parent) {
        val icon: GuiComponentIcon

        init {
            val resource = engine.resources.load {
                try {
                    read(path) {
                        val image = decodePNG(it) {
                            state.engine.allocate(it)
                        }
                        state.engine.graphics.createTexture(image, 0)
                    }
                } catch (e: IOException) {
                    logger.warn { "Failed to load screenshot: $e" }
                    engine.graphics.textureEmpty()
                }
            }
            icon = addHori(15.0, 20.0, 40.0, -1.0) {
                GuiComponentIcon(it, resource)
            }
            val save = addHori(5.0, 20.0, -1.0, -1.0) { button(it, "Save") }
            val delete = addHori(5.0, 20.0, 100.0, -1.0) {
                button(it, "Delete")
            }
            selection(save, delete)

            icon.on(GuiEvent.CLICK_LEFT) {
                icon.texture?.let { texture ->
                    state.engine.guiStack.add("10-Menu",
                            GuiScreenshot(state, gui, texture,
                                    gui.style))
                }
            }
            save.on(GuiEvent.CLICK_LEFT) {
                try {
                    scapes.dialogs.saveScreenshotDialog { copy(path, it) }
                } catch (e: IOException) {
                    logger.warn { "Failed to export screenshot: $e" }
                }
            }
            delete.on(GuiEvent.CLICK_LEFT) {
                try {
                    delete(path)
                    scrollPane.remove(this)
                } catch (e: IOException) {
                    logger.warn { "Failed to delete screenshot: $e" }
                }
            }
        }
    }

    companion object : KLogging()
}
