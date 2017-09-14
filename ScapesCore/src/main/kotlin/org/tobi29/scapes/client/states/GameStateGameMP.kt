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

package org.tobi29.scapes.client.states

import kotlinx.coroutines.experimental.runBlocking
import kotlinx.coroutines.experimental.yield
import org.tobi29.scapes.block.Material
import org.tobi29.scapes.block.TerrainTextureRegistry
import org.tobi29.scapes.client.ChatHistory
import org.tobi29.scapes.client.Playlist
import org.tobi29.scapes.client.ScapesClient
import org.tobi29.scapes.client.connection.ClientConnection
import org.tobi29.scapes.client.gui.GuiHud
import org.tobi29.scapes.client.gui.GuiWidgetConnectionProfiler
import org.tobi29.scapes.client.states.scenes.SceneScapesVoxelWorld
import org.tobi29.scapes.engine.GameState
import org.tobi29.scapes.engine.ScapesEngine
import org.tobi29.scapes.engine.graphics.Scene
import org.tobi29.scapes.engine.graphics.renderScene
import org.tobi29.scapes.engine.gui.*
import org.tobi29.scapes.engine.gui.debug.GuiWidgetDebugValues
import org.tobi29.scapes.engine.resource.awaitDone
import org.tobi29.scapes.engine.utils.logging.KLogging
import org.tobi29.scapes.entity.model.EntityModelBlockBreakShared
import org.tobi29.scapes.entity.model.MobLivingModelHumanShared
import org.tobi29.scapes.entity.particle.ParticleTransparentAtlas

open class GameStateGameMP(clientSupplier: (GameStateGameMP) -> ClientConnection,
                           private val loadScene: Scene,
                           engine: ScapesEngine) : GameState(engine) {
    internal val client: ClientConnection
    val playlist: Playlist
    val chatHistory: ChatHistory
    val hud: GuiHud
    val inputGui: Gui
    val debug: Gui
    val debugWidget: GuiComponentWidget
    protected val terrainTextureRegistry: TerrainTextureRegistry
    protected val particleTransparentAtlas: ParticleTransparentAtlas
    protected var scene: SceneScapesVoxelWorld? = null
    private val connectionSentProfiler: GuiWidgetDebugValues
    private val connectionReceivedProfiler: GuiWidgetDebugValues
    private val modelHumanShared: MobLivingModelHumanShared
    private val modelBlockBreakShared: EntityModelBlockBreakShared
    private var init = false

    init {
        val scapes = engine[ScapesClient.COMPONENT]
        chatHistory = ChatHistory(engine.events)
        playlist = Playlist(scapes.home.resolve("playlists"), engine)
        client = clientSupplier(this)
        terrainTextureRegistry = TerrainTextureRegistry(engine)
        particleTransparentAtlas = ParticleTransparentAtlas(engine)
        modelHumanShared = MobLivingModelHumanShared(engine)
        modelBlockBreakShared = EntityModelBlockBreakShared(engine)
        val style = engine.guiStyle
        hud = GuiHud(this, style)
        inputGui = GuiState(this, style)
        debug = GuiState(this, style)
        debugWidget = debug.add(32.0, 32.0, 160.0, 184.0) {
            GuiWidgetDebugClient(it)
        }
        debugWidget.visible = false
        connectionSentProfiler = debug.add(32.0, 32.0, 360.0, 256.0) {
            GuiWidgetConnectionProfiler(it, client.profilerSent)
        }
        connectionSentProfiler.visible = false
        connectionReceivedProfiler = debug.add(32.0, 32.0, 360.0, 256.0) {
            GuiWidgetConnectionProfiler(it, client.profilerReceived)
        }
        connectionReceivedProfiler.visible = false
    }

    @Synchronized
    fun switchScene(newScene: SceneScapesVoxelWorld) {
        runBlocking { scene?.dispose() }
        scene = newScene
        switchPipelineWhenLoaded { gl ->
            val scene = renderScene(gl, newScene)
            ;{
            val sceneRender = scene()
            engine.resources.awaitDone()
            ;{ delta ->
            sceneRender(delta)
        }
        }
        }
    }

    fun client(): ClientConnection {
        return client
    }

    override fun dispose() {
        runBlocking { scene?.dispose() }
        client.stop()
        terrainTextureRegistry.texture.markDisposed()
        engine.sounds.stop("music")
        client.plugins.removeFileSystems(engine.files)
        client.plugins.dispose()
        engine.graphics.textures.clearCache()
        engine.sounds.clearCache()
        logger.info { "Stopped game!" }
    }

    override fun init() {
        engine.guiStack.addUnfocused("04-Input", inputGui)
        engine.guiStack.addUnfocused("05-HUD", hud)
        engine.guiStack.addUnfocused("99-SceneDebug", debug)
        client.plugins.addFileSystems(engine.files)
        client.plugins.init()
        client.plugins.plugins.forEach { it.initClient(this) }
        var time = System.currentTimeMillis()
        val registry = client.plugins.registry
        val materials = registry.get<Material>("Core", "Material")
        for (type in materials.values) {
            type?.registerTextures(terrainTextureRegistry)
        }
        val size = terrainTextureRegistry.init()
        terrainTextureRegistry.initTexture(4)
        time = System.currentTimeMillis() - time
        for (type in materials.values) {
            type?.createModels(terrainTextureRegistry)
        }
        logger.info { "Loaded terrain models with $size textures in ${time}ms." }
        particleTransparentAtlas.init()
        particleTransparentAtlas.initTexture(4)
        client.start()
        switchPipeline { gl ->
            renderScene(gl, loadScene)
        }
        init = true
    }

    suspend fun awaitInit() {
        while (!init) {
            yield()
        }
    }

    override val isMouseGrabbed: Boolean
        get() = !(scene?.world()?.player?.hasGui() ?: false)

    override fun step(delta: Double) {
        chatHistory.update()
        scene?.world()?.player?.let { playlist.update(it, delta) }
        scene?.world()?.update(delta)
    }

    override fun renderStep(delta: Double) {
        scene?.step(delta)
    }

    fun terrainTextureRegistry(): TerrainTextureRegistry {
        return terrainTextureRegistry
    }

    fun particleTransparentAtlas(): ParticleTransparentAtlas {
        return particleTransparentAtlas
    }

    fun modelHumanShared(): MobLivingModelHumanShared {
        return modelHumanShared
    }

    fun modelBlockBreakShared(): EntityModelBlockBreakShared {
        return modelBlockBreakShared
    }

    fun setHudVisible(visible: Boolean) {
        hud.visible = visible
        inputGui.visible = visible
    }

    private inner class GuiWidgetDebugClient(parent: GuiLayoutData) : GuiComponentWidget(
            parent, "Debug Values") {
        init {
            val geometry = addVert(10.0, 10.0, 10.0, 2.0, -1.0, 15.0) {
                GuiComponentTextButton(it, 12, "Geometry")
            }
            val wireframe = addVert(10.0, 2.0, -1.0, 15.0) {
                GuiComponentTextButton(it, 12, "Wireframe")
            }
            val distance = addVert(10.0, 2.0, -1.0, 15.0) {
                GuiComponentTextButton(it, 12, "Static Render Distance")
            }
            val reloadGeometry = addVert(10.0, 2.0, -1.0, 15.0) {
                GuiComponentTextButton(it, 12, "Reload Geometry")
            }
            val connSent = addVert(10.0, 2.0, -1.0, 15.0) {
                GuiComponentTextButton(it, 12, "Conn. Sent")
            }
            val connSentReset = addVert(10.0, 2.0, -1.0, 15.0) {
                GuiComponentTextButton(it, 12, "Conn. Sent Reset")
            }
            val connReceived = addVert(10.0, 2.0, -1.0, 15.0) {
                GuiComponentTextButton(it, 12, "Conn. Received")
            }
            val connReceivedReset = addVert(10.0, 2.0, 10.0, 10.0, -1.0, 15.0) {
                GuiComponentTextButton(it, 12, "Conn. Received Reset")
            }

            geometry.on(GuiEvent.CLICK_LEFT) { event ->
                scene?.toggleChunkDebug()
            }
            wireframe.on(GuiEvent.CLICK_LEFT) { event ->
                scene?.toggleWireframe()
            }
            distance.on(GuiEvent.CLICK_LEFT) { event ->
                client.mob { it.world.terrain.toggleStaticRenderDistance() }
            }
            reloadGeometry.on(GuiEvent.CLICK_LEFT) { event ->
                client.mob { it.world.terrain.reloadGeometry() }
            }
            connSent.on(
                    GuiEvent.CLICK_LEFT) { event -> connectionSentProfiler.visible = !connectionSentProfiler.visible }
            connSentReset.on(GuiEvent.CLICK_LEFT) { event ->
                client.profilerSent.clear()
                connectionSentProfiler.clear()
            }
            connReceived.on(GuiEvent.CLICK_LEFT
            ) { event -> connectionReceivedProfiler.visible = !connectionReceivedProfiler.visible }
            connReceivedReset.on(GuiEvent.CLICK_LEFT) { event ->
                client.profilerReceived.clear()
                connectionReceivedProfiler.clear()
            }
        }
    }

    companion object : KLogging()
}
