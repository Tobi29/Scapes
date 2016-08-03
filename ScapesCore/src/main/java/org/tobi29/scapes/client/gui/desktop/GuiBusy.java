/*
 * Copyright 2012-2016 Tobi29
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

import org.tobi29.scapes.client.gui.GuiComponentBusy;
import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.gui.GuiComponentGroup;
import org.tobi29.scapes.engine.gui.GuiComponentTextButton;
import org.tobi29.scapes.engine.gui.GuiStyle;

public class GuiBusy extends GuiDesktop {
    private final GuiComponentBusy busy;
    private final GuiComponentTextButton label;

    public GuiBusy(GameState state, GuiStyle style) {
        super(state, style);
        spacer();
        GuiComponentGroup pane = addHori(0, 0, 300, -1, GuiComponentGroup::new);
        spacer();
        pane.spacer();
        busy = pane.addVert(0, 10, 32, 32, GuiComponentBusy::new);
        label = row(pane, p -> button(p, "Loading..."));
        pane.spacer();
    }

    public void setColor(float r, float g, float b, float a) {
        busy.setColor(r, g, b, a);
    }

    public void setLabel(String label) {
        this.label.setText(label);
    }
}
