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
package org.tobi29.scapes.chunk

import java8.util.stream.Stream
import mu.KLogging
import org.tobi29.scapes.chunk.terrain.TerrainClient
import org.tobi29.scapes.chunk.terrain.TerrainRenderInfo
import org.tobi29.scapes.client.connection.ClientConnection
import org.tobi29.scapes.client.states.GameStateGameMP
import org.tobi29.scapes.client.states.scenes.SceneScapesVoxelWorld
import org.tobi29.scapes.connection.PlayConnection
import org.tobi29.scapes.engine.graphics.BlendingMode
import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.graphics.Shader
import org.tobi29.scapes.engine.utils.forEach
import org.tobi29.scapes.engine.utils.graphics.Cam
import org.tobi29.scapes.engine.utils.io.tag.TagStructure
import org.tobi29.scapes.engine.utils.math.AABB
import org.tobi29.scapes.engine.utils.math.floor
import org.tobi29.scapes.engine.utils.math.max
import org.tobi29.scapes.engine.utils.math.sqrt
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.engine.utils.math.vector.Vector3i
import org.tobi29.scapes.engine.utils.profiler.profilerSection
import org.tobi29.scapes.engine.utils.stream
import org.tobi29.scapes.entity.client.EntityClient
import org.tobi29.scapes.entity.client.MobClient
import org.tobi29.scapes.entity.client.MobPlayerClientMain
import org.tobi29.scapes.entity.getEntities
import org.tobi29.scapes.entity.model.EntityModel
import org.tobi29.scapes.entity.model.MobModel
import org.tobi29.scapes.packets.PacketServer
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class WorldClient(val connection: ClientConnection, cam: Cam, seed: Long,
                  terrainSupplier: (WorldClient) -> TerrainClient,
                  environmentSupplier: (WorldClient) -> EnvironmentClient,
                  playerTag: TagStructure, playerID: UUID) : World<EntityClient>(
        connection.plugins, connection.game.engine.taskExecutor,
        connection.plugins.registry(),
        seed), PlayConnection<PacketServer> {
    val scene: SceneScapesVoxelWorld
    val player: MobPlayerClientMain
    val game: GameStateGameMP
    val terrain: TerrainClient
    val environment: EnvironmentClient
    val playerModel: MobModel?
    private val infoLayers = ConcurrentHashMap<String, () -> TerrainRenderInfo.InfoLayer>()
    private val shaderTerrain1: Shader
    private val shaderTerrain2: Shader
    private val shaderEntity: Shader
    private val entityModels = ConcurrentHashMap<UUID, EntityModel>()
    private var disposed = false

    init {
        game = connection.game
        environment = environmentSupplier(this)
        player = connection.plugins.worldType().newPlayer(this, Vector3d.ZERO,
                Vector3d.ZERO, 0.0, 0.0, "")
        player.setEntityID(playerID)
        player.read(playerTag)
        scene = SceneScapesVoxelWorld(this, cam)
        playerModel = player.createModel()
        connection.plugins.plugins().forEach { plugin ->
            plugin.worldInit(this)
        }
        terrain = terrainSupplier(this)
        logger.info { "Received player entity: $player with id: $playerID" }
        val scapesTag = game.engine.tagStructure.getStructure("Scapes")
        val graphics = connection.game.engine.graphics
        shaderTerrain1 = graphics.createShader("Scapes:shader/Terrain"
        ) { information ->
            information.supplyPreCompile { shader ->
                shader.supplyProperty("ENABLE_ANIMATIONS",
                        scapesTag?.getBoolean("Animations") ?: false)
                shader.supplyProperty("LOD_LOW", false)
                shader.supplyProperty("LOD_HIGH", true)
            }
        }
        shaderTerrain2 = graphics.createShader("Scapes:shader/Terrain"
        ) { information ->
            information.supplyPreCompile { shader ->
                shader.supplyProperty("ENABLE_ANIMATIONS", false)
                shader.supplyProperty("LOD_LOW", true)
                shader.supplyProperty("LOD_HIGH", false)
            }
        }
        shaderEntity = graphics.createShader("Scapes:shader/Entity")
    }

    fun addEntityModel(entity: EntityClient) {
        val model = entity.createModel()
        if (model != null) {
            entityModels.put(entity.getUUID(), model)
        }
    }

    fun removeEntityModel(entity: EntityClient) {
        entityModels.remove(entity.getUUID())
    }

    override fun addEntity(entity: EntityClient): Boolean {
        return terrain.addEntity(entity)
    }

    override fun removeEntity(entity: EntityClient): Boolean {
        return terrain.removeEntity(entity)
    }

    override fun hasEntity(entity: EntityClient): Boolean {
        return player === entity || terrain.hasEntity(entity)
    }

    override fun getEntity(uuid: UUID): EntityClient? {
        if (player.getUUID() == uuid) {
            return player
        }
        return terrain.getEntity(uuid)
    }

    override fun getEntities(consumer: (Stream<EntityClient>) -> Unit) {
        terrain.getEntities(consumer)
        consumer(stream(player))
    }

    @Suppress("USELESS_CAST")
    override fun getEntities(x: Int,
                             y: Int,
                             z: Int,
                             consumer: (Stream<EntityClient>) -> Unit) {
        terrain.getEntities(x, y, z, consumer)
        consumer(stream(player).map { it as EntityClient }.filter { entity ->
            val pos = entity.getCurrentPos()
            pos.intX() == x && pos.intY() == y && pos.intZ() == z
        })
    }

    override fun getEntitiesAtLeast(minX: Int,
                                    minY: Int,
                                    minZ: Int,
                                    maxX: Int,
                                    maxY: Int,
                                    maxZ: Int,
                                    consumer: (Stream<EntityClient>) -> Unit) {
        terrain.getEntities(minX, minY, minZ, maxX, maxY, maxZ) { consumer(it) }
        consumer(stream(player))
    }

    override fun entityAdded(entity: EntityClient) {
        addEntityModel(entity)
    }

    override fun entityRemoved(entity: EntityClient) {
        removeEntityModel(entity)
    }

    fun update(delta: Double) {
        profilerSection("Entities") {
            val pos = player.getCurrentPos()
            if (terrain.isBlockTicking(pos.intX(), pos.intY(), pos.intZ())) {
                player.update(delta)
                player.move(delta)
            } else {
                player.updatePosition()
            }
        }
        profilerSection("Terrain") { terrain.update(delta) }
        profilerSection("Environment") {
            environment.tick(delta)
        }
        profilerSection("Textures") {
            scene.terrainTextureRegistry().update(delta)
        }
        profilerSection("Skybox") {
            scene.skybox().update(delta)
        }
        spawn = Vector3i(player.getCurrentPos())
    }

    fun updateRender(cam: Cam,
                     delta: Double) {
        playerModel?.renderUpdate(delta)
        entityModels.values.forEach { it.renderUpdate(delta) }
        terrain.renderer.renderUpdate(cam)
    }

    fun render(gl: GL,
               cam: Cam,
               debug: Boolean) {
        val time = System.currentTimeMillis() % 10000000 / 1000.0f
        val sunLightReduction = environment.sunLightReduction(
                cam.position.doubleX(),
                cam.position.doubleY()) / 15.0f
        val playerLight = max(
                player.leftWeapon().material().playerLight(player.leftWeapon()),
                player.rightWeapon().material().playerLight(
                        player.rightWeapon()))
        val sunlightNormal = environment.sunLightNormal(cam.position.doubleX(),
                cam.position.doubleY())
        shaderTerrain1.setUniform3f(4, scene.fogR(), scene.fogG(), scene.fogB())
        shaderTerrain1.setUniform1f(5,
                scene.fogDistance() * scene.renderDistance())
        shaderTerrain1.setUniform1i(6, 1)
        shaderTerrain1.setUniform1f(7, time)
        shaderTerrain1.setUniform1f(8, sunLightReduction)
        shaderTerrain1.setUniform3f(9, sunlightNormal.floatX(),
                sunlightNormal.floatY(), sunlightNormal.floatZ())
        shaderTerrain1.setUniform1f(10, playerLight)
        shaderTerrain2.setUniform3f(4, scene.fogR(), scene.fogG(), scene.fogB())
        shaderTerrain2.setUniform1f(5,
                scene.fogDistance() * scene.renderDistance())
        shaderTerrain2.setUniform1i(6, 1)
        shaderTerrain2.setUniform1f(7, time)
        shaderTerrain2.setUniform1f(8, sunLightReduction)
        shaderTerrain2.setUniform3f(9, sunlightNormal.floatX(),
                sunlightNormal.floatY(), sunlightNormal.floatZ())
        shaderTerrain2.setUniform1f(10, playerLight)
        shaderEntity.setUniform3f(4, scene.fogR(), scene.fogG(), scene.fogB())
        shaderEntity.setUniform1f(5,
                scene.fogDistance() * scene.renderDistance())
        shaderEntity.setUniform1i(6, 1)
        shaderEntity.setUniform1f(7, time)
        shaderEntity.setUniform1f(8, sunLightReduction)
        shaderEntity.setUniform3f(9, sunlightNormal.floatX(),
                sunlightNormal.floatY(), sunlightNormal.floatZ())
        shaderEntity.setUniform1f(10, playerLight)
        gl.setBlending(BlendingMode.NONE)
        scene.terrainTextureRegistry().texture().bind(gl)
        terrain.renderer.render(gl, shaderTerrain1, shaderTerrain2, cam,
                debug)
        gl.setBlending(BlendingMode.NORMAL)
        if (game.hud().isVisible) {
            playerModel?.render(gl, this, cam, shaderEntity)
        }
        val aabb = AABB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        entityModels.values.forEach({
            it.shapeAABB(aabb)
            cam.frustum.inView(aabb) != 0
        }) { it.render(gl, this, cam, shaderEntity) }
        scene.terrainTextureRegistry().texture().bind(gl)
        terrain.renderer.renderAlpha(gl, shaderTerrain1, shaderTerrain2, cam)
        scene.particles().render(gl, cam)
    }

    fun checkBlocked(x1: Int,
                     y1: Int,
                     z1: Int,
                     x2: Int,
                     y2: Int,
                     z2: Int): Boolean {
        val dx = x2 - x1
        val dy = y2 - y1
        val dz = z2 - z1
        val step = (1 / sqrt(dx * dx + dy * dy + dz * dz.toFloat())).toDouble()
        var i = 0.0
        while (i <= 1) {
            val x = floor(x1 + dx * i)
            val y = floor(y1 + dy * i)
            val z = floor(z1 + dz * i)
            if (!terrain.type(x, y, z).isTransparent(terrain, x, y, z)) {
                return true
            }
            i += step
        }
        return false
    }

    fun playSound(audio: String,
                  entity: EntityClient,
                  pitch: Float = 1.0f,
                  gain: Float = 1.0f) {
        if (entity is MobClient) {
            playSound(audio, entity.getCurrentPos(), entity.speed(), pitch,
                    gain)
        } else {
            playSound(audio, entity.getCurrentPos(), Vector3d.ZERO, pitch, gain)
        }
    }

    fun playSound(audio: String,
                  position: Vector3d,
                  velocity: Vector3d,
                  pitch: Float = 1.0f,
                  gain: Float = 1.0f) {
        game.engine.sounds.playSound(audio, "sound.World", position,
                velocity, pitch,
                gain)
    }

    fun infoLayer(name: String,
                  layer: () -> TerrainRenderInfo.InfoLayer) {
        infoLayers.put(name, layer)
    }

    fun infoLayers(): Stream<Map.Entry<String, () -> TerrainRenderInfo.InfoLayer>> {
        return infoLayers.entries.stream<Map.Entry<String, () -> TerrainRenderInfo.InfoLayer>>()
    }

    fun disposed(): Boolean {
        return disposed
    }

    fun dispose() {
        terrain.dispose()
        disposed = true
    }

    override fun send(packet: PacketServer) {
        connection.send(packet)
    }

    override fun getWorldEntities(): Stream<MobPlayerClientMain> {
        return stream(player)
    }

    companion object : KLogging()
}
