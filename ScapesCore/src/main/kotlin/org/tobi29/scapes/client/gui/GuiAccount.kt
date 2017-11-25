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

import org.tobi29.scapes.client.DialogProvider
import org.tobi29.scapes.client.ScapesClient
import org.tobi29.scapes.connection.Account
import org.tobi29.scapes.engine.GameState
import org.tobi29.scapes.engine.gui.*
import org.tobi29.scapes.engine.utils.Algorithm
import org.tobi29.scapes.engine.utils.io.IOException
import org.tobi29.scapes.engine.utils.io.checksum
import org.tobi29.scapes.engine.utils.io.filesystem.write
import org.tobi29.scapes.engine.utils.io.process
import org.tobi29.scapes.engine.utils.logging.KLogging
import java.security.KeyPair

class GuiAccount(state: GameState,
                 previous: Gui,
                 account: Account,
                 style: GuiStyle) : GuiMenu(state, "Account", style) {
    private var keyPair: KeyPair
    private var nickname = ""

    init {
        val scapes = engine[ScapesClient.COMPONENT]
        keyPair = account.keyPair()
        nickname = account.nickname()
        pane.addVert(16.0, 5.0, -1.0, 18.0) { GuiComponentText(it, "Key:") }
        val slab = row(pane)
        val keyCopy = slab.addHori(5.0, 5.0, -1.0, -1.0) {
            button(it, "Copy")
        }
        val keyPaste = slab.addHori(5.0, 5.0, -1.0, -1.0) {
            button(it, "Paste")
        }
        val keyCopyID = slab.addHori(5.0, 5.0, -1.0, -1.0) {
            button(it, "Copy ID")
        }
        val id = pane.addVert(16.0, 5.0, -1.0, 12.0) {
            GuiComponentText(it,
                    "ID: ${checksum(keyPair.public.encoded, Algorithm.SHA1)}")
        }
        pane.addVert(16.0, 5.0, -1.0, 18.0) {
            GuiComponentText(it, "Nickname:")
        }
        val nicknameRow = row(pane)
        val nickname = nicknameRow.addHori(5.0, 5.0, -1.0, -1.0) {
            it.selectable = true
            GuiComponentTextField(it, 18, this.nickname)
        }
        val nicknameHelp = nicknameRow.addHori(5.0, 5.0, 30.0, -1.0) {
            GuiComponentTextButton(it, 18, "?")
        }
        val skin = rowCenter(pane) { button(it, "Skin") }
        val error = pane.addVert(16.0, 5.0, -1.0, 18.0) {
            GuiComponentText(it, "")
        }

        keyCopy.on(GuiEvent.CLICK_LEFT) {
            state.engine.container.clipboardCopy(Account.key(keyPair))
        }
        keyPaste.on(GuiEvent.CLICK_LEFT) {
            val str = state.engine.container.clipboardPaste()
            val keyPair = Account.key(str.replace(REPLACE, ""))
            if (keyPair != null) {
                this.keyPair = keyPair
                id.text = "ID: ${checksum(keyPair.public.encoded,
                        Algorithm.SHA1)}"
                error.text = ""
            } else {
                error.text = "Invalid key!"
            }
        }
        keyCopyID.on(GuiEvent.CLICK_LEFT) {
            state.engine.container.clipboardCopy(
                    checksum(keyPair.public.encoded,
                            Algorithm.SHA1).toString())
        }
        nicknameHelp.on(GuiEvent.CLICK_LEFT) {
            state.engine.guiStack.swap(this,
                    GuiMessage(state, this, "Nickname", """
The nickname is displayed as your name
on servers, it can be changed at any time
if needed, without changing your identity.

A valid nickname must be between
6 and 20 characters long and may only
consist of letters and digits.
""", style))
        }
        skin.on(GuiEvent.CLICK_LEFT) {
            try {
                val path = scapes.home.resolve("Skin.png")
                state.engine[DialogProvider.COMPONENT].openSkinDialog { _, stream ->
                    write(path) { output -> stream.process { output.put(it) } }
                }
            } catch (e: IOException) {
                logger.warn { "Failed to import skin: $e" }
            }
        }

        val save = addControl { button(it, "Save") }
        save.on(GuiEvent.CLICK_LEFT) {
            this.nickname = nickname.text
            Account.isNameValid(this.nickname)?.let { invalid ->
                error.text = "Invalid Nickname:\n$invalid"
                return@on
            }
            try {
                Account(keyPair, this.nickname).write(
                        scapes.home.resolve("Account.properties"))
            } catch (e: IOException) {
                logger.error { "Failed to write account file: $e" }
            }

            state.engine.guiStack.swap(this, previous)
        }
        if (account.valid()) {
            val back = addControl { button(it, "Back") }
            back.on(GuiEvent.CLICK_LEFT) { fireAction(GuiAction.BACK) }
            on(GuiAction.BACK) { state.engine.guiStack.swap(this, previous) }
        }
    }

    companion object : KLogging() {
        private val REPLACE = "[^A-Za-z0-9+/= ]".toRegex()
    }
}
