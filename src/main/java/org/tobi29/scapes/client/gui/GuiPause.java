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
                new GuiComponentVisiblePane(200, 0, 400, 512);
        GuiComponentTextButton achievements =
                new GuiComponentTextButton(16, 120, 368, 30, 18, "Statistics");
        achievements.addLeftClick(event -> player.connection()
                .send(new PacketInteraction(
                        PacketInteraction.OPEN_STATISTICS)));
        GuiComponentTextButton options =
                new GuiComponentTextButton(16, 160, 368, 30, 18, "Options");
        options.addLeftClick(
                event -> player.openGui(new GuiOptionsInGame(state)));
        GuiComponentTextButton disconnect;
        if (state instanceof GameStateGameSP) {
            disconnect = new GuiComponentTextButton(16, 426, 368, 30, 18,
                    "Save and quit");
        } else {
            disconnect = new GuiComponentTextButton(16, 426, 368, 30, 18,
                    "Disconnect");
        }
        disconnect.addLeftClick(event -> state.engine()
                .setState(new GameStateMenu(state.engine())));
        GuiComponentTextButton back =
                new GuiComponentTextButton(16, 466, 368, 30, 18, "Back");
        back.addLeftClick(event -> player.closeGui());
        pane.add(new GuiComponentText(16, 16, 32, "Pause"));
        pane.add(new GuiComponentSeparator(24, 64, 352, 2));
        pane.add(achievements);
        pane.add(options);
        pane.add(new GuiComponentSeparator(24, 408, 352, 2));
        pane.add(disconnect);
        pane.add(back);
        add(pane);
    }
}
