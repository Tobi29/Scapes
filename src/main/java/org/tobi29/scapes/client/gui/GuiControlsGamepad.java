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
import org.tobi29.scapes.engine.gui.*;
import org.tobi29.scapes.engine.input.ControllerJoystick;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;

public class GuiControlsGamepad extends Gui {
    public GuiControlsGamepad(GameState state, Gui prev, ScapesClient game,
            TagStructure tagStructure, ControllerJoystick controller) {
        super(GuiAlignment.CENTER);
        game.setFreezeInputMode(true);
        GuiComponentVisiblePane pane =
                new GuiComponentVisiblePane(200, 0, 400, 512);
        GuiComponentScrollPaneList scrollPane =
                new GuiComponentScrollPaneList(16, 80, 368, 350, 40);
        TagStructure movementTag = tagStructure.getStructure("Movement");
        scrollPane.add(new GuiComponentText(40, 0, 18, "Movement"));
        addAxis("Horizontal", "X", movementTag, controller, scrollPane);
        addAxis("Vertical", "Y", movementTag, controller, scrollPane);
        addButton("Jump", "Jump", movementTag, controller, scrollPane);

        TagStructure cameraTag = tagStructure.getStructure("Camera");
        scrollPane.add(new GuiComponentText(40, 0, 18, "Camera"));
        addAxis("Horizontal", "X", cameraTag, controller, scrollPane);
        addAxis("Vertical", "Y", cameraTag, controller, scrollPane);
        addSlider("Sensitivity", "Sensitivity", cameraTag, scrollPane);

        TagStructure guiTag = tagStructure.getStructure("GUI");
        TagStructure guiCursorTag = guiTag.getStructure("Cursor");
        TagStructure guiScrollTag = guiTag.getStructure("Scroll");
        scrollPane.add(new GuiComponentText(40, 0, 18, "Cursor"));
        addAxis("Horizontal", "X", guiCursorTag, controller, scrollPane);
        addAxis("Vertical", "Y", guiCursorTag, controller, scrollPane);
        addSlider("Sensitivity", "Sensitivity", guiCursorTag, scrollPane);
        addButton("Primary", "Primary", guiTag, controller, scrollPane);
        addButton("Secondary", "Secondary", guiTag, controller, scrollPane);
        addAxis("Scroll", "Y", guiScrollTag, controller, scrollPane);
        addSlider("Scroll Sensitivity", "Sensitivity", guiScrollTag,
                scrollPane);

        TagStructure actionTag = tagStructure.getStructure("Action");
        scrollPane.add(new GuiComponentText(40, 0, 18, "Action"));
        addButton("Left", "Left", actionTag, controller, scrollPane);
        addButton("Right", "Right", actionTag, controller, scrollPane);

        TagStructure menuTag = tagStructure.getStructure("Menu");
        scrollPane.add(new GuiComponentText(40, 0, 18, "Menu"));
        addButton("Inventory", "Inventory", menuTag, controller, scrollPane);
        addButton("Chat", "Chat", menuTag, controller, scrollPane);
        addButton("Menu", "Menu", menuTag, controller, scrollPane);

        TagStructure hotbarTag = tagStructure.getStructure("Hotbar");
        scrollPane.add(new GuiComponentText(40, 0, 18, "Hotbar"));
        addButton("Right", "Add", hotbarTag, controller, scrollPane);
        addButton("Left", "Subtract", hotbarTag, controller, scrollPane);
        addButton("Left Hand", "Left", hotbarTag, controller, scrollPane);

        GuiComponentTextButton back =
                new GuiComponentTextButton(112, 466, 176, 30, 18, "Save");
        back.addLeftClick(event -> {
            game.setFreezeInputMode(false);
            game.reloadInput();
            state.remove(this);
            state.add(prev);
        });
        pane.add(new GuiComponentText(16, 16, 32, "Controls"));
        pane.add(new GuiComponentSeparator(24, 64, 352, 2));
        pane.add(scrollPane);
        pane.add(new GuiComponentSeparator(24, 448, 352, 2));
        pane.add(back);
        add(pane);
    }

    private static void addButton(String name, String id,
            TagStructure tagStructure, ControllerJoystick controller,
            GuiComponent parent) {
        GuiComponentControlsButton button =
                new GuiComponentControlsButton(15, 0, 328, 30, 18, name, id,
                        tagStructure, controller);
        parent.add(button);
    }

    private static void addAxis(String name, String id,
            TagStructure tagStructure, ControllerJoystick controller,
            GuiComponent parent) {
        GuiComponentControlsAxis button =
                new GuiComponentControlsAxis(15, 0, 328, 30, 18, name, id,
                        tagStructure, controller);
        parent.add(button);
    }

    private static void addSlider(String name, String id,
            TagStructure tagStructure, GuiComponent parent) {
        GuiComponentSlider slider =
                new GuiComponentSlider(15, 0, 328, 30, 18, name,
                        (tagStructure.getDouble(id) + 10.0d) * 0.05d,
                        (text, value) -> text + ": " +
                                (int) (value * 2000.0d - 1000.0d) + '%');
        slider.addHover(event -> tagStructure
                .setDouble(id, slider.value * 20.0f - 10.0f));
        parent.add(slider);
    }
}
