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
import org.tobi29.scapes.block.GameRegistry;
import org.tobi29.scapes.block.Material;
import org.tobi29.scapes.block.TerrainTextureRegistry;
import org.tobi29.scapes.client.ChatHistory;
import org.tobi29.scapes.client.Playlist;
import org.tobi29.scapes.client.connection.ClientConnection;
import org.tobi29.scapes.client.states.scenes.SceneScapesVoxelWorld;
import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.gui.debug.GuiWidgetDebugValues;
import org.tobi29.scapes.engine.input.ControllerKey;
import org.tobi29.scapes.engine.opengl.GL;
import org.tobi29.scapes.engine.opengl.scenes.Scene;
import org.tobi29.scapes.engine.utils.io.IOFunction;
import org.tobi29.scapes.entity.client.MobPlayerClientMain;
import org.tobi29.scapes.entity.particle.ParticleBlock;
import org.tobi29.scapes.entity.skin.ClientSkinStorage;
import org.tobi29.scapes.packets.PacketPingClient;

import java.io.IOException;

public class GameStateGameMP extends GameState {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(GameStateGameMP.class);
    protected final ClientConnection client;
    private final ChatHistory chatHistory;
    private final Playlist playlist;
    private final GuiWidgetDebugValues.Element tickDebug;
    private final TerrainTextureRegistry terrainTextureRegistry;
    private final ClientSkinStorage skinStorage;
    private double pingWait;

    protected GameStateGameMP(
            IOFunction<GameStateGameMP, ClientConnection> clientSupplier,
            Scene scene, ScapesEngine engine) throws IOException {
        super(engine, scene);
        chatHistory = new ChatHistory();
        playlist = new Playlist(engine.sounds());
        client = clientSupplier.apply(this);
        tickDebug = engine.debugValues().get("Client-TPS");
        terrainTextureRegistry = new TerrainTextureRegistry(engine);
        skinStorage = new ClientSkinStorage(engine.graphics().textures()
                .get("Scapes:image/entity/mob/Player"));
    }

    public ClientConnection client() {
        return client;
    }

    @Override
    public void dispose(GL gl) {
        client.stop();
        engine.sounds().stop("music");
        terrainTextureRegistry.dispose(gl);
        skinStorage.dispose(gl);
        ParticleBlock.clear();
        if (client.plugins() != null) {
            client.plugins().dispose();
            client.plugins().removeFileSystems(engine.files());
        }
        LOGGER.info("Stopped game!");
    }

    @Override
    public void init(GL gl) {
        client.plugins().addFileSystems(engine.files());
        client.plugins().init();
        client.plugins().plugins().forEach(plugin -> plugin.initClient(this));
        client.plugins().plugins()
                .forEach(plugin -> plugin.initClientEnd(this));
        long time = System.currentTimeMillis();
        GameRegistry registry = client.plugins().registry();
        for (Material type : registry.materials()) {
            if (type != null) {
                type.registerTextures(terrainTextureRegistry);
            }
        }
        int size = terrainTextureRegistry.init();
        time = System.currentTimeMillis() - time;
        for (Material type : registry.materials()) {
            if (type != null) {
                type.createModels(terrainTextureRegistry);
            }
        }
        LOGGER.info("Loaded terrain models with {} textures in {} ms.", size,
                time);
        client.start();
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
        MobPlayerClientMain player = scene.player();
        chatHistory.update();
        playlist.update(player, delta);
        scene.update(delta);
        pingWait -= delta;
        while (pingWait <= 0.0) {
            pingWait += 1.0;
            client.send(new PacketPingClient(System.currentTimeMillis()));
        }
        tickDebug.setValue(1.0 / delta);
    }

    public TerrainTextureRegistry terrainTextureRegistry() {
        return terrainTextureRegistry;
    }

    public ClientSkinStorage skinStorage() {
        return skinStorage;
    }

    public ChatHistory chatHistory() {
        return chatHistory;
    }

    public Playlist playlist() {
        return playlist;
    }
}
