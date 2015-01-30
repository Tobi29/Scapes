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
import org.tobi29.scapes.engine.input.ControllerDefault;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;

public class GuiControlsDefault extends Gui {
    public GuiControlsDefault(GameState state, Gui prev,
            TagStructure tagStructure, ControllerDefault controller) {
        super(GuiAlignment.CENTER);
        GuiComponentVisiblePane pane =
                new GuiComponentVisiblePane(200, 0, 400, 512);
        GuiComponentScrollPaneList scrollPane =
                new GuiComponentScrollPaneList(16, 80, 368, 350, 40);
        TagStructure movementTag = tagStructure.getStructure("Movement");
        scrollPane.add(new GuiComponentText(40, 0, 18, "Movement"));
        addButton("Forward", "Forward", movementTag, controller, scrollPane);
        addButton("Backward", "Backward", movementTag, controller, scrollPane);
        addButton("Left", "Left", movementTag, controller, scrollPane);
        addButton("Right", "Right", movementTag, controller, scrollPane);
        addButton("Sprint", "Sprint", movementTag, controller, scrollPane);
        addButton("Jump", "Jump", movementTag, controller, scrollPane);

        TagStructure cameraTag = tagStructure.getStructure("Camera");
        scrollPane.add(new GuiComponentText(40, 0, 18, "Camera"));
        addSlider("Sensitivity", "Sensitivity", cameraTag, scrollPane);

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
        addButton("Slot 1", "0", hotbarTag, controller, scrollPane);
        addButton("Slot 2", "1", hotbarTag, controller, scrollPane);
        addButton("Slot 3", "2", hotbarTag, controller, scrollPane);
        addButton("Slot 4", "3", hotbarTag, controller, scrollPane);
        addButton("Slot 5", "4", hotbarTag, controller, scrollPane);
        addButton("Slot 6", "5", hotbarTag, controller, scrollPane);
        addButton("Slot 7", "6", hotbarTag, controller, scrollPane);
        addButton("Slot 8", "7", hotbarTag, controller, scrollPane);
        addButton("Slot 9", "8", hotbarTag, controller, scrollPane);
        addButton("Slot 10", "9", hotbarTag, controller, scrollPane);

        TagStructure miscTag = tagStructure.getStructure("Misc");

        TagStructure miscScrollTag = miscTag.getStructure("Scroll");
        scrollPane.add(new GuiComponentText(40, 0, 18, "Miscellaneous"));
        addSlider("Scroll Sensitivity", "Sensitivity", miscScrollTag,
                scrollPane);

        GuiComponentTextButton back =
                new GuiComponentTextButton(112, 466, 176, 30, 18, "Save");
        back.addLeftClick(event -> {
            ((ScapesClient) state.getEngine().getGame()).reloadInput();
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
            TagStructure tagStructure, ControllerDefault controller,
            GuiComponent parent) {
        GuiComponentControlsButton button =
                new GuiComponentControlsButton(15, 0, 328, 30, 18, name, id,
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
