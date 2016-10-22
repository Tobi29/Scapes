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
package org.tobi29.scapes.client.input.gamepad

import java8.util.stream.Stream
import org.tobi29.scapes.engine.ScapesEngine
import org.tobi29.scapes.engine.gui.GuiAction
import org.tobi29.scapes.engine.gui.GuiController
import org.tobi29.scapes.engine.gui.GuiCursor
import org.tobi29.scapes.engine.input.ControllerBasic
import org.tobi29.scapes.engine.input.ControllerJoystick
import org.tobi29.scapes.engine.input.ControllerKey
import org.tobi29.scapes.engine.input.ControllerKeyReference
import org.tobi29.scapes.engine.utils.*
import kotlin.Pair

class GuiControllerGamepad(engine: ScapesEngine,
                           private val controller: ControllerJoystick, private val primaryButton: ControllerKeyReference,
                           private val secondaryButton: ControllerKeyReference,
                           private val upButton: ControllerKeyReference, private val downButton: ControllerKeyReference,
                           private val leftButton: ControllerKeyReference,
                           private val rightButton: ControllerKeyReference) : GuiController(
        engine) {

    override fun update(delta: Double) {
        controller.pressEvents().forEach { event ->
            when (event.state()) {
                ControllerBasic.PressState.PRESS -> handlePress(event.key())
            }
        }
    }

    override fun focusTextField(data: GuiController.TextFieldData,
                                multiline: Boolean) {
        // TODO: Implement keyboard for gamepads
    }

    override fun processTextField(data: GuiController.TextFieldData,
                                  multiline: Boolean): Boolean {
        return false // TODO: Implement keyboard for gamepads
    }

    override fun cursors(): Stream<GuiCursor> {
        return stream()
    }

    override fun clicks(): Stream<Pair<GuiCursor, ControllerBasic.PressEvent>> {
        return stream()
    }

    override fun captureCursor(): Boolean {
        return true
    }

    private fun handlePress(key: ControllerKey) {
        if (primaryButton.isPressed(key,
                controller) && engine.guiStack.fireAction(GuiAction.ACTIVATE)) {
            return
        }
        if (secondaryButton.isPressed(key,
                controller) && engine.guiStack.fireAction(GuiAction.BACK)) {
            return
        }
        if (upButton.isPressed(key, controller) && engine.guiStack.fireAction(
                GuiAction.UP)) {
            return
        }
        if (downButton.isPressed(key, controller) && engine.guiStack.fireAction(
                GuiAction.DOWN)) {
            return
        }
        if (leftButton.isPressed(key, controller) && engine.guiStack.fireAction(
                GuiAction.LEFT)) {
            return
        }
        if (rightButton.isPressed(key,
                controller) && engine.guiStack.fireAction(GuiAction.RIGHT)) {
            return
        }
        firePress(key)
    }
}
