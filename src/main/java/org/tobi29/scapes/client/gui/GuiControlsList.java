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
import org.tobi29.scapes.client.input.InputMode;
import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.gui.Gui;
import org.tobi29.scapes.engine.gui.GuiComponentTextButton;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GuiControlsList extends GuiMenu {
    public GuiControlsList(GameState state, Gui previous) {
        super(state, "Controls", previous);
        Map<String, InputMode> inputModes = new ConcurrentHashMap<>();
        ((ScapesClient) state.engine().game()).inputModes().forEach(
                inputMode -> inputModes.put(inputMode.toString(), inputMode));
        int y = 80;
        for (InputMode inputMode : inputModes.values()) {
            GuiComponentTextButton controls =
                    new GuiComponentTextButton(pane, 16, y, 368, 30, 18,
                            inputMode.toString());
            controls.addLeftClick(event -> {
                state.remove(this);
                state.add(inputMode.createControlsGUI(state, this));
            });
            y += 40;
        }
    }
}
