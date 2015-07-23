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
import org.tobi29.scapes.server.ScapesServer;

import java.io.IOException;

public class GameStateGameSP extends GameStateGameMP {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(GameStateGameSP.class);
    private final ScapesServer server;

    public GameStateGameSP(ClientConnection client, ScapesServer server,
            Scene scene, ScapesEngine engine) {
        super(client, scene, engine);
        this.server = server;
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
}
