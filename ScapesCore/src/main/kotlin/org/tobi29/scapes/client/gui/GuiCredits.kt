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
import org.tobi29.scapes.engine.gui.*
import org.tobi29.scapes.engine.utils.IOException
import org.tobi29.scapes.engine.utils.logging.KLogging

class GuiCredits(state: GameState,
                 previous: Gui,
                 style: GuiStyle) : GuiDesktop(
        state, style) {

    init {
        val credits = StringBuilder(200)
        try {
            state.engine.files["Scapes:Readme.txt"].get().reader().use { reader ->
                var line: String? = reader.readLine()
                while (line != null) {
                    credits.append(line).append('\n')
                    line = reader.readLine()
                }
            }
        } catch (e: IOException) {
            logger.error { "Error reading Readme.txt: $e" }
        }

        addHori(0.0, 0.0, 120.0, -1.0, ::GuiComponentGroup)
        addHori(0.0, 0.0, -1.0, 18.0
        ) { GuiComponentCredits(it, credits.toString()) }
        val pane = addHori(0.0, 0.0, 96.0, -1.0, ::GuiComponentVisiblePane)
        val back = pane.addVert(13.0, 64.0, -1.0, 30.0) {
            button(it, "Back")
        }

        selection(back)

        on(GuiAction.BACK) {
            engine.sounds.stop("music.Credits")
            engine.guiStack.swap(this, previous)
        }
        back.on(GuiEvent.CLICK_LEFT) { event -> fireAction(GuiAction.BACK) }

        engine.sounds.stop("music")
        engine.sounds.playMusic("Scapes:sound/Credits.ogg",
                "music.Credits", true, 1.0, 1.0)
    }

    companion object : KLogging()
}
