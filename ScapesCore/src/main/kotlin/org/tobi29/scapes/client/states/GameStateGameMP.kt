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

import kotlinx.coroutines.experimental.*
import org.tobi29.logging.KLogging
import org.tobi29.scapes.block.ItemTypeTextured
import org.tobi29.scapes.block.TerrainTextureRegistry
import org.tobi29.scapes.chunk.WorldClient
import org.tobi29.scapes.client.ChatHistory
import org.tobi29.scapes.client.Playlist
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
import org.tobi29.scapes.entity.model.EntityModelBlockBreakShared
import org.tobi29.scapes.entity.model.MobLivingModelHumanShared
import org.tobi29.scapes.entity.particle.ParticleTransparentAtlas
import org.tobi29.scapes.entity.particle.ParticleTransparentAtlasBuilder
import org.tobi29.scapes.inventory.ItemType
import org.tobi29.server.RemoteAddress
import org.tobi29.stdex.atomic.AtomicBoolean

open class GameStateGameMP(
    clientSupplier: (GameStateGameMP) -> ClientConnection,
    val config: WorldClient.Config,
    val playlist: Playlist,
    private val loadScene: Scene,
    val onClose: () -> Unit,
    val onError: (String, RemoteAddress?, Double?) -> Unit,
    engine: ScapesEngine
) : GameState(engine) {
    val client: ClientConnection
    val chatHistory: ChatHistory
    val hud: GuiHud
    val inputGui: Gui
    val debug: Gui
    val debugWidget: GuiComponentWidget
    protected val terrainTextureRegistry: TerrainTextureRegistry
    var particleTransparentAtlas: ParticleTransparentAtlas? = null
        private set
    protected var particleTransparentAtlasBuilder: ParticleTransparentAtlasBuilder? =
        ParticleTransparentAtlasBuilder()
    protected var scene: SceneScapesVoxelWorld? = null
    private val connectionSentProfiler: GuiWidgetDebugValues
    private val connectionReceivedProfiler: GuiWidgetDebugValues
    private val modelHumanShared: MobLivingModelHumanShared
    private val modelBlockBreakShared: Deferred<EntityModelBlockBreakShared>
    private val init = AtomicBoolean(false)

    init {
        chatHistory = ChatHistory(engine.events)
        client = clientSupplier(this)
        terrainTextureRegistry = TerrainTextureRegistry(engine)
        modelHumanShared = MobLivingModelHumanShared(engine)
        modelBlockBreakShared = async(engine) {
            EntityModelBlockBreakShared(engine, Array(9) {
                engine.graphics.textures["Scapes:image/entity/Break${it + 1}"].getAsync()
            })
        }
        val style = engine.guiStyle
        hud = GuiHud(style)
        inputGui = Gui(style)
        debug = Gui(style)
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
        engine.guiStack.remove(inputGui)
        engine.guiStack.remove(hud)
        engine.guiStack.remove(debug)
        client.stop()
        terrainTextureRegistry.texture.markDisposed()
        engine.sounds.stop("music")
        engine.graphics.textures.clearCache()
        engine.sounds.clearCache()
        logger.info { "Stopped game!" }
    }

    override fun init() {
        engine.guiStack.addUnfocused("04-Input", inputGui)
        engine.guiStack.addUnfocused("05-HUD", hud)
        engine.guiStack.addUnfocused("99-SceneDebug", debug)
        client.plugins.init()
        client.plugins.plugins.forEach { it.initClient(this) }
        var time = System.currentTimeMillis()
        val registry = client.plugins.registry
        val materials = registry.get<ItemType>("Core", "ItemType")
        for (type in materials.values) {
            (type as? ItemTypeTextured)?.registerTextures(
                terrainTextureRegistry
            )
        }
        val size = terrainTextureRegistry.init()
        terrainTextureRegistry.initTexture(4)
        time = System.currentTimeMillis() - time
        for (type in materials.values) {
            (type as? ItemTypeTextured)?.createModels(terrainTextureRegistry)
        }
        logger.info { "Loaded terrain models with $size textures in ${time}ms." }
        switchPipeline { gl ->
            renderScene(gl, loadScene)
        }
        launch(engine.taskExecutor) {
            particleTransparentAtlasBuilder?.let {
                particleTransparentAtlasBuilder = null
                particleTransparentAtlas = it.build(engine) {
                    createTexture(it, 0)
                }
            }
            client.start()
            init.set(true)
        }
    }

    suspend fun awaitInit() {
        while (!init.get()) {
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
        return particleTransparentAtlas ?: throw IllegalStateException(
            "Particle atlas not initialized yet"
        )
    }

    fun particleTransparentAtlasBuilder(): ParticleTransparentAtlasBuilder {
        return particleTransparentAtlasBuilder ?: throw IllegalStateException(
            "Particle atlas already initialized"
        )
    }

    fun modelHumanShared(): MobLivingModelHumanShared {
        return modelHumanShared
    }

    fun modelBlockBreakShared(): Deferred<EntityModelBlockBreakShared> {
        return modelBlockBreakShared
    }

    fun setHudVisible(visible: Boolean) {
        hud.visible = visible
        inputGui.visible = visible
    }

    private inner class GuiWidgetDebugClient(parent: GuiLayoutData) :
        GuiComponentWidget(
            parent, "Debug Values"
        ) {
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
                GuiEvent.CLICK_LEFT
            ) { event ->
                connectionSentProfiler.visible = !connectionSentProfiler.visible
            }
            connSentReset.on(GuiEvent.CLICK_LEFT) { event ->
                client.profilerSent.clear()
                connectionSentProfiler.clear()
            }
            connReceived.on(
                GuiEvent.CLICK_LEFT
            ) { event ->
                connectionReceivedProfiler.visible =
                        !connectionReceivedProfiler.visible
            }
            connReceivedReset.on(GuiEvent.CLICK_LEFT) { event ->
                client.profilerReceived.clear()
                connectionReceivedProfiler.clear()
            }
        }
    }

    companion object : KLogging()
}
