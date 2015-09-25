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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.connection.ConnectionType;
import org.tobi29.scapes.engine.server.*;
import org.tobi29.scapes.engine.utils.UnsupportedJVMException;
import org.tobi29.scapes.engine.utils.io.PacketBundleChannel;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.entity.skin.ServerSkin;
import org.tobi29.scapes.packets.Packet;
import org.tobi29.scapes.server.ControlPanel;
import org.tobi29.scapes.server.MessageLevel;
import org.tobi29.scapes.server.ScapesServer;
import org.tobi29.scapes.server.command.Command;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class ServerConnection extends AbstractServerConnection {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ServerConnection.class);
    private final int mayPlayers;
    private final String controlPassword;
    private final ScapesServer server;
    private final KeyPair keyPair;
    private final Map<String, PlayerConnection> players =
            new ConcurrentHashMap<>();
    private final Map<String, PlayerConnection> playerByName =
            new ConcurrentHashMap<>();
    private final Set<Command.Executor> executors =
            Collections.newSetFromMap(new ConcurrentHashMap<>());
    private boolean allowsJoin = true, allowsCreation = true;

    public ServerConnection(ScapesServer server, TagStructure tagStructure) {
        super(server.taskExecutor(), ConnectionInfo.header(), tagStructure);
        mayPlayers = tagStructure.getInteger("MaxPlayers");
        controlPassword = tagStructure.getString("ControlPassword");
        this.server = server;
        try {
            KeyPairGenerator keyPairGenerator =
                    KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(tagStructure.getInteger("RSASize"));
            keyPair = keyPairGenerator.genKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new UnsupportedJVMException(e);
        }
    }

    public Optional<PlayerConnection> player(String id) {
        return Optional.ofNullable(players.get(id));
    }

    public Optional<PlayerConnection> playerByName(String name) {
        return Optional.ofNullable(playerByName.get(name));
    }

    public Optional<ServerSkin> skin(byte[] checksum) {
        for (PlayerConnection player : playerByName.values()) {
            if (Arrays.equals(player.skin().checksum(), checksum)) {
                return Optional.of(player.skin());
            }
        }
        return Optional.empty();
    }

    public ScapesServer server() {
        return server;
    }

    public Stream<PlayerConnection> players() {
        return playerByName.values().stream();
    }

    public void send(Packet packet) {
        playerByName.values().forEach(player -> player.send(packet));
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

    protected KeyPair keyPair() {
        return keyPair;
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
            if (playerByName.containsKey(player.nickname())) {
                return Optional.of("User with same name online");
            }
            players.put(player.id(), player);
            playerByName.put(player.nickname(), player);
            addExecutor(player);
        }
        return Optional.empty();
    }

    protected void removePlayer(PlayerConnection player) {
        players.remove(player.id());
        playerByName.remove(player.nickname());
        removeExecutor(player);
    }

    public void message(String message, MessageLevel level) {
        executors.forEach(executor -> executor.message(message, level));
    }

    @Override
    protected Optional<Connection> newConnection(SocketChannel channel, byte id)
            throws IOException {
        switch (ConnectionType.get(id)) {
            case GET_INFO:
                return Optional.of(new GetInfoConnection(channel,
                        server().serverInfo()));
            case PLAY:
                PacketBundleChannel bundleChannel =
                        new PacketBundleChannel(channel);
                return Optional.of(new PlayerConnection(bundleChannel, this));
            case CONTROL:
                if (controlPassword.isEmpty()) {
                    break;
                }
                ControlPanelProtocol controlChannel =
                        new ControlPanelProtocol(channel, controlPassword,
                                keyPair);
                ControlPanel controlPanel =
                        new ControlPanel(controlChannel, server);
                addExecutor(controlPanel);
                controlChannel.closeHook(() -> removeExecutor(controlPanel));
                return Optional.of(controlChannel);
        }
        return Optional.empty();
    }
}
