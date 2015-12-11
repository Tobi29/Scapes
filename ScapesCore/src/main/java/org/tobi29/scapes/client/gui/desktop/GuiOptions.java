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

import org.tobi29.scapes.client.gui.GuiAccount;
import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.gui.Gui;
import org.tobi29.scapes.engine.gui.GuiComponentSlider;
import org.tobi29.scapes.engine.gui.GuiComponentTextButton;
import org.tobi29.scapes.engine.gui.GuiStyle;

public class GuiOptions extends GuiMenu {
    public GuiOptions(GameState state, Gui previous, GuiStyle style) {
        super(state, "Options", previous, style);
        GuiComponentSlider musicVolume = pane.addVert(16, 5,
                p -> new GuiComponentSlider(p, 368, 30, 18, "Music",
                        state.engine().config().volume("music")));
        GuiComponentSlider soundVolume = pane.addVert(16, 5,
                p -> new GuiComponentSlider(p, 368, 30, 18, "Sound",
                        state.engine().config().volume("sound")));
        GuiComponentTextButton controls =
                pane.addVert(16, 5, p -> button(p, 368, "Controls"));
        GuiComponentTextButton graphics =
                pane.addVert(16, 5, p -> button(p, 368, "Video settings"));
        GuiComponentTextButton account =
                pane.addVert(16, 5, p -> button(p, 368, "Account"));

        musicVolume.onDragLeft(event -> state.engine().config()
                .setVolume("music", musicVolume.value()));
        soundVolume.onDragLeft(event -> state.engine().config()
                .setVolume("sound", soundVolume.value()));
        controls.onClickLeft(event -> {
            state.engine().guiStack()
                    .add("10-Menu", new GuiControlsList(state, this, style));
        });
        graphics.onClickLeft(event -> {
            state.engine().guiStack()
                    .add("10-Menu", new GuiVideoSettings(state, this, style));
        });
        account.onClickLeft(event -> {
            state.engine().guiStack()
                    .add("10-Menu", new GuiAccount(state, this, style));
        });
    }
}