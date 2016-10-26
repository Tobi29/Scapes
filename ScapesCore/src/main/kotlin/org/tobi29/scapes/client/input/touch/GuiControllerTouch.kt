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
package org.tobi29.scapes.client.input.touch

import java8.util.stream.Stream
import org.tobi29.scapes.engine.ScapesEngine
import org.tobi29.scapes.engine.gui.*
import org.tobi29.scapes.engine.input.ControllerBasic
import org.tobi29.scapes.engine.input.ControllerTouch
import org.tobi29.scapes.engine.utils.math.vector.MutableVector2d
import org.tobi29.scapes.engine.utils.math.vector.Vector2d
import org.tobi29.scapes.engine.utils.math.vector.times
import org.tobi29.scapes.engine.utils.stream
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class GuiControllerTouch(engine: ScapesEngine, private val controller: ControllerTouch) : GuiController(
        engine) {
    private val fingers = ConcurrentHashMap<ControllerTouch.Tracker, Finger>()
    private var clicks: List<Pair<GuiCursor, ControllerBasic.PressEvent>> = emptyList()

    override fun update(delta: Double) {
        val ratio = 540.0 / engine.container.containerHeight()
        val newClicks = ArrayList<Pair<GuiCursor, ControllerBasic.PressEvent>>()
        val newFingers = ConcurrentHashMap<ControllerTouch.Tracker, Finger>()
        controller.fingers().forEach { tracker ->
            var fetch: Finger? = fingers[tracker]
            if (fetch == null) {
                fetch = Finger(tracker.pos)
                val finger = fetch
                handleFinger(finger, ratio)
                val guiPos = finger.cursor.currentGuiPos()
                finger.dragX = guiPos.x
                finger.dragY = guiPos.y
                engine.guiStack.fireEvent(
                        GuiComponentEvent(guiPos.x, guiPos.y),
                        { component, event -> true })?.let { component ->
                    if (!fingers.values.stream().filter { it.dragging == component }.findAny().isPresent) {
                        finger.dragging = component
                        component.gui.sendNewEvent(GuiEvent.PRESS_LEFT,
                                GuiComponentEvent(
                                        guiPos.x,
                                        guiPos.y),
                                component)
                    }
                }
                fingers.put(tracker, finger)
            } else {
                handleFinger(fetch, ratio)
            }
            newFingers.put(tracker, fetch)
        }
        fingers.keys.stream().filter { tracker ->
            !newFingers.containsKey(tracker)
        }.forEach { tracker ->
            val finger = fingers.remove(tracker)
            val component = finger?.dragging
            if (finger != null && component != null) {
                val guiPos = finger.cursor.currentGuiPos()
                if (System.currentTimeMillis() - finger.start < 250) {
                    component.gui.sendNewEvent(GuiEvent.CLICK_LEFT,
                            GuiComponentEvent(guiPos.x,
                                    guiPos.y), component)
                }
                component.gui.sendNewEvent(GuiEvent.DROP_LEFT,
                        GuiComponentEvent(guiPos.x,
                                guiPos.y), component)
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

    override fun cursors(): Stream<GuiCursor> {
        return fingers.values.stream().map { it.cursor }
    }

    override fun clicks(): Stream<Pair<GuiCursor, ControllerBasic.PressEvent>> {
        return clicks.stream()
    }

    override fun captureCursor(): Boolean {
        return false
    }

    private fun handleFinger(finger: Finger,
                             ratio: Double) {
        finger.cursor.set(finger.tracker.now(),
                finger.tracker.now().times(ratio))
        val component = finger.dragging
        if (component != null) {
            val guiPos = finger.cursor.currentGuiPos()
            val relativeX = guiPos.x - finger.dragX
            val relativeY = guiPos.y - finger.dragY
            finger.dragX = guiPos.x
            finger.dragY = guiPos.y
            component.gui.sendNewEvent(GuiEvent.DRAG_LEFT,
                    GuiComponentEvent(guiPos.x,
                            guiPos.y, relativeX, relativeY),
                    component)
            val source = finger.source.times(ratio)
            engine.guiStack.fireRecursiveEvent(GuiEvent.SCROLL,
                    GuiComponentEvent(source.x, source.y,
                            relativeX, relativeY))
        }
    }

    private class Finger(val tracker: MutableVector2d) {
        var source: Vector2d
        var start: Long
        var cursor = GuiCursor()
        var dragging: GuiComponent? = null
        var dragX: Double = 0.0
        var dragY: Double = 0.0

        init {
            source = tracker.now()
            start = System.currentTimeMillis()
        }
    }
}
