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

import org.tobi29.scapes.engine.gui.Gui;
import org.tobi29.scapes.engine.gui.GuiAlignment;
import org.tobi29.scapes.engine.gui.GuiComponentTextButton;

public class GuiLoading extends Gui {
    private final GuiComponentLoading progress;
    private final GuiComponentTextButton label;

    public GuiLoading() {
        super(GuiAlignment.CENTER);
        progress = new GuiComponentLoading(this, 300, 280, 200, 16);
        label = new GuiComponentTextButton(this, 300, 310, 200, 32, 18,
                "Loading...");
    }

    public void setProgress(float value) {
        progress.setProgress(value);
    }

    public void setLabel(String label) {
        this.label.setText(label);
    }
}
