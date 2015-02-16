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
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.entity.client.MobPlayerClientMain;
import org.tobi29.scapes.entity.particle.ParticleBlock;
import org.tobi29.scapes.packets.PacketPing;

public class GameStateGameMP extends GameState {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(GameStateGameMP.class);
    protected final ClientConnection client;
    protected final Sync sync =
            new Sync(60, 5000000000L, true, "Client-Update");
    protected final Playlist playlist;
    protected final int loadingRadius;
    private final GuiWidgetDebugValues.Element tickDebug;
    protected double pingWait;

    protected GameStateGameMP(ClientConnection client, Scene scene,
            ScapesEngine engine) {
        super(engine, scene);
        loadingRadius = (int) FastMath.ceil((10 +
                engine.getTagStructure().getStructure("Scapes")
                        .getFloat("RenderDistance") * 246) / 16.0f) << 4;
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
        resetAPI();
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
    public void stepComponent(double delta) {
        if (!(scene instanceof SceneScapesVoxelWorld)) {
            sync.init();
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
        MobPlayerClientMain player = scene.getPlayer();
        playlist.update(player, delta);
        scene.getWorld().update(delta);
        pingWait -= delta;
        while (pingWait <= 0.0) {
            pingWait += 1.0;
            client.send(new PacketPing(System.currentTimeMillis()));
        }
        sync.capTPS();
        tickDebug.setValue(sync.getTPS());
    }

    protected void resetAPI() {
        LOGGER.info("Reset api!");
        engine.getSounds().stopMusic();
        ParticleBlock.clear();
        if (client != null) {
            if (client.getPlugins() != null) {
                client.getPlugins().dispose();
                client.getPlugins().removeFileSystems(engine.getFiles());
            }
        }
    }

    public Playlist getPlaylist() {
        return playlist;
    }
}
