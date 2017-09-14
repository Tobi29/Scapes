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
import org.tobi29.scapes.client.InputManagerScapes
import org.tobi29.scapes.client.InputModeChangeEvent
import org.tobi29.scapes.client.ScapesClient
import org.tobi29.scapes.client.connection.ClientConnection
import org.tobi29.scapes.client.loadShader
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
import org.tobi29.scapes.engine.utils.shader.BooleanExpression
import org.tobi29.scapes.engine.utils.shader.IntegerExpression
import org.tobi29.scapes.engine.utils.tag.TagMap
import org.tobi29.scapes.entity.client.EntityClient
import org.tobi29.scapes.entity.client.MobClient
import org.tobi29.scapes.entity.client.MobPlayerClientMain
import org.tobi29.scapes.entity.model.EntityModel
import org.tobi29.scapes.packets.PacketServer

class WorldClient(val connection: ClientConnection,
                  cam: Cam,
                  seed: Long,
                  terrainSupplier: (WorldClient) -> TerrainClient,
                  environmentSupplier: (WorldClient) -> EnvironmentClient,
                  playerTag: TagMap,
                  playerID: UUID) : World<EntityClient>(
        connection.plugins, connection.game.engine.taskExecutor,
        connection.plugins.registry, seed), PlayConnection<PacketServer> {
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
    private val infoLayers = ConcurrentHashMap<String, () -> TerrainRenderInfo.InfoLayer>()
    private val entityModels = ConcurrentHashMap<UUID, EntityModel>()
    private var disposed = false

    init {
        environment = environmentSupplier(this)
        terrain = terrainSupplier(this)
        scene = SceneScapesVoxelWorld(this, cam)
        connection.plugins.plugins.forEach { it.worldInit(this) }

        player = connection.plugins.worldType.newPlayer(this)
        player.setEntityID(playerID)
        player.read(playerTag)
        player.setInputMode(game.engine[InputManagerScapes.COMPONENT].inputMode)
        entityAdded(player, false)

        logger.info { "Received player entity: $player with id: $playerID" }

        scene.skybox().init()
        terrain.init()
    }

    fun addEntityModel(model: EntityModel) {
        entityModels.put(model.entity.uuid, model)
    }

    fun removeEntityModel(model: EntityModel) {
        entityModels.remove(model.entity.uuid)
    }

    override fun addEntity(entity: EntityClient,
                           spawn: Boolean): Boolean {
        return terrain.addEntity(entity, spawn)
    }

    override fun removeEntity(entity: EntityClient): Boolean {
        return terrain.removeEntity(entity)
    }

    override fun hasEntity(entity: EntityClient): Boolean {
        return player == entity || terrain.hasEntity(entity)
    }

    override fun getEntity(uuid: UUID): EntityClient? {
        if (player.uuid == uuid) {
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

    override fun entityAdded(entity: EntityClient,
                             spawn: Boolean) {
        entity.addedToWorld()
    }

    override fun entityRemoved(entity: EntityClient) {
        entity.removedFromWorld()
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
        profilerSection("Tasks") {
            process()
        }
        spawn = Vector3i(player.getCurrentPos())
    }

    fun updateRender(cam: Cam,
                     delta: Double) {
        entityModels.values.forEach { it.renderUpdate(delta) }
        terrain.renderer.renderUpdate(cam)
    }

    fun addToPipeline(gl: GL,
                      cam: Cam,
                      debug: Boolean): suspend () -> (Double) -> Unit {
        val scapes = game.engine[ScapesClient.COMPONENT]
        val resolutionMultiplier = scapes.resolutionMultiplier
        val width = round(gl.contentWidth * resolutionMultiplier)
        val height = round(gl.contentHeight * resolutionMultiplier)
        val animations = scapes.animations

        val shaderTerrain1 = gl.loadShader(
                "Scapes:shader/Terrain", mapOf(
                "SCENE_WIDTH" to IntegerExpression(width),
                "SCENE_HEIGHT" to IntegerExpression(height),
                "ENABLE_ANIMATIONS" to BooleanExpression(animations),
                "LOD_LOW" to BooleanExpression(false),
                "LOD_HIGH" to BooleanExpression(true)
        ))
        val shaderTerrain2 = gl.loadShader(
                "Scapes:shader/Terrain", mapOf(
                "SCENE_WIDTH" to IntegerExpression(width),
                "SCENE_HEIGHT" to IntegerExpression(height),
                "ENABLE_ANIMATIONS" to BooleanExpression(false),
                "LOD_LOW" to BooleanExpression(true),
                "LOD_HIGH" to BooleanExpression(false)
        ))
        val shaderEntity = gl.loadShader(
                "Scapes:shader/Entity", mapOf(
                "SCENE_WIDTH" to IntegerExpression(width),
                "SCENE_HEIGHT" to IntegerExpression(height)
        ))

        val particles = scene.particles().addToPipeline(gl, width, height, cam)

        return {
            val sTerrain1 = shaderTerrain1.getAsync()
            val sTerrain2 = shaderTerrain2.getAsync()
            val sEntity = shaderEntity.getAsync()
            val renderParticles = particles()
            ;{ delta ->
            val cx = cam.position.x
            val cy = cam.position.y
            val cz = cam.position.z
            val time = gl.timer.toFloat()
            val sunLightReduction =
                    environment.sunLightReduction(cx, cy) / 15.0f
            val playerLight = max(
                    player.leftWeapon().material().playerLight(
                            player.leftWeapon()),
                    player.rightWeapon().material().playerLight(
                            player.rightWeapon()))
            val sunlightNormal = environment.sunLightNormal(cx, cy)
            val snx = sunlightNormal.floatX()
            val sny = sunlightNormal.floatY()
            val snz = sunlightNormal.floatZ()
            val fr = scene.fogR()
            val fg = scene.fogG()
            val fb = scene.fogB()
            val d = scene.fogDistance() * scene.renderDistance()
            sTerrain1.setUniform3f(gl, 4, fr, fg, fb)
            sTerrain1.setUniform1f(gl, 5, d)
            sTerrain1.setUniform1i(gl, 6, 1)
            sTerrain1.setUniform1f(gl, 7, time)
            sTerrain1.setUniform1f(gl, 8, sunLightReduction)
            sTerrain1.setUniform3f(gl, 9, snx, sny, snz)
            sTerrain1.setUniform1f(gl, 10, playerLight)
            sTerrain2.setUniform3f(gl, 4, fr, fg, fb)
            sTerrain2.setUniform1f(gl, 5, d)
            sTerrain2.setUniform1i(gl, 6, 1)
            sTerrain2.setUniform1f(gl, 7, time)
            sTerrain2.setUniform1f(gl, 8, sunLightReduction)
            sTerrain2.setUniform3f(gl, 9, snx, sny, snz)
            sTerrain2.setUniform1f(gl, 10, playerLight)
            sEntity.setUniform3f(gl, 4, fr, fg, fb)
            sEntity.setUniform1f(gl, 5, d)
            sEntity.setUniform1i(gl, 6, 1)
            sEntity.setUniform1f(gl, 7, time)
            sEntity.setUniform1f(gl, 8, sunLightReduction)
            sEntity.setUniform3f(gl, 9, snx, sny, snz)
            sEntity.setUniform1f(gl, 10, playerLight)
            gl.setBlending(BlendingMode.NONE)
            scene.terrainTextureRegistry().texture.bind(gl)
            terrain.renderer.render(gl, sTerrain1, sTerrain2, cam, debug)
            gl.setBlending(BlendingMode.NORMAL)
            val aabb = AABB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
            entityModels.values.asSequence().filter {
                it.shapeAABB(aabb)
                cam.frustum.inView(aabb) != 0
            }.forEach {
                it.render(gl, this, cam, sEntity)
            }
            scene.terrainTextureRegistry().texture.bind(gl)
            terrain.renderer.renderAlpha(gl, sTerrain1, sTerrain2, cam)
            renderParticles(delta)
        }
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
                  pitch: Double = 1.0,
                  gain: Double = 1.0,
                  referenceDistance: Double = 1.0,
                  rolloffFactor: Double = 1.0) {
        if (entity is MobClient) {
            playSound(audio, entity.getCurrentPos(), entity.speed(), pitch,
                    gain, referenceDistance, rolloffFactor)
        } else {
            playSound(audio, entity.getCurrentPos(), Vector3d.ZERO, pitch, gain,
                    referenceDistance, rolloffFactor)
        }
    }

    fun playSound(audio: String,
                  position: Vector3d,
                  velocity: Vector3d,
                  pitch: Double = 1.0,
                  gain: Double = 1.0,
                  referenceDistance: Double = 1.0,
                  rolloffFactor: Double = 1.0) {
        game.engine.sounds.playSound(audio, "sound.World", position,
                velocity, pitch, gain, referenceDistance, rolloffFactor)
    }

    fun infoLayer(name: String,
                  layer: () -> TerrainRenderInfo.InfoLayer) {
        infoLayers.put(name, layer)
    }

    fun infoLayers() = infoLayers.readOnly()

    fun disposed(): Boolean {
        return disposed
    }

    suspend fun dispose() {
        events.disable()
        terrain.dispose()
        entityRemoved(player)
        player.setInputMode(null)
        disposed = true
    }

    override fun send(packet: PacketServer) {
        connection.send(packet)
    }

    companion object : KLogging()
}
