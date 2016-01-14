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
import org.tobi29.scapes.Debug;
import org.tobi29.scapes.block.GameRegistry;
import org.tobi29.scapes.block.Material;
import org.tobi29.scapes.block.TerrainTextureRegistry;
import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.client.ChatHistory;
import org.tobi29.scapes.client.Playlist;
import org.tobi29.scapes.client.ScapesClient;
import org.tobi29.scapes.client.connection.ClientConnection;
import org.tobi29.scapes.client.gui.GuiComponentGraph;
import org.tobi29.scapes.client.gui.GuiHud;
import org.tobi29.scapes.client.gui.GuiWidgetConnectionProfiler;
import org.tobi29.scapes.client.input.InputMode;
import org.tobi29.scapes.client.states.scenes.SceneScapesVoxelWorld;
import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.gui.*;
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
    private final GuiHud hud;
    private final Gui debug;
    private final GuiWidgetPerformanceClient performanceWidget;
    private final Playlist playlist;
    private final GuiWidgetDebugValues.Element tickDebug;
    private final GuiWidgetDebugClient debugWidget;
    private final GuiWidgetDebugValues connectionSentProfiler,
            connectionReceivedProfiler;
    private final TerrainTextureRegistry terrainTextureRegistry;
    private final ClientSkinStorage skinStorage;
    private InputMode currentInput;
    private double pingWait;

    protected GameStateGameMP(
            IOFunction<GameStateGameMP, ? extends ClientConnection> clientSupplier,
            Scene scene, ScapesEngine engine) throws IOException {
        super(engine, scene);
        chatHistory = new ChatHistory();
        playlist = new Playlist(engine);
        client = clientSupplier.apply(this);
        tickDebug = engine.debugValues().get("Client-TPS");
        terrainTextureRegistry = new TerrainTextureRegistry(engine);
        skinStorage = new ClientSkinStorage(engine.graphics().textures()
                .get("Scapes:image/entity/mob/Player"));
        GuiStyle style = engine.guiStyle();
        hud = new GuiHud(this, style);
        debug = new GuiState(this, style);
        debugWidget = debug.add(32, 32, 160, 200, GuiWidgetDebugClient::new);
        debugWidget.setVisible(false);
        connectionSentProfiler = debug.add(32, 32, 360, 256,
                p -> new GuiWidgetConnectionProfiler(p, client.profilerSent()));
        connectionSentProfiler.setVisible(false);
        connectionReceivedProfiler = debug.add(32, 32, 360, 256,
                p -> new GuiWidgetConnectionProfiler(p,
                        client.profilerReceived()));
        connectionReceivedProfiler.setVisible(false);
        performanceWidget =
                debug.add(32, 32, 240, 96, GuiWidgetPerformanceClient::new);
        performanceWidget.setVisible(false);
    }

    public ClientConnection client() {
        return client;
    }

    @Override
    public void dispose(GL gl) {
        terrainTextureRegistry.dispose();
    }

    @Override
    public void dispose() {
        client.stop();
        skinStorage.dispose();
        engine.sounds().stop("music");
        ParticleBlock.clear();
        if (client.plugins() != null) {
            client.plugins().dispose();
            client.plugins().removeFileSystems(engine.files());
        }
        LOGGER.info("Stopped game!");
    }

    @Override
    public void init(GL gl) {
        engine.guiStack().add("05-HUD", hud);
        engine.guiStack().add("99-SceneDebug", debug);
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
    public void step(double delta) {
        if (!(scene instanceof SceneScapesVoxelWorld)) {
            return;
        }
        SceneScapesVoxelWorld scene = (SceneScapesVoxelWorld) this.scene;
        engine.controller().ifPresent(controller -> {
            if (controller.isPressed(ControllerKey.KEY_F1)) {
                setHudVisible(!hud.isVisible());
            }
            if (Debug.enabled() && controller.isPressed(ControllerKey.KEY_F6)) {
                debugWidget.setVisible(!debugWidget.isVisible());
            }
        });
        InputMode input = ((ScapesClient) engine.game()).inputMode();
        if (input != currentInput) {
            Gui gui = new GuiState(this, engine.guiStyle());
            input.createInGameGUI(gui, scene.world());
            engine.guiStack().add("04-Input", gui);
            currentInput = input;
        }
        MobPlayerClientMain player = scene.player();
        chatHistory.update();
        playlist.update(player, delta);
        player.world().update(delta);
        updateTimestamp(delta);
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

    public void updateTimestamp(double delta) {
        performanceWidget.graph.addStamp(delta, 1);
    }

    public void renderTimestamp(double delta) {
        performanceWidget.graph.addStamp(delta, 0);
    }

    public void setHudVisible(boolean visible) {
        hud.setVisible(visible);
    }

    public GuiHud hud() {
        return hud;
    }

    private static class GuiWidgetPerformanceClient extends GuiComponentWidget {
        private final GuiComponentGraph graph;

        private GuiWidgetPerformanceClient(GuiLayoutData parent) {
            super(parent, "Performance Graph");
            graph = addVert(0, 0, -1, -1,
                    p -> new GuiComponentGraph(p, 2, new float[]{1.0f, 0.0f},
                            new float[]{0.0f, 0.0f}, new float[]{0.0f, 1.0f},
                            new float[]{1.0f, 1.0f}));
        }
    }

    private class GuiWidgetDebugClient extends GuiComponentWidget {
        private GuiWidgetDebugClient(GuiLayoutData parent) {
            super(parent, "Debug Values");
            GuiComponentTextButton geometry = addVert(10, 10, 10, 2, 140, 15,
                    p -> new GuiComponentTextButton(p, 12, "Geometry"));
            GuiComponentTextButton wireframe = addVert(10, 2, 140, 15,
                    p -> new GuiComponentTextButton(p, 12, "Wireframe"));
            GuiComponentTextButton distance = addVert(10, 2, 140, 15,
                    p -> new GuiComponentTextButton(p, 12,
                            "Static Render Distance"));
            GuiComponentTextButton reloadGeometry = addVert(10, 2, 140, 15,
                    p -> new GuiComponentTextButton(p, 12, "Reload Geometry"));
            GuiComponentTextButton performance = addVert(10, 2, 140, 15,
                    p -> new GuiComponentTextButton(p, 12, "Performance"));
            GuiComponentTextButton connSent = addVert(10, 2, 140, 15,
                    p -> new GuiComponentTextButton(p, 12, "Conn. Sent"));
            GuiComponentTextButton connSentReset = addVert(10, 2, 140, 15,
                    p -> new GuiComponentTextButton(p, 12, "Conn. Sent Reset"));
            GuiComponentTextButton connReceived = addVert(10, 2, 140, 15,
                    p -> new GuiComponentTextButton(p, 12, "Conn. Received"));
            GuiComponentTextButton connReceivedReset = addVert(10, 2, 140, 15,
                    p -> new GuiComponentTextButton(p, 12,
                            "Conn. Received Reset"));

            geometry.onClickLeft(event -> {
                Scene scene = GameStateGameMP.this.scene;
                if (scene instanceof SceneScapesVoxelWorld) {
                    ((SceneScapesVoxelWorld) scene).toggleChunkDebug();
                }
            });
            wireframe.onClickLeft(event -> {
                Scene scene = GameStateGameMP.this.scene;
                if (scene instanceof SceneScapesVoxelWorld) {
                    ((SceneScapesVoxelWorld) scene).toggleWireframe();
                }
            });
            distance.onClickLeft(event -> {
                WorldClient world = client.world();
                if (world != null) {
                    world.terrain().toggleStaticRenderDistance();
                }
            });
            reloadGeometry.onClickLeft(event -> {
                WorldClient world = client.world();
                if (world != null) {
                    world.terrain().reloadGeometry();
                }
            });
            performance.onClickLeft(event -> performanceWidget
                    .setVisible(!performanceWidget.isVisible()));
            connSent.onClickLeft(event -> connectionSentProfiler
                    .setVisible(!connectionSentProfiler.isVisible()));
            connSentReset.onClickLeft(event -> {
                client.profilerSent().clear();
                connectionSentProfiler.clear();
            });
            connReceived.onClickLeft(event -> connectionReceivedProfiler
                    .setVisible(!connectionReceivedProfiler.isVisible()));
            connReceivedReset.onClickLeft(event -> {
                client.profilerReceived().clear();
                connectionReceivedProfiler.clear();
            });
        }
    }
}
