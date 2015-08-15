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

import org.tobi29.scapes.client.gui.GuiDisconnected;
import org.tobi29.scapes.client.states.scenes.SceneError;
import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.opengl.GL;

import java.net.InetSocketAddress;
import java.util.Optional;

public class GameStateServerDisconnect extends GameState {
    private final Optional<InetSocketAddress> address;
    private final GuiDisconnected gui;
    private double reconnectTimer = 5.0;

    public GameStateServerDisconnect(String message, ScapesEngine engine) {
        this(message, Optional.empty(), engine);
    }

    public GameStateServerDisconnect(String message, InetSocketAddress address,
            ScapesEngine engine) {
        this(message, Optional.of(address), engine);
    }

    public GameStateServerDisconnect(String message,
            Optional<InetSocketAddress> address, ScapesEngine engine) {
        super(engine, new SceneError());
        this.address = address;
        gui = new GuiDisconnected(this, message);
        add(gui);
    }

    @Override
    public void dispose(GL gl) {
    }

    @Override
    public void init(GL gl) {
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
        address.ifPresent(address -> {
            reconnectTimer -= delta;
            if (reconnectTimer <= 0.0) {
                engine.setState(new GameStateLoadMP(address, engine,
                        (SceneError) scene));
            } else {
                gui.setReconnectTimer(reconnectTimer);
            }
        });
    }
}
