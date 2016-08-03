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

package org.tobi29.scapes.client.connection;

import java8.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.client.states.GameStateGameMP;
import org.tobi29.scapes.client.states.GameStateMenu;
import org.tobi29.scapes.client.states.GameStateServerDisconnect;
import org.tobi29.scapes.engine.server.ConnectionEndException;
import org.tobi29.scapes.engine.server.PacketBundleChannel;
import org.tobi29.scapes.engine.server.RemoteAddress;
import org.tobi29.scapes.engine.utils.io.IORunnable;
import org.tobi29.scapes.engine.utils.io.RandomReadableByteStream;
import org.tobi29.scapes.engine.utils.io.RandomWritableByteStream;
import org.tobi29.scapes.engine.utils.task.Joiner;
import org.tobi29.scapes.engine.utils.task.TaskExecutor;
import org.tobi29.scapes.packets.PacketAbstract;
import org.tobi29.scapes.packets.PacketClient;
import org.tobi29.scapes.packets.PacketPingClient;
import org.tobi29.scapes.packets.PacketServer;
import org.tobi29.scapes.plugins.Plugins;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class RemoteClientConnection extends ClientConnection
        implements TaskExecutor.ASyncTask {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(RemoteClientConnection.class);
    private final PacketBundleChannel channel;
    private final Selector selector;
    private final Queue<IORunnable> sendQueue = new ConcurrentLinkedQueue<>();
    private Joiner joiner;
    private State state = State.OPEN;

    public RemoteClientConnection(GameStateGameMP game,
            PacketBundleChannel channel, Plugins plugins, int loadingDistance)
            throws IOException {
        super(game, plugins, loadingDistance);
        this.channel = channel;
        selector = Selector.open();
        this.channel.register(selector, SelectionKey.OP_READ);
    }

    @Override
    public void run(Joiner.Joinable joiner) {
        try {
            while (state == State.OPEN) {
                while (!sendQueue.isEmpty()) {
                    sendQueue.poll().run();
                }
                if (channel.bundleSize() > 0) {
                    channel.queueBundle();
                }
                Optional<RandomReadableByteStream> bundle = channel.fetch();
                if (bundle.isPresent()) {
                    RandomReadableByteStream input = bundle.get();
                    while (input.hasRemaining()) {
                        PacketClient packet = (PacketClient) PacketAbstract
                                .make(plugins.registry(), input.getShort());
                        int pos = input.position();
                        packet.parseClient(this, input);
                        int size = input.position() - pos;
                        profilerReceived.packet(packet, size);
                        packet.runClient(this, world);
                    }
                }
                if (channel.process()) {
                    break;
                }
                try {
                    selector.select(10);
                    selector.selectedKeys().clear();
                } catch (IOException e) {
                    LOGGER.warn("Error when waiting for events: {}",
                            e.toString());
                }
            }
            LOGGER.info("Closed client connection!");
            game.engine().setState(new GameStateMenu(game.engine()));
        } catch (ConnectionEndException e) {
            LOGGER.info("Closed client connection: {}", e.toString());
        } catch (IOException e) {
            LOGGER.info("Lost connection: {}", e.toString());
            game.engine().setState(new GameStateServerDisconnect(e.getMessage(),
                    game.engine()));
        }
        state = State.CLOSED;
        try {
            channel.close();
            selector.close();
        } catch (IOException e) {
            LOGGER.error("Error closing socket: {}", e.toString());
        }
    }

    @Override
    public void start() {
        joiner =
                game.engine().taskExecutor().runTask(this, "Client-Connection");
        game.engine().taskExecutor().addTask(() -> {
            send(new PacketPingClient(System.currentTimeMillis()));
            downloadDebug.setValue(channel.getInputRate() / 128.0);
            uploadDebug.setValue(channel.getOutputRate() / 128.0);
            return state == State.CLOSED ? -1 : 1000;
        }, "Connection-Rate", 1000);
    }

    @Override
    public void stop() {
        channel.requestClose();
        joiner.join();
    }

    @Override
    protected void task(IORunnable runnable) {
        sendQueue.add(runnable);
    }

    @Override
    protected void transmit(PacketServer packet) throws IOException {
        RandomWritableByteStream output = channel.getOutputStream();
        int pos = output.position();
        output.putShort(packet.id(plugins.registry()));
        packet.sendServer(this, output);
        int size = output.position() - pos;
        profilerSent.packet(packet, size);
    }

    @Override
    public Optional<RemoteAddress> address() {
        return channel.getRemoteAddress().map(RemoteAddress::new);
    }

    public void updatePing(long ping) {
        pingDebug.setValue(System.currentTimeMillis() - ping);
    }

    enum State {
        OPEN,
        CLOSED
    }
}
