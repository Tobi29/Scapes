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
import org.tobi29.scapes.server.ControlPanel;
import org.tobi29.scapes.server.ScapesServer;
import org.tobi29.scapes.server.controlpanel.ControlPanelProtocol;

import java.io.IOException;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.Optional;

public class ControlPanelConnection implements Connection, ControlPanel {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ControlPanelConnection.class);
    private final ControlPanelProtocol protocol;
    private final String id;
    private boolean done;

    @SuppressWarnings("ObjectToString")
    public ControlPanelConnection(SocketChannel channel, ScapesServer server)
            throws IOException {
        String password = server.getControlPanelPassword();
        if (password.isEmpty()) {
            throw new IOException("No control panel password set");
        }
        protocol = new ControlPanelProtocol(channel, password,
                Optional.of(server.getConnection().getKeyPair()));
        protocol.addCommand("scapescmd",
                command -> server.getCommandRegistry().get(command[0], this)
                        .execute()
                        .forEach(output -> appendLog(output.toString())));
        id = channel.getRemoteAddress().toString();
        server.addControlPanel(this);
    }

    @Override
    public void register(Selector selector, int opt) throws IOException {
        protocol.register(selector, opt);
    }

    @Override
    public boolean tick(ServerConnection.NetWorkerThread worker) {
        if (!done) {
            boolean processing = false;
            try {
                processing = protocol.process();
            } catch (IOException e) {
                LOGGER.info("Control panel disconnected: {}", e.toString());
                done = true;
            }
            return processing;
        }
        return false;
    }

    @Override
    public boolean isClosed() {
        return done;
    }

    @Override
    public void close() throws IOException {
        done = true;
        protocol.close();
    }

    @Override
    public String getName() {
        return "Server";
    }

    @Override
    public String getID() {
        return id;
    }

    @Override
    public void updatePlayers(String... players) {
        String[] command = new String[players.length + 1];
        command[0] = "updateplayers";
        System.arraycopy(players, 0, command, 1, players.length);
        protocol.send(command);
    }

    @Override
    public void updateWorlds(String... worlds) {
        String[] command = new String[worlds.length + 1];
        command[0] = "updateworlds";
        System.arraycopy(worlds, 0, command, 1, worlds.length);
        protocol.send(command);
    }

    @Override
    public void appendLog(String line) {
        if (line != null) {
            protocol.send("appendlog", line);
        }
    }

    @Override
    public void sendProfilerResults(long ram, Map<String, Double> tps) {
        String[] command = new String[(tps.size() << 1) + 2];
        command[0] = "appendprofiler";
        command[1] = String.valueOf(ram);
        int i = 2;
        for (Map.Entry<String, Double> entry : tps.entrySet()) {
            command[i++] = String.valueOf(entry.getKey());
            command[i++] = String.valueOf(entry.getValue());
        }
        protocol.send(command);
    }

    @Override
    public void replaced() {
        done = true;
    }
}
