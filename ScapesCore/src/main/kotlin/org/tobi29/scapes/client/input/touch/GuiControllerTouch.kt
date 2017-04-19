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
package org.tobi29.scapes.client.input.touch

import org.tobi29.scapes.engine.ScapesEngine
import org.tobi29.scapes.engine.gui.*
import org.tobi29.scapes.engine.input.ControllerBasic
import org.tobi29.scapes.engine.input.ControllerTouch
import org.tobi29.scapes.engine.utils.ConcurrentHashMap
import org.tobi29.scapes.engine.utils.math.vector.MutableVector2d

class GuiControllerTouch(engine: ScapesEngine,
                         private val controller: ControllerTouch) : GuiController(
        engine) {
    private val fingers = ConcurrentHashMap<ControllerTouch.Tracker, Finger>()
    private var clicks: List<Pair<GuiCursor, ControllerBasic.PressEvent>> = emptyList()

    override fun update(delta: Double) {
        val newClicks = ArrayList<Pair<GuiCursor, ControllerBasic.PressEvent>>()
        val newFingers = ConcurrentHashMap<ControllerTouch.Tracker, Finger>()
        controller.fingers().forEach { tracker ->
            var fetch: Finger? = fingers[tracker]
            if (fetch == null) {
                fetch = Finger(tracker.pos)
                val finger = fetch
                handleFinger(finger)
                val guiPos = finger.cursor.currentPos()
                finger.dragX = guiPos.x
                finger.dragY = guiPos.y
                engine.guiStack.fireEvent(GuiComponentEvent(guiPos.x, guiPos.y),
                        { _, _ -> true })?.let { component ->
                    if (!fingers.values.any { it.dragging == component }) {
                        finger.dragging = component
                        component.gui.sendNewEvent(GuiEvent.PRESS_LEFT,
                                GuiComponentEvent(guiPos.x, guiPos.y),
                                component)
                    }
                }
                fingers.put(tracker, finger)
            } else {
                handleFinger(fetch)
            }
            newFingers.put(tracker, fetch)
        }
        fingers.keys.asSequence().filter {
            !newFingers.containsKey(it)
        }.forEach { tracker ->
            fingers.remove(tracker)?.let { finger ->
                finger.dragging?.let { component ->
                    val guiPos = finger.cursor.currentPos()
                    if (!finger.clicked) {
                        finger.clicked = true
                        component.gui.sendNewEvent(GuiEvent.CLICK_LEFT,
                                GuiComponentEvent(guiPos.x, guiPos.y),
                                component)
                    }
                    component.gui.sendNewEvent(GuiEvent.DROP_LEFT,
                            GuiComponentEvent(guiPos.x, guiPos.y), component)
                }
            }
        }
        clicks = newClicks
    }

    override fun focusTextField(data: GuiController.TextFieldData,
                                multiline: Boolean) {
        engine.container.dialog("Input", data, multiline)
    }

    override fun processTextField(data: GuiController.TextFieldData,
                                  multiline: Boolean): Boolean {
        return true
    }

    override fun cursors(): Sequence<GuiCursor> {
        return fingers.values.asSequence().map { it.cursor }
    }

    override fun clicks(): Sequence<Pair<GuiCursor, ControllerBasic.PressEvent>> {
        return clicks.asSequence()
    }

    override fun captureCursor(): Boolean {
        return false
    }

    private fun handleFinger(finger: Finger) {
        finger.cursor.set(finger.tracker.now())
        finger.dragging?.let { component ->
            val guiPos = finger.cursor.currentPos()
            val relativeX = guiPos.x - finger.dragX
            val relativeY = guiPos.y - finger.dragY
            finger.dragX = guiPos.x
            finger.dragY = guiPos.y
            component.gui.sendNewEvent(GuiEvent.DRAG_LEFT,
                    GuiComponentEvent(guiPos.x, guiPos.y, relativeX, relativeY),
                    component)
            val source = finger.source
            engine.guiStack.fireRecursiveEvent(GuiEvent.SCROLL,
                    GuiComponentEvent(source.x, source.y, relativeX, relativeY))
            if (System.nanoTime() - finger.start >= 250000000L && !finger.clicked) {
                finger.clicked = true
                finger.dragging?.let { component ->
                    component.gui.sendNewEvent(GuiEvent.CLICK_RIGHT,
                            GuiComponentEvent(guiPos.x, guiPos.y), component)
                }
            }
        }
    }

    private class Finger(val tracker: MutableVector2d) {
        var source = tracker.now()
        var start = System.nanoTime()
        var cursor = GuiCursor()
        var dragging: GuiComponent? = null
        var dragX: Double = 0.0
        var dragY: Double = 0.0
        var clicked = false
    }
}
