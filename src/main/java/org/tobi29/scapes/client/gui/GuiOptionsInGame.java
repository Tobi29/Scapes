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
                new GuiComponentVisiblePane(200, 0, 400, 512);
        GuiComponentSlider musicVolume =
                new GuiComponentSlider(16, 80, 368, 30, 18, "Music",
                        state.getEngine().config().getMusicVolume());
        musicVolume.addHover(event -> state.getEngine().config()
                .setMusicVolume((float) musicVolume.value));
        GuiComponentSlider soundVolume =
                new GuiComponentSlider(16, 120, 368, 30, 18, "Sound",
                        state.getEngine().config().getSoundVolume());
        soundVolume.addHover(event -> state.getEngine().config()
                .setSoundVolume((float) soundVolume.value));
        GuiComponentTextButton fullscreen;
        if (state.getEngine().config().isFullscreen()) {
            fullscreen = new GuiComponentTextButton(16, 160, 368, 30, 18,
                    "Fullscreen: ON");
        } else {
            fullscreen = new GuiComponentTextButton(16, 160, 368, 30, 18,
                    "Fullscreen: OFF");
        }
        fullscreen.addLeftClick(event -> {
            if (!state.getEngine().config().isFullscreen()) {
                fullscreen.setText("Fullscreen: ON");
                state.getEngine().config().setFullscreen(true);
            } else {
                fullscreen.setText("Fullscreen: OFF");
                state.getEngine().config().setFullscreen(false);
            }
            state.getEngine().container().updateContainer();
        });
        GuiComponentTextButton back =
                new GuiComponentTextButton(112, 466, 176, 30, 18, "Back");
        back.addLeftClick(event -> state.getClient().getEntity().closeGui());
        pane.add(new GuiComponentText(16, 16, 32, "Options"));
        pane.add(new GuiComponentSeparator(24, 64, 352, 2));
        pane.add(musicVolume);
        pane.add(soundVolume);
        pane.add(fullscreen);
        pane.add(new GuiComponentSeparator(24, 448, 352, 2));
        pane.add(back);
        add(pane);
    }
}
