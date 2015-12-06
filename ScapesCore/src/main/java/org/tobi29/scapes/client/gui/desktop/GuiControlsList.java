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
package org.tobi29.scapes.client.gui.desktop;

import org.tobi29.scapes.client.ScapesClient;
import org.tobi29.scapes.client.gui.desktop.GuiMenu;
import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.gui.Gui;
import org.tobi29.scapes.engine.gui.GuiComponentTextButton;
import org.tobi29.scapes.engine.gui.GuiStyle;

public class GuiControlsList extends GuiMenu {
    public GuiControlsList(GameState state, Gui previous, GuiStyle style) {
        super(state, "Controls", previous, style);
        ((ScapesClient) state.engine().game()).inputModes()
                .forEach(inputMode -> {
                    GuiComponentTextButton controls = pane.addVert(16, 5,
                            p -> button(p, 368, inputMode.toString()));
                    controls.onClickLeft(event -> {
                        state.engine().guiStack().add("10-Menu",
                                inputMode.createControlsGUI(state, this));
                    });
                });
    }
}
