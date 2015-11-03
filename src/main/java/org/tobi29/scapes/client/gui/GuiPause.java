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
import org.tobi29.scapes.engine.gui.GuiComponentTextButton;
import org.tobi29.scapes.engine.gui.GuiStyle;
import org.tobi29.scapes.entity.client.MobPlayerClientMain;
import org.tobi29.scapes.packets.PacketInteraction;

public class GuiPause extends GuiMenuDouble {
    public GuiPause(GameStateGameMP state, MobPlayerClientMain player,
            GuiStyle style) {
        super(state, "Pause",
                state instanceof GameStateGameSP ? "Save and quit" :
                        "Disconnect", "Back", style);
        GuiComponentTextButton achievements = pane.addVert(16, 5,
                p -> new GuiComponentTextButton(p, 368, 30, 18, "Statistics"));
        GuiComponentTextButton options = pane.addVert(16, 5,
                p -> new GuiComponentTextButton(p, 368, 30, 18, "Options"));

        achievements.addLeftClick(event -> player.connection()
                .send(new PacketInteraction(
                        PacketInteraction.OPEN_STATISTICS)));
        options.addLeftClick(
                event -> player.openGui(new GuiOptionsInGame(state, style)));
        save.addLeftClick(event -> state.engine()
                .setState(new GameStateMenu(state.engine())));
        back.addLeftClick(event -> player.closeGui());
    }
}
