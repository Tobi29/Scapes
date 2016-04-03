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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.client.connection.NewConnection;
import org.tobi29.scapes.client.gui.GuiLoading;
import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.opengl.scenes.Scene;
import org.tobi29.scapes.engine.server.*;
import org.tobi29.scapes.engine.utils.UnsupportedJVMException;
import org.tobi29.scapes.engine.utils.graphics.Image;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.server.ScapesServer;
import org.tobi29.scapes.server.format.WorldSource;
import org.tobi29.scapes.server.ssl.dummy.DummyKeyManagerProvider;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

public class GameStateLoadSocketSP extends GameState {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(GameStateLoadSocketSP.class);
    private int step, port;
    private ScapesServer server;
    private WorldSource source;
    private SocketChannel channel;
    private InetSocketAddress address;
    private NewConnection client;
    private GuiLoading progress;

    public GameStateLoadSocketSP(WorldSource source, ScapesEngine engine,
            Scene scene) {
        super(engine, scene);
        this.source = source;
    }

    @Override
    public void dispose() {
        if (server != null) {
            try {
                server.stop(ScapesServer.ShutdownReason.ERROR);
            } catch (IOException e) {
                LOGGER.error(
                        "Failed to stop internal server after login error:", e);
            }
        }
        if (source != null) {
            try {
                source.close();
            } catch (IOException e) {
                LOGGER.error("Failed to close world source:", e);
            }
        }
    }

    @Override
    public void init() {
        progress = new GuiLoading(this, engine.guiStyle());
        engine.guiStack().add("20-Progress", progress);
    }

    @Override
    public boolean isMouseGrabbed() {
        return false;
    }

    @Override
    public void step(double delta) {
        try {
            switch (step) {
                case 0:
                    progress.setLabel("Creating server...");
                    step++;
                    break;
                case 1:
                    TagStructure tagStructure =
                            engine.tagStructure().getStructure("Scapes")
                                    .getStructure("IntegratedServer");
                    Optional<Image[]> panorama = source.panorama();
                    ServerInfo serverInfo;
                    if (panorama.isPresent()) {
                        serverInfo = new ServerInfo("Local Server",
                                panorama.get()[0]);
                    } else {
                        serverInfo = new ServerInfo("Local Server");
                    }
                    SSLHandle ssl;
                    try {
                        ssl = SSLProvider
                                .sslHandle(DummyKeyManagerProvider.get());
                    } catch (IOException e) {
                        throw new UnsupportedJVMException(e);
                    }
                    server = new ScapesServer(source, tagStructure, serverInfo,
                            ssl, engine);
                    progress.setLabel("Starting server...");
                    step++;
                    break;
                case 2:
                    port = server.connection().start(0);
                    if (port <= 0) {
                        throw new IOException(
                                "Unable to open server socket (Invalid port returned: " +
                                        port + ')');
                    }
                    progress.setLabel("Connecting to server...");
                    step++;
                    break;
                case 3:
                    address = new InetSocketAddress(port);
                    channel = SocketChannel.open(address);
                    channel.configureBlocking(false);
                    step++;
                    break;
                case 4:
                    if (channel.finishConnect()) {
                        step++;
                        progress.setLabel("Sending request...");
                    }
                    break;
                case 5:
                    PacketBundleChannel bundleChannel;
                    // Ignore invalid certificates because local server
                    // cannot provide a valid one
                    ssl = SSLProvider.sslHandle(certificates -> true);
                    bundleChannel =
                            new PacketBundleChannel(new RemoteAddress(address),
                                    channel, engine.taskExecutor(), ssl, true);
                    int loadingRadius = FastMath.round(
                            engine.tagStructure().getStructure("Scapes")
                                    .getDouble("RenderDistance")) + 32;
                    Account account = Account.read(
                            engine.home().resolve("Account.properties"));
                    client = new NewConnection(engine, bundleChannel, account,
                            loadingRadius);
                    step++;
                    break;
                case 6:
                    Optional<String> status = client.login();
                    if (status.isPresent()) {
                        progress.setLabel(status.get());
                    } else {
                        step++;
                        progress.setLabel("Loading world...");
                    }
                    break;
                case 7:
                    GameStateGameSP game =
                            new GameStateGameSP(client.finish(), source, server,
                                    scene, engine);
                    server = null;
                    source = null;
                    engine.setState(game);
                    break;
            }
        } catch (IOException e) {
            LOGGER.error("Failed to start internal server:", e);
            engine.setState(new GameStateServerDisconnect(e.getMessage(),
                    Optional.empty(), engine));
            step = -1;
            return;
        }
        progress.setProgress(step / 7.0f);
    }
}
