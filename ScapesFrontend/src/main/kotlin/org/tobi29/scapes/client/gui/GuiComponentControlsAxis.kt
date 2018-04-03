/*
 * Copyright 2012-2018 Tobi29
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

import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import org.tobi29.coroutines.Timer
import org.tobi29.coroutines.loop
import org.tobi29.io.tag.MutableTagMap
import org.tobi29.io.tag.toInt
import org.tobi29.io.tag.toTag
import org.tobi29.scapes.engine.gui.GuiComponentButtonHeavy
import org.tobi29.scapes.engine.gui.GuiComponentText
import org.tobi29.scapes.engine.gui.GuiEvent
import org.tobi29.scapes.engine.gui.GuiLayoutData
import org.tobi29.scapes.engine.input.ControllerJoystick
import org.tobi29.stdex.ConcurrentHashSet
import org.tobi29.stdex.atomic.AtomicBoolean
import org.tobi29.stdex.atomic.AtomicLong
import org.tobi29.stdex.toIntClamped
import kotlin.collections.set

class GuiComponentControlsAxis(parent: GuiLayoutData,
                               textSize: Int,
                               private val name: String,
                               private val id: String,
                               private val tagMap: MutableTagMap,
                               private val controller: ControllerJoystick) : GuiComponentButtonHeavy(
        parent) {
    private val text: GuiComponentText
    private val blacklist = ConcurrentHashSet<Int>()
    private val editing = AtomicBoolean(false)
    private val editingStamp = AtomicLong(Long.MIN_VALUE)
    private var axis = 0

    init {
        text = addSubHori(4.0, 0.0, -1.0,
                textSize.toDouble()) { GuiComponentText(it, "") }
        tagMap[id]?.toInt()?.let { axis = it }
        on(GuiEvent.CLICK_LEFT) {
            if (!editing.getAndSet(true)) {
                blacklist.clear()
                val blacklist = ArrayList<Int>()
                for ((i, value) in controller.axes.withIndex()) {
                    if (value > 0.5) blacklist.add(i)
                }
                val editStamp = editingStamp.incrementAndGet()
                launch(engine.taskExecutor) {
                    Timer().apply { init() }.loop(Timer.toDiff(60.0),
                            { delay((it / 1000000L).toIntClamped()) }) {
                        for ((i, value) in controller.axes.withIndex()) {
                            val blacklisted = blacklist.contains(i)
                            if (value > 0.5) {
                                if (!blacklisted) {
                                    axis = i
                                    tagMap[id] = axis.toTag()
                                    editing.set(false)
                                    updateText()
                                    break
                                }
                            } else if (blacklisted) {
                                blacklist.remove(i)
                            }
                        }
                        isVisible && editing.get() && editStamp == editingStamp.get()
                    }
                }
                updateText()
            }
        }
        on(GuiEvent.HOVER_LEAVE) { if (editing.getAndSet(false)) updateText() }
        updateText()
    }

    private fun updateText() {
        val text = StringBuilder(16)
        val editing = editing.get()
        if (editing) {
            text.append('<')
        }
        text.append(name)
        text.append(": Axis ")
        text.append(axis)
        if (editing) {
            text.append('>')
        }
        this.text.text = text.toString()
    }
}
