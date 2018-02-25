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

import org.tobi29.scapes.client.ScapesClient
import org.tobi29.scapes.engine.GameState
import org.tobi29.scapes.engine.gui.Gui
import org.tobi29.scapes.engine.gui.GuiStyle
import org.tobi29.scapes.engine.input.ControllerButtons
import org.tobi29.io.tag.MutableTagMap
import org.tobi29.io.tag.mapMut

class GuiControlsDefault(
        state: GameState,
        previous: Gui,
        game: ScapesClient,
        tagMap: MutableTagMap,
        controller: ControllerButtons,
        style: GuiStyle
) : GuiControls(state, previous, style) {
    init {
        val movementTag = tagMap.mapMut("Movement")
        addText("Movement")
        addButton("Forward", "Forward", movementTag, controller)
        addButton("Backward", "Backward", movementTag, controller)
        addButton("Left", "Left", movementTag, controller)
        addButton("Right", "Right", movementTag, controller)
        addButton("Sprint", "Sprint", movementTag, controller)
        addButton("Jump", "Jump", movementTag, controller)

        val cameraTag = tagMap.mapMut("Camera")
        addText("Camera")
        addSlider("Sensitivity", "Sensitivity", cameraTag)

        val actionTag = tagMap.mapMut("Action")
        addText("Action")
        addButton("Left", "Left", actionTag, controller)
        addButton("Right", "Right", actionTag, controller)

        val menuTag = tagMap.mapMut("Menu")
        addText("Menu")
        addButton("Inventory", "Inventory", menuTag, controller)
        addButton("Chat", "Chat", menuTag, controller)
        addButton("Menu", "Menu", menuTag, controller)

        val hotbarTag = tagMap.mapMut("Hotbar")
        addText("Hotbar")
        addButton("Right Hand Right", "AddRight", hotbarTag, controller)
        addButton("Right Hand Left", "SubtractRight", hotbarTag, controller)
        addButton("Left Hand Right", "AddLeft", hotbarTag, controller)
        addButton("Left Hand Left", "SubtractLeft", hotbarTag, controller)
        addButton("Both Right", "Add", hotbarTag, controller)
        addButton("Both Left", "Subtract", hotbarTag, controller)
        addButton("Right Slot 1", "0Right", hotbarTag, controller)
        addButton("Right Slot 2", "1Right", hotbarTag, controller)
        addButton("Right Slot 3", "2Right", hotbarTag, controller)
        addButton("Right Slot 4", "3Right", hotbarTag, controller)
        addButton("Right Slot 5", "4Right", hotbarTag, controller)
        addButton("Right Slot 6", "5Right", hotbarTag, controller)
        addButton("Right Slot 7", "6Right", hotbarTag, controller)
        addButton("Right Slot 8", "7Right", hotbarTag, controller)
        addButton("Right Slot 9", "8Right", hotbarTag, controller)
        addButton("Right Slot 10", "9Right", hotbarTag, controller)
        addButton("Left Slot 1", "0Left", hotbarTag, controller)
        addButton("Left Slot 2", "1Left", hotbarTag, controller)
        addButton("Left Slot 3", "2Left", hotbarTag, controller)
        addButton("Left Slot 4", "3Left", hotbarTag, controller)
        addButton("Left Slot 5", "4Left", hotbarTag, controller)
        addButton("Left Slot 6", "5Left", hotbarTag, controller)
        addButton("Left Slot 7", "6Left", hotbarTag, controller)
        addButton("Left Slot 8", "7Left", hotbarTag, controller)
        addButton("Left Slot 9", "8Left", hotbarTag, controller)
        addButton("Left Slot 10", "9Left", hotbarTag, controller)
        addButton("Both Slots 1", "0", hotbarTag, controller)
        addButton("Both Slots 2", "1", hotbarTag, controller)
        addButton("Both Slots 3", "2", hotbarTag, controller)
        addButton("Both Slots 4", "3", hotbarTag, controller)
        addButton("Both Slots 5", "4", hotbarTag, controller)
        addButton("Both Slots 6", "5", hotbarTag, controller)
        addButton("Both Slots 7", "6", hotbarTag, controller)
        addButton("Both Slots 8", "7", hotbarTag, controller)
        addButton("Both Slots 9", "8", hotbarTag, controller)
        addButton("Both Slots 10", "9", hotbarTag, controller)

        val miscTag = tagMap.mapMut("Misc")
        val miscScrollTag = miscTag.mapMut("Scroll")
        addText("Miscellaneous")
        addSlider("Scroll Sensitivity", "Sensitivity", miscScrollTag)
    }
}
