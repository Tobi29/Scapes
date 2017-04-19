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
import org.tobi29.scapes.engine.input.ControllerJoystick
import org.tobi29.scapes.engine.utils.tag.MutableTagMap
import org.tobi29.scapes.engine.utils.tag.set
import org.tobi29.scapes.engine.utils.tag.toInt

class GuiComponentControlsAxis(parent: GuiLayoutData,
                               textSize: Int,
                               private val name: String,
                               private val id: String,
                               private val tagMap: MutableTagMap,
                               private val controller: ControllerJoystick) : GuiComponentButtonHeavy(
        parent) {
    private val text: GuiComponentText
    private val blacklist = ArrayList<Int>()
    private var editing: Byte = 0
    private var axis = 0

    init {
        text = addSubHori(4.0, 0.0, -1.0,
                textSize.toDouble()) { GuiComponentText(it, "") }
        tagMap[id]?.toInt()?.let { axis = it }
        on(GuiEvent.CLICK_LEFT) { event ->
            if (editing.toInt() == 0) {
                blacklist.clear()
                val axes = controller.axes()
                for (i in 0..axes - 1) {
                    if (controller.axis(i) > 0.5) {
                        blacklist.add(i)
                    }
                }
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

    private fun updateText() {
        val text = StringBuilder(16)
        if (editing > 0) {
            text.append('<')
        }
        text.append(name)
        text.append(": Axis ")
        text.append(axis)
        if (editing > 0) {
            text.append('>')
        }
        this.text.text = text.toString()
    }

    override fun updateComponent(delta: Double) {
        if (editing > 1) {
            val axes = controller.axes()
            for (i in 0..axes - 1) {
                val blacklisted = blacklist.contains(i)
                if (controller.axis(i) > 0.5) {
                    if (!blacklisted) {
                        axis = i
                        tagMap[id] = axis
                        editing = 0
                        updateText()
                        break
                    }
                } else if (blacklisted) {
                    blacklist.remove(i)
                }
            }
        } else if (editing > 0) {
            editing = 2
        }
    }
}
