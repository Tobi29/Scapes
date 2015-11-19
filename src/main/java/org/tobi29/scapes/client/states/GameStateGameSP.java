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
import org.tobi29.scapes.engine.opengl.GL;
import org.tobi29.scapes.engine.opengl.scenes.Scene;
import org.tobi29.scapes.engine.utils.io.IOFunction;
import org.tobi29.scapes.server.ScapesServer;
import org.tobi29.scapes.server.format.WorldSource;

import java.io.IOException;

public class GameStateGameSP extends GameStateGameMP {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(GameStateGameSP.class);
    private final WorldSource source;
    private final ScapesServer server;

    public GameStateGameSP(
            IOFunction<GameStateGameMP, ClientConnection> clientSupplier,
            WorldSource source, ScapesServer server, Scene scene,
            ScapesEngine engine) throws IOException {
        super(clientSupplier, scene, engine);
        this.server = server;
        this.source = source;
    }

    @Override
    public void dispose(GL gl) {
        super.dispose(gl);
        try {
            server.stop(ScapesServer.ShutdownReason.STOP);
            source.panorama(client.world().scene().panorama());
        } catch (IOException e) {
            LOGGER.error("Error stopping internal server: {}", e.toString());
        }
        LOGGER.info("Stopped internal server!");
    }

    @Override
    public void stepComponent(double delta) {
        super.stepComponent(delta);
        if (server.shouldStop()) {
            engine.setState(
                    new GameStateServerDisconnect("Server stopping", engine));
        }
    }
}
