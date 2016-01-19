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
package org.tobi29.scapes.client.gui.touch;

import java8.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.gui.*;
import org.tobi29.scapes.engine.input.FileType;
import org.tobi29.scapes.engine.server.Account;
import org.tobi29.scapes.engine.utils.io.ChecksumUtil;
import org.tobi29.scapes.engine.utils.io.ProcessStream;
import org.tobi29.scapes.engine.utils.io.filesystem.FilePath;
import org.tobi29.scapes.engine.utils.io.filesystem.FileUtil;

import java.io.IOException;
import java.security.KeyPair;
import java.util.regex.Pattern;

public class GuiTouchAccount extends GuiTouchMenu {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(GuiTouchAccount.class);
    private static final Pattern REPLACE = Pattern.compile("[^A-Za-z0-9+/= ]");
    private KeyPair keyPair;
    private String nickname = "";

    public GuiTouchAccount(GameState state, Gui previous, GuiStyle style) {
        super(state, "Account", "Save", style);
        try {
            Account account = Account.read(
                    state.engine().home().resolve("Account.properties"));
            keyPair = account.keyPair();
            nickname = account.nickname();
        } catch (IOException e) {
            LOGGER.error("Failed to read account file: {}", e.toString());
        }
        GuiComponentGroupSlab slab = row(pane);
        slab.addHori(10, 10, -1, 36, p -> new GuiComponentFlowText(p, "Key:"));
        GuiComponentButton keyCopy =
                slab.addHori(10, 10, -1, -1, p -> button(p, "Copy"));
        GuiComponentButton keyPaste =
                slab.addHori(10, 10, -1, -1, p -> button(p, "Paste"));
        GuiComponentButton keyCopyID =
                slab.addHori(10, 10, -1, -1, p -> button(p, "Copy ID"));
        GuiComponentText id = pane.addVert(112, 5, -1, 24,
                p -> new GuiComponentText(p, "ID: " + ChecksumUtil
                        .checksum(keyPair.getPublic().getEncoded(),
                                ChecksumUtil.Algorithm.SHA1)));
        slab = row(pane);
        slab.addHori(10, 10, -1, 36,
                p -> new GuiComponentFlowText(p, "Nickname:"));
        GuiComponentTextField nickname = slab.addHori(10, 10, -1, 60,
                p -> new GuiComponentTextField(p, 36, this.nickname));
        GuiComponentTextButton skin = row(pane, p -> button(p, "Skin"));
        GuiComponentText error =
                pane.addVert(112, 10, -1, 36, p -> new GuiComponentText(p, ""));

        keyCopy.onClickLeft(event -> state.engine().container()
                .clipboardCopy(Account.key(keyPair)));
        keyPaste.onClickLeft(event -> {
            String str = state.engine().container().clipboardPaste();
            Optional<KeyPair> keyPair =
                    Account.key(REPLACE.matcher(str).replaceAll(""));
            if (keyPair.isPresent()) {
                this.keyPair = keyPair.get();
                id.setText("ID: " + ChecksumUtil
                        .checksum(this.keyPair.getPublic().getEncoded(),
                                ChecksumUtil.Algorithm.SHA1));
                error.setText("");
            } else {
                error.setText("Invalid key!");
            }
        });
        keyCopyID.onClickLeft(event -> state.engine().container().clipboardCopy(
                ChecksumUtil.checksum(keyPair.getPublic().getEncoded(),
                        ChecksumUtil.Algorithm.SHA1).toString()));
        skin.onClickLeft(event -> {
            try {
                FilePath path = state.engine().home().resolve("Skin.png");
                state.engine().container()
                        .openFileDialog(FileType.IMAGE, "Import skin", false,
                                (name, input) -> FileUtil.write(path,
                                        output -> ProcessStream
                                                .process(input, output::put)));
            } catch (IOException e) {
                LOGGER.warn("Failed to import skin: {}", e.toString());
            }
        });
        back.onClickLeft(event -> {
            this.nickname = nickname.text();
            if (!Account.valid(this.nickname)) {
                error.setText("Invalid Nickname!");
                return;
            }
            try {
                new Account(keyPair, this.nickname).write(state.engine().home()
                        .resolve("Account.properties"));
            } catch (IOException e) {
                LOGGER.error("Failed to write account file: {}", e.toString());
            }
            state.engine().guiStack().add("10-Menu", previous);
        });
    }
}