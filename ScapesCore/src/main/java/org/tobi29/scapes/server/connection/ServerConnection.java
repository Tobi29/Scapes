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
import java8.util.stream.Stream;
import org.tobi29.scapes.connection.ConnectionType;
import org.tobi29.scapes.engine.server.*;
import org.tobi29.scapes.engine.utils.Checksum;
import org.tobi29.scapes.engine.utils.Streams;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.entity.skin.ServerSkin;
import org.tobi29.scapes.packets.PacketClient;
import org.tobi29.scapes.plugins.Plugins;
import org.tobi29.scapes.server.ControlPanel;
import org.tobi29.scapes.server.MessageLevel;
import org.tobi29.scapes.server.ScapesServer;
import org.tobi29.scapes.server.command.Command;
import org.tobi29.scapes.server.extension.event.NewConnectionEvent;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ServerConnection extends AbstractServerConnection {
    private final int mayPlayers;
    private final String controlPassword;
    private final ScapesServer server;
    private final Plugins plugins;
    private final Map<String, PlayerConnection> players =
            new ConcurrentHashMap<>();
    private final Map<String, PlayerConnection> playerByName =
            new ConcurrentHashMap<>();
    private final Set<Command.Executor> executors =
            Collections.newSetFromMap(new ConcurrentHashMap<>());
    private boolean allowsJoin = true, allowsCreation = true;

    public ServerConnection(ScapesServer server, TagStructure tagStructure,
            SSLHandle ssl) {
        super(server.taskExecutor(), ConnectionInfo.header(), ssl);
        plugins = server.plugins();
        mayPlayers = tagStructure.getInteger("MaxPlayers");
        controlPassword = tagStructure.getString("ControlPassword");
        this.server = server;
    }

    public Optional<PlayerConnection> player(String id) {
        return Optional.ofNullable(players.get(id));
    }

    public Optional<PlayerConnection> playerByName(String name) {
        return Optional.ofNullable(playerByName.get(name));
    }

    public Optional<ServerSkin> skin(Checksum checksum) {
        for (PlayerConnection player : playerByName.values()) {
            if (player.skin().checksum().equals(checksum)) {
                return Optional.of(player.skin());
            }
        }
        return Optional.empty();
    }

    public ScapesServer server() {
        return server;
    }

    public Plugins plugins() {
        return plugins;
    }

    public Stream<PlayerConnection> players() {
        return Streams.of(playerByName.values());
    }

    public void send(PacketClient packet) {
        Streams.forEach(playerByName.values(), player -> player.send(packet));
    }

    public boolean doesAllowJoin() {
        return allowsJoin;
    }

    public void setAllowsJoin(boolean allowsJoin) {
        this.allowsJoin = allowsJoin;
    }

    public boolean doesAllowCreation() {
        return allowsCreation;
    }

    public void setAllowsCreation(boolean allowsCreation) {
        this.allowsCreation = allowsCreation;
    }

    public void addExecutor(Command.Executor executor) {
        executors.add(executor);
    }

    public void removeExecutor(Command.Executor executor) {
        executors.remove(executor);
    }

    protected Optional<String> addPlayer(PlayerConnection player) {
        synchronized (players) {
            if (players.size() >= mayPlayers) {
                return Optional.of("Server full");
            }
            if (players.containsKey(player.id())) {
                return Optional.of("User already online");
            }
            if (playerByName.containsKey(player.name())) {
                return Optional.of("User with same name online");
            }
            players.put(player.id(), player);
            playerByName.put(player.name(), player);
            addExecutor(player);
        }
        return Optional.empty();
    }

    protected void removePlayer(PlayerConnection player) {
        players.remove(player.id());
        playerByName.remove(player.name());
        removeExecutor(player);
    }

    public void message(String message, MessageLevel level) {
        Streams.forEach(executors,
                executor -> executor.message(message, level));
    }

    @Override
    protected Optional<String> accept(SocketChannel channel) {
        NewConnectionEvent event = new NewConnectionEvent(channel);
        server.extensions().fireEvent(event);
        if (!event.success()) {
            return Optional.of(event.reason());
        }
        return Optional.empty();
    }

    @Override
    protected Optional<Connection> newConnection(PacketBundleChannel channel,
            byte id) throws IOException {
        switch (ConnectionType.get(id)) {
            case GET_INFO:
                return Optional.of(new GetInfoConnection(channel,
                        server().serverInfo()));
            case PLAY:
                return Optional.of(new RemotePlayerConnection(channel, this));
            case CONTROL:
                if (controlPassword.isEmpty()) {
                    break;
                }
                ControlPanelProtocol controlChannel =
                        new ControlPanelProtocol(channel, false,
                                controlPassword);
                ControlPanel controlPanel =
                        new ControlPanel(controlChannel, server);
                addExecutor(controlPanel);
                controlChannel.closeHook(() -> removeExecutor(controlPanel));
                return Optional.of(controlChannel);
        }
        return Optional.empty();
    }
}
