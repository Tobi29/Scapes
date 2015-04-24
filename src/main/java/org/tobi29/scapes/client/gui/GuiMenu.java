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
import org.tobi29.scapes.engine.gui.*;

public class GuiMenu extends Gui {
    protected final GameState state;
    protected final GuiComponentVisiblePane pane;
    protected final GuiComponentTextButton back;

    protected GuiMenu(GameState state, String title, Gui previous) {
        this(state, title, "Back", previous);
    }

    protected GuiMenu(GameState state, String title, String back,
            Gui previous) {
        super(GuiAlignment.CENTER);
        this.state = state;
        pane = new GuiComponentVisiblePane(200, 0, 400, 512);
        pane.add(new GuiComponentText(16, 16, 32, title));
        pane.add(new GuiComponentSeparator(24, 64, 352, 2));
        pane.add(new GuiComponentSeparator(24, 448, 352, 2));
        this.back = new GuiComponentTextButton(112, 466, 176, 30, 18, back);
        this.back.addLeftClick(event -> {
            state.remove(this);
            state.add(previous);
        });
        pane.add(this.back);
        add(pane);
    }
}
