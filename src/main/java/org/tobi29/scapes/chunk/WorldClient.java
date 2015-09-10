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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.chunk.terrain.TerrainClient;
import org.tobi29.scapes.chunk.terrain.TerrainRenderInfo;
import org.tobi29.scapes.client.connection.ClientConnection;
import org.tobi29.scapes.client.states.GameStateGameMP;
import org.tobi29.scapes.client.states.scenes.SceneScapesVoxelWorld;
import org.tobi29.scapes.engine.opengl.BlendingMode;
import org.tobi29.scapes.engine.opengl.GL;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.engine.opengl.shader.ShaderManager;
import org.tobi29.scapes.engine.utils.graphics.Cam;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.math.AABB;
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
import org.tobi29.scapes.packets.Packet;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WorldClient extends World {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(WorldClient.class);
    private final Map<Integer, EntityClient> entities =
            new ConcurrentHashMap<>();
    private final Map<String, Supplier<TerrainRenderInfo.InfoLayer>>
            infoLayers = new ConcurrentHashMap<>();
    private final SceneScapesVoxelWorld scene;
    private final MobPlayerClientMain player;
    private final MobModel playerModel;
    private final ClientConnection connection;
    private final GameStateGameMP game;
    private final TerrainClient terrain;
    private final Map<Integer, EntityModel> entityModels =
            new ConcurrentHashMap<>();
    private final ParticleManager particleManager = new ParticleManager(this);
    private final EnvironmentClient environment;

    public WorldClient(ClientConnection connection, Cam cam, long seed,
            Function<WorldClient, TerrainClient> terrainSupplier,
            Function<WorldClient, EnvironmentClient> environmentSupplier,
            TagStructure playerTag, int playerID) {
        super(connection.plugins(), connection.game().engine().taskExecutor(),
                connection.plugins().registry());
        this.connection = connection;
        game = connection.game();
        player = connection.plugins().worldType()
                .newPlayer(this, Vector3d.ZERO, Vector3d.ZERO, 0.0, 0.0, "");
        player.read(playerTag);
        this.seed = seed;
        scene = new SceneScapesVoxelWorld(this, cam);
        playerModel = player.createModel().get();
        addEntity(player, playerID);
        LOGGER.info("Received player entity: {} with id: {}", player, playerID);
        environment = environmentSupplier.apply(this);
        connection.plugins().plugins()
                .forEach(plugin -> plugin.worldInit(this));
        terrain = terrainSupplier.apply(this);
    }

    public List<MobClient> entities(List<MobClient> exceptions,
            Frustum hitField) {
        return entities.values().stream()
                .filter(entity -> entity instanceof MobClient)
                .map(entity -> (MobClient) entity)
                .filter(mob -> !exceptions.contains(mob) &&
                        hitField.inView(mob.aabb()) > 0)
                .collect(Collectors.toList());
    }

    public void addEntity(EntityClient add, int id) {
        if (id != player.entityID()) {
            if (add != player) {
                add.createModel()
                        .ifPresent(model -> entityModels.put(id, model));
            }
            add.setEntityID(id);
            entities.put(id, add);
        }
    }

    public void removeEntity(EntityClient del) {
        if (del != null) {
            entities.remove(del.entityID());
            entityModels.remove(del.entityID());
        }
    }

    public Stream<EntityClient> entities() {
        return entities.values().stream();
    }

    public Optional<EntityClient> entity(int i) {
        return Optional.ofNullable(entities.get(i));
    }

    public void update(double delta) {
        entities.values().stream().forEach(entity -> {
            if (terrain.isBlockTicking(FastMath.floor(entity.x()),
                    FastMath.floor(entity.y()), FastMath.floor(entity.z()))) {
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
        scene.terrainTextureRegistry().update(delta);
        scene.skybox().update(delta);
        spawn = player.pos();
    }

    public void updateRender(Cam cam, double delta) {
        playerModel.renderUpdate(delta);
        entityModels.values().forEach(model -> model.renderUpdate(delta));
        terrain.renderer().renderUpdate(cam);
    }

    public void render(GL gl, Cam cam, float animationDistance, boolean debug) {
        float time = (System.currentTimeMillis() % 10000000) / 1000.0f;
        float sunLightReduction = environment
                .sunLightReduction(cam.position.doubleX(),
                        cam.position.doubleY()) / 15.0f;
        float playerLight = FastMath.max(
                player.leftWeapon().material().playerLight(player.leftWeapon()),
                player.rightWeapon().material()
                        .playerLight(player.rightWeapon()));
        Vector3 sunlightNormal = environment
                .sunLightNormal(cam.position.doubleX(), cam.position.doubleY());
        ShaderManager shaderManager = gl.shaders();
        Shader shaderTerrain = shaderManager.get("Scapes:shader/Terrain", gl);
        shaderTerrain.setUniform3f(4, scene.fogR(), scene.fogG(), scene.fogB());
        shaderTerrain
                .setUniform1f(5, scene.fogDistance() * scene.renderDistance());
        shaderTerrain.setUniform1i(6, 1);
        shaderTerrain.setUniform2f(7, gl.sceneWidth(), gl.sceneHeight());
        shaderTerrain.setUniform1f(8, time);
        shaderTerrain.setUniform1f(9, sunLightReduction);
        shaderTerrain.setUniform3f(10, sunlightNormal.floatX(),
                sunlightNormal.floatY(), sunlightNormal.floatZ());
        shaderTerrain.setUniform1f(11, playerLight);
        shaderTerrain.setUniform1f(12, animationDistance * cam.far);
        Shader shaderEntity = shaderManager.get("Scapes:shader/Entity", gl);
        shaderEntity.setUniform3f(4, scene.fogR(), scene.fogG(), scene.fogB());
        shaderEntity
                .setUniform1f(5, scene.fogDistance() * scene.renderDistance());
        shaderEntity.setUniform1i(6, 1);
        shaderEntity.setUniform2f(7, gl.sceneWidth(), gl.sceneHeight());
        shaderEntity.setUniform1f(8, time);
        shaderEntity.setUniform1f(9, sunLightReduction);
        shaderEntity.setUniform3f(10, sunlightNormal.floatX(),
                sunlightNormal.floatY(), sunlightNormal.floatZ());
        shaderEntity.setUniform1f(11, playerLight);
        gl.setBlending(BlendingMode.NONE);
        scene.terrainTextureRegistry().texture().bind(gl);
        terrain.renderer().render(gl, shaderTerrain, cam, debug);
        gl.setBlending(BlendingMode.NORMAL);
        if (!scene.isGuiHidden()) {
            playerModel.render(gl, this, cam, shaderEntity);
        }
        AABB aabb = new AABB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        entityModels.values().stream().filter(model -> {
            model.shapeAABB(aabb);
            return cam.frustum.inView(aabb) != 0;
        }).forEach(model -> model.render(gl, this, cam, shaderEntity));
        particleManager.render(gl, cam);
        scene.terrainTextureRegistry().texture().bind(gl);
        terrain.renderer().renderAlpha(gl, shaderTerrain, cam);
    }

    public TerrainClient terrain() {
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
            if (!terrain.type(x, y, z).isTransparent(terrain, x, y, z)) {
                return true;
            }
        }
        return false;
    }

    public void playSound(String audio, EntityClient entity) {
        playSound(audio, entity, 1.0f, 1.0f);
    }

    public void playSound(String audio, EntityClient entity, float pitch,
            float gain) {
        if (entity instanceof MobClient) {
            playSound(audio, entity.pos(), ((MobClient) entity).speed(), pitch,
                    gain);
        } else {
            playSound(audio, entity.pos(), Vector3d.ZERO, pitch, gain);
        }
    }

    public void playSound(String audio, Vector3 position, Vector3 velocity) {
        playSound(audio, position, velocity, 1.0f, 1.0f);
    }

    public void playSound(String audio, Vector3 position, Vector3 velocity,
            float pitch, float gain) {
        game.engine().sounds()
                .playSound(audio, "sound.World", position, velocity, pitch,
                        gain);
    }

    public MobPlayerClientMain player() {
        return player;
    }

    public MobModel playerModel() {
        return playerModel;
    }

    public GameStateGameMP game() {
        return game;
    }

    public SceneScapesVoxelWorld scene() {
        return scene;
    }

    public ParticleManager particleManager() {
        return particleManager;
    }

    public EnvironmentClient environment() {
        return environment;
    }

    public void infoLayer(String name,
            Supplier<TerrainRenderInfo.InfoLayer> layer) {
        infoLayers.put(name, layer);
    }

    public Stream<Map.Entry<String, Supplier<TerrainRenderInfo.InfoLayer>>> infoLayers() {
        return infoLayers.entrySet().stream();
    }

    public void dispose(GL gl) {
        terrain.dispose(gl);
    }

    @Override
    public void send(Packet packet) {
        connection.send(packet);
    }
}
