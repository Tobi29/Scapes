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

import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.gui.*;

public class GuiMenuDouble extends GuiDesktop {
    protected final GuiComponentVisiblePane pane;
    protected final GuiComponentTextButton back, save;

    protected GuiMenuDouble(GameState state, String title, Gui previous,
            GuiStyle style) {
        this(state, title, "Save", "Back", previous, style);
    }

    protected GuiMenuDouble(GameState state, String title, GuiStyle style) {
        this(state, title, "Save", "Back", style);
    }

    protected GuiMenuDouble(GameState state, String title, String save,
            String back, Gui previous, GuiStyle style) {
        this(state, title, save, back, style);
        this.back.onClickLeft(event -> {
            state.engine().guiStack().add("10-Menu", previous);
        });
    }

    protected GuiMenuDouble(GameState state, String title, String save,
            String back, GuiStyle style) {
        super(state, style, GuiAlignment.CENTER);
        pane = add(280, 0, p -> new GuiComponentVisiblePane(p, 400, 540));
        pane.addVert(16, 14, p -> new GuiComponentText(p, 32, title));
        pane.addVert(24, 6, p -> new GuiComponentSeparator(p, 352, 2));
        pane.add(24, 446, p -> new GuiComponentSeparator(p, 352, 2));
        this.save = pane.add(112, 464, p -> button(p, 176, save));
        this.back = pane.add(112, 504, p -> button(p, 176, back));
    }
}
