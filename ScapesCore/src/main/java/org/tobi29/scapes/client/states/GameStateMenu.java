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

package org.tobi29.scapes.client.states;

import java8.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.client.gui.GuiVersion;
import org.tobi29.scapes.client.gui.desktop.GuiAccount;
import org.tobi29.scapes.client.gui.desktop.GuiGenerateAccount;
import org.tobi29.scapes.client.gui.desktop.GuiMainMenu;
import org.tobi29.scapes.client.gui.touch.GuiTouchAccount;
import org.tobi29.scapes.client.gui.touch.GuiTouchGenerateAccount;
import org.tobi29.scapes.client.gui.touch.GuiTouchMainMenu;
import org.tobi29.scapes.client.states.scenes.SceneMenu;
import org.tobi29.scapes.engine.Container;
import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.gui.Gui;
import org.tobi29.scapes.engine.gui.GuiStyle;
import org.tobi29.scapes.engine.server.Account;
import org.tobi29.scapes.engine.utils.io.filesystem.FilePath;

import java.io.IOException;

public class GameStateMenu extends GameState {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(GameStateMenu.class);
    private final SceneMenu scene;

    public GameStateMenu(ScapesEngine engine) {
        this(engine, new SceneMenu(engine));
    }

    private GameStateMenu(ScapesEngine engine, SceneMenu scene) {
        super(engine, scene);
        this.scene = scene;
    }

    @Override
    public void dispose() {
        engine.sounds().stop("music");
    }

    @Override
    public void init() {
        GuiStyle style = engine.guiStyle();
        engine.guiStack()
                .addUnfocused("00-Version", new GuiVersion(this, style));
        Optional<Account> account;
        FilePath file = engine.home().resolve("Account.properties");
        try {
            account = Account.read(file);
        } catch (IOException e) {
            LOGGER.error("Failed to read account file: {}", e.toString());
            account = Optional.empty();
        }
        Container.FormFactor formFactor = engine.container().formFactor();
        engine.guiStack()
                .add("10-Menu", menu(account, file, style, formFactor));
    }

    @Override
    public boolean isMouseGrabbed() {
        return false;
    }

    @Override
    public void step(double delta) {
    }

    private Gui menu(Optional<Account> account, FilePath path, GuiStyle style,
            Container.FormFactor formFactor) {
        if (!account.isPresent()) {
            switch (formFactor) {
                case PHONE:
                    return new GuiTouchGenerateAccount(this, path,
                            newAccount -> menu(newAccount, style, formFactor),
                            style);
                default:
                    return new GuiGenerateAccount(this, path,
                            newAccount -> menu(newAccount, style, formFactor),
                            style);
            }
        }
        return menu(account.get(), style, formFactor);
    }

    private Gui menu(Account account, GuiStyle style,
            Container.FormFactor formFactor) {
        if (!account.valid()) {
            switch (formFactor) {
                case PHONE:
                    return new GuiTouchAccount(this, menu(style, formFactor),
                            account, style);
                default:
                    return new GuiAccount(this, menu(style, formFactor),
                            account, style);
            }
        }
        return menu(style, formFactor);
    }

    private Gui menu(GuiStyle style, Container.FormFactor formFactor) {
        switch (formFactor) {
            case PHONE:
                return new GuiTouchMainMenu(this, scene, style);
            default:
                return new GuiMainMenu(this, scene, style);
        }
    }
}
