/*
 * Copyright 2012-2015 Tobi29
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

package org.tobi29.scapes.client.gui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.engine.server.Account;
import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.gui.*;
import org.tobi29.scapes.engine.utils.Pair;
import org.tobi29.scapes.engine.utils.io.ChecksumUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.Optional;
import java.util.regex.Pattern;

public class GuiAccount extends GuiMenu {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(GuiAccount.class);
    private static final Pattern REPLACE = Pattern.compile("[^A-Za-z0-9+/= ]");
    private KeyPair keyPair;
    private String nickname = "";

    @SuppressWarnings("unchecked")
    public GuiAccount(GameState state, Gui previous) {
        super(state, "Account", "Save");
        try {
            Account account = Account
                    .read(state.engine().home().resolve("Account.properties"));
            keyPair = account.keyPair();
            nickname = account.nickname();
        } catch (IOException e) {
            LOGGER.error("Failed to read account file: {}", e.toString());
        }
        new GuiComponentText(pane, 16, 80, 18, "Key:");
        GuiComponentButton keyCopy =
                new GuiComponentTextButton(pane, 16, 100, 174, 30, 18, "Copy");
        GuiComponentButton keyPaste =
                new GuiComponentTextButton(pane, 210, 100, 174, 30, 18,
                        "Paste");
        GuiComponentText hash = new GuiComponentText(pane, 16, 140, 12,
                "Hash: " + ChecksumUtil
                        .getChecksum(keyPair.getPrivate().getEncoded(),
                                ChecksumUtil.Algorithm.SHA1));
        GuiComponentText id = new GuiComponentText(pane, 16, 160, 12, "ID: " +
                ChecksumUtil.getChecksum(keyPair.getPublic().getEncoded(),
                        ChecksumUtil.Algorithm.SHA1));
        new GuiComponentText(pane, 16, 180, 18, "Nickname:");
        GuiComponentTextField nickname =
                new GuiComponentTextField(pane, 16, 200, 368, 30, 18,
                        this.nickname);
        GuiComponentTextButton skin =
                new GuiComponentTextButton(pane, 112, 260, 176, 30, 18, "Skin");
        GuiComponentText error = new GuiComponentText(pane, 16, 320, 18, "");

        keyCopy.addLeftClick(event -> state.engine().controller()
                .clipboardCopy(Account.key(keyPair)));
        keyPaste.addLeftClick(event -> {
            String str = state.engine().controller().clipboardPaste();
            Optional<KeyPair> keyPair =
                    Account
                            .key(REPLACE.matcher(str).replaceAll(""));
            if (keyPair.isPresent()) {
                this.keyPair = keyPair.get();
                hash.setText("Hash: " + ChecksumUtil
                        .getChecksum(this.keyPair.getPrivate().getEncoded()));
                id.setText("ID: " + ChecksumUtil
                        .getChecksum(this.keyPair.getPublic().getEncoded()));
                error.setText("");
            } else {
                error.setText("Invalid key!");
            }
        });
        skin.addLeftClick(event -> {
            try {
                Path path = state.engine().home().resolve("Skin.png");
                Path[] imports = state.engine().container().openFileDialog(
                        new Pair[]{new Pair<>("*.png", "PNG Picture")},
                        "Import skin", false);
                for (Path copy : imports) {
                    Files.copy(copy, path);
                }
            } catch (IOException e) {
                LOGGER.warn("Failed to import skin: {}", e.toString());
            }
        });
        back.addLeftClick(event -> {
            this.nickname = nickname.text();
            if (!Account.valid(this.nickname)) {
                error.setText("Invalid Nickname!");
                return;
            }
            try {
                new Account(keyPair, this.nickname)
                        .write(state.engine().home()
                                .resolve("Account.properties"));
            } catch (IOException e) {
                LOGGER.error("Failed to write account file: {}", e.toString());
            }
            state.remove(this);
            state.add(previous);
        });
    }
}
