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

import org.tobi29.scapes.engine.GameState
import org.tobi29.scapes.engine.gui.GuiComponentText
import org.tobi29.scapes.engine.gui.GuiComponentTextField
import org.tobi29.scapes.engine.gui.GuiEvent
import org.tobi29.scapes.engine.gui.GuiStyle
import org.tobi29.scapes.engine.server.RemoteAddress

class GuiAddServer(state: GameState,
                   previous: GuiServerSelect,
                   style: GuiStyle) : GuiMenuDouble(state, "Add Server",
        previous, style) {
    init {
        pane.addVert(16.0, 5.0, -1.0, 18.0) { GuiComponentText(it, "IP:") }
        val ip = row(pane) {
            it.selectable = true
            GuiComponentTextField(it, 18, "")
        }
        pane.addVert(16.0, 5.0, -1.0, 18.0) { GuiComponentText(it, "Port:") }
        val port = row(pane) {
            it.selectable = true
            GuiComponentTextField(it, 18, "12345")
        }

        save.on(GuiEvent.CLICK_LEFT) {
            val portInt = try {
                port.text().toInt()
            } catch (e: NumberFormatException) {
                12345
            }
            val address = RemoteAddress(ip.text(), portInt)
            previous.addServer(address.toTag())
            previous.updateServers()
            state.engine.guiStack.swap(this, previous)
        }
    }
}
