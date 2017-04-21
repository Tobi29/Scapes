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

package org.tobi29.scapes.client.input

import org.tobi29.scapes.chunk.WorldClient
import org.tobi29.scapes.client.gui.GuiMessage
import org.tobi29.scapes.engine.GameState
import org.tobi29.scapes.engine.ScapesEngine
import org.tobi29.scapes.engine.gui.Gui
import org.tobi29.scapes.engine.gui.GuiControllerDummy
import org.tobi29.scapes.engine.input.InputMode
import org.tobi29.scapes.engine.utils.math.vector.Vector2d

interface InputModeScapes : InputMode {
    val requiresCameraSmoothing get() = true

    fun createControlsGUI(state: GameState,
                          prev: Gui): Gui

    fun createInGameGUI(gui: Gui,
                        world: WorldClient) {
    }

    fun walk(): Vector2d

    fun hitDirection(): Vector2d

    fun left(): Boolean

    fun right(): Boolean

    fun jump(): Boolean
}

class InputModeDummy(private val engine: ScapesEngine) : InputModeScapes {
    override fun poll(delta: Double) = false

    override fun createControlsGUI(state: GameState,
                                   prev: Gui) =
            GuiMessage(state, prev, "", "", prev.style)

    override fun walk() = Vector2d.ZERO

    override fun hitDirection() = Vector2d.ZERO

    override fun left() = false

    override fun right() = false

    override fun jump() = false

    override fun guiController() = GuiControllerDummy(engine)
}
