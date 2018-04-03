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
import org.tobi29.io.IOException
import org.tobi29.io.use
import org.tobi29.logging.KLogging
import org.tobi29.math.threadLocalRandom
import org.tobi29.scapes.client.ScapesClient
import org.tobi29.scapes.engine.GameState
import org.tobi29.scapes.engine.graphics.TextureFilter
import org.tobi29.scapes.engine.gui.*
import org.tobi29.scapes.plugins.spi.PluginDescription
import org.tobi29.scapes.plugins.spi.PluginReference
import org.tobi29.scapes.plugins.spi.refer
import org.tobi29.stdex.longHashCode

class GuiCreateWorld(
    state: GameState,
    previous: GuiSaveSelect,
    worldTypes: List<PluginDescription>,
    plugins: List<PluginDescription>,
    style: GuiStyle
) : GuiMenuDouble(
    state, "New World",
    previous, style
) {
    val addons: MutableList<PluginDescription> = ArrayList()
    private var environmentID = 0

    init {
        val scapes = engine[ScapesClient.COMPONENT]
        val saves = scapes.saves
        pane.addVert(16.0, 5.0, -1.0, 18.0) { GuiComponentText(it, "Name:") }
        val name = row(pane) {
            it.selectable = true
            GuiComponentTextField(it, 18, "New World")
        }
        pane.addVert(16.0, 5.0, -1.0, 18.0) { GuiComponentText(it, "Seed:") }
        val seed = row(pane) {
            it.selectable = true
            GuiComponentTextField(it, 18, "")
        }
        val environment = row(pane) {
            button(it, "Generator: " + worldTypes[environmentID].name)
        }
        val addonsButton = row(pane) { button(it, "Addons") }

        environment.on(GuiEvent.CLICK_LEFT) { event ->
            environmentID++
            if (environmentID >= worldTypes.size) {
                environmentID = 0
            }
            addons.clear()
            environment.setText(
                "Generator: " + worldTypes[environmentID].name
            )
        }
        addonsButton.on(GuiEvent.CLICK_LEFT) { event ->
            state.engine.guiStack.swap(
                this, GuiAddons(
                    state, this,
                    worldTypes[environmentID].id, plugins, style
                )
            )
        }
        save.on(GuiEvent.CLICK_LEFT) { event ->
            if (name.text.isEmpty()) {
                name.text = "New World"
            }
            try {
                val saveName = name.text
                if (saves.exists(saveName)) {
                    state.engine.guiStack.swap(
                        this,
                        GuiMessage(
                            state, this, "Error", SAVE_EXISTS,
                            style
                        )
                    )
                    return@on
                }
                val randomSeed = if (seed.text.isEmpty()) {
                    threadLocalRandom().nextLong()
                } else {
                    try {
                        seed.text.toLong()
                    } catch (e: NumberFormatException) {
                        seed.text.longHashCode()
                    }
                }
                val pluginFiles = ArrayList<PluginReference>()
                val worldType = worldTypes[environmentID]
                worldType.let { pluginFiles.add(it.refer()) }
                addons.forEach { pluginFiles.add(it.refer()) }
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

    private inner class GuiAddons(
        state: GameState,
        previous: GuiCreateWorld,
        parent: String,
        plugins: List<PluginDescription>,
        style: GuiStyle
    ) : GuiMenuSingle(
        state, "Addons", "Apply", previous, style
    ) {
        init {
            val scrollPane = pane.addVert(
                16.0, 5.0, -1.0, 350.0
            ) { GuiComponentScrollPane(it, 70) }.viewport
            plugins.asSequence().filter { it.parent == parent }
                .forEach { plugin ->
                    scrollPane.addVert(0.0, 0.0, -1.0, 70.0) {
                        Element(
                            it,
                            plugin
                        )
                    }
                }
        }

        private inner class Element(
            parent: GuiLayoutData,
            addon: PluginDescription
        ) : GuiComponentGroupSlab(parent) {
            private var active = false

            init {
                val icon =
                    addHori(15.0, 15.0, 40.0, -1.0) { GuiComponentIcon(it) }
                add(5.0, 20.0, -1.0, 30.0) { button(it, addon.name) }
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

                icon.texture = engine.resources.load {
                    engine.graphics.createTexture(
                        addon.icon.readAsync { decodePng(it) },
                        minFilter = TextureFilter.LINEAR,
                        magFilter = TextureFilter.LINEAR
                    )
                }
            }
        }
    }

    companion object : KLogging() {
        private val SAVE_EXISTS =
            "This save already exists!\n" + "Please choose a different name."
    }
}
