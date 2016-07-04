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

import org.tobi29.scapes.client.states.GameStateGameMP;
import org.tobi29.scapes.engine.gui.GuiAction;
import org.tobi29.scapes.engine.gui.GuiComponentText;
import org.tobi29.scapes.engine.gui.GuiStyle;

public class GuiInGameMessage extends GuiMenu {
    public GuiInGameMessage(GameStateGameMP state, String title, String message,
            GuiStyle style) {
        super(state, title, style);
        pane.addVert(16, 5, -1, 18, p -> new GuiComponentText(p, message));

        on(GuiAction.BACK, () -> state.client().entity().closeGui());
    }
}
