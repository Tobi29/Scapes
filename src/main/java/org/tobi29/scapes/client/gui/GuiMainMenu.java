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

import org.tobi29.scapes.client.states.scenes.SceneMenu;
import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.gui.*;

public class GuiMainMenu extends Gui {
    public GuiMainMenu(GameState state, SceneMenu scene, GuiStyle style) {
        super(style, GuiAlignment.LEFT);
        GuiComponentVisiblePane pane =
                new GuiComponentVisiblePane(this, 0, 0, 144, 512);
        new GuiComponentLogo(pane, 0, 4, 144, 80);
        GuiComponentTextButton singlePlayer =
                new GuiComponentTextButton(pane, 16, 120, 120, 30, 18,
                        "Singleplayer");
        singlePlayer.addLeftClick(event -> {
            state.remove(this);
            state.add(new GuiSaveSelect(state, this, scene, style));
        });
        GuiComponentTextButton multiPlayer =
                new GuiComponentTextButton(pane, 16, 160, 120, 30, 18,
                        "Multiplayer");
        multiPlayer.addLeftClick(event -> {
            state.remove(this);
            state.add(new GuiServerSelect(state, this, style));
        });
        GuiComponentTextButton options =
                new GuiComponentTextButton(pane, 16, 200, 120, 30, 18,
                        "Options");
        options.addLeftClick(event -> {
            state.remove(this);
            state.add(new GuiOptions(state, this, style));
        });
        GuiComponentTextButton credits =
                new GuiComponentTextButton(pane, 16, 240, 120, 30, 18,
                        "Credits");
        credits.addLeftClick(event -> {
            state.remove(this);
            state.add(new GuiCredits(state, this, style));
        });
        GuiComponentTextButton plugins =
                new GuiComponentTextButton(pane, 16, 280, 120, 30, 18,
                        "Plugins");
        plugins.addLeftClick(event -> {
            state.remove(this);
            state.add(new GuiPlugins(state, this, style));
        });
        GuiComponentTextButton playlists =
                new GuiComponentTextButton(pane, 16, 320, 120, 30, 18,
                        "Playlists");
        playlists.addLeftClick(event -> {
            state.remove(this);
            state.add(new GuiPlaylists(state, this, style));
        });
        GuiComponentTextButton screenshots =
                new GuiComponentTextButton(pane, 16, 360, 120, 30, 18,
                        "Screenshots");
        screenshots.addLeftClick(event -> {
            state.remove(this);
            state.add(new GuiScreenshots(state, this, style));
        });
        GuiComponentTextButton quit =
                new GuiComponentTextButton(pane, 16, 400, 120, 30, 18, "Quit");
        quit.addLeftClick(event -> state.engine().stop());
    }
}
