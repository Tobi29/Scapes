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
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;

public class GuiAddServer extends Gui {
    public GuiAddServer(GameState state, GuiServerSelect prev) {
        super(GuiAlignment.CENTER);
        GuiComponentVisiblePane pane =
                new GuiComponentVisiblePane(200, 0, 400, 512);
        GuiComponentTextField ip =
                new GuiComponentTextField(16, 110, 368, 30, 18, "");
        GuiComponentTextButton create =
                new GuiComponentTextButton(112, 466, 176, 30, 18, "Create");
        create.addLeftClick(event -> {
            TagStructure tagStructure = new TagStructure();
            tagStructure.setString("IP", ip.getText());
            prev.addServer(tagStructure);
            state.remove(this);
            prev.updateServers();
            state.add(prev);
        });
        pane.add(new GuiComponentText(16, 16, 32, "Add Server"));
        pane.add(new GuiComponentSeparator(24, 64, 352, 2));
        pane.add(new GuiComponentText(16, 80, 18, "IP:"));
        pane.add(ip);
        pane.add(new GuiComponentSeparator(24, 448, 352, 2));
        pane.add(create);
        add(pane);
    }
}
