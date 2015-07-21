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

import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.gui.Gui;
import org.tobi29.scapes.engine.gui.GuiAlignment;
import org.tobi29.scapes.engine.gui.GuiComponentTextButton;
import org.tobi29.scapes.engine.gui.GuiComponentVisiblePane;

public class GuiMainMenu extends Gui {
    public GuiMainMenu(GameState state) {
        super(GuiAlignment.LEFT);
        GuiComponentVisiblePane pane =
                new GuiComponentVisiblePane(0, 0, 144, 512);
        GuiComponentLogo logo = new GuiComponentLogo(0, 4, 144, 80);
        GuiComponentTextButton singlePlayer =
                new GuiComponentTextButton(16, 120, 120, 30, 18,
                        "Singleplayer");
        singlePlayer.addLeftClick(event -> {
            state.remove(this);
            state.add(new GuiSaveSelect(state, this));
        });
        GuiComponentTextButton multiPlayer =
                new GuiComponentTextButton(16, 160, 120, 30, 18, "Multiplayer");
        multiPlayer.addLeftClick(event -> {
            state.remove(this);
            state.add(new GuiServerSelect(state, this));
        });
        GuiComponentTextButton options =
                new GuiComponentTextButton(16, 200, 120, 30, 18, "Options");
        options.addLeftClick(event -> {
            state.remove(this);
            state.add(new GuiOptions(state, this));
        });
        GuiComponentTextButton credits =
                new GuiComponentTextButton(16, 240, 120, 30, 18, "Credits");
        credits.addLeftClick(event -> {
            state.remove(this);
            state.add(new GuiCredits(state, this));
        });
        GuiComponentTextButton plugins =
                new GuiComponentTextButton(16, 280, 120, 30, 18, "Plugins");
        plugins.addLeftClick(event -> {
            state.remove(this);
            state.add(new GuiPlugins(state, this));
        });
        GuiComponentTextButton playlists =
                new GuiComponentTextButton(16, 320, 120, 30, 18, "Playlists");
        playlists.addLeftClick(event -> {
            state.remove(this);
            state.add(new GuiPlaylists(state, this));
        });
        GuiComponentTextButton screenshots =
                new GuiComponentTextButton(16, 360, 120, 30, 18, "Screenshots");
        screenshots.addLeftClick(event -> {
            state.remove(this);
            state.add(new GuiScreenshots(state, this));
        });
        GuiComponentTextButton quit =
                new GuiComponentTextButton(16, 400, 120, 30, 18, "Quit");
        quit.addLeftClick(event -> state.engine().stop());
        pane.add(logo);
        pane.add(singlePlayer);
        pane.add(multiPlayer);
        pane.add(options);
        pane.add(credits);
        pane.add(plugins);
        pane.add(playlists);
        pane.add(screenshots);
        pane.add(quit);
        add(pane);
    }
}
