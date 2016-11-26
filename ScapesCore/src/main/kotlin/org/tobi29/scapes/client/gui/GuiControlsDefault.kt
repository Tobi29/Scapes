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

package org.tobi29.scapes.client.gui

import org.tobi29.scapes.client.ScapesClient
import org.tobi29.scapes.engine.GameState
import org.tobi29.scapes.engine.gui.Gui
import org.tobi29.scapes.engine.gui.GuiStyle
import org.tobi29.scapes.engine.input.ControllerDefault
import org.tobi29.scapes.engine.utils.io.tag.TagStructure

class GuiControlsDefault(state: GameState, previous: Gui, game: ScapesClient,
                         tagStructure: TagStructure, controller: ControllerDefault,
                         style: GuiStyle) : GuiControls(state, previous, game,
        style) {
    init {
        val movementTag = tagStructure.structure("Movement")
        addText("Movement")
        addButton("Forward", "Forward", movementTag, controller)
        addButton("Backward", "Backward", movementTag, controller)
        addButton("Left", "Left", movementTag, controller)
        addButton("Right", "Right", movementTag, controller)
        addButton("Sprint", "Sprint", movementTag, controller)
        addButton("Jump", "Jump", movementTag, controller)

        val cameraTag = tagStructure.structure("Camera")
        addText("Camera")
        addSlider("Sensitivity", "Sensitivity", cameraTag)

        val actionTag = tagStructure.structure("Action")
        addText("Action")
        addButton("Left", "Left", actionTag, controller)
        addButton("Right", "Right", actionTag, controller)

        val menuTag = tagStructure.structure("Menu")
        addText("Menu")
        addButton("Inventory", "Inventory", menuTag, controller)
        addButton("Chat", "Chat", menuTag, controller)
        addButton("Menu", "Menu", menuTag, controller)

        val hotbarTag = tagStructure.structure("Hotbar")
        addText("Hotbar")
        addButton("Right", "Add", hotbarTag, controller)
        addButton("Left", "Subtract", hotbarTag, controller)
        addButton("Left Hand", "Left", hotbarTag, controller)
        addButton("Both Hands", "Both", hotbarTag, controller)
        addButton("Slot 1", "0", hotbarTag, controller)
        addButton("Slot 2", "1", hotbarTag, controller)
        addButton("Slot 3", "2", hotbarTag, controller)
        addButton("Slot 4", "3", hotbarTag, controller)
        addButton("Slot 5", "4", hotbarTag, controller)
        addButton("Slot 6", "5", hotbarTag, controller)
        addButton("Slot 7", "6", hotbarTag, controller)
        addButton("Slot 8", "7", hotbarTag, controller)
        addButton("Slot 9", "8", hotbarTag, controller)
        addButton("Slot 10", "9", hotbarTag, controller)

        val miscTag = tagStructure.structure("Misc")
        val miscScrollTag = miscTag.structure("Scroll")
        addText("Miscellaneous")
        addSlider("Scroll Sensitivity", "Sensitivity", miscScrollTag)
    }
}
