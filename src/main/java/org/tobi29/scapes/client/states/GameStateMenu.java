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

package org.tobi29.scapes.client.states;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.client.ClientAccount;
import org.tobi29.scapes.client.ScapesClient;
import org.tobi29.scapes.client.gui.GuiAccount;
import org.tobi29.scapes.client.gui.GuiMainMenu;
import org.tobi29.scapes.client.gui.GuiVersion;
import org.tobi29.scapes.client.states.scenes.SceneMenu;
import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.utils.io.filesystem.File;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.UUID;

public class GameStateMenu extends GameState {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(GameStateMenu.class);

    public GameStateMenu(ScapesEngine engine) {
        super(engine, new SceneMenu());
    }

    @Override
    public void dispose() {
        engine.getSounds().stopMusic();
    }

    @Override
    public void init() {
        ClientAccount account = ((ScapesClient) engine.getGame()).getAccount();
        try {
            File file = engine.getFiles().getFile("File:Account.properties");
            if (file.exists()) {
                Properties properties = new Properties();
                try (InputStream streamIn = file.read()) {
                    properties.load(streamIn);
                    account.setUUID(
                            UUID.fromString(properties.getProperty("UUID")));
                    account.setPassword(properties.getProperty("Password"));
                    account.setNickname(properties.getProperty("Nickname"));
                    if (account.isValid()) {
                        add(new GuiMainMenu(this));
                    } else {
                        add(new GuiAccount(this, new GuiMainMenu(this),
                                account));
                    }
                }
            } else {
                add(new GuiAccount(this, new GuiMainMenu(this), account));
            }
        } catch (IOException e) {
            LOGGER.error("Failed to read account file: {}", e.toString());
            add(new GuiAccount(this, new GuiMainMenu(this), account));
        }
        add(new GuiVersion());
    }

    @Override
    public boolean isMouseGrabbed() {
        return false;
    }

    @Override
    public boolean isThreaded() {
        return false;
    }

    @Override
    public boolean forceRender() {
        return false;
    }

    @Override
    public void stepComponent() {
    }
}
