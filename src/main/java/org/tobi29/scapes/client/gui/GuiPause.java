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

import org.tobi29.scapes.client.states.GameStateGameMP;
import org.tobi29.scapes.client.states.GameStateGameSP;
import org.tobi29.scapes.client.states.GameStateMenu;
import org.tobi29.scapes.engine.gui.*;
import org.tobi29.scapes.entity.client.MobPlayerClientMain;
import org.tobi29.scapes.packets.PacketInteraction;

public class GuiPause extends Gui {
    public GuiPause(GameStateGameMP state, MobPlayerClientMain player) {
        super(GuiAlignment.CENTER);
        GuiComponentVisiblePane pane =
                new GuiComponentVisiblePane(this, 200, 0, 400, 512);
        new GuiComponentText(pane, 16, 16, 32, "Pause");
        new GuiComponentSeparator(pane, 24, 64, 352, 2);
        GuiComponentTextButton achievements =
                new GuiComponentTextButton(pane, 16, 120, 368, 30, 18,
                        "Statistics");
        GuiComponentTextButton options =
                new GuiComponentTextButton(pane, 16, 160, 368, 30, 18,
                        "Options");
        String disconnectText;
        if (state instanceof GameStateGameSP) {
            disconnectText = "Save and quit";
        } else {
            disconnectText = "Disconnect";
        }
        GuiComponentTextButton disconnect =
                new GuiComponentTextButton(pane, 16, 426, 368, 30, 18,
                        disconnectText);
        new GuiComponentSeparator(pane, 24, 408, 352, 2);
        GuiComponentTextButton back =
                new GuiComponentTextButton(pane, 16, 466, 368, 30, 18, "Back");

        achievements.addLeftClick(event -> player.connection()
                .send(new PacketInteraction(
                        PacketInteraction.OPEN_STATISTICS)));
        options.addLeftClick(
                event -> player.openGui(new GuiOptionsInGame(state)));
        disconnect.addLeftClick(event -> state.engine()
                .setState(new GameStateMenu(state.engine())));
        back.addLeftClick(event -> player.closeGui());
    }
}
