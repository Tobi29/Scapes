/*
 * Copyright 2012-2015 Tobi29
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
package org.tobi29.scapes.client.gui;

import org.tobi29.scapes.client.ScapesClient;
import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.gui.Gui;
import org.tobi29.scapes.engine.gui.GuiStyle;
import org.tobi29.scapes.engine.input.ControllerJoystick;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;

public class GuiControlsGamepad extends GuiControls {
    public GuiControlsGamepad(GameState state, Gui previous, ScapesClient game,
            TagStructure tagStructure, ControllerJoystick controller,
            GuiStyle style) {
        super(state, previous, game, style);
        TagStructure movementTag = tagStructure.getStructure("Movement");
        addText("Movement");
        addAxis("Horizontal", "X", movementTag, controller);
        addAxis("Vertical", "Y", movementTag, controller);
        addButton("Jump", "Jump", movementTag, controller);

        TagStructure cameraTag = tagStructure.getStructure("Camera");
        addText("Camera");
        addAxis("Horizontal", "X", cameraTag, controller);
        addAxis("Vertical", "Y", cameraTag, controller);
        addSlider("Sensitivity", "Sensitivity", cameraTag);

        TagStructure guiTag = tagStructure.getStructure("GUI");
        TagStructure guiCursorTag = guiTag.getStructure("Cursor");
        TagStructure guiScrollTag = guiTag.getStructure("Scroll");
        addText("Cursor");
        addAxis("Horizontal", "X", guiCursorTag, controller);
        addAxis("Vertical", "Y", guiCursorTag, controller);
        addSlider("Sensitivity", "Sensitivity", guiCursorTag);
        addButton("Primary", "Primary", guiTag, controller);
        addButton("Secondary", "Secondary", guiTag, controller);
        addAxis("Scroll", "Y", guiScrollTag, controller);
        addSlider("Scroll Sensitivity", "Sensitivity", guiScrollTag);

        TagStructure actionTag = tagStructure.getStructure("Action");
        addText("Action");
        addButton("Left", "Left", actionTag, controller);
        addButton("Right", "Right", actionTag, controller);

        TagStructure menuTag = tagStructure.getStructure("Menu");
        addText("Menu");
        addButton("Inventory", "Inventory", menuTag, controller);
        addButton("Chat", "Chat", menuTag, controller);
        addButton("Menu", "Menu", menuTag, controller);

        TagStructure hotbarTag = tagStructure.getStructure("Hotbar");
        addText("Hotbar");
        addButton("Right", "Add", hotbarTag, controller);
        addButton("Left", "Subtract", hotbarTag, controller);
        addButton("Left Hand", "Left", hotbarTag, controller);
    }
}
