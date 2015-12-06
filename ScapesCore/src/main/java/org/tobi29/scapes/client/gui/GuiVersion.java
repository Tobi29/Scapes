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

import org.tobi29.scapes.Version;
import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.gui.GuiAlignment;
import org.tobi29.scapes.engine.gui.GuiComponentText;
import org.tobi29.scapes.engine.gui.GuiState;
import org.tobi29.scapes.engine.gui.GuiStyle;

public class GuiVersion extends GuiState {
    public GuiVersion(GameState state, GuiStyle style) {
        super(state, style, GuiAlignment.RIGHT);
        add(880, 520, p -> new GuiComponentText(p, 16,
                'v' + Version.VERSION.toString()));
    }
}
