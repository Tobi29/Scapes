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

import org.tobi29.scapes.client.gui.GuiComponentLogo;
import org.tobi29.scapes.client.states.scenes.SceneMenu;
import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.gui.GuiAlignment;
import org.tobi29.scapes.engine.gui.GuiComponentTextButton;
import org.tobi29.scapes.engine.gui.GuiComponentVisiblePane;
import org.tobi29.scapes.engine.gui.GuiStyle;

public class GuiMainMenu extends GuiDesktop {
    public GuiMainMenu(GameState state, SceneMenu scene, GuiStyle style) {
        super(state, style, GuiAlignment.LEFT);
        GuiComponentVisiblePane pane =
                add(0, 0, p -> new GuiComponentVisiblePane(p, 144, 540));
        pane.addVert(0, 10, 5, 20, p -> new GuiComponentLogo(p, 144, 80, 18));
        GuiComponentTextButton singlePlayer =
                pane.addVert(16, 5, p -> button(p, 120, "Singleplayer"));
        GuiComponentTextButton multiPlayer =
                pane.addVert(16, 5, p -> button(p, 120, "Multiplayer"));
        GuiComponentTextButton options =
                pane.addVert(16, 5, p -> button(p, 120, "Options"));
        GuiComponentTextButton credits =
                pane.addVert(16, 5, p -> button(p, 120, "Credits"));
        GuiComponentTextButton plugins =
                pane.addVert(16, 5, p -> button(p, 120, "Plugins"));
        GuiComponentTextButton playlists =
                pane.addVert(16, 5, p -> button(p, 120, "Playlists"));
        GuiComponentTextButton screenshots =
                pane.addVert(16, 5, p -> button(p, 120, "Screenshots"));
        GuiComponentTextButton quit =
                pane.addVert(16, 5, p -> button(p, 120, "Quit"));

        singlePlayer.onClickLeft(event -> {
            state.engine().guiStack().add("10-Menu",
                    new GuiSaveSelect(state, this, scene, style));
        });
        multiPlayer.onClickLeft(event -> {
            state.engine().guiStack()
                    .add("10-Menu", new GuiServerSelect(state, this, style));
        });
        options.onClickLeft(event -> {
            state.engine().guiStack()
                    .add("10-Menu", new GuiOptions(state, this, style));
        });
        credits.onClickLeft(event -> {
            state.engine().guiStack()
                    .add("10-Menu", new GuiCredits(state, this, style));
        });
        plugins.onClickLeft(event -> {
            state.engine().guiStack()
                    .add("10-Menu", new GuiPlugins(state, this, style));
        });
        playlists.onClickLeft(event -> {
            state.engine().guiStack()
                    .add("10-Menu", new GuiPlaylists(state, this, style));
        });
        screenshots.onClickLeft(event -> {
            state.engine().guiStack()
                    .add("10-Menu", new GuiScreenshots(state, this, style));
        });
        quit.onClickLeft(event -> state.engine().stop());
    }
}
