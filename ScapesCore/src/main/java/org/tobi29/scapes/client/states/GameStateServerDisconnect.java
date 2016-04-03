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

import java8.util.Optional;
import org.tobi29.scapes.client.gui.desktop.GuiDisconnected;
import org.tobi29.scapes.client.states.scenes.SceneError;
import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.server.RemoteAddress;

public class GameStateServerDisconnect extends GameState {
    private final Optional<RemoteAddress> address;
    private final GuiDisconnected gui;
    private double reconnectTimer = 5.0;

    public GameStateServerDisconnect(String message, ScapesEngine engine) {
        this(message, Optional.empty(), engine);
    }

    public GameStateServerDisconnect(String message, RemoteAddress address,
            ScapesEngine engine) {
        this(message, Optional.of(address), engine);
    }

    public GameStateServerDisconnect(String message,
            Optional<RemoteAddress> address, ScapesEngine engine) {
        super(engine, new SceneError());
        this.address = address;
        gui = new GuiDisconnected(this, message, engine.guiStyle());
    }

    @Override
    public void init() {
        engine.guiStack().add("10-Menu", gui);
    }

    @Override
    public boolean isMouseGrabbed() {
        return false;
    }

    @Override
    public void step(double delta) {
        address.ifPresent(address -> {
            reconnectTimer -= delta;
            if (reconnectTimer <= 0.0) {
                engine.setState(new GameStateLoadMP(address, engine, scene));
            } else {
                gui.setReconnectTimer(reconnectTimer);
            }
        });
    }
}
