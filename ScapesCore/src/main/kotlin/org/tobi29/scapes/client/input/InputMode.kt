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

package org.tobi29.scapes.client.input

import org.tobi29.scapes.chunk.WorldClient
import org.tobi29.scapes.engine.GameState
import org.tobi29.scapes.engine.gui.Gui
import org.tobi29.scapes.engine.gui.GuiController
import org.tobi29.scapes.entity.client.MobPlayerClientMain

interface InputMode {
    fun poll(): Boolean

    fun createControlsGUI(state: GameState,
                          prev: Gui): Gui

    fun createInGameGUI(gui: Gui,
                        world: WorldClient) {
    }

    fun playerController(player: MobPlayerClientMain): MobPlayerClientMain.Controller

    fun guiController(): GuiController
}
