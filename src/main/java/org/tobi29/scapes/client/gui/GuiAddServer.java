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
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;

public class GuiAddServer extends GuiMenuDouble {
    public GuiAddServer(GameState state, GuiServerSelect previous) {
        super(state, "Add Server", previous);
        GuiComponentTextField ip =
                new GuiComponentTextField(16, 110, 368, 30, 18, "");
        GuiComponentTextField port =
                new GuiComponentTextField(16, 180, 368, 30, 18, "12345");
        save.addLeftClick(event -> {
            TagStructure tagStructure = new TagStructure();
            tagStructure.setString("Address", ip.getText());
            try {
                tagStructure
                        .setInteger("Port", Integer.valueOf(port.getText()));
            } catch (NumberFormatException e) {
                tagStructure.setInteger("Port", 12345);
            }
            previous.addServer(tagStructure);
            state.remove(this);
            previous.updateServers();
            state.add(previous);
        });
        pane.add(new GuiComponentText(16, 80, 18, "IP:"));
        pane.add(ip);
        pane.add(new GuiComponentText(16, 150, 18, "Port:"));
        pane.add(port);
    }
}
