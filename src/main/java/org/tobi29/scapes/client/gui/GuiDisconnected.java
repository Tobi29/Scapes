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

import org.tobi29.scapes.client.states.GameStateMenu;
import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.gui.*;
import org.tobi29.scapes.engine.utils.math.FastMath;

public class GuiDisconnected extends Gui {
    private final GuiComponentText reconnectTimer;

    public GuiDisconnected(GameState state, String message, GuiStyle style) {
        super(style, GuiAlignment.CENTER);
        GuiComponentVisiblePane pane =
                add(200, 0, p -> new GuiComponentVisiblePane(p, 400, 512));
        pane.add(16, 16, p -> new GuiComponentText(p, 32, "Error"));
        pane.add(24, 64, p -> new GuiComponentSeparator(p, 352, 2));
        pane.add(16, 80, p -> new GuiComponentText(p, 12, message));
        reconnectTimer =
                pane.add(16, 400, p -> new GuiComponentText(p, 32, ""));
        pane.add(24, 448, p -> new GuiComponentSeparator(p, 352, 2));
        GuiComponentTextButton back = pane.add(112, 466,
                p -> new GuiComponentTextButton(p, 176, 30, 18, "Back"));

        back.addLeftClick(event -> state.engine()
                .setState(new GameStateMenu(state.engine())));
    }

    public void setReconnectTimer(double time) {
        reconnectTimer.setText("Reconnecting in: " + FastMath.floor(time));
    }
}
