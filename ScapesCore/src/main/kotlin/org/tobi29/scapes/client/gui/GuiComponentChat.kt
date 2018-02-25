/*
 * Copyright 2012-2017 Tobi29
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

package org.tobi29.scapes.client.gui

import org.tobi29.scapes.client.ChatChangeEvent
import org.tobi29.scapes.client.ChatHistory
import org.tobi29.scapes.engine.graphics.FontRenderer
import org.tobi29.scapes.engine.gui.GuiComponent
import org.tobi29.scapes.engine.gui.GuiLayoutData
import org.tobi29.scapes.engine.gui.GuiRenderer
import org.tobi29.math.vector.Vector2d
import org.tobi29.utils.ListenerRegistrar

class GuiComponentChat(parent: GuiLayoutData,
                       private val chatHistory: ChatHistory) : GuiComponent(
        parent) {

    override fun ListenerRegistrar.listeners() {
        listen<ChatChangeEvent>({ it.chatHistory == chatHistory }) {
            dirty()
        }
    }

    override fun ignoresEvents(): Boolean {
        return true
    }

    override fun updateMesh(renderer: GuiRenderer,
                            size: Vector2d) {
        var yy = -16.0
        chatHistory.lines { line ->
            gui.style.font.render(
                    FontRenderer.to(renderer, 0.0, yy, 1.0, 1.0, 1.0, 1.0),
                    line, 16.0)
            yy -= 20.0
        }
    }
}
