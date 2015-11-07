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
                add(0, 0, p -> new GuiComponentVisiblePane(p, 144, 512));
        pane.addVert(0, 10, 5, 20, p -> new GuiComponentLogo(p, 144, 80));
        GuiComponentTextButton singlePlayer = pane.addVert(16, 5,
                p -> new GuiComponentTextButton(p, 120, 30, 18,
                        "Singleplayer"));
        GuiComponentTextButton multiPlayer = pane.addVert(16, 5,
                p -> new GuiComponentTextButton(p, 120, 30, 18, "Multiplayer"));
        GuiComponentTextButton options = pane.addVert(16, 5,
                p -> new GuiComponentTextButton(p, 120, 30, 18, "Options"));
        GuiComponentTextButton credits = pane.addVert(16, 5,
                p -> new GuiComponentTextButton(p, 120, 30, 18, "Credits"));
        GuiComponentTextButton plugins = pane.addVert(16, 5,
                p -> new GuiComponentTextButton(p, 120, 30, 18, "Plugins"));
        GuiComponentTextButton playlists = pane.addVert(16, 5,
                p -> new GuiComponentTextButton(p, 120, 30, 18, "Playlists"));
        GuiComponentTextButton screenshots = pane.addVert(16, 5,
                p -> new GuiComponentTextButton(p, 120, 30, 18, "Screenshots"));
        GuiComponentTextButton quit = pane.addVert(16, 5,
                p -> new GuiComponentTextButton(p, 120, 30, 18, "Quit"));

        singlePlayer.addLeftClick(event -> {
            state.remove(this);
            state.add(new GuiSaveSelect(state, this, scene, style));
        });
        multiPlayer.addLeftClick(event -> {
            state.remove(this);
            state.add(new GuiServerSelect(state, this, style));
        });
        options.addLeftClick(event -> {
            state.remove(this);
            state.add(new GuiOptions(state, this, style));
        });
        credits.addLeftClick(event -> {
            state.remove(this);
            state.add(new GuiCredits(state, this, style));
        });
        plugins.addLeftClick(event -> {
            state.remove(this);
            state.add(new GuiPlugins(state, this, style));
        });
        playlists.addLeftClick(event -> {
            state.remove(this);
            state.add(new GuiPlaylists(state, this, style));
        });
        screenshots.addLeftClick(event -> {
            state.remove(this);
            state.add(new GuiScreenshots(state, this, style));
        });
        quit.addLeftClick(event -> state.engine().stop());
    }
}
