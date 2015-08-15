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
import org.tobi29.scapes.client.connection.ClientConnection;
import org.tobi29.scapes.client.gui.GuiLoading;
import org.tobi29.scapes.client.states.scenes.SceneMenu;
import org.tobi29.scapes.connection.Account;
import org.tobi29.scapes.connection.ServerInfo;
import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.opengl.GL;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.server.ScapesServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.util.Optional;

public class GameStateLoadSP extends GameState {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(GameStateLoadSP.class);
    private final Path path;
    private int step, port;
    private ScapesServer server;
    private SocketChannel channel;
    private ClientConnection client;
    private GuiLoading progress;

    public GameStateLoadSP(Path path, ScapesEngine engine, SceneMenu scene) {
        super(engine, scene);
        this.path = path;
        scene.setSpeed(0.0f);
    }

    @Override
    public void dispose(GL gl) {
    }

    @Override
    public void init(GL gl) {
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
    public void stepComponent(double delta) {
        try {
            switch (step) {
                case 0:
                    String filename = path.getFileName().toString();
                    filename = filename.substring(0, filename.lastIndexOf('.'));
                    TagStructure tagStructure =
                            engine.tagStructure().getStructure("Scapes")
                                    .getStructure("IntegratedServer");
                    ServerInfo serverInfo = new ServerInfo(filename,
                            path.resolve("Panorama0.png"));
                    server = new ScapesServer(path, tagStructure, serverInfo,
                            engine);
                    progress.setLabel("Starting server...");
                    step++;
                    break;
                case 1:
                    port = server.connection().start(23456);
                    if (port <= 0) {
                        throw new IOException(
                                "Unable to open server socket (Invalid port returned: " +
                                        port + ')');
                    }
                    progress.setLabel("Connecting to server...");
                    step++;
                    break;
                case 2:
                    InetSocketAddress address =
                            new InetSocketAddress("localhost", port);
                    if (address.isUnresolved()) {
                        throw new IOException("Could not resolve address");
                    }
                    channel = SocketChannel.open(address);
                    channel.configureBlocking(false);
                    step++;
                    break;
                case 3:
                    if (channel.finishConnect()) {
                        step++;
                    }
                    progress.setLabel("Logging in...");
                    break;
                case 4:
                    int loadingRadius = FastMath.round(
                            engine.tagStructure().getStructure("Scapes")
                                    .getDouble("RenderDistance"));
                    Account.Client account = Account.read(
                            engine.home().resolve("Account.properties"));
                    client = new ClientConnection(engine, channel, account,
                            loadingRadius);
                    step++;
                    break;
                case 5:
                    if (client.login()) {
                        step++;
                    }
                    progress.setLabel("Loading world...");
                    break;
                case 6:
                    remove(progress);
                    GameStateGameSP game =
                            new GameStateGameSP(client, server, scene, engine);
                    engine.setState(game);
                    break;
            }
        } catch (IOException e) {
            LOGGER.error("Failed to start internal server:", e);
            try {
                server.stop(ScapesServer.ShutdownReason.ERROR);
            } catch (IOException e1) {
                LOGGER.error(
                        "Failed to stop internal server after login error:",
                        e1);
            }
            engine.setState(new GameStateServerDisconnect(e.getMessage(),
                    Optional.empty(), engine));
            step = -1;
            return;
        }
        progress.setProgress(step / 6.0f);
    }
}
