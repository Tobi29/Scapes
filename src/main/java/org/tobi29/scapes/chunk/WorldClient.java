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

package org.tobi29.scapes.chunk;

import org.tobi29.scapes.chunk.terrain.TerrainClient;
import org.tobi29.scapes.client.connection.ClientConnection;
import org.tobi29.scapes.client.states.GameStateGameMP;
import org.tobi29.scapes.client.states.scenes.SceneScapesVoxelWorld;
import org.tobi29.scapes.engine.opengl.BlendingMode;
import org.tobi29.scapes.engine.opengl.GraphicsSystem;
import org.tobi29.scapes.engine.opengl.OpenGL;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.engine.opengl.shader.ShaderManager;
import org.tobi29.scapes.engine.utils.graphics.Cam;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.Frustum;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;
import org.tobi29.scapes.entity.client.EntityClient;
import org.tobi29.scapes.entity.client.MobClient;
import org.tobi29.scapes.entity.client.MobPlayerClientMain;
import org.tobi29.scapes.entity.model.EntityModel;
import org.tobi29.scapes.entity.model.MobModel;
import org.tobi29.scapes.entity.particle.ParticleManager;
import org.tobi29.scapes.plugins.Dimension;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WorldClient extends World {
    private final Map<Integer, EntityClient> entities =
            new ConcurrentHashMap<>();
    private final SceneScapesVoxelWorld scene;
    private final MobPlayerClientMain player;
    private final MobModel playerModel;
    private final GameStateGameMP game;
    private final TerrainClient terrain;
    private final Map<Integer, EntityModel> entityModels =
            new ConcurrentHashMap<>();
    private final ParticleManager particleManager = new ParticleManager(this);

    public WorldClient(ClientConnection connection, Cam cam, long seed,
            String name, TerrainSupplier terrainSupplier) {
        super(connection, connection.getPlugins(),
                connection.getGame().getEngine().getTaskExecutor(),
                connection.getPlugins().getRegistry());
        game = connection.getGame();
        player =
                new MobPlayerClientMain(this, Vector3d.ZERO, Vector3d.ZERO, 0.0,
                        0.0, "", "");
        this.seed = seed;
        environment =
                ((Dimension) plugins.getPlugin(name)).createEnvironment(this);
        scene = new SceneScapesVoxelWorld(this, cam);
        playerModel = player.createModel();
        terrain = terrainSupplier.get(this);
    }

    public List<MobClient> getEntities(List<MobClient> exceptions,
            Frustum hitField) {
        return entities.values().stream()
                .filter(entity -> entity instanceof MobClient)
                .map(entity -> (MobClient) entity)
                .filter(mob -> !exceptions.contains(mob) &&
                        hitField.inView(mob.getAABB()) > 0)
                .collect(Collectors.toList());
    }

    public void addEntity(EntityClient add, int id) {
        if (id != player.getEntityID()) {
            if (add != player) {
                EntityModel model = add.createModel();
                if (model != null) {
                    entityModels.put(id, model);
                }
            }
            add.setEntityID(id);
            entities.put(id, add);
        }
    }

    public void removeEntity(EntityClient del) {
        if (del != null) {
            entities.remove(del.getEntityID());
            entityModels.remove(del.getEntityID());
        }
    }

    public Stream<EntityClient> getEntities() {
        return entities.values().stream();
    }

    public EntityClient getEntity(int i) {
        return entities.get(i);
    }

    public void update(double delta) {
        entities.values().stream().forEach(entity -> {
            if (terrain.isBlockTicking(FastMath.floor(entity.getX()),
                    FastMath.floor(entity.getY()),
                    FastMath.floor(entity.getZ()))) {
                entity.update(delta);
                if (entity instanceof MobClient) {
                    ((MobClient) entity).move(delta);
                }
            } else {
                if (entity == player) {
                    player.updatePosition();
                } else {
                    removeEntity(entity);
                }
            }
        });
        terrain.update(delta);
        particleManager.update(delta);
        environment.tick(delta);
        scene.getTerrainTextureRegistry().update(delta);
        scene.getSkybox().update(delta);
        spawn = player.getPos();
    }

    public void updateRender(GraphicsSystem graphics, Cam cam, double delta) {
        playerModel.renderUpdate(graphics, this, delta);
        entityModels.values()
                .forEach(model -> model.renderUpdate(graphics, this, delta));
        terrain.getTerrainRenderer().renderUpdate(graphics, cam);
    }

    public void render(GraphicsSystem graphics, Cam cam,
            float animationDistance, boolean debug) {
        float time = (System.currentTimeMillis() % 10000000) / 1000.0f;
        OpenGL openGL = graphics.getOpenGL();
        float sunLightReduction = environment
                .getSunLightReduction(cam.position.doubleX(),
                        cam.position.doubleY()) / 15.0f;
        Vector3 sunlightNormal = environment
                .getSunLightNormal(cam.position.doubleX(),
                        cam.position.doubleY());
        ShaderManager shaderManager = graphics.getShaderManager();
        Shader shaderTerrain =
                shaderManager.getShader("Scapes:shader/Terrain", graphics);
        shaderTerrain.setUniform3f(4, scene.getFogR(), scene.getFogG(),
                scene.getFogB());
        shaderTerrain.setUniform1f(5,
                scene.getFogDistance() * scene.getRenderDistance());
        shaderTerrain.setUniform1i(6, 1);
        shaderTerrain.setUniform2f(7, graphics.getSceneWidth(),
                graphics.getSceneHeight());
        shaderTerrain.setUniform1f(8, time);
        shaderTerrain.setUniform1f(9, sunLightReduction);
        shaderTerrain.setUniform3f(10, sunlightNormal.floatX(),
                sunlightNormal.floatY(), sunlightNormal.floatZ());
        shaderTerrain.setUniform1f(11, FastMath.max(
                player.getLeftWeapon().getMaterial()
                        .getPlayerLight(player.getLeftWeapon()),
                player.getRightWeapon().getMaterial()
                        .getPlayerLight(player.getRightWeapon())));
        shaderTerrain.setUniform1f(12, animationDistance * cam.far);
        openGL.setBlending(BlendingMode.NONE);
        scene.getTerrainTextureRegistry().getTexture().bind(graphics);
        terrain.getTerrainRenderer()
                .render(graphics, shaderTerrain, cam, debug);
        openGL.setBlending(BlendingMode.NORMAL);
        Shader shaderEntity =
                shaderManager.getShader("Scapes:shader/Entity", graphics);
        shaderEntity.setUniform3f(4, scene.getFogR(), scene.getFogG(),
                scene.getFogB());
        shaderEntity.setUniform1f(5,
                scene.getFogDistance() * scene.getRenderDistance());
        shaderEntity.setUniform1i(6, 1);
        shaderEntity.setUniform2f(7, graphics.getSceneWidth(),
                graphics.getSceneHeight());
        shaderEntity.setUniform1f(8, time);
        shaderEntity.setUniform1f(9, sunLightReduction);
        shaderEntity.setUniform3f(10, sunlightNormal.floatX(),
                sunlightNormal.floatY(), sunlightNormal.floatZ());
        shaderEntity.setUniform1f(11, FastMath.max(
                player.getLeftWeapon().getMaterial()
                        .getPlayerLight(player.getLeftWeapon()),
                player.getRightWeapon().getMaterial()
                        .getPlayerLight(player.getRightWeapon())));
        if (!scene.isGuiHidden()) {
            playerModel.render(graphics, this, cam, shaderEntity);
        }
        entityModels.values().forEach(
                model -> model.render(graphics, this, cam, shaderEntity));
        particleManager.render(graphics, cam);
        scene.getTerrainTextureRegistry().getTexture().bind(graphics);
        terrain.getTerrainRenderer().renderAlpha(graphics, shaderTerrain, cam);
    }

    public TerrainClient getTerrain() {
        return terrain;
    }

    public boolean checkBlocked(int x1, int y1, int z1, int x2, int y2,
            int z2) {
        int dx = x2 - x1;
        int dy = y2 - y1;
        int dz = z2 - z1;
        double step = 1 / FastMath.sqrt(dx * dx + dy * dy + dz * dz);
        for (double i = 0; i <= 1; i += step) {
            int x = FastMath.floor(x1 + dx * i);
            int y = FastMath.floor(y1 + dy * i);
            int z = FastMath.floor(z1 + dz * i);
            if (!terrain.getBlockType(x, y, z)
                    .isTransparent(terrain, x, y, z)) {
                return true;
            }
        }
        return false;
    }

    public void playSound(String audio, EntityClient entity) {
        playSound(audio, entity, 16.0f);
    }

    public void playSound(String audio, EntityClient entity, float range) {
        playSound(audio, entity, 1.0f, 1.0f, range);
    }

    public void playSound(String audio, EntityClient entity, float pitch,
            float gain, float range) {
        if (entity instanceof MobClient) {
            playSound(audio, entity.getPos(), ((MobClient) entity).getSpeed(),
                    pitch, gain, range);
        } else {
            playSound(audio, entity.getPos(), Vector3d.ZERO, pitch, gain,
                    range);
        }
    }

    public void playSound(String audio, Vector3 position, Vector3 velocity,
            float range) {
        playSound(audio, position, velocity, 1.0f, 1.0f, range);
    }

    public void playSound(String name, Vector3 position, Vector3 velocity) {
        playSound(name, position, velocity, 16.0f);
    }

    public void playSound(String audio, Vector3 position, Vector3 velocity,
            float pitch, float gain, float range) {
        game.getEngine().getSounds()
                .playSound(audio, position, velocity, pitch, gain, range);
    }

    public MobPlayerClientMain getPlayer() {
        return player;
    }

    public MobModel getPlayerModel() {
        return playerModel;
    }

    public GameStateGameMP getGame() {
        return game;
    }

    public SceneScapesVoxelWorld getScene() {
        return scene;
    }

    public ParticleManager getParticleManager() {
        return particleManager;
    }

    public void dispose() {
        terrain.dispose();
    }

    @FunctionalInterface
    public interface TerrainSupplier {
        TerrainClient get(WorldClient world);
    }
}
