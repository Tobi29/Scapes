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

package org.tobi29.scapes.client.gui.touch;

import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.gui.GuiComponentText;
import org.tobi29.scapes.engine.gui.GuiComponentTextField;
import org.tobi29.scapes.engine.gui.GuiEvent;
import org.tobi29.scapes.engine.gui.GuiStyle;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;

public class GuiTouchAddServer extends GuiTouchMenuDouble {
    public GuiTouchAddServer(GameState state, GuiTouchServerSelect previous,
            GuiStyle style) {
        super(state, "Add Server", previous, style);
        pane.addVert(112, 10, -1, 36, p -> new GuiComponentText(p, "IP:"));
        GuiComponentTextField ip =
                row(pane, p -> new GuiComponentTextField(p, 36, ""));
        pane.addVert(112, 10, -1, 36, p -> new GuiComponentText(p, "Port:"));
        GuiComponentTextField port =
                row(pane, p -> new GuiComponentTextField(p, 36, "12345"));

        selection(ip);
        selection(port);

        save.on(GuiEvent.CLICK_LEFT, event -> {
            TagStructure tagStructure = new TagStructure();
            tagStructure.setString("Address", ip.text());
            try {
                tagStructure.setInteger("Port", Integer.valueOf(port.text()));
            } catch (NumberFormatException e) {
                tagStructure.setInteger("Port", 12345);
            }
            previous.addServer(tagStructure);
            previous.updateServers();
            state.engine().guiStack().swap(this, previous);
        });
    }
}
