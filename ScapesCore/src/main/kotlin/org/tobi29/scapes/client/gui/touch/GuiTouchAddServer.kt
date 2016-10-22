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

package org.tobi29.scapes.client.gui.touch

import org.tobi29.scapes.engine.GameState
import org.tobi29.scapes.engine.gui.GuiComponentText
import org.tobi29.scapes.engine.gui.GuiComponentTextField
import org.tobi29.scapes.engine.gui.GuiEvent
import org.tobi29.scapes.engine.gui.GuiStyle
import org.tobi29.scapes.engine.utils.io.tag.TagStructure
import org.tobi29.scapes.engine.utils.io.tag.setInt

class GuiTouchAddServer(state: GameState, previous: GuiTouchServerSelect,
                        style: GuiStyle) : GuiTouchMenuDouble(state,
        "Add Server", previous, style) {
    init {
        pane.addVert(112.0, 10.0, -1.0, 36.0) {
            GuiComponentText(it, "IP:")
        }
        val ip = row(pane) { GuiComponentTextField(it, 36, "") }
        pane.addVert(112.0, 10.0, -1.0, 36.0) {
            GuiComponentText(it, "Port:")
        }
        val port = row(pane) { GuiComponentTextField(it, 36, "12345") }

        selection(ip)
        selection(port)

        save.on(GuiEvent.CLICK_LEFT) { event ->
            val tagStructure = TagStructure()
            tagStructure.setString("Address", ip.text())
            try {
                tagStructure.setInt("Port", port.text().toInt())
            } catch (e: NumberFormatException) {
                tagStructure.setInt("Port", 12345)
            }

            previous.addServer(tagStructure)
            previous.updateServers()
            state.engine.guiStack.swap(this, previous)
        }
    }
}
