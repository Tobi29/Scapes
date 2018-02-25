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

import kotlinx.coroutines.experimental.CoroutineName
import kotlinx.coroutines.experimental.launch
import org.tobi29.scapes.connection.Account
import org.tobi29.scapes.engine.GameState
import org.tobi29.scapes.engine.gui.Gui
import org.tobi29.scapes.engine.gui.GuiStyle
import org.tobi29.io.filesystem.FilePath

class GuiGenerateAccount(state: GameState,
                         path: FilePath,
                         next: (Account) -> Gui,
                         style: GuiStyle) : GuiBusy(
        state, style) {
    init {
        setLabel("Creating account...")
        launch(state.engine.taskExecutor + CoroutineName("Generate-Account")) {
            val account = Account.generate(path)
            state.engine.guiStack.swap(this@GuiGenerateAccount, next(account))
        }
    }
}
