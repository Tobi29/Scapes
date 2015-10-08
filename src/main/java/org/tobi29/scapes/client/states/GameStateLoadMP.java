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
import org.tobi29.scapes.client.connection.NewConnection;
import org.tobi29.scapes.client.gui.GuiLoading;
import org.tobi29.scapes.client.states.scenes.SceneMenu;
import org.tobi29.scapes.connection.ConnectionType;
import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.opengl.GL;
import org.tobi29.scapes.engine.server.Account;
import org.tobi29.scapes.engine.utils.BufferCreator;
import org.tobi29.scapes.engine.utils.math.FastMath;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Optional;

public class GameStateLoadMP extends GameState {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(GameStateLoadMP.class);
    private final InetSocketAddress address;
    private final ByteBuffer headerBuffer = BufferCreator
            .wrap(new byte[]{'S', 'c', 'a', 'p', 'e', 's',
                    ConnectionType.PLAY.data()});
    private int step;
    private SocketChannel channel;
    private NewConnection client;
    private GuiLoading progress;

    public GameStateLoadMP(InetSocketAddress address, ScapesEngine engine,
            SceneMenu scene) {
        super(engine, scene);
        this.address = address;
        scene.setSpeed(0.0f);
    }

    @Override
    public void dispose(GL gl) {
    }

    @Override
    public void init(GL gl) {
        progress = new GuiLoading();
        add(progress);
        progress.setLabel("Connecting to server...");
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
                        progress.setLabel("Sending request...");
                    }
                    break;
                case 2:
                    channel.write(headerBuffer);
                    if (!headerBuffer.hasRemaining()) {
                        step++;
                    }
                    break;
                case 3:
                    int loadingRadius = FastMath.round(
                            engine.tagStructure().getStructure("Scapes")
                                    .getDouble("RenderDistance")) + 32;
                    Account account = Account.read(
                            engine.home().resolve("Account.properties"));
                    client = new NewConnection(engine, channel, account,
                            loadingRadius);
                    step++;
                    break;
                case 4:
                    Optional<String> status = client.login();
                    if (status.isPresent()) {
                        progress.setLabel(status.get());
                    } else {
                        step++;
                        progress.setLabel("Loading world...");
                    }
                    break;
                case 5:
                    remove(progress);
                    GameStateGameMP game =
                            new GameStateGameMP(client.finish(), scene, engine);
                    engine.setState(game);
                    break;
            }
        } catch (IOException e) {
            LOGGER.error("Failed to connect to server:", e);
            engine.setState(
                    new GameStateServerDisconnect(e.getMessage(), address,
                            engine));
            step = -1;
            return;
        }
        progress.setProgress(step / 5.0f);
    }
}
