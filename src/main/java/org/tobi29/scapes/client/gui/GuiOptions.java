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
import org.tobi29.scapes.engine.gui.GuiComponentSlider;
import org.tobi29.scapes.engine.gui.GuiComponentTextButton;

public class GuiOptions extends GuiMenu {
    public GuiOptions(GameState state, Gui previous) {
        super(state, "Options", previous);
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
        GuiComponentTextButton controls =
                new GuiComponentTextButton(16, 160, 368, 30, 18, "Controls");
        controls.addLeftClick(event -> {
            state.remove(this);
            state.add(new GuiControlsList(state, this));
        });
        GuiComponentTextButton graphics =
                new GuiComponentTextButton(16, 200, 368, 30, 18,
                        "Video settings");
        graphics.addLeftClick(event -> {
            state.remove(this);
            state.add(new GuiVideoSettings(state, this));
        });
        GuiComponentTextButton account =
                new GuiComponentTextButton(16, 240, 368, 30, 18, "Account");
        account.addLeftClick(event -> {
            state.remove(this);
            state.add(new GuiAccount(state, this));
        });
        pane.add(musicVolume);
        pane.add(soundVolume);
        pane.add(controls);
        pane.add(graphics);
        pane.add(account);
    }
}
