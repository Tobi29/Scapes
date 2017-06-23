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
import org.tobi29.scapes.engine.graphics.TextureFilter
import org.tobi29.scapes.engine.graphics.TextureWrap
import org.tobi29.scapes.engine.gui.*
import org.tobi29.scapes.engine.resource.Resource
import org.tobi29.scapes.engine.utils.graphics.decodePNG
import org.tobi29.scapes.engine.utils.hash
import org.tobi29.scapes.engine.utils.io.BufferedReadChannelStream
import org.tobi29.scapes.engine.utils.io.Channels
import org.tobi29.scapes.engine.utils.io.IOException
import org.tobi29.scapes.engine.utils.io.filesystem.FilePath
import org.tobi29.scapes.engine.utils.logging.KLogging
import org.tobi29.scapes.engine.utils.math.threadLocalRandom
import org.tobi29.scapes.engine.utils.use
import org.tobi29.scapes.plugins.PluginFile
import java.util.zip.ZipFile

class GuiCreateWorld(state: GameState,
                     previous: GuiSaveSelect,
                     worldTypes: List<PluginFile>,
                     plugins: List<PluginFile>,
                     style: GuiStyle) : GuiMenuDouble(state, "New World",
        previous, style) {
    val addons: MutableList<PluginFile> = ArrayList()
    private var environmentID = 0

    init {
        val game = state.engine.game as ScapesClient
        val saves = game.saves
        pane.addVert(16.0, 5.0, -1.0, 18.0) {
            GuiComponentText(it, "Name:")
        }
        val name = row(pane) { GuiComponentTextField(it, 18, "New World") }
        pane.addVert(16.0, 5.0, -1.0, 18.0) {
            GuiComponentText(it, "Seed:")
        }
        val seed = row(pane) { GuiComponentTextField(it, 18, "") }
        val environment = row(pane) {
            button(it,
                    "Generator: " + worldTypes[environmentID].name())
        }
        val addonsButton = row(pane) { button(it, "Addons") }

        selection(name)
        selection(seed)
        selection(environment)
        selection(addonsButton)

        environment.on(GuiEvent.CLICK_LEFT) { event ->
            environmentID++
            if (environmentID >= worldTypes.size) {
                environmentID = 0
            }
            addons.clear()
            environment.setText(
                    "Generator: " + worldTypes[environmentID].name())
        }
        addonsButton.on(GuiEvent.CLICK_LEFT) { event ->
            state.engine.guiStack.swap(this, GuiAddons(state, this,
                    worldTypes[environmentID].id(), plugins, style))
        }
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
                    threadLocalRandom().nextLong()
                } else {
                    try {
                        seed.text().toLong()
                    } catch (e: NumberFormatException) {
                        hash(seed.text())
                    }
                }
                val pluginFiles = ArrayList<FilePath>()
                val worldType = worldTypes[environmentID]
                worldType.file()?.let { pluginFiles.add(it) }
                addons.asSequence().map { it.file() }.filterNotNull().forEach {
                    pluginFiles.add(it)
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

    private inner class GuiAddons(state: GameState,
                                  previous: GuiCreateWorld,
                                  parent: String,
                                  plugins: List<PluginFile>,
                                  style: GuiStyle) : GuiMenuSingle(
            state, "Addons", "Apply", previous, style) {
        init {
            val scrollPane = pane.addVert(16.0, 5.0, -1.0, 350.0
            ) { GuiComponentScrollPane(it, 70) }.viewport
            plugins.asSequence().filter { it.parent() == parent }.forEach { plugin ->
                scrollPane.addVert(0.0, 0.0, -1.0, 70.0) { Element(it, plugin) }
            }
        }

        private inner class Element(parent: GuiLayoutData,
                                    addon: PluginFile) : GuiComponentGroupSlab(
                parent) {
            private var active = false

            init {
                val icon = addHori(15.0, 15.0, 40.0,
                        -1.0) { parent: GuiLayoutData ->
                    GuiComponentIcon(parent)
                }
                add(5.0, 20.0, -1.0, 30.0) { button(it, addon.name()) }
                val edit = add(5.0, 20.0, 30.0, 30.0) {
                    button(it, if (active) "X" else "")
                }

                active = addons.contains(addon)
                edit.on(GuiEvent.CLICK_LEFT) { event ->
                    active = !active
                    if (active) {
                        edit.setText("X")
                        addons.add(addon)
                    } else {
                        edit.setText("")
                        addons.remove(addon)
                    }
                }
                try {
                    addon.file()?.let {
                        ZipFile(it.toFile()).use { zip ->
                            val stream = BufferedReadChannelStream(
                                    Channels.newChannel(zip.getInputStream(
                                            zip.getEntry(
                                                    "scapes/plugin/Icon.png"))))
                            val image = decodePNG(stream) {
                                state.engine.allocate(it)
                            }
                            val texture = state.engine.graphics.createTexture(
                                    image, 0,
                                    TextureFilter.LINEAR,
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

    companion object : KLogging() {
        private val SAVE_EXISTS = "This save already exists!\n" + "Please choose a different name."
    }
}
