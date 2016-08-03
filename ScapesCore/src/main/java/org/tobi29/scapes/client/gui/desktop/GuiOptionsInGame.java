/*
 * Copyright 2012-2016 Tobi29
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

import org.tobi29.scapes.client.states.GameStateGameMP;
import org.tobi29.scapes.engine.gui.*;

public class GuiOptionsInGame extends GuiMenu {
    public GuiOptionsInGame(GameStateGameMP state, GuiStyle style) {
        super(state, "Options", style);
        GuiComponentSlider musicVolume = row(pane, p -> slider(p, "Music",
                state.engine().config().volume("music")));
        GuiComponentSlider soundVolume = row(pane, p -> slider(p, "Sound",
                state.engine().config().volume("sound")));
        GuiComponentTextButton fullscreen;
        if (state.engine().config().isFullscreen()) {
            fullscreen = row(pane, p -> button(p, "Fullscreen: ON"));
        } else {
            fullscreen = row(pane, p -> button(p, "Fullscreen: OFF"));
        }

        selection(musicVolume);
        selection(soundVolume);
        selection(fullscreen);

        musicVolume.on(GuiEvent.CHANGE, event -> state.engine().config()
                .setVolume("music", musicVolume.value()));
        soundVolume.on(GuiEvent.CHANGE, event -> state.engine().config()
                .setVolume("sound", soundVolume.value()));
        fullscreen.on(GuiEvent.CLICK_LEFT, event -> {
            if (!state.engine().config().isFullscreen()) {
                fullscreen.setText("Fullscreen: ON");
                state.engine().config().setFullscreen(true);
            } else {
                fullscreen.setText("Fullscreen: OFF");
                state.engine().config().setFullscreen(false);
            }
            state.engine().container().updateContainer();
        });
        on(GuiAction.BACK, () -> state.client().entity().closeGui());
    }
}
