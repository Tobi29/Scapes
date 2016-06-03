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

import org.tobi29.scapes.client.gui.desktop.GuiMenuDouble;
import org.tobi29.scapes.client.gui.desktop.GuiServerSelect;
import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.gui.GuiComponentText;
import org.tobi29.scapes.engine.gui.GuiComponentTextField;
import org.tobi29.scapes.engine.gui.GuiStyle;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;

public class GuiAddServer extends GuiMenuDouble {
    public GuiAddServer(GameState state, GuiServerSelect previous,
            GuiStyle style) {
        super(state, "Add Server", previous, style);
        pane.addVert(16, 5, -1, 18, p -> new GuiComponentText(p, "IP:"));
        GuiComponentTextField ip =
                row(pane, p -> new GuiComponentTextField(p, 18, ""));
        pane.addVert(16, 5, -1, 18, p -> new GuiComponentText(p, "Port:"));
        GuiComponentTextField port =
                row(pane, p -> new GuiComponentTextField(p, 18, "12345"));

        save.onClickLeft(event -> {
            TagStructure tagStructure = new TagStructure();
            tagStructure.setString("Address", ip.text());
            try {
                tagStructure.setInteger("Port", Integer.valueOf(port.text()));
            } catch (NumberFormatException e) {
                tagStructure.setInteger("Port", 12345);
            }
            previous.addServer(tagStructure);
            previous.updateServers();
            state.engine().guiStack().add("10-Menu", previous);
        });
    }
}
