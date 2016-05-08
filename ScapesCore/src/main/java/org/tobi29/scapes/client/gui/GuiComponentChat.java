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

import org.tobi29.scapes.client.ChatHistory;
import org.tobi29.scapes.engine.gui.GuiComponent;
import org.tobi29.scapes.engine.gui.GuiLayoutData;
import org.tobi29.scapes.engine.gui.GuiRenderer;
import org.tobi29.scapes.engine.opengl.FontRenderer;
import org.tobi29.scapes.engine.utils.math.vector.Vector2;

import java.util.Iterator;

public class GuiComponentChat extends GuiComponent {
    private final ChatHistory chatHistory;

    public GuiComponentChat(GuiLayoutData parent, ChatHistory chatHistory) {
        super(parent);
        this.chatHistory = chatHistory;
        chatHistory.listener(this, this::dirty);
    }

    @Override
    public boolean ignoresEvents() {
        return true;
    }

    @Override
    protected void updateMesh(GuiRenderer renderer, Vector2 size) {
        int yy = -16;
        Iterator<String> iterator = chatHistory.lines().iterator();
        while (iterator.hasNext()) {
            String line = iterator.next();
            gui.style().font().render(FontRenderer
                            .to(renderer, 0.0f, yy, 1.0f, 1.0f, 1.0f, 1.0f), line,
                    16.0f);
            yy -= 20;
        }
    }
}
