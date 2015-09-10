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
package org.tobi29.scapes.client.connection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.chunk.IDStorage;
import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.client.states.GameStateGameMP;
import org.tobi29.scapes.client.states.GameStateServerDisconnect;
import org.tobi29.scapes.connection.PlayConnection;
import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.gui.debug.GuiWidgetDebugValues;
import org.tobi29.scapes.engine.utils.io.PacketBundleChannel;
import org.tobi29.scapes.engine.utils.io.ReadableByteStream;
import org.tobi29.scapes.engine.utils.io.WritableByteStream;
import org.tobi29.scapes.engine.utils.task.Joiner;
import org.tobi29.scapes.engine.utils.task.TaskExecutor;
import org.tobi29.scapes.entity.client.MobPlayerClientMain;
import org.tobi29.scapes.packets.Packet;
import org.tobi29.scapes.packets.PacketClient;
import org.tobi29.scapes.packets.PacketPingClient;
import org.tobi29.scapes.packets.PacketServer;
import org.tobi29.scapes.plugins.Plugins;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ClientConnection
        implements TaskExecutor.ASyncTask, PlayConnection {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ClientConnection.class);
    private final ScapesEngine engine;
    private final PacketBundleChannel channel;
    private final Selector selector;
    private final GuiWidgetDebugValues.Element pingDebug, downloadDebug,
            uploadDebug;
    private final Queue<Packet> sendQueue = new ConcurrentLinkedQueue<>();
    private final int loadingDistance;
    private final Plugins plugins;
    private GameStateGameMP game;
    private MobPlayerClientMain entity;
    private Joiner joiner;
    private State state = State.OPEN;
    private WorldClient world;

    public ClientConnection(ScapesEngine engine, PacketBundleChannel channel,
            Plugins plugins, int loadingDistance) throws IOException {
        this.engine = engine;
        this.channel = channel;
        this.plugins = plugins;
        this.loadingDistance = loadingDistance;
        selector = Selector.open();
        this.channel.register(selector, SelectionKey.OP_READ);
        GuiWidgetDebugValues debugValues = engine.debugValues();
        pingDebug = debugValues.get("Connection-Ping");
        downloadDebug = debugValues.get("Connection-Down");
        uploadDebug = debugValues.get("Connection-Up");
    }

    @Override
    public void run(Joiner joiner) {
        try {
            while (!joiner.marked()) {
                WritableByteStream output = channel.getOutputStream();
                while (!sendQueue.isEmpty()) {
                    Packet packet = sendQueue.poll();
                    output.putShort(packet.id(plugins.registry()));
                    ((PacketServer) packet).sendServer(this, output);
                }
                if (channel.bundleSize() > 0) {
                    channel.queueBundle();
                }
                Optional<ReadableByteStream> bundle = channel.fetch();
                if (bundle.isPresent()) {
                    ReadableByteStream input = bundle.get();
                    while (input.hasRemaining()) {
                        PacketClient packet = (PacketClient) Packet
                                .make(plugins.registry(), input.getShort());
                        packet.parseClient(this, input);
                        packet.runClient(this, world);
                    }
                }
                if (!channel.process() && !joiner.marked()) {
                    try {
                        selector.select(10);
                        selector.selectedKeys().clear();
                    } catch (IOException e) {
                        LOGGER.warn("Error when waiting for events: {}",
                                e.toString());
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.info("Lost connection: {}", e.toString());
            engine.setState(new GameStateServerDisconnect(e.getMessage(),
                    channel.getRemoteAddress(), engine));
        }
        try {
            close();
        } catch (IOException e) {
            LOGGER.error("Error closing socket: {}", e.toString());
        }
        LOGGER.info("Closed client connection!");
    }

    private void close() throws IOException {
        channel.close();
        selector.close();
    }

    @Override
    public synchronized void send(Packet packet) {
        if (!(packet instanceof PacketServer)) {
            throw new IllegalArgumentException(
                    "Packet is not a to-server packet!");
        }
        sendQueue.add(packet);
    }

    public int loadingRadius() {
        return loadingDistance;
    }

    public void start(GameStateGameMP game) {
        this.game = game;
        joiner = engine.taskExecutor().runTask(this, "Client-Connection");
        engine.taskExecutor().addTask(() -> {
            send(new PacketPingClient(System.currentTimeMillis()));
            downloadDebug.setValue(channel.getInputRate() / 128.0);
            uploadDebug.setValue(channel.getOutputRate() / 128.0);
            return state == State.CLOSED ? -1 : 1000;
        }, "Connection-Rate", 1000, false);
    }

    public void stop() {
        state = State.CLOSED;
        joiner.join();
    }

    public MobPlayerClientMain entity() {
        return entity;
    }

    public WorldClient world() {
        return world;
    }

    public GameStateGameMP game() {
        return game;
    }

    public Plugins plugins() {
        return plugins;
    }

    public void changeWorld(WorldClient world) {
        this.world = world;
        entity = world.player();
        game.setScene(world.scene());
    }

    public void updatePing(long ping) {
        pingDebug.setValue(System.currentTimeMillis() - ping);
    }

    enum State {
        OPEN,
        CLOSED
    }
}
