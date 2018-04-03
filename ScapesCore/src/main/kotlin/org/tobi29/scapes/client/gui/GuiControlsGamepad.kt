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

import org.tobi29.io.tag.MutableTagMap
import org.tobi29.io.tag.mapMut
import org.tobi29.scapes.engine.GameState
import org.tobi29.scapes.engine.gui.Gui
import org.tobi29.scapes.engine.gui.GuiStyle
import org.tobi29.scapes.engine.input.ControllerJoystick

class GuiControlsGamepad(state: GameState,
                         previous: Gui,
                         tagMap: MutableTagMap,
                         controller: ControllerJoystick,
                         style: GuiStyle) : GuiControls(state, previous,
        style) {
    init {
        val movementTag = tagMap.mapMut("Movement")
        addText("Movement")
        addAxis("Horizontal", "X", movementTag, controller)
        addAxis("Vertical", "Y", movementTag, controller)
        addButton("Jump", "Jump", movementTag, controller)

        val cameraTag = tagMap.mapMut("Camera")
        addText("Camera")
        addAxis("Horizontal", "X", cameraTag, controller)
        addAxis("Vertical", "Y", cameraTag, controller)
        addSlider("Sensitivity", "Sensitivity", cameraTag)

        val guiTag = tagMap.mapMut("GUI")
        addText("Cursor")
        addButton("Primary", "Primary", guiTag, controller)
        addButton("Secondary", "Secondary", guiTag, controller)
        addButton("Select Up", "Up", guiTag, controller)
        addButton("Select Down", "Down", guiTag, controller)
        addButton("Select Left", "Left", guiTag, controller)
        addButton("Select Right", "Right", guiTag, controller)

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
    }
}
