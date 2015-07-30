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

import org.tobi29.scapes.client.states.GameStateGameMP;
import org.tobi29.scapes.engine.gui.*;

public class GuiInGameMessage extends Gui {
    public GuiInGameMessage(GameStateGameMP state, String title,
            String message) {
        super(GuiAlignment.CENTER);
        GuiComponentVisiblePane pane =
                new GuiComponentVisiblePane(this, 200, 0, 400, 512);
        new GuiComponentText(pane, 16, 16, 32, title);
        new GuiComponentSeparator(pane, 24, 64, 352, 2);
        new GuiComponentText(pane, 16, 80, 18, message);
        new GuiComponentSeparator(pane, 24, 448, 352, 2);
        GuiComponentTextButton back =
                new GuiComponentTextButton(pane, 112, 466, 176, 30, 18, "Back");

        back.addLeftClick(event -> state.client().entity().closeGui());
    }
}
