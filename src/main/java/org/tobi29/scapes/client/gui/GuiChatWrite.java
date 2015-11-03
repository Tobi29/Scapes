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

import org.tobi29.scapes.client.states.GameStateGameMP;
import org.tobi29.scapes.engine.gui.Gui;
import org.tobi29.scapes.engine.gui.GuiAlignment;
import org.tobi29.scapes.engine.gui.GuiComponentTextField;
import org.tobi29.scapes.engine.gui.GuiStyle;
import org.tobi29.scapes.engine.input.ControllerKey;
import org.tobi29.scapes.entity.client.MobPlayerClientMain;
import org.tobi29.scapes.packets.PacketChat;

public class GuiChatWrite extends Gui {
    private final GuiComponentTextField write;
    private final GameStateGameMP state;
    private final MobPlayerClientMain player;

    public GuiChatWrite(GameStateGameMP state, MobPlayerClientMain player,
            GuiStyle style) {
        super(style, GuiAlignment.LEFT);
        this.state = state;
        this.player = player;
        write = add(12, 480,
                p -> new GuiComponentTextField(p, 600, 30, 16, "", 64, false,
                        true));
        add(8, 416, p -> new GuiComponentChat(p, state.chatHistory(), 0, 0));
    }

    @Override
    public void updateComponent() {
        if (state.engine().controller().isPressed(ControllerKey.KEY_ENTER)) {
            String text = write.text();
            if (!text.isEmpty()) {
                state.client().send(new PacketChat(text));
            }
            player.closeGui();
        }
    }
}
