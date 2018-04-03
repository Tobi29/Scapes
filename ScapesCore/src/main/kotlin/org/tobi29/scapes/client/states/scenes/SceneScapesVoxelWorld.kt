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
package org.tobi29.scapes.client.states.scenes

import kotlinx.coroutines.experimental.runBlocking
import org.tobi29.coroutines.Timer
import org.tobi29.graphics.Cam
import org.tobi29.graphics.gaussianBlurOffset
import org.tobi29.graphics.gaussianBlurWeight
import org.tobi29.io.IOException
import org.tobi29.logging.KLogging
import org.tobi29.math.threadLocalRandom
import org.tobi29.math.vector.Vector3d
import org.tobi29.math.vector.plus
import org.tobi29.scapes.block.TerrainTextureRegistry
import org.tobi29.scapes.chunk.WorldClient
import org.tobi29.scapes.chunk.WorldSkybox
import org.tobi29.scapes.client.gui.GuiComponentChat
import org.tobi29.scapes.client.states.GameStateGameMP
import org.tobi29.scapes.client.states.GameStateGameSP
import org.tobi29.scapes.engine.graphics.*
import org.tobi29.scapes.engine.gui.debug.GuiWidgetDebugValues
import org.tobi29.scapes.engine.shader.ArrayExpression
import org.tobi29.scapes.engine.shader.BooleanExpression
import org.tobi29.scapes.engine.shader.IntegerExpression
import org.tobi29.scapes.entity.client.MobPlayerClientMain
import org.tobi29.scapes.entity.model.EntityModel
import org.tobi29.scapes.entity.model.MobModel
import org.tobi29.scapes.entity.particle.*
import org.tobi29.scapes.entity.skin.ClientSkinStorage
import org.tobi29.scapes.server.format.WorldSource
import org.tobi29.scapes.server.format.newPanorama
import org.tobi29.stdex.math.ceilToInt
import org.tobi29.stdex.math.clamp
import org.tobi29.stdex.math.floorToInt
import org.tobi29.stdex.math.sqr
import org.tobi29.utils.chain
import kotlin.math.*

class SceneScapesVoxelWorld(
    private val world: WorldClient,
    private val config: Config,
    private val cam: Cam
) : Scene(world.game.engine) {
    val state: GameStateGameMP
    private val skinStorage = world.connection.skinStorage
    private val cameraPositionXDebug: GuiWidgetDebugValues.Element
    private val cameraPositionYDebug: GuiWidgetDebugValues.Element
    private val cameraPositionZDebug: GuiWidgetDebugValues.Element
    private val lightDebug: GuiWidgetDebugValues.Element
    private val blockLightDebug: GuiWidgetDebugValues.Element
    private val sunLightDebug: GuiWidgetDebugValues.Element
    private val terrainTextureRegistry: TerrainTextureRegistry
    private val particles: ParticleSystem
    private val skybox: WorldSkybox
    private val textureNoise = engine.graphics.textures["Scapes:image/Noise"]
    private var brightness = 0.0f
    private var renderDistance = 0.0f
    private var fov = 0.0f
    private var flashDir = 0
    private var flashTime: Long = 0
    private var flashStart: Long = 0
    private var chunkGeometryDebug = false
    private var wireframe = false

    init {
        state = world.game
        particles = ParticleSystem(world, 60.0)
        terrainTextureRegistry = world.game.terrainTextureRegistry()
        val debugValues = world.game.engine.debugValues
        cameraPositionXDebug = debugValues["Camera-Position-X"]
        cameraPositionYDebug = debugValues["Camera-Position-Y"]
        cameraPositionZDebug = debugValues["Camera-Position-Z"]
        lightDebug = debugValues["Camera-Light"]
        blockLightDebug = debugValues["Camera-Block-Light"]
        sunLightDebug = debugValues["Camera-Sun-Light"]
        skybox = world.environment.createSkybox(world)
        // TODO: Move somewhere better
        particles.register(
            ParticleEmitterBlock(
                particles,
                terrainTextureRegistry.texture
            )
        )
        particles.register(ParticleEmitter3DBlock(particles))
        particles.register(ParticleEmitterFallenBodyPart(particles))
        particles.register(
            ParticleEmitterTransparent(
                particles,
                world.game.particleTransparentAtlas()
            )
        )

        world.game.hud.removeAll()
        world.game.hud.add(8.0, 434.0, -1.0, -1.0) {
            GuiComponentChat(it, world.game.chatHistory)
        }
    }

    override fun appendToPipeline(gl: GL): suspend () -> (Double) -> Unit {
        val config = config
        val width = (gl.contentWidth * config.resolutionMultiplier).roundToInt()
        val height =
            (gl.contentHeight * config.resolutionMultiplier).roundToInt()
        val fxaa = config.fxaa
        val bloom = config.bloom
        val exposure = config.autoExposure

        val sceneBuffer = gl.createFramebuffer(
            width, height, 1, true, true, false, TextureFilter.LINEAR
        )
        val scene = render(gl)
        val render: suspend () -> (Double) -> Unit = if (fxaa) {
            val shaderFXAA = engine.graphics.loadShader(
                "Scapes:shader/FXAA.stag", mapOf(
                    "SCENE_WIDTH" to IntegerExpression(width),
                    "SCENE_HEIGHT" to IntegerExpression(height)
                )
            )
            val fxaaBuffer = gl.createFramebuffer(
                width, height, 1, true, true, false, TextureFilter.LINEAR
            )
            ;{
                val renderScene = scene()
                val tNoise = textureNoise.getAsync()
                val render = gl.into(fxaaBuffer) { delta ->
                    gl.clearDepth()
                    renderScene(delta)
                }
                val pp = gl.into(sceneBuffer,
                    postProcess(gl, shaderFXAA.getAsync(), fxaaBuffer) {
                        gl.activeTexture(3)
                        tNoise.bind(gl)
                        gl.activeTexture(0)
                        setUniform1i(gl, 4, 3)
                    })
                chain(render, pp)
            }
        } else {
            ;{
                val renderScene = scene()
                gl.into(sceneBuffer) { delta ->
                    gl.clearDepth()
                    renderScene(delta)
                }
            }
        }
        val exposureFBO = if (exposure) {
            gl.createFramebuffer(1, 1, 1, false, true, false)
        } else {
            null
        }
        val pp: suspend () -> (Double) -> Unit = if (bloom) {
            val blurSamples = 5
            val shaderComposite1 = engine.graphics.loadShader(
                "Scapes:shader/Composite1.stag", mapOf(
                    "BLUR_LENGTH" to IntegerExpression(blurSamples),
                    "BLUR_OFFSET" to ArrayExpression(
                        gaussianBlurOffset(blurSamples, 0.01)
                    ),
                    "BLUR_WEIGHT" to ArrayExpression(
                        gaussianBlurWeight(blurSamples) {
                            cos(it * PI).pow(0.1)
                        })
                )
            )
            val shaderComposite2 = engine.graphics.loadShader(
                "Scapes:shader/Composite2.stag", mapOf(
                    "BLUR_LENGTH" to IntegerExpression(blurSamples),
                    "BLUR_OFFSET" to ArrayExpression(
                        gaussianBlurOffset(blurSamples, 0.01)
                    ),
                    "BLUR_WEIGHT" to ArrayExpression(
                        gaussianBlurWeight(blurSamples) {
                            cos(it * PI).pow(0.1)
                        }),
                    "ENABLE_BLOOM" to BooleanExpression(true),
                    "ENABLE_EXPOSURE" to BooleanExpression(exposure)
                )
            )
            val compositeBuffer = gl.createFramebuffer(
                width, height, 2, true, true, false,
                TextureFilter.LINEAR
            )
            ;{
                val pp1 = gl.into(
                    compositeBuffer,
                    postProcess(
                        gl, shaderComposite1.getAsync(),
                        sceneBuffer
                    )
                )
                val pp2 = postProcess(
                    gl, shaderComposite2.getAsync(),
                    compositeBuffer
                ) {
                    if (exposureFBO != null) {
                        gl.activeTexture(3)
                        exposureFBO.texturesColor[0].bind(gl)
                        gl.activeTexture(0)
                    }
                    setUniform1f(gl, 6, brightness)
                    setUniform1f(gl, 7, 2.0f.pow(skybox.exposure()))
                    setUniform1i(gl, 8, 2)
                    setUniform1i(gl, 9, 3)
                }
                chain(pp1, pp2)
            }
        } else {
            val blurSamples = 5
            val shaderComposite = engine.graphics.loadShader(
                "Scapes:shader/Composite2.stag", mapOf(
                    "BLUR_LENGTH" to IntegerExpression(blurSamples),
                    "BLUR_OFFSET" to ArrayExpression(
                        gaussianBlurOffset(blurSamples, 0.01)
                    ),
                    "BLUR_WEIGHT" to ArrayExpression(
                        gaussianBlurWeight(blurSamples) {
                            cos(it * PI).pow(0.1)
                        }),
                    "ENABLE_BLOOM" to BooleanExpression(false),
                    "ENABLE_EXPOSURE" to BooleanExpression(exposure)
                )
            )
            ;{
                postProcess(gl, shaderComposite.getAsync(), sceneBuffer) {
                    if (exposureFBO != null) {
                        gl.activeTexture(3)
                        exposureFBO.texturesColor[0].bind(gl)
                        gl.activeTexture(0)
                    }
                    setUniform1f(gl, 6, brightness)
                    setUniform1f(gl, 7, 2.0f.pow(skybox.exposure()))
                    setUniform1i(gl, 8, 2)
                    setUniform1i(gl, 9, 3)
                }
            }
        }
        if (exposureFBO != null) {
            val blurSamples = 11
            val shaderExposure = engine.graphics.loadShader(
                "Scapes:shader/Exposure.stag", mapOf(
                    "BLUR_LENGTH" to IntegerExpression(blurSamples),
                    "BLUR_OFFSET" to ArrayExpression(
                        gaussianBlurOffset(blurSamples, 0.5).also {
                            for (i in it.indices) {
                                it[i] += 0.5
                            }
                        }),
                    "BLUR_WEIGHT" to ArrayExpression(
                        gaussianBlurWeight(blurSamples) {
                            cos(it * PI).pow(0.1)
                        })
                )
            )
            val exposureTimer = Timer()
            exposureTimer.init()
            val exp: suspend () -> (Double) -> Unit = {
                gl.into(exposureFBO, postProcess(
                    gl, shaderExposure.getAsync(),
                    sceneBuffer
                ) {
                    val tickDiff = exposureTimer.tick()
                    val delta = Timer.toDelta(tickDiff).coerceIn(0.0001, 0.1)
                    setUniform1f(gl, 4, (delta * 0.5).toFloat())
                })
            }

            return { chain(render(), pp(), exp()) }
        } else {
            return { chain(render(), pp()) }
        }
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
        flashTime = System.currentTimeMillis() +
                ((damage).ceilToInt() * 10).toLong() + 100
        flashStart = System.currentTimeMillis()
        val random = threadLocalRandom()
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

    fun render(gl: GL): suspend () -> (Double) -> Unit {
        val config = config
        val resolutionMultiplier = config.resolutionMultiplier
        val width = (gl.contentWidth * resolutionMultiplier).roundToInt()
        val height = (gl.contentHeight * resolutionMultiplier).roundToInt()
        val renderWorld = renderWorld(gl, cam, width, height)
        return {
            val render = renderWorld()
            ; { delta ->
            val player = world.player
            val playerModel = player.getOrNull(
                EntityModel.COMPONENT
            ) as? MobModel
            val blackout = 1.0f - clamp(
                1.0f - player.health().toFloat() * 0.05f,
                0.0f, 1.0f
            )
            brightness += ((blackout - brightness) * 0.1).toFloat()
            brightness = clamp(brightness, 0f, 1f)
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
                (playerModel?.pos() ?: Vector3d.ZERO).plus(
                    player.viewOffset()
                ),
                player.speed(),
                pitch.toFloat(), yaw.toFloat(), tilt.toFloat()
            )
            cam.setPerspective(gl.aspectRatio().toFloat(), fov)
            engine.sounds.setListener(
                cam.position.now(), player.rot(),
                player.speed()
            )
            terrainTextureRegistry.render(gl)
            render(delta)
        }
        }
    }

    fun step(delta: Double) {
        val player = world.player
        val newRenderDistance =
            world.terrain.renderer.actualRenderDistance().toFloat()
        if (renderDistance > newRenderDistance) {
            renderDistance = newRenderDistance
        } else {
            val factor = min(1.0, delta)
            renderDistance += ((newRenderDistance - renderDistance) * factor).toFloat()
        }
        val newFov = min(
            sqrt(
                sqr(player.speedX()) + sqr(
                    player.speedY()
                )
            ).toFloat() * 2.0f + 90.0f,
            120.0f
        )
        val factor = min(1.0, delta * 10.0)
        fov += ((newFov - fov) * factor).toFloat()
        cameraPositionXDebug.setValue(cam.position.x)
        cameraPositionYDebug.setValue(cam.position.y)
        cameraPositionZDebug.setValue(cam.position.z)
        val xx = cam.position.x.floorToInt()
        val yy = cam.position.y.floorToInt()
        val zz = cam.position.z.floorToInt()
        lightDebug.setValue(world.terrain.light(xx, yy, zz))
        blockLightDebug.setValue(world.terrain.blockLight(xx, yy, zz))
        sunLightDebug.setValue(world.terrain.sunLight(xx, yy, zz))
        world.updateRender(cam, delta)
        skybox.renderUpdate(cam, delta)
        skinStorage.update(player.game.client())
    }

    suspend fun dispose() {
        world.dispose()
        particles.dispose()
        skybox.dispose()
    }

    fun takePanorama(gl: GL) {
        val fbo = engine.graphics.createFramebuffer(
            256, 256, 1, true, false,
            false
        )
        val panorama = fbo.activate(gl) {
            takePanorama(gl, fbo)
        }
        if (state is GameStateGameSP) {
            try {
                state.source.panorama(panorama)
            } catch (e: IOException) {
                logger.warn(e) { "Failed to save panorama" }
            }
        }
    }

    fun takePanorama(
        gl: GL,
        fbo: Framebuffer
    ): WorldSource.Panorama {
        world.game.setHudVisible(false)
        val cam = Cam(this.cam.near, this.cam.far)
        cam.setPerspective(1.0f, 90.0f)
        val render = renderWorld(gl, cam, 256, 256).let {
            runBlocking { it() }
        }
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
            cam.setView(
                this.cam.position.now(), this.cam.velocity.now(), pitch,
                yaw, 0.0f
            )
            gl.clearDepth()
            render(0.0)
            gl.screenShotFBOColor(0, 0, fbo.width(), fbo.height())
        }
        world.game.setHudVisible(true)
        return panorama
    }

    fun renderWorld(
        gl: GL,
        cam: Cam,
        width: Int,
        height: Int
    ): suspend () -> (Double) -> Unit {
        val skyboxFBO = gl.createFramebuffer(
            width, height, 1,
            false, true, false
        )
        val shaderTextured = engine.graphics.loadShader(SHADER_TEXTURED)
        val renderSkybox = skybox.appendToPipeline(gl, cam)
        val renderBackground: suspend () -> (Double) -> Unit = {
            val skyboxRender = renderSkybox()
            gl.into(skyboxFBO) { delta ->
                gl.matrixStack.push { matrix ->
                    gl.enableCulling()
                    gl.enableDepthTest()
                    gl.disableDepthMask()
                    gl.setBlending(BlendingMode.NORMAL)
                    matrix.identity()
                    matrix.modelViewProjection().perspective(
                        cam.fov,
                        gl.aspectRatio().toFloat(), cam.near, cam.far
                    )
                    matrix.modelViewProjection().camera(cam)
                    matrix.modelView().camera(cam)
                    skyboxRender(delta)
                }
            }
        }
        val world = world.addToPipeline(gl, cam, chunkGeometryDebug)
        return {
            val backgroundRender = renderBackground()
            val worldRender = world()
            val copyBackground = postProcess(
                gl, shaderTextured.getAsync(),
                skyboxFBO
            )
            ;{ delta ->
            backgroundRender(delta)
            copyBackground(delta)
            gl.enableDepthTest()
            gl.enableDepthMask()
            gl.activeTexture(1)
            skyboxFBO.texturesColor[0].bind(gl)
            gl.activeTexture(0)
            val wireframe = this.wireframe
            if (wireframe) {
                gl.enableWireframe()
            }
            gl.matrixStack.push { matrix ->
                gl.enableCulling()
                gl.enableDepthTest()
                gl.setBlending(BlendingMode.NORMAL)
                matrix.identity()
                matrix.modelViewProjection().perspective(
                    cam.fov,
                    gl.aspectRatio().toFloat(), cam.near, cam.far
                )
                matrix.modelViewProjection().camera(cam)
                matrix.modelView().camera(cam)
                worldRender(delta)
            }
            if (wireframe) {
                gl.disableWireframe()
            }
        }
        }
    }

    fun toggleChunkDebug() {
        chunkGeometryDebug = !chunkGeometryDebug
        state.dirtyPipeline()
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

    companion object : KLogging()

    data class Config(
        val resolutionMultiplier: Double = 1.0,
        val animations: Boolean = true,
        val fxaa: Boolean = true,
        val bloom: Boolean = true,
        val autoExposure: Boolean = true
    )
}
