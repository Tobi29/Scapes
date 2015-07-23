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
import org.tobi29.scapes.connection.Connection;
import org.tobi29.scapes.connection.PlayConnection;
import org.tobi29.scapes.engine.utils.SleepUtil;
import org.tobi29.scapes.engine.utils.UnsupportedJVMException;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.task.Joiner;
import org.tobi29.scapes.engine.utils.task.TaskExecutor;
import org.tobi29.scapes.entity.skin.ServerSkin;
import org.tobi29.scapes.packets.Packet;
import org.tobi29.scapes.packets.PacketChat;
import org.tobi29.scapes.server.ScapesServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ServerConnection implements PlayConnection {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ServerConnection.class);
    private final int mayPlayers, workerCount;
    private final ScapesServer server;
    private final TaskExecutor taskExecutor;
    private final KeyPair keyPair;
    private final Map<String, PlayerConnection> players =
            new ConcurrentHashMap<>();
    private final Map<String, PlayerConnection> playerByName =
            new ConcurrentHashMap<>();
    private final List<NetWorkerThread> workers = new ArrayList<>();
    private Joiner joiner;
    private boolean allowsJoin = true, allowsCreation = true;

    public ServerConnection(ScapesServer server, TaskExecutor taskExecutor,
            TagStructure tagStructure) {
        this.taskExecutor = taskExecutor;
        mayPlayers = tagStructure.getInteger("MaxPlayers");
        workerCount = tagStructure.getInteger("WorkerCount");
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

    @Override
    public void send(Packet packet) {
        playerByName.values().forEach(player -> player.send(packet));
    }

    @Override
    public int loadingRadius() {
        throw new UnsupportedOperationException(
                "Cannot check loading radius from server.");
    }

    public void send(Packet packet, List<PlayerConnection> exceptions) {
        playerByName.values().stream()
                .filter(player -> !exceptions.contains(player))
                .forEach(player -> player.send(packet));
    }

    public int start(int port) throws IOException {
        InetSocketAddress address = start(new InetSocketAddress(port));
        return address.getPort();
    }

    @SuppressWarnings("unchecked")
    public <A extends SocketAddress> A start(A address) throws IOException {
        LOGGER.info("Starting worker {} threads...", workerCount);
        List<Joiner> joiners = new ArrayList<>(workerCount + 1);
        for (int i = 0; i < workerCount; i++) {
            NetWorkerThread worker = new NetWorkerThread();
            joiners.add(taskExecutor.runTask(worker, "Connection-Worker-" + i));
            workers.add(worker);
        }
        LOGGER.info("Starting socket thread...");
        ServerSocketChannel channel = ServerSocketChannel.open();
        channel.configureBlocking(false);
        channel.bind(address);
        joiners.add(taskExecutor.runTask(joiner -> {
            try {
                while (!joiner.marked()) {
                    SocketChannel client = channel.accept();
                    if (client == null) {
                        SleepUtil.sleep(100);
                    } else {
                        client.configureBlocking(false);
                        int load = Integer.MAX_VALUE;
                        NetWorkerThread bestWorker = null;
                        for (NetWorkerThread worker : workers) {
                            int workerLoad = worker.connections.size();
                            if (workerLoad < load) {
                                bestWorker = worker;
                                load = workerLoad;
                            }
                        }
                        if (bestWorker == null) {
                            client.close();
                        } else {
                            bestWorker.addConnection(
                                    new UnknownConnection(client, this));
                        }
                    }
                }
            } finally {
                channel.close();
            }
        }, "Socket"));
        joiner = new Joiner(joiners);
        return (A) channel.getLocalAddress();
    }

    public void stop() {
        joiner.join();
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

    protected Optional<String> addPlayer(PlayerConnection connection) {
        synchronized (players) {
            if (players.size() >= mayPlayers) {
                return Optional.of("Server full");
            }
            if (players.containsKey(connection.id())) {
                return Optional.of("User already online");
            }
            if (playerByName.containsKey(connection.nickname())) {
                return Optional.of("User with same name online");
            }
            players.put(connection.id(), connection);
            playerByName.put(connection.nickname(), connection);
        }
        return Optional.empty();
    }

    protected void removePlayer(PlayerConnection connection) {
        players.remove(connection.id());
        playerByName.remove(connection.nickname());
    }

    public void chat(String message) {
        LOGGER.info("Chat (*): {}", message);
        send(new PacketChat(message));
    }

    public static class NetWorkerThread implements TaskExecutor.ASyncTask {
        private final Queue<Connection> connectionQueue =
                new ConcurrentLinkedQueue<>();
        private final List<Connection> connections = new ArrayList<>();
        private final Selector selector;

        public NetWorkerThread() throws IOException {
            selector = Selector.open();
        }

        public void addConnection(Connection connection) throws IOException {
            connectionQueue.add(connection);
            connection.register(selector, SelectionKey.OP_READ);
            selector.wakeup();
        }

        @Override
        public void run(Joiner joiner) {
            try {
                while (!joiner.marked()) {
                    boolean processing = false;
                    while (!connectionQueue.isEmpty()) {
                        Connection connection = connectionQueue.poll();
                        connections.add(connection);
                    }
                    for (Connection connection : connections) {
                        if (connection.tick(this)) {
                            processing = true;
                        }
                    }
                    Iterator<Connection> iterator = connections.iterator();
                    while (iterator.hasNext()) {
                        Connection connection = iterator.next();
                        if (connection.isClosed()) {
                            try {
                                connection.close();
                            } catch (IOException e) {
                                LOGGER.warn("Failed to close connection: {}",
                                        e.toString());
                            }
                            iterator.remove();
                        }
                    }
                    if (!processing && !joiner.marked()) {
                        try {
                            selector.select(10);
                            selector.selectedKeys().clear();
                        } catch (IOException e) {
                            LOGGER.warn("Error when waiting for events: {}",
                                    e.toString());
                        }
                    }
                }
            } finally {
                for (Connection connection : connections) {
                    try {
                        connection.close();
                    } catch (IOException e) {
                        LOGGER.warn("Failed to close connection: {}",
                                e.toString());
                    }
                }
                try {
                    selector.close();
                } catch (IOException e) {
                    LOGGER.warn("Failed to close selector: {}", e.toString());
                }
            }
        }
    }
}
