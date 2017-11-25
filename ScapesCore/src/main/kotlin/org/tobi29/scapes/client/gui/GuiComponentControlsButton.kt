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

import org.tobi29.scapes.engine.gui.GuiComponentButtonHeavy
import org.tobi29.scapes.engine.gui.GuiComponentText
import org.tobi29.scapes.engine.gui.GuiEvent
import org.tobi29.scapes.engine.gui.GuiLayoutData
import org.tobi29.scapes.engine.input.ControllerButtons
import org.tobi29.scapes.engine.input.ControllerKey
import org.tobi29.scapes.engine.input.ControllerKeyReference
import org.tobi29.scapes.engine.utils.ListenerRegistrar
import org.tobi29.scapes.engine.utils.listenAlive
import org.tobi29.scapes.engine.utils.tag.MutableTagMap
import org.tobi29.scapes.engine.utils.tag.toTag
import kotlin.collections.set

class GuiComponentControlsButton(
        parent: GuiLayoutData,
        textSize: Int,
        private val name: String,
        private val id: String,
        private val tagMap: MutableTagMap,
        private val controller: ControllerButtons
) : GuiComponentButtonHeavy(parent) {
    private val text: GuiComponentText
    private val keys = ArrayList<ControllerKey>()
    private var editing: Byte = 0
    private var key: ControllerKeyReference?

    init {
        text = addSubHori(4.0, 0.0, -1.0,
                textSize.toDouble()) { GuiComponentText(it, "") }
        key = ControllerKeyReference.valueOf(tagMap[id].toString())
        on(GuiEvent.CLICK_LEFT) { event ->
            if (editing.toInt() == 0) {
                editing = 1
                updateText()
            }
        }
        on(GuiEvent.HOVER_LEAVE) { event ->
            if (editing > 1) {
                editing = 0
                updateText()
            }
        }
        updateText()
    }

    override fun ListenerRegistrar.listeners() {
        listenAlive<ControllerButtons.PressEvent>(
                {
                    it.state.controller == controller
                            && it.action != ControllerButtons.Action.REPEAT
                }) { event ->
            synchronized(keys) {
                when (event.action) {
                    ControllerButtons.Action.PRESS -> {
                        if (editing <= 1) return@listenAlive
                        keys.add(event.key)
                        event.muted = true
                    }
                    ControllerButtons.Action.RELEASE -> {
                        if (!keys.isEmpty()) {
                            key = ControllerKeyReference(keys)
                            tagMap[id] = key.toString().toTag()
                            editing = 0
                            keys.clear()
                        }
                    }
                }
            }
            updateText()
        }
    }

    private fun updateText() {
        val text = StringBuilder(16)
        if (editing > 0) {
            text.append('<')
            text.append(name)
            text.append(": ")
            synchronized(keys) {
                if (!keys.isEmpty()) {
                    text.append(ControllerKeyReference(keys).humanName())
                } else {
                    text.append("...")
                }
            }
            text.append('>')
        } else {
            text.append(name)
            text.append(": ")
            text.append(key?.humanName() ?: "Not set")
        }
        this.text.text = text.toString()
    }

    override fun updateComponent(delta: Double) {
        if (editing > 0) {
            editing = 2
        }
    }
}
