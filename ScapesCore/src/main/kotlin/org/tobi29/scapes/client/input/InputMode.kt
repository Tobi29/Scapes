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
import org.tobi29.scapes.engine.GameState
import org.tobi29.scapes.engine.gui.Gui
import org.tobi29.scapes.engine.gui.GuiController
import org.tobi29.scapes.engine.utils.EventDispatcher
import org.tobi29.scapes.engine.utils.ListenerOwner
import org.tobi29.scapes.engine.utils.math.vector.Vector2d

interface InputMode : ListenerOwner {
    val events: EventDispatcher

    fun poll(delta: Double): Boolean

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

    fun guiController(): GuiController
}
