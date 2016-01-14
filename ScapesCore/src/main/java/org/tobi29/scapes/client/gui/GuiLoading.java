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

import org.tobi29.scapes.client.gui.desktop.GuiDesktop;
import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.gui.GuiComponentGroup;
import org.tobi29.scapes.engine.gui.GuiComponentTextButton;
import org.tobi29.scapes.engine.gui.GuiStyle;

public class GuiLoading extends GuiDesktop {
    private final GuiComponentLoading progress;
    private final GuiComponentTextButton label;

    public GuiLoading(GameState state, GuiStyle style) {
        super(state, style);
        spacer();
        GuiComponentGroup pane = addHori(0, 0, 200, -1, GuiComponentGroup::new);
        spacer();
        pane.spacer();
        progress = pane.addVert(0, 7, -1, 16,
                p -> new GuiComponentLoading(p, 200, 16));
        label = pane.addVert(0, 7, -1, 32, p -> button(p, 18, "Loading..."));
        pane.spacer();
    }

    public void setProgress(float value) {
        progress.setProgress(value);
    }

    public void setLabel(String label) {
        this.label.setText(label);
    }
}
