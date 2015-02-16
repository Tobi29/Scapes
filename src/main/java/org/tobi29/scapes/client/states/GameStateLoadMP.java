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
import org.tobi29.scapes.client.ScapesClient;
import org.tobi29.scapes.client.connection.ClientConnection;
import org.tobi29.scapes.client.gui.GuiLoading;
import org.tobi29.scapes.client.states.scenes.SceneMenu;
import org.tobi29.scapes.connection.ConnectionCloseException;
import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.utils.math.FastMath;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

public class GameStateLoadMP extends GameState {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(GameStateLoadMP.class);
    private final InetSocketAddress address;
    private int step;
    private SocketChannel channel;
    private ClientConnection client;
    private GuiLoading progress;

    public GameStateLoadMP(InetSocketAddress address, ScapesEngine engine,
            SceneMenu scene) {
        super(engine, scene);
        this.address = address;
        scene.setSpeed(0.0f);
    }

    @Override
    public void dispose() {
    }

    @Override
    public void init() {
        progress = new GuiLoading();
        add(progress);
        progress.setLabel("Creating server...");
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
        return true;
    }

    @Override
    public void stepComponent(double delta) {
        try {
            switch (step) {
                case 0:
                    if (address.isUnresolved()) {
                        throw new IOException("Address unresolved");
                    }
                    channel = SocketChannel.open(address);
                    channel.configureBlocking(false);
                    step++;
                    break;
                case 1:
                    if (channel.finishConnect()) {
                        step++;
                    }
                    progress.setLabel("Logging in...");
                    break;
                case 2:
                    int loadingRadius = (int) FastMath.ceil((10 +
                            engine.getTagStructure().getStructure("Scapes")
                                    .getFloat("RenderDistance") * 246) /
                            16.0f) << 4;
                    client = new ClientConnection(engine, channel,
                            ((ScapesClient) engine.getGame()).getAccount(),
                            loadingRadius + 16);
                    step++;
                    break;
                case 3:
                    if (client.login()) {
                        step++;
                    }
                    progress.setLabel("Loading world...");
                    break;
                case 4:
                    remove(progress);
                    GameStateGameMP game =
                            new GameStateGameMP(client, scene, engine);
                    engine.setState(game);
                    break;
            }
        } catch (IOException | ConnectionCloseException e) {
            LOGGER.error("Failed to connect to server:", e);
            engine.setState(
                    new GameStateServerDisconnect(e.getMessage(), engine));
            step = -1;
            return;
        }
        progress.setProgress(step / 4.0f);
    }
}
