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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.client.gui.desktop.GuiAccount;
import org.tobi29.scapes.client.gui.desktop.GuiControlsList;
import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.gui.*;
import org.tobi29.scapes.engine.server.Account;

import java.io.IOException;

public class GuiTouchOptions extends GuiTouchMenu {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(GuiTouchOptions.class);

    public GuiTouchOptions(GameState state, Gui previous, GuiStyle style) {
        super(state, "Options", previous, style);
        GuiComponentSlider musicVolume = row(pane, p -> slider(p, "Music",
                state.engine().config().volume("music")));
        GuiComponentSlider soundVolume = row(pane, p -> slider(p, "Sound",
                state.engine().config().volume("sound")));
        GuiComponentGroupSlab slab = row(pane);
        GuiComponentTextButton controls =
                slab.addHori(10, 10, -1, -1, p -> button(p, "Controls"));
        GuiComponentTextButton graphics =
                slab.addHori(10, 10, -1, -1, p -> button(p, "Video settings"));
        GuiComponentTextButton account = row(pane, p -> button(p, "Account"));

        musicVolume.onDragLeft(event -> state.engine().config()
                .setVolume("music", musicVolume.value()));
        soundVolume.onDragLeft(event -> state.engine().config()
                .setVolume("sound", soundVolume.value()));
        controls.onClickLeft(event -> {
            state.engine().guiStack()
                    .add("10-Menu", new GuiControlsList(state, this, style));
        });
        graphics.onClickLeft(event -> {
            state.engine().guiStack().add("10-Menu",
                    new GuiTouchVideoSettings(state, this, style));
        });
        account.onClickLeft(event -> {
            try {
                Account account1 = Account.get(
                        state.engine().home().resolve("Account.properties"));
                state.engine().guiStack().add("10-Menu",
                        new GuiTouchAccount(state, this, account1, style));
            } catch (IOException e) {
                LOGGER.error("Failed to read account file: {}", e.toString());
            }
        });
    }
}
