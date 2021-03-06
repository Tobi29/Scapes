/*
 * Copyright 2012-2018 Tobi29
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

import org.tobi29.graphics.Cam
import org.tobi29.io.tag.TagMap
import org.tobi29.logging.KLogging
import org.tobi29.math.AABB3
import org.tobi29.math.vector.Vector3d
import org.tobi29.math.vector.floorToInt
import org.tobi29.profiler.profilerSection
import org.tobi29.scapes.block.light
import org.tobi29.scapes.chunk.terrain.TerrainClient
import org.tobi29.scapes.chunk.terrain.TerrainRenderInfo
import org.tobi29.scapes.chunk.terrain.isTransparent
import org.tobi29.scapes.client.connection.ClientConnection
import org.tobi29.scapes.client.input.InputManagerScapes
import org.tobi29.scapes.client.input.InputModeChangeEvent
import org.tobi29.scapes.client.states.scenes.SceneScapesVoxelWorld
import org.tobi29.scapes.connection.PlayConnection
import org.tobi29.scapes.engine.graphics.BlendingMode
import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.graphics.loadShader
import org.tobi29.scapes.engine.graphics.space
import org.tobi29.scapes.engine.shader.BooleanExpression
import org.tobi29.scapes.engine.shader.IntegerExpression
import org.tobi29.scapes.entity.client.EntityClient
import org.tobi29.scapes.entity.client.MobClient
import org.tobi29.scapes.entity.client.MobPlayerClientMain
import org.tobi29.scapes.entity.model.EntityModel
import org.tobi29.scapes.packets.PacketServer
import org.tobi29.stdex.ConcurrentHashMap
import org.tobi29.stdex.math.clamp
import org.tobi29.stdex.math.floorToInt
import org.tobi29.stdex.readOnly
import org.tobi29.utils.EventDispatcher
import org.tobi29.uuid.Uuid
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

class WorldClient(
    val connection: ClientConnection,
    private val config: Config,
    cam: Cam,
    seed: Long,
    terrainSupplier: (WorldClient) -> TerrainClient,
    environmentSupplier: (WorldClient) -> EnvironmentClient,
    playerTag: TagMap,
    playerID: Uuid
) : World<EntityClient>(
    connection.plugins, connection.game.engine.taskExecutor,
    connection.plugins.registry, seed
), PlayConnection<PacketServer> {
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
    private val infoLayers =
        ConcurrentHashMap<String, () -> TerrainRenderInfo.InfoLayer>()
    private val entityModels = ConcurrentHashMap<Uuid, EntityModel>()
    private var disposed = false

    init {
        environment = environmentSupplier(this)
        terrain = terrainSupplier(this)
        scene = SceneScapesVoxelWorld(this, config.sceneConfig, cam)
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

    override fun addEntity(
        entity: EntityClient,
        spawn: Boolean
    ): Boolean {
        return terrain.addEntity(entity, spawn)
    }

    override fun removeEntity(entity: EntityClient): Boolean {
        return terrain.removeEntity(entity)
    }

    override fun hasEntity(entity: EntityClient): Boolean {
        return player == entity || terrain.hasEntity(entity)
    }

    override fun getEntity(uuid: Uuid): EntityClient? {
        if (player.uuid == uuid) {
            return player
        }
        return terrain.getEntity(uuid)
    }

    override fun getEntities(): Sequence<EntityClient> {
        return terrain.getEntities() + player
    }

    override fun getEntities(
        x: Int,
        y: Int,
        z: Int
    ): Sequence<EntityClient> {
        val sequence = terrain.getEntities(x, y, z)
        val pos = player.getCurrentPos()
        if (pos.x.floorToInt() == x && pos.y.floorToInt() == y && pos.z.floorToInt() == z) {
            return sequence + player
        }
        return sequence
    }

    override fun getEntitiesAtLeast(
        minX: Int,
        minY: Int,
        minZ: Int,
        maxX: Int,
        maxY: Int,
        maxZ: Int
    ): Sequence<EntityClient> {
        return terrain.getEntitiesAtLeast(
            minX, minY, minZ, maxX, maxY,
            maxZ
        ) + player
    }

    override fun entityAdded(
        entity: EntityClient,
        spawn: Boolean
    ) {
        entity.addedToWorld()
    }

    override fun entityRemoved(entity: EntityClient) {
        entity.removedFromWorld()
    }

    fun update(delta: Double) {
        profilerSection("Entities") {
            val pos = player.getCurrentPos()
            if (terrain.isBlockTicking(
                    pos.x.floorToInt(), pos.y.floorToInt(),
                    pos.z.floorToInt()
                )) {
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
        spawn = player.getCurrentPos().floorToInt()
    }

    fun updateRender(
        cam: Cam,
        delta: Double
    ) {
        entityModels.values.forEach { it.renderUpdate(delta) }
        terrain.renderer.renderUpdate(cam)
    }

    fun addToPipeline(
        gl: GL,
        cam: Cam,
        debug: Boolean
    ): suspend () -> (Double) -> Unit {
        val config = config
        val resolutionMultiplier = config.sceneConfig.resolutionMultiplier
        val width = (gl.contentWidth * resolutionMultiplier).roundToInt()
        val height = (gl.contentHeight * resolutionMultiplier).roundToInt()
        val animations = config.sceneConfig.animations

        val shaderTerrain1 = game.engine.graphics.loadShader(
            "Scapes:shader/Terrain.stag", mapOf(
                "SCENE_WIDTH" to IntegerExpression(width),
                "SCENE_HEIGHT" to IntegerExpression(height),
                "ENABLE_ANIMATIONS" to BooleanExpression(animations),
                "LOD_LOW" to BooleanExpression(false),
                "LOD_HIGH" to BooleanExpression(true)
            )
        )
        val shaderTerrain2 = game.engine.graphics.loadShader(
            "Scapes:shader/Terrain.stag", mapOf(
                "SCENE_WIDTH" to IntegerExpression(width),
                "SCENE_HEIGHT" to IntegerExpression(height),
                "ENABLE_ANIMATIONS" to BooleanExpression(false),
                "LOD_LOW" to BooleanExpression(true),
                "LOD_HIGH" to BooleanExpression(false)
            )
        )
        val shaderEntity = game.engine.graphics.loadShader(
            "Scapes:shader/Entity.stag", mapOf(
                "SCENE_WIDTH" to IntegerExpression(width),
                "SCENE_HEIGHT" to IntegerExpression(height)
            )
        )

        val particles = scene.particles().addToPipeline(gl, width, height, cam)

        terrain.renderer.lodDistance =
                clamp(gl.space * 96.0, 32.0, 96.0).roundToInt()

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
                player.leftWeapon().light,
                player.rightWeapon().light
            ).toFloat()
            val sunlightNormal = environment.sunLightNormal(cx, cy)
            val snx = sunlightNormal.x.toFloat()
            val sny = sunlightNormal.y.toFloat()
            val snz = sunlightNormal.z.toFloat()
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
            val aabb = AABB3()
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

    fun checkBlocked(
        x1: Int,
        y1: Int,
        z1: Int,
        x2: Int,
        y2: Int,
        z2: Int
    ): Boolean {
        val dx = x2 - x1
        val dy = y2 - y1
        val dz = z2 - z1
        val step = (1 / sqrt(dx * dx + dy * dy + dz * dz.toFloat())).toDouble()
        var i = 0.0
        while (i <= 1) {
            val x = (x1 + dx * i).floorToInt()
            val y = (y1 + dy * i).floorToInt()
            val z = (z1 + dz * i).floorToInt()
            if (!terrain.isTransparent(x, y, z)) {
                return true
            }
            i += step
        }
        return false
    }

    fun playSound(
        audio: String,
        entity: EntityClient,
        pitch: Double = 1.0,
        gain: Double = 1.0,
        referenceDistance: Double = 1.0,
        rolloffFactor: Double = 1.0
    ) {
        if (entity is MobClient) {
            playSound(
                audio, entity.getCurrentPos(), entity.speed(), pitch,
                gain, referenceDistance, rolloffFactor
            )
        } else {
            playSound(
                audio, entity.getCurrentPos(), Vector3d.ZERO, pitch, gain,
                referenceDistance, rolloffFactor
            )
        }
    }

    fun playSound(
        audio: String,
        position: Vector3d,
        velocity: Vector3d,
        pitch: Double = 1.0,
        gain: Double = 1.0,
        referenceDistance: Double = 1.0,
        rolloffFactor: Double = 1.0
    ) {
        game.engine.sounds.playSound(
            audio, "sound.World", position,
            velocity, pitch, gain, referenceDistance, rolloffFactor
        )
    }

    fun infoLayer(
        name: String,
        layer: () -> TerrainRenderInfo.InfoLayer
    ) {
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

    data class Config(
        val sceneConfig: SceneScapesVoxelWorld.Config = SceneScapesVoxelWorld.Config()
    )
}
