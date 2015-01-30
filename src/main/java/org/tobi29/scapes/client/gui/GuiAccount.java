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
import org.tobi29.scapes.client.ClientAccount;
import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.gui.*;
import org.tobi29.scapes.engine.utils.io.filesystem.File;
import org.tobi29.scapes.engine.utils.platform.PlatformDialogs;

import java.io.IOException;
import java.util.Properties;
import java.util.UUID;

public class GuiAccount extends Gui {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(GuiAccount.class);
    private final GuiComponentVisiblePane pane;
    private boolean failed;

    public GuiAccount(GameState state, Gui prev, ClientAccount account) {
        super(GuiAlignment.CENTER);
        pane = new GuiComponentVisiblePane(200, 0, 400, 512);
        GuiComponentTextField uuid =
                new GuiComponentTextField(16, 100, 368, 30, 18,
                        account.getUUID().toString());
        GuiComponentTextField password =
                new GuiComponentTextField(16, 160, 368, 30, 18,
                        account.getPassword(), true);
        GuiComponentTextField nickname =
                new GuiComponentTextField(16, 220, 368, 30, 18,
                        account.getNickname());
        GuiComponentTextButton skin =
                new GuiComponentTextButton(112, 260, 176, 30, 18, "Skin");
        skin.addLeftClick(event -> {
            try {
                File file =
                        state.getEngine().getFiles().getFile("File:Skin.png");
                file.importFromUser(new PlatformDialogs.Extension[]{
                                new PlatformDialogs.Extension("*.png",
                                        "PNG Picture")}, "Import skin",
                        state.getEngine().getGraphics().getContainer());
            } catch (IOException e) {
                LOGGER.warn("Failed to import skin: {}", e.toString());
            }
        });
        GuiComponentTextButton save =
                new GuiComponentTextButton(112, 466, 176, 30, 18, "Save");
        save.addLeftClick(event -> {
            try {
                account.setUUID(UUID.fromString(uuid.getText()));
                account.setPassword(password.getText());
                account.setNickname(nickname.getText());
                if (account.isValid()) {
                    try {
                        File file = state.getEngine().getFiles()
                                .getFile("File:Account.properties");
                        Properties properties = new Properties();
                        properties.setProperty("UUID",
                                account.getUUID().toString());
                        properties
                                .setProperty("Password", account.getPassword());
                        properties
                                .setProperty("Nickname", account.getNickname());
                        file.write(
                                streamOut -> properties.store(streamOut, ""));
                    } catch (IOException e) {
                        LOGGER.error("Failed to write account file: {}",
                                e.toString());
                    }
                    state.remove(this);
                    state.add(prev);
                } else if (!failed) {
                    failSave();
                }
            } catch (IllegalArgumentException e) {
                failSave();
            }
        });
        pane.add(new GuiComponentText(16, 16, 32, "Login"));
        pane.add(new GuiComponentSeparator(24, 64, 352, 2));
        pane.add(uuid);
        pane.add(new GuiComponentText(16, 80, 18, "UUID:"));
        pane.add(password);
        pane.add(new GuiComponentText(16, 140, 18, "Password:"));
        pane.add(nickname);
        pane.add(new GuiComponentText(16, 200, 18, "Nickname:"));
        pane.add(skin);
        pane.add(new GuiComponentSeparator(24, 448, 352, 2));
        pane.add(save);
        add(pane);
    }

    private void failSave() {
        if (!failed) {
            pane.add(new GuiComponentText(16, 320, 18, "Invalid account!"));
            failed = true;
        }
    }
}
