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
import org.tobi29.graphics.encodePng
import org.tobi29.io.IOException
import org.tobi29.io.filesystem.write
import org.tobi29.logging.KLogging
import org.tobi29.scapes.client.DialogProvider
import org.tobi29.scapes.client.Screenshots
import org.tobi29.scapes.engine.GameState
import org.tobi29.scapes.engine.graphics.TextureFilter
import org.tobi29.scapes.engine.gui.*

class GuiScreenshots(
    state: GameState, previous: Gui, style: GuiStyle
) : GuiMenuSingle(state, "Screenshots", previous, style) {
    private val scrollPane = pane.addVert(16.0, 5.0, -1.0, -1.0) {
        GuiComponentScrollPane(it, 70)
    }.viewport

    init {
        rebuild()
    }

    private fun rebuild() {
        engine[Screenshots.COMPONENT].screenshots().forEach { screenshot ->
            scrollPane.addVert(0.0, 0.0, -1.0, 70.0) {
                Element(it, screenshot, this)
            }
        }
    }

    private inner class Element(
        parent: GuiLayoutData,
        screenshot: Screenshots.Screenshot,
        gui: GuiScreenshots
    ) : GuiComponentGroupSlabHeavy(parent) {
        init {
            val resource = engine.resources.load {
                engine.graphics.createTexture(
                    screenshot.image.await(),
                    minFilter = TextureFilter.LINEAR,
                    magFilter = TextureFilter.LINEAR
                )
            }
            val icon = addHori(15.0, 20.0, 40.0, -1.0) {
                GuiComponentIcon(it, resource)
            }
            val save = addHori(5.0, 20.0, -1.0, -1.0) {
                button(it, "Save")
            }
            val delete = addHori(5.0, 20.0, 100.0, -1.0) {
                button(it, "Delete")
            }

            icon.on(GuiEvent.CLICK_LEFT) {
                icon.texture?.let { texture ->
                    state.engine.guiStack.add(
                        "10-Menu",
                        GuiScreenshot(state, gui, texture, gui.style)
                    )
                }
            }
            save.on(GuiEvent.CLICK_LEFT) {
                try {
                    state.engine[DialogProvider.COMPONENT].saveScreenshotDialog {
                        launch(engine.taskExecutor) {
                            val bitmap = screenshot.image.await()
                            write(it) { encodePng(bitmap, it, 9, false) }
                        }
                    }
                } catch (e: IOException) {
                    logger.warn(e) { "Failed to export screenshot" }
                }
            }
            delete.on(GuiEvent.CLICK_LEFT) {
                if (screenshot.delete()) scrollPane.remove(this)
            }
        }
    }

    companion object : KLogging()
}
