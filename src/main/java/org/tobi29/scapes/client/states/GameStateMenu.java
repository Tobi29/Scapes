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
import org.tobi29.scapes.client.gui.GuiAccount;
import org.tobi29.scapes.client.gui.GuiMainMenu;
import org.tobi29.scapes.client.gui.GuiVersion;
import org.tobi29.scapes.client.states.scenes.SceneMenu;
import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.gui.GuiStyle;
import org.tobi29.scapes.engine.opengl.GL;
import org.tobi29.scapes.engine.server.Account;

import java.io.IOException;
import java.nio.file.Path;

public class GameStateMenu extends GameState {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(GameStateMenu.class);
    private final SceneMenu scene;

    public GameStateMenu(ScapesEngine engine) {
        this(engine, new SceneMenu());
    }

    private GameStateMenu(ScapesEngine engine, SceneMenu scene) {
        super(engine, scene);
        this.scene = scene;
    }

    @Override
    public void dispose(GL gl) {
        engine.sounds().stop("music");
    }

    @Override
    public void init(GL gl) {
        GuiStyle style = engine.globalGUI().style();
        try {
            Path file = engine.home().resolve("Account.properties");
            Account account = Account.read(file);
            if (account.valid()) {
                add(new GuiMainMenu(this, scene, style));
            } else {
                account.write(file);
                add(new GuiAccount(this, new GuiMainMenu(this, scene, style),
                        style));
            }
        } catch (IOException e) {
            LOGGER.error("Failed to read account file: {}", e.toString());
            add(new GuiAccount(this, new GuiMainMenu(this, scene, style),
                    style));
        }
        add(new GuiVersion(style));
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
    public void stepComponent(double delta) {
    }
}
