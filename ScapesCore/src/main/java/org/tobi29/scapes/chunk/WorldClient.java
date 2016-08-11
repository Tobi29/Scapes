/*
 * Copyright 2012-2016 Tobi29
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

import java8.util.Optional;
import java8.util.function.Consumer;
import java8.util.function.Function;
import java8.util.function.Supplier;
import java8.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.chunk.terrain.TerrainClient;
import org.tobi29.scapes.chunk.terrain.TerrainRenderInfo;
import org.tobi29.scapes.client.connection.ClientConnection;
import org.tobi29.scapes.client.states.GameStateGameMP;
import org.tobi29.scapes.client.states.scenes.SceneScapesVoxelWorld;
import org.tobi29.scapes.connection.PlayConnection;
import org.tobi29.scapes.engine.graphics.BlendingMode;
import org.tobi29.scapes.engine.graphics.GL;
import org.tobi29.scapes.engine.graphics.GraphicsSystem;
import org.tobi29.scapes.engine.graphics.Shader;
import org.tobi29.scapes.engine.utils.Streams;
import org.tobi29.scapes.engine.utils.graphics.Cam;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.math.AABB;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.Frustum;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;
import org.tobi29.scapes.engine.utils.profiler.Profiler;
import org.tobi29.scapes.entity.EntityCollector;
import org.tobi29.scapes.entity.EntityContainer;
import org.tobi29.scapes.entity.client.EntityClient;
import org.tobi29.scapes.entity.client.MobClient;
import org.tobi29.scapes.entity.client.MobPlayerClientMain;
import org.tobi29.scapes.entity.model.EntityModel;
import org.tobi29.scapes.entity.model.MobModel;
import org.tobi29.scapes.packets.PacketServer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WorldClient extends World<EntityClient>
        implements EntityContainer<EntityClient>, PlayConnection<PacketServer> {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(WorldClient.class);
    private final Map<String, Supplier<TerrainRenderInfo.InfoLayer>>
            infoLayers = new ConcurrentHashMap<>();
    private final SceneScapesVoxelWorld scene;
    private final MobPlayerClientMain player;
    private final MobModel playerModel;
    private final ClientConnection connection;
    private final GameStateGameMP game;
    private final TerrainClient terrain;
    private final Shader shaderTerrain1, shaderTerrain2, shaderEntity;
    private final Map<UUID, EntityModel> entityModels =
            new ConcurrentHashMap<>();
    private final EnvironmentClient environment;
    private boolean disposed;

    public WorldClient(ClientConnection connection, Cam cam, long seed,
            Function<WorldClient, ? extends TerrainClient> terrainSupplier,
            Function<WorldClient, ? extends EnvironmentClient> environmentSupplier,
            TagStructure playerTag, UUID playerID) {
        super(connection.plugins(), connection.game().engine().taskExecutor(),
                connection.plugins().registry(), seed);
        this.connection = connection;
        game = connection.game();
        environment = environmentSupplier.apply(this);
        player = connection.plugins().worldType()
                .newPlayer(this, Vector3d.ZERO, Vector3d.ZERO, 0.0, 0.0, "");
        player.setEntityID(playerID);
        player.read(playerTag);
        scene = new SceneScapesVoxelWorld(this, cam);
        playerModel = player.createModel().get();
        connection.plugins().plugins()
                .forEach(plugin -> plugin.worldInit(this));
        terrain = terrainSupplier.apply(this);
        LOGGER.info("Received player entity: {} with id: {}", player, playerID);
        TagStructure scapesTag =
                game.engine().tagStructure().getStructure("Scapes");
        GraphicsSystem graphics = connection.game().engine().graphics();
        shaderTerrain1 = graphics.createShader("Scapes:shader/Terrain",
                information -> information.supplyPreCompile(shader -> {
                    shader.supplyProperty("ENABLE_ANIMATIONS",
                            scapesTag.getBoolean("Animations"));
                    shader.supplyProperty("LOD_LOW", false);
                    shader.supplyProperty("LOD_HIGH", true);
                }));
        shaderTerrain2 = graphics.createShader("Scapes:shader/Terrain",
                information -> information.supplyPreCompile(shader -> {
                    shader.supplyProperty("ENABLE_ANIMATIONS", false);
                    shader.supplyProperty("LOD_LOW", true);
                    shader.supplyProperty("LOD_HIGH", false);
                }));
        shaderEntity = graphics.createShader("Scapes:shader/Entity");
    }

    public void addEntityModel(EntityClient entity) {
        entity.createModel()
                .ifPresent(model -> entityModels.put(entity.uuid(), model));
    }

    public void removeEntityModel(EntityClient entity) {
        entityModels.remove(entity.uuid());
    }

    @Override
    public boolean addEntity(EntityClient entity) {
        return terrain.addEntity(entity);
    }

    @Override
    public boolean removeEntity(EntityClient entity) {
        return terrain.removeEntity(entity);
    }

    @Override
    public boolean hasEntity(EntityClient entity) {
        return player == entity || terrain.hasEntity(entity);
    }

    @Override
    public Optional<EntityClient> entity(UUID uuid) {
        if (player.uuid().equals(uuid)) {
            return Optional.of(player);
        }
        return terrain.entity(uuid);
    }

    @Override
    public void entities(Consumer<Stream<? extends EntityClient>> consumer) {
        terrain.entities(consumer);
        consumer.accept(Streams.of(player));
    }

    @Override
    public void entities(int x, int y, int z,
            Consumer<Stream<? extends EntityClient>> consumer) {
        terrain.entities(x, y, z, consumer);
        consumer.accept(Streams.of(player)
                .filter(entity -> FastMath.floor(entity.x()) == x)
                .filter(entity -> FastMath.floor(entity.y()) == y)
                .filter(entity -> FastMath.floor(entity.z()) == z));
    }

    @Override
    public void entitiesAtLeast(int minX, int minY, int minZ, int maxX,
            int maxY, int maxZ,
            Consumer<Stream<? extends EntityClient>> consumer) {
        terrain.entities(minX, minY, minZ, maxX, maxY, maxZ, consumer);
        consumer.accept(Streams.of(player));
    }

    @Override
    public void entityAdded(EntityClient entity) {
        addEntityModel(entity);
    }

    @Override
    public void entityRemoved(EntityClient entity) {
        removeEntityModel(entity);
    }

    public void update(double delta) {
        try (Profiler.C ignored = Profiler.section("Entities")) {
            if (terrain.isBlockTicking(FastMath.floor(player.x()),
                    FastMath.floor(player.y()), FastMath.floor(player.z()))) {
                player.update(delta);
                player.move(delta);
            } else {
                player.updatePosition();
            }
        }
        try (Profiler.C ignored = Profiler.section("Terrain")) {
            terrain.update(delta);
        }
        try (Profiler.C ignored = Profiler.section("Environment")) {
            environment.tick(delta);
        }
        try (Profiler.C ignored = Profiler.section("Textures")) {
            scene.terrainTextureRegistry().update(delta);
        }
        try (Profiler.C ignored = Profiler.section("Skybox")) {
            scene.skybox().update(delta);
        }
        spawn = player.pos();
    }

    public void updateRender(Cam cam, double delta) {
        playerModel.renderUpdate(delta);
        Streams.forEach(entityModels.values(),
                model -> model.renderUpdate(delta));
        terrain.renderer().renderUpdate(cam);
    }

    public void render(GL gl, Cam cam, boolean debug) {
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
        shaderTerrain1
                .setUniform3f(4, scene.fogR(), scene.fogG(), scene.fogB());
        shaderTerrain1
                .setUniform1f(5, scene.fogDistance() * scene.renderDistance());
        shaderTerrain1.setUniform1i(6, 1);
        shaderTerrain1.setUniform1f(7, time);
        shaderTerrain1.setUniform1f(8, sunLightReduction);
        shaderTerrain1.setUniform3f(9, sunlightNormal.floatX(),
                sunlightNormal.floatY(), sunlightNormal.floatZ());
        shaderTerrain1.setUniform1f(10, playerLight);
        shaderTerrain2
                .setUniform3f(4, scene.fogR(), scene.fogG(), scene.fogB());
        shaderTerrain2
                .setUniform1f(5, scene.fogDistance() * scene.renderDistance());
        shaderTerrain2.setUniform1i(6, 1);
        shaderTerrain2.setUniform1f(7, time);
        shaderTerrain2.setUniform1f(8, sunLightReduction);
        shaderTerrain2.setUniform3f(9, sunlightNormal.floatX(),
                sunlightNormal.floatY(), sunlightNormal.floatZ());
        shaderTerrain2.setUniform1f(10, playerLight);
        shaderEntity.setUniform3f(4, scene.fogR(), scene.fogG(), scene.fogB());
        shaderEntity
                .setUniform1f(5, scene.fogDistance() * scene.renderDistance());
        shaderEntity.setUniform1i(6, 1);
        shaderEntity.setUniform1f(7, time);
        shaderEntity.setUniform1f(8, sunLightReduction);
        shaderEntity.setUniform3f(9, sunlightNormal.floatX(),
                sunlightNormal.floatY(), sunlightNormal.floatZ());
        shaderEntity.setUniform1f(10, playerLight);
        gl.setBlending(BlendingMode.NONE);
        scene.terrainTextureRegistry().texture().bind(gl);
        terrain.renderer()
                .render(gl, shaderTerrain1, shaderTerrain2, cam, debug);
        gl.setBlending(BlendingMode.NORMAL);
        if (game.hud().isVisible()) {
            playerModel.render(gl, this, cam, shaderEntity);
        }
        AABB aabb = new AABB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        Streams.forEach(entityModels.values(), model -> {
            model.shapeAABB(aabb);
            return cam.frustum.inView(aabb) != 0;
        }, model -> model.render(gl, this, cam, shaderEntity));
        scene.terrainTextureRegistry().texture().bind(gl);
        terrain.renderer().renderAlpha(gl, shaderTerrain1, shaderTerrain2, cam);
        scene.particles().render(gl, cam);
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

    public EnvironmentClient environment() {
        return environment;
    }

    public void infoLayer(String name,
            Supplier<TerrainRenderInfo.InfoLayer> layer) {
        infoLayers.put(name, layer);
    }

    public Stream<Map.Entry<String, Supplier<TerrainRenderInfo.InfoLayer>>> infoLayers() {
        return Streams.of(infoLayers.entrySet());
    }

    public boolean disposed() {
        return disposed;
    }

    public void dispose() {
        terrain.dispose();
        disposed = true;
    }

    @Override
    public void send(PacketServer packet) {
        connection.send(packet);
    }

    @Override
    protected Stream<MobPlayerClientMain> worldEntities() {
        return Streams.of(player);
    }
}
