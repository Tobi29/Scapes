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

package org.tobi29.scapes.client.states;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.client.connection.ClientConnection;
import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.gui.debug.GuiWidgetDebugValues;
import org.tobi29.scapes.engine.opengl.GL;
import org.tobi29.scapes.engine.opengl.scenes.Scene;
import org.tobi29.scapes.server.ControlPanel;
import org.tobi29.scapes.server.ScapesServer;

import java.io.IOException;
import java.util.Map;

public class GameStateGameSP extends GameStateGameMP {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(GameStateGameSP.class);
    private final ScapesServer server;

    public GameStateGameSP(ClientConnection client, ScapesServer server,
            Scene scene, ScapesEngine engine) {
        super(client, scene, engine);
        this.server = server;
        server.addControlPanel(new ClientControlPanel());
    }

    @Override
    public void dispose(GL gl) {
        super.dispose(gl);
        server.stop(ScapesServer.ShutdownReason.STOP);
        try {
            server.worldFormat().save();
            server.worldFormat()
                    .savePanorama(client.world().scene().panorama());
        } catch (IOException e) {
            LOGGER.error("Error stopping internal server: {}", e.toString());
        }
        LOGGER.info("Stopped internal server!");
    }

    private class ClientControlPanel implements ControlPanel {
        @Override
        public String id() {
            return "LocalClient";
        }

        @Override
        public void updatePlayers(String[] players) {
        }

        @Override
        public void updateWorlds(String[] worlds) {
        }

        @Override
        public void appendLog(String line) {
        }

        @Override
        public void sendProfilerResults(long ram, Map<String, Double> tps) {
            GuiWidgetDebugValues debug = engine.debugValues();
            for (Map.Entry<String, Double> entry : tps.entrySet()) {
                debug.get("Server-CPU-" + entry.getKey())
                        .setValue(entry.getValue());
            }
        }

        @Override
        public void replaced() {
        }

        @Override
        public boolean isClosed() {
            return false;
        }

        @Override
        public String name() {
            return "Host";
        }
    }
}
