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
package org.tobi29.scapes.client.gui.touch;

import org.tobi29.scapes.client.gui.GuiAccount;
import org.tobi29.scapes.client.gui.desktop.GuiControlsList;
import org.tobi29.scapes.client.gui.desktop.GuiVideoSettings;
import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.gui.Gui;
import org.tobi29.scapes.engine.gui.GuiComponentSlider;
import org.tobi29.scapes.engine.gui.GuiComponentTextButton;
import org.tobi29.scapes.engine.gui.GuiStyle;

public class GuiTouchOptions extends GuiTouchMenu {
    public GuiTouchOptions(GameState state, Gui previous, GuiStyle style) {
        super(state, "Options", previous, style);
        GuiComponentSlider musicVolume = pane.addVert(112, 10,
                p -> new GuiComponentSlider(p, 736, 60, 36, "Music",
                        state.engine().config().volume("music")));
        GuiComponentSlider soundVolume = pane.addVert(112, 10,
                p -> new GuiComponentSlider(p, 736, 60, 36, "Sound",
                        state.engine().config().volume("sound")));
        GuiComponentTextButton controls =
                pane.addHori(112, 10, 10, 10, p -> button(p, 358, "Controls"));
        GuiComponentTextButton graphics = pane.addHori(10, 10, 112, 10,
                p -> button(p, 358, "Video settings"));
        GuiComponentTextButton account =
                pane.addVert(112, 10, p -> button(p, 736, "Account"));

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
