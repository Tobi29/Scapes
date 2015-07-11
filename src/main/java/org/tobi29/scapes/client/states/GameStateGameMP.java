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
import org.tobi29.scapes.Scapes;
import org.tobi29.scapes.client.Playlist;
import org.tobi29.scapes.client.connection.ClientConnection;
import org.tobi29.scapes.client.states.scenes.SceneScapesVoxelWorld;
import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.gui.debug.GuiWidgetDebugValues;
import org.tobi29.scapes.engine.input.ControllerKey;
import org.tobi29.scapes.engine.opengl.GL;
import org.tobi29.scapes.engine.opengl.scenes.Scene;
import org.tobi29.scapes.entity.client.MobPlayerClientMain;
import org.tobi29.scapes.entity.particle.ParticleBlock;
import org.tobi29.scapes.packets.PacketPingClient;

public class GameStateGameMP extends GameState {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(GameStateGameMP.class);
    protected final ClientConnection client;
    protected final Playlist playlist;
    private final GuiWidgetDebugValues.Element tickDebug;
    protected double pingWait;

    protected GameStateGameMP(ClientConnection client, Scene scene,
            ScapesEngine engine) {
        super(engine, scene);
        playlist = new Playlist(engine.sounds());
        this.client = client;
        tickDebug = engine.debugValues().get("Client-TPS");
    }

    public ClientConnection getClient() {
        return client;
    }

    @Override
    public void dispose(GL gl) {
        client.stop();
        engine.sounds().stopMusic();
        ParticleBlock.clear();
        if (client.getPlugins() != null) {
            client.getPlugins().dispose();
            client.getPlugins().removeFileSystems(engine.files());
        }
        LOGGER.info("Stopped game!");
    }

    @Override
    public void init(GL gl) {
        client.getPlugins().addFileSystems(engine.files());
        client.getPlugins().init();
        client.start(this);
    }

    @Override
    public boolean isMouseGrabbed() {
        return scene instanceof SceneScapesVoxelWorld &&
                ((SceneScapesVoxelWorld) scene).isMouseGrabbed();
    }

    @Override
    public boolean isThreaded() {
        return scene instanceof SceneScapesVoxelWorld;
    }

    @Override
    public void stepComponent(double delta) {
        if (!(scene instanceof SceneScapesVoxelWorld)) {
            return;
        }
        SceneScapesVoxelWorld scene = (SceneScapesVoxelWorld) this.scene;
        if (engine.controller().isPressed(ControllerKey.KEY_F1)) {
            scene.setGuiHide(!scene.isGuiHidden());
        }
        if (Scapes.debug &&
                engine.controller().isPressed(ControllerKey.KEY_F6)) {
            scene.toggleDebug();
        }
        MobPlayerClientMain player = scene.getPlayer();
        playlist.update(player, delta);
        scene.update(delta);
        pingWait -= delta;
        while (pingWait <= 0.0) {
            pingWait += 1.0;
            client.send(new PacketPingClient(System.currentTimeMillis()));
        }
        tickDebug.setValue(1.0 / delta);
    }

    public Playlist getPlaylist() {
        return playlist;
    }
}
