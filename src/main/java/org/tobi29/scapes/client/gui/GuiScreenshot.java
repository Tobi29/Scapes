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
import org.tobi29.scapes.engine.opengl.texture.Texture;

public class GuiScreenshot extends Gui {
    private final Gui own;

    public GuiScreenshot(GameState state, Gui prev, Texture texture) {
        super(GuiAlignment.CENTER);
        GuiComponentVisiblePane pane =
                new GuiComponentVisiblePane(200, 0, 400, 512);
        own = this;
        GuiComponentIcon image = new GuiComponentIcon(16, 80, 368,
                (int) ((double) texture.getHeight() / texture.getWidth() * 368),
                texture);
        GuiComponentTextButton back =
                new GuiComponentTextButton(112, 466, 176, 30, 18, "Back");
        back.addLeftClick(event -> {
            state.remove(own);
            state.add(prev);
        });
        pane.add(new GuiComponentText(16, 16, 32, "Screenshots"));
        pane.add(new GuiComponentSeparator(24, 64, 352, 2));
        pane.add(image);
        pane.add(new GuiComponentSeparator(24, 448, 352, 2));
        pane.add(back);
        add(pane);
    }
}
