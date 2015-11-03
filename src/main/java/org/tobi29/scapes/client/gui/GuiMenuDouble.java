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

public class GuiMenuDouble extends Gui {
    protected final GameState state;
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
        this.back.addLeftClick(event -> {
            state.remove(this);
            state.add(previous);
        });
    }

    protected GuiMenuDouble(GameState state, String title, String save,
            String back, GuiStyle style) {
        super(style, GuiAlignment.CENTER);
        this.state = state;
        pane = add(200, 0, p -> new GuiComponentVisiblePane(p, 400, 512));
        pane.addVert(16, 14, p -> new GuiComponentText(p, 32, title));
        pane.addVert(24, 6, p -> new GuiComponentSeparator(p, 352, 2));
        pane.add(24, 408, p -> new GuiComponentSeparator(p, 352, 2));
        this.save = pane.add(112, 426,
                p -> new GuiComponentTextButton(p, 176, 30, 18, save));
        this.back = pane.add(112, 466,
                p -> new GuiComponentTextButton(p, 176, 30, 18, back));
    }
}
