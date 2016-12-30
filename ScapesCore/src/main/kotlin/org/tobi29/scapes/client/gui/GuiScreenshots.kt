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

package org.tobi29.scapes.client.gui

import mu.KLogging
import org.tobi29.scapes.engine.GameState
import org.tobi29.scapes.engine.gui.*
import org.tobi29.scapes.engine.resource.Resource
import org.tobi29.scapes.engine.utils.graphics.decodePNG
import org.tobi29.scapes.engine.utils.io.filesystem.*
import java.io.IOException

class GuiScreenshots(state: GameState, previous: Gui, style: GuiStyle) : GuiMenu(
        state, "Screenshots", previous, style) {
    private val scrollPane: GuiComponentScrollPaneViewport

    init {
        scrollPane = pane.addVert(16.0, 5.0, -1.0, -1.0) {
            GuiComponentScrollPane(it, 70)
        }.viewport
        try {
            val path = state.engine.home.resolve("screenshots")
            val files = listRecursive(path,
                    { isRegularFile(it) && isNotHidden(it) })
            files.asSequence().sorted().forEach { file ->
                val element = scrollPane.addVert(0.0, 0.0, -1.0,
                        70.0) { Element(it, file, this) }
                state.engine.taskExecutor.runTask({
                    try {
                        val texture = read(file) {
                            val image = decodePNG(it) {
                                state.engine.allocate(it)
                            }
                            state.engine.graphics.createTexture(image, 0)
                        }
                        element.icon.texture = Resource(texture)
                    } catch (e: IOException) {
                        logger.warn { "Failed to load screenshot: $e" }
                    }
                }, "Load-Screenshot")
            }
        } catch (e: IOException) {
            logger.warn { "Failed to read screenshots: $e" }
        }
    }

    private inner class Element(parent: GuiLayoutData, path: FilePath,
                                gui: GuiScreenshots) : GuiComponentGroupSlabHeavy(
            parent) {
        val icon: GuiComponentIcon

        init {
            icon = addHori(15.0, 20.0, 40.0,
                    -1.0) { parent: GuiLayoutData -> GuiComponentIcon(parent) }
            val save = addHori(5.0, 20.0, -1.0, -1.0) { button(it, "Save") }
            val delete = addHori(5.0, 20.0, 100.0, -1.0) {
                button(it, "Delete")
            }
            selection(save, delete)

            icon.on(GuiEvent.CLICK_LEFT) { event ->
                icon.texture?.let { texture ->
                    state.engine.guiStack.add("10-Menu",
                            GuiScreenshot(state, gui, texture,
                                    gui.style))
                }
            }
            save.on(GuiEvent.CLICK_LEFT) { event ->
                try {
                    val export = state.engine.container.saveFileDialog(
                            arrayOf(Pair("*.png", "PNG Picture")),
                            "Export screenshot")
                    if (export != null) {
                        copy(path, export)
                    }
                } catch (e: IOException) {
                    logger.warn { "Failed to export screenshot: $e" }
                }
            }
            delete.on(GuiEvent.CLICK_LEFT) { event ->
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