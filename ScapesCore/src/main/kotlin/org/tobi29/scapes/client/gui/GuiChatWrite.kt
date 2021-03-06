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

import org.tobi29.scapes.client.states.GameStateGameMP
import org.tobi29.scapes.engine.gui.GuiAction
import org.tobi29.scapes.engine.gui.GuiComponentTextField
import org.tobi29.scapes.engine.gui.GuiStyle
import org.tobi29.scapes.entity.client.MobPlayerClientMain
import org.tobi29.scapes.packets.PacketChat

class GuiChatWrite(state: GameStateGameMP,
                   private val player: MobPlayerClientMain,
                   style: GuiStyle) : GuiDesktop(state, style) {
    private val write: GuiComponentTextField
    private val client = state.client

    init {
        write = add(12.0, 480.0, 600.0, 30.0) {
            GuiComponentTextField(it, 16, "", 64, false)
        }
        add(8.0, 416.0, -1.0, -1.0) {
            GuiComponentChat(it, state.chatHistory)
        }

        on(GuiAction.ACTIVATE) {
            val text = write.text
            if (!text.isEmpty()) {
                client.send(PacketChat(client.plugins.registry, text))
            }
            player.closeGui()
        }
        on(GuiAction.BACK) { player.closeGui() }

        currentSelection = write
    }
}
