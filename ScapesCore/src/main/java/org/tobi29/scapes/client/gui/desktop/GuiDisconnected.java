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

import org.tobi29.scapes.client.states.GameStateMenu;
import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.gui.GuiComponentText;
import org.tobi29.scapes.engine.gui.GuiStyle;
import org.tobi29.scapes.engine.utils.math.FastMath;

public class GuiDisconnected extends GuiMenu {
    private final GuiComponentText reconnectTimer;

    public GuiDisconnected(GameState state, String message, GuiStyle style) {
        super(state, "Error", style);
        pane.addVert(16, 5, -1, 12, p -> new GuiComponentText(p, message));
        reconnectTimer = pane.addVert(16, 320, -1, 32,
                p -> new GuiComponentText(p, ""));

        back.onClickLeft(event -> state.engine()
                .setState(new GameStateMenu(state.engine())));
    }

    public void setReconnectTimer(double time) {
        reconnectTimer.setText("Reconnecting in: " + FastMath.floor(time));
    }
}
