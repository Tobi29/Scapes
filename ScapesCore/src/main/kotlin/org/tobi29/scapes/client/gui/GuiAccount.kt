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

import mu.KLogging
import org.tobi29.scapes.connection.Account
import org.tobi29.scapes.engine.GameState
import org.tobi29.scapes.engine.gui.*
import org.tobi29.scapes.engine.input.FileType
import org.tobi29.scapes.engine.utils.io.Algorithm
import org.tobi29.scapes.engine.utils.io.checksum
import org.tobi29.scapes.engine.utils.io.filesystem.write
import org.tobi29.scapes.engine.utils.io.process
import org.tobi29.scapes.engine.utils.io.put
import java.io.IOException
import java.security.KeyPair

class GuiAccount(state: GameState,
                 previous: Gui,
                 account: Account,
                 style: GuiStyle) : GuiMenu(state, "Account", "Save", style) {
    private var keyPair: KeyPair
    private var nickname = ""

    init {
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
        val nickname = row(pane) {
            GuiComponentTextField(it, 18, this.nickname)
        }
        val skin = rowCenter(pane) { button(it, "Skin") }
        val error = pane.addVert(16.0, 5.0, -1.0, 18.0) {
            GuiComponentText(it, "")
        }

        selection(keyCopy, keyPaste, keyCopyID)
        selection(nickname)
        selection(skin)

        keyCopy.on(GuiEvent.CLICK_LEFT) { event ->
            state.engine.container.clipboardCopy(Account.key(keyPair))
        }
        keyPaste.on(GuiEvent.CLICK_LEFT) { event ->
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
        keyCopyID.on(GuiEvent.CLICK_LEFT
        ) { event ->
            state.engine.container.clipboardCopy(
                    checksum(keyPair.public.encoded,
                            Algorithm.SHA1).toString())
        }
        skin.on(GuiEvent.CLICK_LEFT) { event ->
            try {
                val path = state.engine.home.resolve("Skin.png")
                state.engine.container.openFileDialog(FileType.IMAGE,
                        "Import skin", false
                ) { name, input -> write(path) { process(input, put(it)) } }
            } catch (e: IOException) {
                logger.warn { "Failed to import skin: $e" }
            }
        }
        on(GuiAction.BACK) {
            this.nickname = nickname.text()
            if (!Account.valid(this.nickname)) {
                error.text = "Invalid Nickname!"
                return@on
            }
            try {
                Account(keyPair, this.nickname).write(
                        state.engine.home.resolve("Account.properties"))
            } catch (e: IOException) {
                logger.error { "Failed to write account file: $e" }
            }

            state.engine.guiStack.swap(this, previous)
        }
    }

    companion object : KLogging() {
        private val REPLACE = "[^A-Za-z0-9+/= ]".toRegex()
    }
}
