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
        on(GuiAction.BACK,
                () -> state.engine().guiStack().swap(this, previous));
    }

    protected GuiTouchMenuDouble(GameState state, String title, String save,
            String back, GuiStyle style) {
        super(state, style);
        pane = addHori(0, 0, -1, -1, GuiComponentVisiblePane::new);
        pane.addVert(130, 14, -1, 48, p -> new GuiComponentText(p, title));
        pane.addVert(130, 6, -1, 2, GuiComponentSeparator::new);
        pane.addVert(0, 0, 0, 0, -1, -1, Long.MIN_VALUE,
                GuiComponentGroup::new);
        pane.addVert(130, 6, 130, 6, -1, 2, Long.MIN_VALUE,
                GuiComponentSeparator::new);
        GuiComponentGroupSlab bottom =
                pane.addVert(112, 4, 112, 28, -1, 60, Long.MIN_VALUE,
                        GuiComponentGroupSlab::new);
        this.back = bottom.addHori(0, 0, 10, 0, -1, -1, p -> button(p, back));
        this.save = bottom.addHori(10, 0, 0, 0, -1, -1, p -> button(p, save));

        selection(this.back);
        selection(this.save);

        this.back.on(GuiEvent.CLICK_LEFT,
                (event, engine) -> fireAction(GuiAction.BACK, engine));
    }
}
