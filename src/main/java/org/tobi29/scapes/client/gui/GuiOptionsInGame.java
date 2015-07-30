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
import org.tobi29.scapes.engine.gui.*;

public class GuiOptionsInGame extends Gui {
    public GuiOptionsInGame(GameStateGameMP state) {
        super(GuiAlignment.CENTER);
        GuiComponentVisiblePane pane =
                new GuiComponentVisiblePane(this, 200, 0, 400, 512);
        new GuiComponentText(pane, 16, 16, 32, "Options");
        new GuiComponentSeparator(pane, 24, 64, 352, 2);
        GuiComponentSlider musicVolume =
                new GuiComponentSlider(pane, 16, 80, 368, 30, 18, "Music",
                        state.engine().config().musicVolume());
        GuiComponentSlider soundVolume =
                new GuiComponentSlider(pane, 16, 120, 368, 30, 18, "Sound",
                        state.engine().config().soundVolume());
        GuiComponentTextButton fullscreen;
        if (state.engine().config().isFullscreen()) {
            fullscreen = new GuiComponentTextButton(pane, 16, 160, 368, 30, 18,
                    "Fullscreen: ON");
        } else {
            fullscreen = new GuiComponentTextButton(pane, 16, 160, 368, 30, 18,
                    "Fullscreen: OFF");
        }
        new GuiComponentSeparator(pane, 24, 448, 352, 2);
        GuiComponentTextButton back =
                new GuiComponentTextButton(pane, 112, 466, 176, 30, 18, "Back");

        musicVolume.addHover(event -> state.engine().config()
                .setMusicVolume((float) musicVolume.value));
        soundVolume.addHover(event -> state.engine().config()
                .setSoundVolume((float) soundVolume.value));
        fullscreen.addLeftClick(event -> {
            if (!state.engine().config().isFullscreen()) {
                fullscreen.setText("Fullscreen: ON");
                state.engine().config().setFullscreen(true);
            } else {
                fullscreen.setText("Fullscreen: OFF");
                state.engine().config().setFullscreen(false);
            }
            state.engine().container().updateContainer();
        });
        back.addLeftClick(event -> state.client().entity().closeGui());
    }
}
