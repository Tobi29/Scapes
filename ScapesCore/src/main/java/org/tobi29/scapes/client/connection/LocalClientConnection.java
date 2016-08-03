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
import org.tobi29.scapes.client.states.GameStateGameMP;
import org.tobi29.scapes.client.states.GameStateMenu;
import org.tobi29.scapes.engine.server.Account;
import org.tobi29.scapes.engine.server.RemoteAddress;
import org.tobi29.scapes.engine.utils.io.IORunnable;
import org.tobi29.scapes.packets.PacketClient;
import org.tobi29.scapes.packets.PacketServer;
import org.tobi29.scapes.plugins.Plugins;
import org.tobi29.scapes.server.connection.LocalPlayerConnection;

import java.io.IOException;

public class LocalClientConnection extends ClientConnection {
    private final Account account;
    private final LocalPlayerConnection player;

    public LocalClientConnection(GameStateGameMP game,
            LocalPlayerConnection player, Plugins plugins, int loadingDistance,
            Account account) {
        super(game, plugins, loadingDistance);
        this.player = player;
        this.account = account;
    }

    public void receive(PacketClient packet) throws IOException {
        packet.localClient();
        packet.runClient(this, world);
    }

    @Override
    public void start() {
        synchronized (player) {
            if (player.isClosed()) {
                return;
            }
            try {
                player.start(account);
                player.server().addClient(player);
            } catch (IOException e) {
                player.error(e);
            }
        }
    }

    @Override
    public void stop() {
        if (!player.isClosed()) {
            player.stop();
            game.engine().setState(new GameStateMenu(game.engine()));
        }
    }

    @Override
    protected void task(IORunnable runnable) {
        synchronized (player) {
            if (player.isClosed()) {
                return;
            }
            try {
                runnable.run();
            } catch (IOException e) {
                player.error(e);
            }
        }
    }

    @Override
    protected void transmit(PacketServer packet) throws IOException {
        player.receive(packet);
    }

    @Override
    public Optional<RemoteAddress> address() {
        return Optional.empty();
    }
}
