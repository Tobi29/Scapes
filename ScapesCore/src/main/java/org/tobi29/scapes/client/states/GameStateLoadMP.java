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
import org.tobi29.scapes.client.gui.desktop.GuiCertificateWarning;
import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.opengl.scenes.Scene;
import org.tobi29.scapes.engine.server.*;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.task.Joiner;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.security.cert.X509Certificate;
import java.util.concurrent.atomic.AtomicBoolean;

public class GameStateLoadMP extends GameState {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(GameStateLoadMP.class);
    private final RemoteAddress address;
    private int step;
    private SocketChannel channel;
    private NewConnection client;
    private GuiLoading progress;

    public GameStateLoadMP(RemoteAddress address, ScapesEngine engine,
            Scene scene) {
        super(engine, scene);
        this.address = address;
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
                    progress.setLabel("Connecting to server...");
                    step++;
                    break;
                case 1:
                    Optional<InetSocketAddress> socketAddress = AddressResolver
                            .resolve(address, engine.taskExecutor());
                    if (socketAddress.isPresent()) {
                        channel = SocketChannel.open();
                        channel.configureBlocking(false);
                        channel.connect(socketAddress.get());
                        step++;
                    }
                    break;
                case 2:
                    if (channel.finishConnect()) {
                        step++;
                        progress.setLabel("Sending request...");
                    }
                    break;
                case 3:
                    PacketBundleChannel bundleChannel;
                    SSLHandle ssl = SSLProvider.sslHandle(certificates -> {
                        engine.guiStack().remove(progress);
                        try {
                            for (X509Certificate certificate : certificates) {
                                AtomicBoolean result = new AtomicBoolean();
                                Joiner.Joinable joinable =
                                        new Joiner.Joinable();
                                GuiCertificateWarning warning =
                                        new GuiCertificateWarning(this,
                                                certificate, value -> {
                                            result.set(value);
                                            joinable.join();
                                        }, progress.style());
                                engine.guiStack().add("10-Menu", warning);
                                joinable.joiner().join(warning::valid);
                                engine.guiStack().remove(warning);
                                if (!result.get()) {
                                    return false;
                                }
                            }
                            return true;
                        } finally {
                            engine.guiStack().add("20-Progress", progress);
                        }
                    });
                    bundleChannel = new PacketBundleChannel(address, channel,
                            engine.taskExecutor(), ssl, true);
                    int loadingRadius = FastMath.round(
                            engine.tagStructure().getStructure("Scapes")
                                    .getDouble("RenderDistance")) + 32;
                    Account account = Account.read(
                            engine.home().resolve("Account.properties"));
                    client = new NewConnection(engine, bundleChannel, account,
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
                    GameStateGameMP game =
                            new GameStateGameMP(client.finish(), scene, engine);
                    engine.setState(game);
                    break;
            }
        } catch (IOException e) {
            LOGGER.error("Failed to connect to server:", e);
            engine.setState(
                    new GameStateServerDisconnect(e.getMessage(), engine));
            step = -1;
            return;
        }
        progress.setProgress(step / 5.0f);
    }
}
