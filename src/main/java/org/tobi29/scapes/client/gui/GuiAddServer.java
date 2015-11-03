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
import org.tobi29.scapes.engine.gui.GuiComponentText;
import org.tobi29.scapes.engine.gui.GuiComponentTextField;
import org.tobi29.scapes.engine.gui.GuiStyle;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;

public class GuiAddServer extends GuiMenuDouble {
    public GuiAddServer(GameState state, GuiServerSelect previous,
            GuiStyle style) {
        super(state, "Add Server", previous, style);
        GuiComponentTextField ip = pane.add(16, 110,
                p -> new GuiComponentTextField(p, 368, 30, 18, ""));
        GuiComponentTextField port = pane.add(16, 180,
                p -> new GuiComponentTextField(p, 368, 30, 18, "12345"));
        save.addLeftClick(event -> {
            TagStructure tagStructure = new TagStructure();
            tagStructure.setString("Address", ip.text());
            try {
                tagStructure.setInteger("Port", Integer.valueOf(port.text()));
            } catch (NumberFormatException e) {
                tagStructure.setInteger("Port", 12345);
            }
            previous.addServer(tagStructure);
            state.remove(this);
            previous.updateServers();
            state.add(previous);
        });
        pane.add(16, 80, p -> new GuiComponentText(p, 18, "IP:"));
        pane.add(16, 150, p -> new GuiComponentText(p, 18, "Port:"));
    }
}
