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
package org.tobi29.scapes.server.connection;

import java8.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.client.connection.LocalClientConnection;
import org.tobi29.scapes.client.states.GameStateGameMP;
import org.tobi29.scapes.engine.server.AbstractServerConnection;
import org.tobi29.scapes.engine.server.Account;
import org.tobi29.scapes.engine.server.ConnectionCloseException;
import org.tobi29.scapes.engine.utils.BufferCreator;
import org.tobi29.scapes.engine.utils.MutableSingle;
import org.tobi29.scapes.engine.utils.graphics.Image;
import org.tobi29.scapes.engine.utils.graphics.PNG;
import org.tobi29.scapes.engine.utils.io.ChecksumUtil;
import org.tobi29.scapes.engine.utils.io.IORunnable;
import org.tobi29.scapes.engine.utils.io.filesystem.FilePath;
import org.tobi29.scapes.engine.utils.io.filesystem.FileUtil;
import org.tobi29.scapes.entity.skin.ServerSkin;
import org.tobi29.scapes.packets.Packet;
import org.tobi29.scapes.packets.PacketClient;
import org.tobi29.scapes.packets.PacketServer;
import org.tobi29.scapes.server.MessageLevel;

import java.io.IOException;
import java.nio.channels.Selector;

public class LocalPlayerConnection extends PlayerConnection {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(LocalPlayerConnection.class);
    private final LocalClientConnection client;
    private State state = State.OPEN;

    public LocalPlayerConnection(ServerConnection server, GameStateGameMP game,
            int loadingDistance, Account account) {
        super(server);
        loadingRadius = loadingDistance;
        client = new LocalClientConnection(game, this, server.plugins(),
                loadingDistance, account);
    }

    public Optional<String> start(Account account) throws IOException {
        MutableSingle<Image> image = new MutableSingle<>();
        FilePath path = client.game().engine().home().resolve("Skin.png");
        if (FileUtil.exists(path)) {
            image.a = FileUtil.readReturn(path,
                    stream -> PNG.decode(stream, BufferCreator::bytes));
        } else {
            client.game().engine().files()
                    .get("Scapes:image/entity/mob/Player.png").read(stream -> {
                image.a = PNG.decode(stream, BufferCreator::bytes);
            });
        }
        if (image.a.width() != 64 || image.a.height() != 64) {
            return Optional.of("Invalid skin!");
        }
        nickname = account.nickname();
        skin = new ServerSkin(image.a);
        id = ChecksumUtil.checksum(account.keyPair().getPublic().getEncoded(),
                ChecksumUtil.Algorithm.SHA1).toString();
        Optional<String> response = server.addPlayer(this);
        if (response.isPresent()) {
            return response;
        }
        added = true;
        setWorld();
        return Optional.empty();
    }

    public void stop() {
        task(() -> {
            throw new ConnectionCloseException("Disconnected");
        });
    }

    public void error(Exception e) {
        server.message("Player disconnected: " + nickname + " (" + e +
                ')', MessageLevel.SERVER_INFO);
        state = State.CLOSED;
        client.stop();
    }

    public LocalClientConnection client() {
        return client;
    }

    public synchronized void receive(PacketServer packet) {
        if (state == State.CLOSED) {
            return;
        }
        try {
            packet.localServer();
            packet.runServer(this, entity.world());
        } catch (ConnectionCloseException e) {
            error(e);
        }
    }

    @Override
    public synchronized void send(Packet packet) {
        if (state == State.CLOSED) {
            return;
        }
        try {
            sendPacket(packet);
        } catch (IOException e) {
            error(e);
        }
    }

    @Override
    protected synchronized void task(IORunnable runnable) {
        if (state == State.CLOSED) {
            return;
        }
        try {
            client.receive(runnable);
        } catch (IOException e) {
            error(e);
        }
    }

    @Override
    protected synchronized void transmit(Packet packet) {
        if (state == State.CLOSED) {
            return;
        }
        try {
            client.receive((PacketClient) packet);
        } catch (IOException e) {
            error(e);
        }
    }

    @Override
    public synchronized void close() throws IOException {
        super.close();
        state = State.CLOSED;
    }

    @Override
    public void register(Selector selector, int opt) throws IOException {
    }

    @Override
    public boolean tick(AbstractServerConnection.NetWorkerThread worker) {
        return false;
    }

    @Override
    public boolean isClosed() {
        return state == State.CLOSED;
    }

    enum State {
        OPEN,
        CLOSED
    }
}
