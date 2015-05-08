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
import org.tobi29.scapes.engine.opengl.scenes.Scene;
import org.tobi29.scapes.engine.utils.Sync;
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
        playlist = new Playlist(engine.getSounds());
        this.client = client;
        tickDebug = engine.getDebugValues().get("Client-TPS");
    }

    public ClientConnection getClient() {
        return client;
    }

    @Override
    public void dispose() {
        client.stop();
        engine.getSounds().stopMusic();
        ParticleBlock.clear();
        if (client.getPlugins() != null) {
            client.getPlugins().dispose();
            client.getPlugins().removeFileSystems(engine.getFiles());
        }
        LOGGER.info("Stopped game!");
    }

    @Override
    public void init() {
        client.getPlugins().addFileSystems(engine.getFiles());
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
    public boolean forceRender() {
        return !(scene instanceof SceneScapesVoxelWorld);
    }

    @Override
    public void stepComponent(Sync sync) {
        if (!(scene instanceof SceneScapesVoxelWorld)) {
            return;
        }
        SceneScapesVoxelWorld scene = (SceneScapesVoxelWorld) this.scene;
        if (engine.getController().isPressed(ControllerKey.KEY_F1)) {
            scene.setGuiHide(!scene.isGuiHidden());
        }
        if (Scapes.debug &&
                engine.getController().isPressed(ControllerKey.KEY_F6)) {
            scene.toggleDebug();
        }
        double delta = sync.getSpeedFactor();
        MobPlayerClientMain player = scene.getPlayer();
        playlist.update(player, delta);
        scene.update(delta);
        pingWait -= delta;
        while (pingWait <= 0.0) {
            pingWait += 1.0;
            client.send(new PacketPingClient(System.currentTimeMillis()));
        }
        tickDebug.setValue(sync.getTPS());
    }

    public Playlist getPlaylist() {
        return playlist;
    }
}
