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

import java8.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.client.states.GameStateGameMP;
import org.tobi29.scapes.connection.PlayConnection;
import org.tobi29.scapes.engine.gui.debug.GuiWidgetDebugValues;
import org.tobi29.scapes.engine.server.RemoteAddress;
import org.tobi29.scapes.engine.utils.io.IORunnable;
import org.tobi29.scapes.entity.client.MobPlayerClientMain;
import org.tobi29.scapes.packets.PacketServer;
import org.tobi29.scapes.plugins.Plugins;

import java.io.IOException;

public abstract class ClientConnection implements PlayConnection<PacketServer> {
    protected static final Logger LOGGER =
            LoggerFactory.getLogger(ClientConnection.class);
    protected final GameStateGameMP game;
    protected final GuiWidgetDebugValues.Element pingDebug, downloadDebug,
            uploadDebug;
    protected final ConnectionProfiler profilerSent = new ConnectionProfiler(),
            profilerReceived = new ConnectionProfiler();
    protected final int loadingDistance;
    protected final Plugins plugins;
    protected MobPlayerClientMain entity;
    protected WorldClient world;

    protected ClientConnection(GameStateGameMP game, Plugins plugins,
            int loadingDistance) {
        this.game = game;
        this.plugins = plugins;
        this.loadingDistance = loadingDistance;
        GuiWidgetDebugValues debugValues = game.engine().debugValues();
        pingDebug = debugValues.get("Connection-Ping");
        downloadDebug = debugValues.get("Connection-Down");
        uploadDebug = debugValues.get("Connection-Up");
    }

    @Override
    public void send(PacketServer packet) {
        task(() -> sendPacket(packet));
    }

    protected void sendPacket(PacketServer packet) throws IOException {
        transmit(packet);
    }

    public abstract void start();

    public abstract void stop();

    protected abstract void task(IORunnable runnable);

    protected abstract void transmit(PacketServer packet) throws IOException;

    public int loadingRadius() {
        return loadingDistance;
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

    public ConnectionProfiler profilerSent() {
        return profilerSent;
    }

    public ConnectionProfiler profilerReceived() {
        return profilerReceived;
    }

    public abstract Optional<RemoteAddress> address();

    public void changeWorld(WorldClient world) {
        this.world = world;
        entity = world.player();
        game.setScene(world.scene());
    }
}
