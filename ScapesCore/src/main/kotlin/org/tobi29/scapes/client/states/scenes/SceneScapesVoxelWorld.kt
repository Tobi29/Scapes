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
package org.tobi29.scapes.client.states.scenes

import mu.KLogging
import org.tobi29.scapes.block.TerrainTextureRegistry
import org.tobi29.scapes.chunk.WorldClient
import org.tobi29.scapes.chunk.WorldSkybox
import org.tobi29.scapes.client.gui.GuiComponentChat
import org.tobi29.scapes.client.states.GameStateGameMP
import org.tobi29.scapes.client.states.GameStateGameSP
import org.tobi29.scapes.engine.graphics.*
import org.tobi29.scapes.engine.gui.debug.GuiWidgetDebugValues
import org.tobi29.scapes.engine.utils.graphics.Cam
import org.tobi29.scapes.engine.utils.graphics.gaussianBlurOffset
import org.tobi29.scapes.engine.utils.graphics.gaussianBlurWeight
import org.tobi29.scapes.engine.utils.join
import org.tobi29.scapes.engine.utils.math.*
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.engine.utils.math.vector.plus
import org.tobi29.scapes.entity.client.MobPlayerClientMain
import org.tobi29.scapes.entity.particle.*
import org.tobi29.scapes.entity.skin.ClientSkinStorage
import org.tobi29.scapes.server.format.WorldSource
import org.tobi29.scapes.server.format.newPanorama
import java.io.IOException
import java.util.concurrent.ThreadLocalRandom

class SceneScapesVoxelWorld(private val world: WorldClient, private val cam: Cam) : Scene(
        world.game.engine) {
    val state: GameStateGameMP
    val fxaa: Boolean
    val bloom: Boolean
    private val model: Model
    private val cameraPositionXDebug: GuiWidgetDebugValues.Element
    private val cameraPositionYDebug: GuiWidgetDebugValues.Element
    private val cameraPositionZDebug: GuiWidgetDebugValues.Element
    private val lightDebug: GuiWidgetDebugValues.Element
    private val blockLightDebug: GuiWidgetDebugValues.Element
    private val sunLightDebug: GuiWidgetDebugValues.Element
    private val terrainTextureRegistry: TerrainTextureRegistry
    private val skinStorage: ClientSkinStorage
    private val skyboxFBO: Framebuffer
    private val exposureFBO: Framebuffer?
    private val particles: ParticleSystem
    private val skybox: WorldSkybox
    private val shaderExposure: Shader
    private val shaderFXAA: Shader
    private val shaderComposite1: Shader
    private val shaderComposite2: Shader
    private val shaderTextured: Shader
    private val textureNoise = engine.graphics.textures["Scapes:image/Noise"]
    private var brightness = 0.0f
    private var renderDistance = 0.0f
    private var fov = 0.0f
    private var flashDir = 0
    private var flashTime: Long = 0
    private var flashStart: Long = 0
    var isMouseGrabbed = false
        private set
    private var chunkGeometryDebug = false
    private var wireframe = false

    init {
        state = world.game
        particles = ParticleSystem(world, 60.0)
        terrainTextureRegistry = world.game.terrainTextureRegistry()
        skinStorage = ClientSkinStorage(world.game.engine,
                world.game.engine.graphics.textures()["Scapes:image/entity/mob/Player"])
        val debugValues = world.game.engine.debugValues
        cameraPositionXDebug = debugValues["Camera-Position-X"]
        cameraPositionYDebug = debugValues["Camera-Position-Y"]
        cameraPositionZDebug = debugValues["Camera-Position-Z"]
        lightDebug = debugValues["Camera-Light"]
        blockLightDebug = debugValues["Camera-Block-Light"]
        sunLightDebug = debugValues["Camera-Sun-Light"]
        model = createVTI(world.game.engine,
                floatArrayOf(0.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f,
                        0.0f, 1.0f, 0.0f, 0.0f),
                floatArrayOf(0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f),
                intArrayOf(0, 1, 2, 3, 2, 1), RenderType.TRIANGLES)
        val scapesTag = world.game.engine.tagStructure.getStructure("Scapes")
        fxaa = scapesTag?.getBoolean("FXAA") ?: false
        bloom = scapesTag?.getBoolean("Bloom") ?: false
        skybox = world.environment.createSkybox(world)
        skyboxFBO = world.game.engine.graphics.createFramebuffer(1, 1, 1, false,
                true, false)
        if (scapesTag?.getBoolean("AutoExposure") ?: false) {
            exposureFBO = world.game.engine.graphics.createFramebuffer(1, 1, 1,
                    false, true, false)
        } else {
            exposureFBO = null
        }
        val graphics = world.game.engine.graphics
        shaderExposure = graphics.createShader("Scapes:shader/Exposure"
        ) { information ->
            information.supplyPreCompile { shader ->
                shader.supplyProperty("BLUR_OFFSET", SAMPLE_OFFSET)
                shader.supplyProperty("BLUR_WEIGHT", SAMPLE_WEIGHT)
                shader.supplyProperty("BLUR_LENGTH", SAMPLE_LENGTH)
            }
        }
        shaderFXAA = graphics.createShader("Scapes:shader/FXAA")
        shaderComposite1 = graphics.createShader("Scapes:shader/Composite1"
        ) { information ->
            information.supplyPreCompile { shader ->
                shader.supplyProperty("BLUR_OFFSET", BLUR_OFFSET)
                shader.supplyProperty("BLUR_WEIGHT", BLUR_WEIGHT)
                shader.supplyProperty("BLUR_LENGTH", BLUR_LENGTH)
            }
        }
        shaderComposite2 = graphics.createShader("Scapes:shader/Composite2"
        ) { information ->
            information.supplyPreCompile { shader ->
                shader.supplyProperty("ENABLE_BLOOM", bloom)
                shader.supplyProperty("ENABLE_EXPOSURE",
                        exposureFBO != null)
                shader.supplyProperty("BLUR_OFFSET", BLUR_OFFSET)
                shader.supplyProperty("BLUR_WEIGHT", BLUR_WEIGHT)
                shader.supplyProperty("BLUR_LENGTH", BLUR_LENGTH)
            }
        }
        shaderTextured = graphics.createShader("Engine:shader/Textured")
        // TODO: Move somewhere better
        particles.register(ParticleEmitterBlock(particles,
                terrainTextureRegistry.texture()))
        particles.register(ParticleEmitter3DBlock(particles))
        particles.register(ParticleEmitterFallenBodyPart(particles))
        particles.register(ParticleEmitterTransparent(particles,
                world.game.particleTransparentAtlas().texture()))
    }

    fun fogR(): Float {
        return skybox.fogR()
    }

    fun fogG(): Float {
        return skybox.fogG()
    }

    fun fogB(): Float {
        return skybox.fogB()
    }

    fun fogDistance(): Float {
        return skybox.fogDistance()
    }

    fun renderDistance(): Float {
        return renderDistance
    }

    fun damageShake(damage: Double) {
        flashTime = System.currentTimeMillis() + (ceil(
                damage) * 10).toLong() + 100
        flashStart = System.currentTimeMillis()
        val random = ThreadLocalRandom.current()
        flashDir = 1 - (random.nextInt(2) shl 1)
    }

    fun player(): MobPlayerClientMain {
        return world.player
    }

    fun world(): WorldClient {
        return world
    }

    fun particles(): ParticleSystem {
        return particles
    }

    fun cam(): Cam {
        return cam
    }

    fun skinStorage(): ClientSkinStorage {
        return skinStorage
    }

    override fun init(gl: GL) {
        val hud = world.game.hud()
        hud.removeAll()
        hud.add(8.0, 434.0, -1.0, -1.0
        ) { GuiComponentChat(it, world.game.chatHistory()) }
        skybox.init(gl)
    }

    override fun renderScene(gl: GL) {
        val player = world.player
        val playerModel = world.playerModel
        val blackout = 1.0f - clamp(1.0f - player.health().toFloat() * 0.05f,
                0.0f, 1.0f)
        brightness += ((blackout - brightness) * 0.1).toFloat()
        brightness = clamp(brightness, 0f, 1f)
        isMouseGrabbed = !player.hasGui()
        val pitch = playerModel?.pitch() ?: 0.0
        var tilt = 0.0
        val yaw = playerModel?.yaw() ?: 0.0
        val flashDiff = flashTime - flashStart
        val flashPos = System.currentTimeMillis() - flashStart
        if (flashDiff > 0.0f && flashPos > 0.0f) {
            val flashDiv = flashPos.toDouble() / flashDiff
            if (flashDiv < 1.0f) {
                if (flashPos.toDouble() / flashDiff > 0.5f) {
                    tilt += (1 - flashDiv) * flashDir.toDouble() * flashDiff.toDouble() * 0.1
                } else {
                    tilt += flashDiv * flashDir.toDouble() * flashDiff.toDouble() * 0.1
                }
            }
        }
        cam.setView(
                (playerModel?.pos() ?: Vector3d.ZERO).plus(player.viewOffset()),
                player.speed(),
                pitch.toFloat(), yaw.toFloat(), tilt.toFloat())
        cam.setPerspective(gl.sceneWidth().toFloat() / gl.sceneHeight(), fov)
        engine.sounds.setListener(cam.position.now(), player.rot(),
                player.speed())
        terrainTextureRegistry.render(gl)
        renderWorld(gl, cam)
    }

    override fun postRender(gl: GL,
                            delta: Double) {
        if (exposureFBO != null) {
            state.fbo(0).texturesColor[0].bind(gl)
            exposureFBO.activate(gl)
            gl.viewport(0, 0, 1, 1)
            gl.setProjectionOrthogonal(0.0f, 0.0f, 1.0f, 1.0f)
            shaderExposure.setUniform1f(4, min(1.0, delta * 0.5).toFloat())
            model.render(gl, shaderExposure)
            exposureFBO.deactivate(gl)
        }
        val player = world.player
        val newRenderDistance = world.terrain.renderer.actualRenderDistance().toFloat()
        if (renderDistance > newRenderDistance) {
            renderDistance = newRenderDistance
        } else {
            val factor = min(1.0, delta)
            renderDistance += ((newRenderDistance - renderDistance) * factor).toFloat()
        }
        val newFov = min(sqrt(sqr(player.speedX()) + sqr(
                player.speedY())).toFloat() * 2.0f + 90.0f,
                120.0f)
        val factor = min(1.0, delta * 10.0)
        fov += ((newFov - fov) * factor).toFloat()
        cameraPositionXDebug.setValue(cam.position.doubleX())
        cameraPositionYDebug.setValue(cam.position.doubleY())
        cameraPositionZDebug.setValue(cam.position.doubleZ())
        val xx = floor(cam.position.doubleX())
        val yy = floor(cam.position.doubleY())
        val zz = floor(cam.position.doubleZ())
        lightDebug.setValue(world.terrain.light(xx, yy, zz))
        blockLightDebug.setValue(world.terrain.blockLight(xx, yy, zz))
        sunLightDebug.setValue(world.terrain.sunLight(xx, yy, zz))
        world.game.renderTimestamp(delta)
        world.updateRender(cam, delta)
        skybox.renderUpdate(cam, delta)
        skinStorage.update(player.game.client())
    }

    override fun postProcessing(gl: GL,
                                pass: Int): Shader {
        var pass = pass
        if (fxaa) {
            if (pass == 0) {
                gl.activeTexture(3)
                textureNoise.get().bind(gl)
                gl.activeTexture(0)
                shaderFXAA.setUniform1i(4, 3)
                return shaderFXAA
            } else {
                pass--
            }
        }
        if (pass == 0 && bloom) {
            return shaderComposite1
        } else {
            if (exposureFBO != null) {
                gl.activeTexture(3)
                exposureFBO.texturesColor[0].bind(gl)
                gl.activeTexture(0)
            }
            shaderComposite2.setUniform1f(6, brightness)
            shaderComposite2.setUniform1f(7,
                    pow(2.0, skybox.exposure().toDouble()).toFloat())
            shaderComposite2.setUniform1i(8, 2)
            shaderComposite2.setUniform1i(9, 3)
            return shaderComposite2
        }
    }

    override fun renderPasses(): Int {
        if (fxaa) {
            if (bloom) {
                return 3
            } else {
                return 2
            }
        } else if (bloom) {
            return 2
        } else {
            return 1
        }
    }

    override fun colorAttachments(): Int {
        if (bloom) {
            return 2
        } else {
            return 1
        }
    }

    override fun dispose(gl: GL) {
        skybox.dispose(gl)
    }

    override fun dispose() {
        world.dispose()
        particles.dispose()
    }

    fun takePanorama(gl: GL) {
        val fbo = engine.graphics.createFramebuffer(256, 256, 1, true, false,
                false)
        fbo.activate(gl)
        val panorama = takePanorama(gl, fbo)
        fbo.deactivate(gl)
        if (state is GameStateGameSP) {
            try {
                state.source.panorama(panorama)
            } catch (e: IOException) {
                logger.warn(e) { "Failed to save panorama" }
            }
        }
    }

    fun takePanorama(gl: GL,
                     fbo: Framebuffer): WorldSource.Panorama {
        world.game.setHudVisible(false)
        val cam = Cam(this.cam.near, this.cam.far)
        cam.setPerspective(1.0f, 90.0f)
        val panorama = newPanorama {
            var pitch = 0.0f
            var yaw = 0.0f
            if (it == 1) {
                yaw = 90.0f
            } else if (it == 2) {
                yaw = 180.0f
            } else if (it == 3) {
                yaw = 270.0f
            } else if (it == 4) {
                pitch = -90.0f
            } else if (it == 5) {
                pitch = 90.0f
            }
            cam.setView(this.cam.position.now(), this.cam.velocity.now(), pitch,
                    yaw, 0.0f)
            gl.clearDepth()
            renderWorld(gl, cam, 256, 256)
            fbo.texturesColor[0].bind(gl)
            gl.screenShotFBO(fbo)
        }
        world.game.setHudVisible(true)
        return panorama
    }

    fun renderWorld(gl: GL,
                    cam: Cam,
                    width: Int = gl.sceneWidth(),
                    height: Int = gl.sceneHeight()) {
        if (width != skyboxFBO.width() || height != skyboxFBO.height()) {
            skyboxFBO.setSize(width, height)
        }
        val matrixStack = gl.matrixStack()
        gl.viewport(0, 0, width, height)
        skyboxFBO.activate(gl)
        gl.disableDepthTest()
        gl.disableDepthMask()
        gl.setProjectionPerspective(width.toFloat(), height.toFloat(), cam)
        matrixStack.push()
        skybox.render(gl, cam)
        matrixStack.pop()
        skyboxFBO.deactivate(gl)
        gl.viewport(0, 0, width, height)
        gl.setProjectionOrthogonal(0.0f, 0.0f, 1.0f, 1.0f)
        skyboxFBO.texturesColor[0].bind(gl)
        model.render(gl, shaderTextured)
        gl.setProjectionPerspective(width.toFloat(), height.toFloat(), cam)
        gl.enableDepthTest()
        gl.enableDepthMask()
        gl.activeTexture(1)
        skyboxFBO.texturesColor[0].bind(gl)
        gl.activeTexture(0)
        val wireframe = this.wireframe
        if (wireframe) {
            gl.enableWireframe()
        }
        world.render(gl, cam, chunkGeometryDebug)
        if (wireframe) {
            gl.disableWireframe()
        }
    }

    fun toggleChunkDebug() {
        chunkGeometryDebug = !chunkGeometryDebug
    }

    fun toggleWireframe() {
        wireframe = !wireframe
    }

    fun terrainTextureRegistry(): TerrainTextureRegistry {
        return terrainTextureRegistry
    }

    fun skybox(): WorldSkybox {
        return skybox
    }

    companion object : KLogging() {
        private val BLUR_OFFSET: String
        private val BLUR_WEIGHT: String
        private val SAMPLE_OFFSET: String
        private val SAMPLE_WEIGHT: String
        private val BLUR_LENGTH: Int
        private val SAMPLE_LENGTH: Int

        init {
            val blurOffset = gaussianBlurOffset(5, 0.01f)
            val blurWeight = gaussianBlurWeight(5) {
                pow(cos(it * PI), 0.1)
            }
            BLUR_LENGTH = blurOffset.size
            BLUR_OFFSET = join(*blurOffset)
            BLUR_WEIGHT = join(*blurWeight)
            val sampleOffset = gaussianBlurOffset(11, 0.5f)
            val sampleWeight = gaussianBlurWeight(11) {
                pow(cos(it * PI), 0.1)
            }
            for (i in sampleOffset.indices) {
                sampleOffset[i] = sampleOffset[i] + 0.5f
            }
            SAMPLE_LENGTH = sampleOffset.size
            SAMPLE_OFFSET = join(*sampleOffset)
            SAMPLE_WEIGHT = join(*sampleWeight)
        }
    }
}
