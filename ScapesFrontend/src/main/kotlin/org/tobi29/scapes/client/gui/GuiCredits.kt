/*
 * Copyright 2012-2018 Tobi29
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

import kotlinx.coroutines.experimental.launch
import org.tobi29.io.IOException
import org.tobi29.io.asString
import org.tobi29.logging.KLogging
import org.tobi29.scapes.engine.GameState
import org.tobi29.scapes.engine.gui.*

class GuiCredits(
    state: GameState,
    previous: Gui,
    style: GuiStyle
) : GuiDesktop(
    state, style
) {

    init {
        addHori(0.0, 0.0, 120.0, -1.0) { GuiComponentGroup(it) }
        val credits = addHori(0.0, 0.0, -1.0, 18.0) {
            GuiComponentCredits(
                it
            )
        }
        launch(engine.taskExecutor) {
            credits.text = try {
                state.engine.files["Scapes:Readme.txt"].readAsync { it.asString() }
            } catch (e: IOException) {
                logger.error { "Error reading Readme.txt: $e" }
                "Failed to load credits"
            }
        }
        val pane = addHori(0.0, 0.0, 96.0, -1.0) { GuiComponentVisiblePane(it) }
        val back = pane.addVert(13.0, 64.0, -1.0, 30.0) {
            button(it, "Back")
        }

        on(GuiAction.BACK) {
            engine.sounds.stop("music.Credits")
            engine.guiStack.swap(this, previous)
        }
        back.on(GuiEvent.CLICK_LEFT) { fireAction(GuiAction.BACK) }

        engine.sounds.stop("music")
        engine.sounds.playMusic(
            "ScapesFrontend:sound/Credits.ogg",
            "music.Credits", true, 1.0, 1.0
        )
    }

    companion object : KLogging()
}
