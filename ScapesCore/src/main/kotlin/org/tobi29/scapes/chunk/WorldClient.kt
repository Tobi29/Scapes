/*
 * Copyright 2012-2017 Tobi29
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

import org.tobi29.scapes.chunk.terrain.TerrainClient
import org.tobi29.scapes.chunk.terrain.TerrainRenderInfo
import org.tobi29.scapes.chunk.terrain.isTransparent
import org.tobi29.scapes.client.InputModeChangeEvent
import org.tobi29.scapes.client.ScapesClient
import org.tobi29.scapes.client.connection.ClientConnection
import org.tobi29.scapes.client.states.scenes.SceneScapesVoxelWorld
import org.tobi29.scapes.connection.PlayConnection
import org.tobi29.scapes.engine.graphics.BlendingMode
import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.utils.ConcurrentHashMap
import org.tobi29.scapes.engine.utils.EventDispatcher
import org.tobi29.scapes.engine.utils.UUID
import org.tobi29.scapes.engine.utils.graphics.Cam
import org.tobi29.scapes.engine.utils.logging.KLogging
import org.tobi29.scapes.engine.utils.math.*
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.engine.utils.math.vector.Vector3i
import org.tobi29.scapes.engine.utils.profiler.profilerSection
import org.tobi29.scapes.engine.utils.readOnly
import org.tobi29.scapes.engine.utils.tag.TagMap
import org.tobi29.scapes.entity.client.EntityClient
import org.tobi29.scapes.entity.client.MobClient
import org.tobi29.scapes.entity.client.MobPlayerClientMain
import org.tobi29.scapes.entity.model.EntityModel
import org.tobi29.scapes.entity.model.MobModel
import org.tobi29.scapes.packets.PacketServer

class WorldClient(val connection: ClientConnection,
                  cam: Cam,
                  seed: Long,
                  terrainSupplier: (WorldClient) -> TerrainClient,
                  environmentSupplier: (WorldClient) -> EnvironmentClient,
                  playerTag: TagMap,
                  playerID: UUID) : World<EntityClient>(
        connection.plugins, connection.game.engine.loop,
        connection.plugins.registry,
        seed), PlayConnection<PacketServer> {
    val game = connection.game
    val events = EventDispatcher(game.engine.events) {
        listen<InputModeChangeEvent> { event ->
            player.setInputMode(event.inputMode)
        }
    }.apply { enable() }
    val scene: SceneScapesVoxelWorld
    val player: MobPlayerClientMain
    val terrain: TerrainClient
    val environment: EnvironmentClient
    val playerModel: MobModel?
    private val infoLayers = ConcurrentHashMap<String, () -> TerrainRenderInfo.InfoLayer>()
    private val entityModels = ConcurrentHashMap<UUID, EntityModel>()
    private var disposed = false

    init {
        environment = environmentSupplier(this)
        player = connection.plugins.worldType.newPlayer(this)
        player.setEntityID(playerID)
        player.read(playerTag)
        terrain = terrainSupplier(this)
        scene = SceneScapesVoxelWorld(this, cam)
        playerModel = player.createModel()
        connection.plugins.plugins.forEach { it.worldInit(this) }

        val scapes = game.engine.game as ScapesClient
        player.setInputMode(scapes.inputMode)

        logger.info { "Received player entity: $player with id: $playerID" }
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
        return player == entity || terrain.hasEntity(entity)
    }

    override fun getEntity(uuid: UUID): EntityClient? {
        if (player.getUUID() == uuid) {
            return player
        }
        return terrain.getEntity(uuid)
    }

    override fun getEntities(): Sequence<EntityClient> {
        return terrain.getEntities() + player
    }

    override fun getEntities(x: Int,
                             y: Int,
                             z: Int): Sequence<EntityClient> {
        val sequence = terrain.getEntities(x, y, z)
        val pos = player.getCurrentPos()
        if (pos.intX() == x && pos.intY() == y && pos.intZ() == z) {
            return sequence + player
        }
        return sequence
    }

    override fun getEntitiesAtLeast(minX: Int,
                                    minY: Int,
                                    minZ: Int,
                                    maxX: Int,
                                    maxY: Int,
                                    maxZ: Int): Sequence<EntityClient> {
        return terrain.getEntitiesAtLeast(minX, minY, minZ, maxX, maxY,
                maxZ) + player
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

    fun addToPipeline(gl: GL,
                      cam: Cam,
                      debug: Boolean): () -> Unit {
        val scapes = game.engine.game as ScapesClient
        val resolutionMultiplier = scapes.resolutionMultiplier
        val width = round(gl.contentWidth() * resolutionMultiplier)
        val height = round(gl.contentHeight() * resolutionMultiplier)
        val animations = scapes.animations

        val shaderTerrain1 = gl.engine.graphics.loadShader(
                "Scapes:shader/Terrain") {
            supplyPreCompile {
                supplyProperty("SCENE_WIDTH", width)
                supplyProperty("SCENE_HEIGHT", height)
                supplyProperty("ENABLE_ANIMATIONS", animations)
                supplyProperty("LOD_LOW", false)
                supplyProperty("LOD_HIGH", true)
            }
        }
        val shaderTerrain2 = gl.engine.graphics.loadShader(
                "Scapes:shader/Terrain") {
            supplyPreCompile {
                supplyProperty("SCENE_WIDTH", width)
                supplyProperty("SCENE_HEIGHT", height)
                supplyProperty("ENABLE_ANIMATIONS", false)
                supplyProperty("LOD_LOW", true)
                supplyProperty("LOD_HIGH", false)
            }
        }
        val shaderEntity = gl.engine.graphics.loadShader(
                "Scapes:shader/Entity") {
            supplyPreCompile {
                supplyProperty("SCENE_WIDTH", width)
                supplyProperty("SCENE_HEIGHT", height)
            }
        }

        val renderParticles = scene.particles().addToPipeline(gl, width, height,
                cam)

        return {
            val time = System.currentTimeMillis() % 10000000 / 1000.0f
            val sunLightReduction = environment.sunLightReduction(
                    cam.position.doubleX(),
                    cam.position.doubleY()) / 15.0f
            val playerLight = max(
                    player.leftWeapon().material().playerLight(
                            player.leftWeapon()),
                    player.rightWeapon().material().playerLight(
                            player.rightWeapon()))
            val sunlightNormal = environment.sunLightNormal(
                    cam.position.doubleX(),
                    cam.position.doubleY())
            val sTerrain1 = shaderTerrain1.get()
            sTerrain1.setUniform3f(gl, 4, scene.fogR(), scene.fogG(),
                    scene.fogB())
            sTerrain1.setUniform1f(gl, 5,
                    scene.fogDistance() * scene.renderDistance())
            sTerrain1.setUniform1i(gl, 6, 1)
            sTerrain1.setUniform1f(gl, 7, time)
            sTerrain1.setUniform1f(gl, 8, sunLightReduction)
            sTerrain1.setUniform3f(gl, 9, sunlightNormal.floatX(),
                    sunlightNormal.floatY(), sunlightNormal.floatZ())
            sTerrain1.setUniform1f(gl, 10, playerLight)
            val sTerrain2 = shaderTerrain2.get()
            sTerrain2.setUniform3f(gl, 4, scene.fogR(), scene.fogG(),
                    scene.fogB())
            sTerrain2.setUniform1f(gl, 5,
                    scene.fogDistance() * scene.renderDistance())
            sTerrain2.setUniform1i(gl, 6, 1)
            sTerrain2.setUniform1f(gl, 7, time)
            sTerrain2.setUniform1f(gl, 8, sunLightReduction)
            sTerrain2.setUniform3f(gl, 9, sunlightNormal.floatX(),
                    sunlightNormal.floatY(), sunlightNormal.floatZ())
            sTerrain2.setUniform1f(gl, 10, playerLight)
            val sEntity = shaderEntity.get()
            sEntity.setUniform3f(gl, 4, scene.fogR(), scene.fogG(),
                    scene.fogB())
            sEntity.setUniform1f(gl, 5,
                    scene.fogDistance() * scene.renderDistance())
            sEntity.setUniform1i(gl, 6, 1)
            sEntity.setUniform1f(gl, 7, time)
            sEntity.setUniform1f(gl, 8, sunLightReduction)
            sEntity.setUniform3f(gl, 9, sunlightNormal.floatX(),
                    sunlightNormal.floatY(), sunlightNormal.floatZ())
            sEntity.setUniform1f(gl, 10, playerLight)
            gl.setBlending(BlendingMode.NONE)
            scene.terrainTextureRegistry().texture.bind(gl)
            terrain.renderer.render(gl, sTerrain1, sTerrain2, cam,
                    debug)
            gl.setBlending(BlendingMode.NORMAL)
            if (game.hud.visible) {
                playerModel?.render(gl, this, cam, sEntity)
            }
            val aabb = AABB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
            entityModels.values.asSequence().filter {
                it.shapeAABB(aabb)
                cam.frustum.inView(aabb) != 0
            }.forEach {
                it.render(gl, this, cam, sEntity)
            }
            scene.terrainTextureRegistry().texture.bind(gl)
            terrain.renderer.renderAlpha(gl, sTerrain1, sTerrain2,
                    cam)
            renderParticles()
        }
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
            if (!terrain.isTransparent(x, y, z)) {
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

    fun infoLayers() = infoLayers.readOnly()

    fun disposed(): Boolean {
        return disposed
    }

    fun dispose() {
        events.disable()
        terrain.dispose()
        player.setInputMode(null)
        disposed = true
    }

    override fun send(packet: PacketServer) {
        connection.send(packet)
    }

    companion object : KLogging()
}
