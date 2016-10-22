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

package org.tobi29.scapes.client.states

import mu.KLogging
import org.tobi29.scapes.client.gui.GuiVersion
import org.tobi29.scapes.client.gui.desktop.GuiAccount
import org.tobi29.scapes.client.gui.desktop.GuiGenerateAccount
import org.tobi29.scapes.client.gui.desktop.GuiMainMenu
import org.tobi29.scapes.client.gui.touch.GuiTouchAccount
import org.tobi29.scapes.client.gui.touch.GuiTouchGenerateAccount
import org.tobi29.scapes.client.gui.touch.GuiTouchMainMenu
import org.tobi29.scapes.client.states.scenes.SceneMenu
import org.tobi29.scapes.connection.Account
import org.tobi29.scapes.engine.Container
import org.tobi29.scapes.engine.GameState
import org.tobi29.scapes.engine.ScapesEngine
import org.tobi29.scapes.engine.gui.Gui
import org.tobi29.scapes.engine.gui.GuiStyle
import org.tobi29.scapes.engine.utils.io.filesystem.FilePath
import java.io.IOException

class GameStateMenu private constructor(engine: ScapesEngine, private val scene2: SceneMenu) : GameState(
        engine, scene2) {
    constructor(engine: ScapesEngine) : this(engine, SceneMenu(engine)) {
    }

    override fun dispose() {
        engine.sounds.stop("music")
    }

    override fun init() {
        val style = engine.guiStyle
        engine.guiStack.addUnfocused("00-Version", GuiVersion(this, style))
        val file = engine.home.resolve("Account.properties")
        val account = try {
            Account.read(file)
        } catch (e: IOException) {
            logger.error { "Failed to read account file: $e" }
            null
        }
        val formFactor = engine.container.formFactor()
        engine.guiStack.add("10-Menu", menu(account, file, style, formFactor))
    }

    override val isMouseGrabbed: Boolean
        get() = false

    override fun step(delta: Double) {
    }

    private fun menu(account: Account?,
                     path: FilePath,
                     style: GuiStyle,
                     formFactor: Container.FormFactor): Gui {
        if (account == null) {
            when (formFactor) {
                Container.FormFactor.PHONE -> return GuiTouchGenerateAccount(
                        this, path,
                        { newAccount -> menu(newAccount, style, formFactor) },
                        style)
                else -> return GuiGenerateAccount(this, path,
                        { newAccount -> menu(newAccount, style, formFactor) },
                        style)
            }
        }
        return menu(account, style, formFactor)
    }

    private fun menu(account: Account,
                     style: GuiStyle,
                     formFactor: Container.FormFactor): Gui {
        if (!account.valid()) {
            when (formFactor) {
                Container.FormFactor.PHONE -> return GuiTouchAccount(this,
                        menu(style, formFactor),
                        account, style)
                else -> return GuiAccount(this, menu(style, formFactor),
                        account, style)
            }
        }
        return menu(style, formFactor)
    }

    private fun menu(style: GuiStyle,
                     formFactor: Container.FormFactor): Gui {
        when (formFactor) {
            Container.FormFactor.PHONE -> return GuiTouchMainMenu(this, scene2,
                    style)
            else -> return GuiMainMenu(this, scene2, style)
        }
    }

    companion object : KLogging()
}
