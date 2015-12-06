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

import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.gui.*;

public class GuiTouchMenuDouble extends GuiTouch {
    protected final GuiComponentVisiblePane pane;
    protected final GuiComponentTextButton back, save;

    protected GuiTouchMenuDouble(GameState state, String title, Gui previous,
            GuiStyle style) {
        this(state, title, "Save", "Back", previous, style);
    }

    protected GuiTouchMenuDouble(GameState state, String title,
            GuiStyle style) {
        this(state, title, "Save", "Back", style);
    }

    protected GuiTouchMenuDouble(GameState state, String title, String save,
            String back, Gui previous, GuiStyle style) {
        this(state, title, save, back, style);
        this.back.onClickLeft(event -> {
            state.engine().guiStack().add("10-Menu", previous);
        });
    }

    protected GuiTouchMenuDouble(GameState state, String title, String save,
            String back, GuiStyle style) {
        super(state, style, GuiAlignment.CENTER);
        pane = add(0, 0, p -> new GuiComponentVisiblePane(p, 960, 540));
        pane.addVert(130, 14, p -> new GuiComponentText(p, 48, title));
        pane.addVert(130, 6, p -> new GuiComponentSeparator(p, 700, 2));
        pane.add(130, 458, p -> new GuiComponentSeparator(p, 700, 2));
        this.back = pane.add(112, 470, p -> button(p, 358, back));
        this.save = pane.add(490, 470, p -> button(p, 358, save));
    }
}
