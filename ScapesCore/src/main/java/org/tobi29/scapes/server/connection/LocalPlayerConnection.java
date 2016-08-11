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

package org.tobi29.scapes.server.connection;

import java8.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.client.connection.LocalClientConnection;
import org.tobi29.scapes.client.states.GameStateGameMP;
import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.server.AbstractServerConnection;
import org.tobi29.scapes.engine.server.Account;
import org.tobi29.scapes.engine.server.ConnectionCloseException;
import org.tobi29.scapes.engine.utils.BufferCreator;
import org.tobi29.scapes.engine.utils.graphics.Image;
import org.tobi29.scapes.engine.utils.graphics.PNG;
import org.tobi29.scapes.engine.utils.io.ChecksumUtil;
import org.tobi29.scapes.engine.utils.io.filesystem.FilePath;
import org.tobi29.scapes.engine.utils.io.filesystem.FileUtil;
import org.tobi29.scapes.entity.skin.ServerSkin;
import org.tobi29.scapes.packets.PacketClient;
import org.tobi29.scapes.packets.PacketDisconnect;
import org.tobi29.scapes.packets.PacketServer;
import org.tobi29.scapes.server.MessageLevel;

import java.io.IOException;
import java.nio.channels.Selector;
import java.util.concurrent.atomic.AtomicReference;

public class LocalPlayerConnection extends PlayerConnection {
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
        ScapesEngine engine = client.game().engine();
        FilePath path = engine.home().resolve("Skin.png");
        Image image;
        if (FileUtil.exists(path)) {
            image = FileUtil.readReturn(path,
                    stream -> PNG.decode(stream, BufferCreator::bytes));
        } else {
            AtomicReference<Image> reference = new AtomicReference<>();
            engine.files().get("Scapes:image/entity/mob/Player.png")
                    .read(stream -> {
                        Image defaultImage =
                                PNG.decode(stream, BufferCreator::bytes);
                        reference.set(defaultImage);
                    });
            image = reference.get();
        }
        if (image.width() != 64 || image.height() != 64) {
            throw new ConnectionCloseException("Invalid skin!");
        }
        nickname = account.nickname();
        skin = new ServerSkin(image);
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
        error(new ConnectionCloseException("Disconnected"));
    }

    public void error(Exception e) {
        server.message("Player disconnected: " + nickname + " (" + e + ')',
                MessageLevel.SERVER_INFO);
        state = State.CLOSED;
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
            packet.runServer(this);
        } catch (ConnectionCloseException e) {
            error(e);
        }
    }

    @Override
    public void send(PacketClient packet) {
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
    protected synchronized void transmit(PacketClient packet) {
        if (state == State.CLOSED) {
            return;
        }
        try {
            client.receive(packet);
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
    public void disconnect(String reason, double time) {
        removeEntity();
        transmit(new PacketDisconnect(reason, time));
        error(new ConnectionCloseException(reason));
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
