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
import org.tobi29.scapes.engine.gui.GuiStyle;

public class GuiOptions extends GuiMenu {
    public GuiOptions(GameState state, Gui previous, GuiStyle style) {
        super(state, "Options", previous, style);
        GuiComponentSlider musicVolume =
                new GuiComponentSlider(pane, 16, 80, 368, 30, 18, "Music",
                        state.engine().config().volume("music"));
        GuiComponentSlider soundVolume =
                new GuiComponentSlider(pane, 16, 120, 368, 30, 18, "Sound",
                        state.engine().config().volume("sound"));
        GuiComponentTextButton controls =
                new GuiComponentTextButton(pane, 16, 160, 368, 30, 18,
                        "Controls");
        GuiComponentTextButton graphics =
                new GuiComponentTextButton(pane, 16, 200, 368, 30, 18,
                        "Video settings");
        GuiComponentTextButton account =
                new GuiComponentTextButton(pane, 16, 240, 368, 30, 18,
                        "Account");

        musicVolume.addHover(event -> state.engine().config()
                .setVolume("music", musicVolume.value()));
        soundVolume.addHover(event -> state.engine().config()
                .setVolume("sound", soundVolume.value()));
        controls.addLeftClick(event -> {
            state.remove(this);
            state.add(new GuiControlsList(state, this, style));
        });
        graphics.addLeftClick(event -> {
            state.remove(this);
            state.add(new GuiVideoSettings(state, this, style));
        });
        account.addLeftClick(event -> {
            state.remove(this);
            state.add(new GuiAccount(state, this, style));
        });
    }
}
