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

package org.tobi29.scapes.vanilla.basics.world

import kotlinx.coroutines.experimental.Deferred
import org.tobi29.coroutines.tryGet
import org.tobi29.graphics.Cam
import org.tobi29.graphics.hsvToRGB
import org.tobi29.io.view
import org.tobi29.math.Random
import org.tobi29.math.threadLocalRandom
import org.tobi29.math.vector.*
import org.tobi29.scapes.chunk.WorldClient
import org.tobi29.scapes.chunk.WorldSkybox
import org.tobi29.scapes.client.loadShader
import org.tobi29.scapes.engine.graphics.*
import org.tobi29.scapes.engine.gui.GuiComponentGroup
import org.tobi29.scapes.engine.gui.GuiComponentGroupSlab
import org.tobi29.scapes.engine.gui.debug.GuiWidgetDebugValues
import org.tobi29.scapes.engine.shader.IntegerExpression
import org.tobi29.scapes.engine.sound.StaticAudio
import org.tobi29.scapes.vanilla.basics.entity.client.MobPlayerClientMainVB
import org.tobi29.scapes.vanilla.basics.entity.particle.ParticleEmitterRain
import org.tobi29.scapes.vanilla.basics.entity.particle.ParticleEmitterSnow
import org.tobi29.scapes.vanilla.basics.entity.server.ComponentMobLivingServerCondition
import org.tobi29.scapes.vanilla.basics.generator.BiomeGenerator
import org.tobi29.scapes.vanilla.basics.generator.ClimateGenerator
import org.tobi29.scapes.vanilla.basics.generator.CloudGenerator
import org.tobi29.scapes.vanilla.basics.gui.GuiComponentCondition
import org.tobi29.scapes.vanilla.basics.gui.GuiComponentHotbar
import org.tobi29.stdex.copy
import org.tobi29.stdex.math.*
import kotlin.math.*

class WorldSkyboxOverworld(
    private val climateGenerator: ClimateGenerator,
    private val biomeGenerator: BiomeGenerator,
    private val world: WorldClient
) : WorldSkybox {
    private val billboardMesh: Model
    private val cloudMesh: Model
    private val skyboxMesh: Model
    private val skyboxBottomMesh: Model
    private val starMesh: Model
    private val textureMoon =
        world.game.engine.graphics.textures["VanillaBasics:image/Moon"]
    private var temperatureDebug: GuiWidgetDebugValues.Element? = null
    private var humidityDebug: GuiWidgetDebugValues.Element? = null
    private var weatherDebug: GuiWidgetDebugValues.Element? = null
    private var biomeDebug: GuiWidgetDebugValues.Element? = null
    private var staminaDebug: GuiWidgetDebugValues.Element? = null
    private var wakeDebug: GuiWidgetDebugValues.Element? = null
    private var hungerDebug: GuiWidgetDebugValues.Element? = null
    private var thirstDebug: GuiWidgetDebugValues.Element? = null
    private var bodyTemperatureDebug: GuiWidgetDebugValues.Element? = null
    private var sleepingDebug: GuiWidgetDebugValues.Element? = null
    private var exposureDebug: GuiWidgetDebugValues.Element? = null
    private var rainAudio: StaticAudio? = null
    private var windAudio: StaticAudio? = null
    private var rainGainWait = 0.0
    private var downfallWait = 0.0
    private var exposure = 0.3
    private var fogBrightness = 0.0f
    private var rainGain = 0.0
    private var fogR = 0.0f
    private var fogG = 0.0f
    private var fogB = 0.0f
    private var fogDistance = 0.0f

    init {
        val seed = world.seed
        billboardMesh = world.game.engine.graphics.createVTI(
            floatArrayOf(
                1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f,
                -1.0f, 1.0f, 1.0f, -1.0f, 1.0f
            ),
            floatArrayOf(0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f),
            intArrayOf(0, 1, 2, 0, 2, 3), RenderType.TRIANGLES
        )
        val cloudMesh = Mesh(true)
        var lastX = 1.0
        var lastY = 0.0
        var x: Double
        var y: Double
        var rad: Double
        cloudMesh.color(1.0, 1.0, 1.0, 1.0)
        run {
            var dir = -22.5
            while (dir < 360.0) {
                rad = dir.toRad()
                x = cos(rad)
                y = sin(rad)
                cloudMesh.texture(0.5, 0.5)
                cloudMesh.vertex(0.0, 0.0, 0.2)
                cloudMesh.texture(0.5 + x * 0.5, 0.5 + y * 0.5)
                cloudMesh.vertex(x, y, -0.1)
                cloudMesh.texture(0.5 + lastX * 0.5, 0.5 + lastY * 0.5)
                cloudMesh.vertex(lastX, lastY, -0.1)
                lastX = x
                lastY = y
                dir += 22.5f
            }
        }
        this.cloudMesh = cloudMesh.finish(world.game.engine.graphics)
        lastX = 1.0
        lastY = 0.0
        val skyboxMesh = Mesh(true)
        run {
            var dir = -22.5
            while (dir < 360.0) {
                rad = dir.toRad()
                x = cos(rad)
                y = sin(rad)
                skyboxMesh.color(COLORS[0].x, COLORS[0].y, COLORS[0].z, 1.0)
                skyboxMesh.vertex(0.0, 0.0, 0.1)
                skyboxMesh.color(COLORS[1].x, COLORS[1].y, COLORS[1].z, 1.0)
                skyboxMesh.vertex(x, y, 0.0)
                skyboxMesh.vertex(lastX, lastY, 0.0)
                skyboxMesh.vertex(lastX, lastY, 0.0)
                skyboxMesh.vertex(x, y, 0.0)
                skyboxMesh.vertex(0.0, 0.0, -1.0)
                lastX = x
                lastY = y
                dir += 22.5f
            }
        }
        this.skyboxMesh = skyboxMesh.finish(world.game.engine.graphics)
        lastX = 1.0
        lastY = 0.0
        val skyboxBottomMesh = Mesh(true)
        skyboxBottomMesh.color(COLORS[1].x, COLORS[1].y, COLORS[1].z, 1.0)
        var dir = -22.5
        while (dir < 360.0) {
            rad = dir.toRad()
            x = cos(rad)
            y = sin(rad)
            skyboxBottomMesh.vertex(lastX, lastY, -0.1)
            skyboxBottomMesh.color(COLORS[1].x, COLORS[1].y, COLORS[1].z, 0.0)
            skyboxBottomMesh.vertex(lastX, lastY, 0.0)
            skyboxBottomMesh.vertex(x, y, 0.0)
            skyboxBottomMesh.vertex(x, y, 0.0)
            skyboxBottomMesh.color(COLORS[1].x, COLORS[1].y, COLORS[1].z, 1.0)
            skyboxBottomMesh.vertex(x, y, -0.1)
            skyboxBottomMesh.vertex(lastX, lastY, -0.1)
            skyboxBottomMesh.vertex(lastX, lastY, -0.1)
            skyboxBottomMesh.vertex(x, y, -0.1)
            skyboxBottomMesh.vertex(0.0, 0.0, -1.0)
            lastX = x
            lastY = y
            dir += 22.5f
        }
        this.skyboxBottomMesh = skyboxBottomMesh.finish(
            world.game.engine.graphics
        )
        val starMesh = Mesh()
        val random = Random(seed)
        for (i in 0..1999) {
            addStar(
                0.001 + sqr(random.nextDouble()) * 0.009,
                random.nextInt(COLORS.size),
                sqr(random.nextDouble()), starMesh, random
            )
        }
        for (i in 0..19) {
            addStar(
                0.01 + random.nextDouble() * 0.02, 0,
                sqr(random.nextDouble()), starMesh, random
            )
        }
        this.starMesh = starMesh.finish(world.game.engine.graphics)
    }

    override fun update(delta: Double) {
        val player = world.player
        val pos = player.getCurrentPos()
        val weather = climateGenerator.weather(
            pos.x,
            pos.y
        )
        rainGainWait -= delta
        if (rainGainWait <= 0.0) {
            val emitter = world.scene.particles().emitter(
                ParticleEmitterRain::class.java
            )
            val rainDrops = emitter.andResetRaindrops
            rainGainWait += 0.05
            rainGain += (rainDrops / 128.0 - rainGain) * 0.04
            rainGain = clamp(rainGain, 0.0, 1.0)
            rainAudio?.gain = rainGain
        }
        windAudio?.gain = clamp(weather * 8.0 - 6.0, 0.0, 1.0)
        downfallWait -= delta
        while (downfallWait <= 0.0) {
            downfallWait += 0.05
            val weatherPos = player.getCurrentPos().plus(
                Vector3d(
                    player.speedX(), player.speedY(),
                    player.speedZ() + 16
                )
            )
            val temperature = climateGenerator.temperature(
                weatherPos.x.floorToInt(),
                weatherPos.y.floorToInt(),
                weatherPos.z.floorToInt()
            )
            val downfallIntensity = max(weather * 2.0 - 1.0, 0.0)
            if (temperature > 0) {
                val amount = (downfallIntensity * 32.0).roundToInt()
                val emitter = world.scene.particles().emitter(
                    ParticleEmitterRain::class.java
                )
                for (i in 0 until amount) {
                    emitter.add { instance ->
                        val random = threadLocalRandom()
                        instance.pos.setXYZ(
                            random.nextDouble() * 32.0 - 16.0,
                            random.nextFloat() * 32.0 - 16.0, 0.0
                        )
                        instance.pos.add(weatherPos)
                        // TODO: Wind speed
                        instance.speed.setXYZ(
                            random.nextDouble() * 0.5 - 0.25,
                            random.nextDouble() * 0.5 - 0.25, -10.0
                        )
                        instance.time = 4.0f
                    }
                }
            } else {
                val amount = (downfallIntensity * 12.0).roundToInt()
                val emitter = world.scene.particles().emitter(
                    ParticleEmitterSnow::class.java
                )
                for (i in 0 until amount) {
                    emitter.add { instance ->
                        val random = threadLocalRandom()
                        instance.pos.setXYZ(
                            random.nextDouble() * 32.0 - 16.0,
                            random.nextDouble() * 32.0 - 16.0, 0.0
                        )
                        instance.pos.add(weatherPos)
                        instance.speed.setXYZ(
                            random.nextDouble() - 0.5,
                            random.nextDouble() - 0.5, -8.0
                        )
                        instance.time = 4.0f
                        instance.dir = random.nextFloat() * TWO_PI.toFloat()
                    }
                }
            }
        }
        // Debug
        temperatureDebug?.setValue(
            climateGenerator.temperature(
                pos.x.floorToInt(),
                pos.y.floorToInt(),
                pos.z.floorToInt()
            )
        )
        humidityDebug?.setValue(
            climateGenerator.humidity(
                pos.x.floorToInt(),
                pos.y.floorToInt(),
                pos.z.floorToInt()
            )
        )
        weatherDebug?.setValue(weather)
        biomeDebug?.setValue(biomeGenerator.get(pos.x, pos.y))
        player.getOrNull(ComponentMobLivingServerCondition.COMPONENT)?.let {
            staminaDebug?.setValue(it.stamina)
            wakeDebug?.setValue(it.wake)
            hungerDebug?.setValue(it.hunger)
            thirstDebug?.setValue(it.thirst)
            bodyTemperatureDebug?.setValue(it.bodyTemperature)
            sleepingDebug?.setValue(it.sleeping)
        }
        exposureDebug?.setValue(exposure)
    }

    override fun init() {
        val engine = world.game.engine
        val debugValues = engine.debugValues
        temperatureDebug = debugValues["Vanilla-Environment-Temperature"]
        humidityDebug = debugValues["Vanilla-Environment-Humidity"]
        weatherDebug = debugValues["Vanilla-Environment-Weather"]
        biomeDebug = debugValues["Vanilla-Environment-Biome"]
        staminaDebug = debugValues["Vanilla-Condition-Stamina"]
        wakeDebug = debugValues["Vanilla-Condition-Wake"]
        hungerDebug = debugValues["Vanilla-Condition-Hunger"]
        thirstDebug = debugValues["Vanilla-Condition-Thirst"]
        bodyTemperatureDebug = debugValues["Vanilla-Condition-Body-Temperature"]
        sleepingDebug = debugValues["Vanilla-Condition-Sleeping"]
        exposureDebug = debugValues["Vanilla-Exposure"]
        rainAudio = engine.sounds.playStaticAudio(
            "VanillaBasics:sound/entity/particle/rain/Rain1.ogg",
            "sound.Weather", 1.0, 0.0
        )
        windAudio = engine.sounds.playStaticAudio(
            "VanillaBasics:sound/entity/particle/rain/Wind1.ogg",
            "sound.Weather", 1.0, 0.0
        )
        val player = world.player
        if (player is MobPlayerClientMainVB) {
            val hud = player.game.hud.addHori(
                0.0, 0.0, -1.0, -1.0,
                ::GuiComponentGroup
            )
            hud.spacer()
            val hudSlab = hud.addVert(
                0.0, 0.0, -1.0, 76.0,
                ::GuiComponentGroupSlab
            )
            val hudBar = hudSlab.addHori(
                0.0, 0.0, 408.0, -1.0,
                ::GuiComponentGroup
            )
            hudBar.addVert(4.0, 4.0, 400.0, 20.0) {
                GuiComponentCondition(it, player)
            }
            hudBar.addVert(4.0, 4.0, 400.0, 40.0) {
                GuiComponentHotbar(it, player)
            }
        }
    }

    override fun renderUpdate(
        cam: Cam,
        delta: Double
    ) {
        val player = world.player
        val scene = world.scene
        val factor = min(1.0, delta * 10.0)
        if (world.terrain.sunLight(
                scene.cam().position.x.floorToInt(),
                scene.cam().position.y.floorToInt(),
                scene.cam().position.z.floorToInt()
            ) > 0) {
            fogBrightness += ((1.0f - fogBrightness) * factor).toFloat()
        } else {
            fogBrightness -= (fogBrightness * factor).toFloat()
        }
        val sunElevation = (climateGenerator.sunElevation(
            cam.position.x,
            cam.position.y
        ) * RAD_2_DEG)
        val skyLight = clamp(sunElevation / 50.0 + 0.5, 0.03, 1.0).toFloat()
        if (player.isHeadInWater) {
            val pos = player.getCurrentPos()
            val light = clamp(
                world.terrain.light(
                    pos.x.floorToInt(), pos.y.floorToInt(),
                    (pos.z + 0.7).floorToInt()
                ) * 0.09333f + 0.2f, 0.0f,
                1.0f
            )
            fogR = 0.1f * light
            fogG = 0.5f * light
            fogB = 0.8f * light
            fogDistance = 0.1f
        } else {
            val latitude = climateGenerator.latitude(cam.position.y)
            val elevation = climateGenerator.sunElevationD(latitude)
            val sunsetLight = abs(
                clamp(elevation * 2.0, -1.0, 1.0)
            ).toFloat()
            fogR = mix(1.2f, skyLight * fogBrightness, sunsetLight)
            fogG = mix(0.4f, 0.9f * skyLight * fogBrightness, sunsetLight)
            fogB = mix(0.2f, 0.9f * skyLight * fogBrightness, sunsetLight)
            fogDistance = 1.0f
        }
        player.getOrNull(ComponentMobLivingServerCondition.COMPONENT)?.let {
            val heatstroke = max((it.bodyTemperature - 37.1) * 7.5, 0.0) + 1.0
            exposure += (heatstroke * 0.3 - exposure) * factor
        }
    }

    override fun appendToPipeline(
        gl: GL,
        cam: Cam
    ): suspend () -> (Double) -> Unit {
        val scene = world.scene
        val shaderSkybox = world.game.engine.graphics.loadShader(
            "VanillaBasics:shader/Skybox"
        )
        val shaderGlow = world.game.engine.graphics.loadShader(
            "VanillaBasics:shader/Glow"
        )
        val shaderClouds1 = world.game.engine.graphics.loadShader(
            "VanillaBasics:shader/Clouds", mapOf(
                "RESOLUTION" to IntegerExpression(CLOUD_TILE_SIZE),
                "TEXTURES" to IntegerExpression(1)
            )
        )
        val shaderClouds2 = world.game.engine.graphics.loadShader(
            "VanillaBasics:shader/Clouds", mapOf(
                "RESOLUTION" to IntegerExpression(CLOUD_TILE_SIZE),
                "TEXTURES" to IntegerExpression(2)
            )
        )
        val texturesClouds = Array(2) { CloudTileLayer() }
        val cloudsOffset = MutableVector2i()
        texturesClouds[0].enabled = true
        val cloudTextureEmpty = world.game.engine.graphics.createTexture(
            1, 1,
            byteArrayOf(-1, -1, -1, 0).view
        )
        val cloudTextureOffset = MutableVector2d()
        val shaderTextured = world.game.engine.graphics.loadShader(
            SHADER_TEXTURED
        )
        return {
            val sSkybox = shaderSkybox.getAsync()
            val sGlow = shaderGlow.getAsync()
            val sTextured = shaderTextured.getAsync()
            val sClouds1 = shaderClouds1.getAsync()
            val sClouds2 = shaderClouds2.getAsync()
            val tMoon = textureMoon.getAsync()
            ;{ delta ->
            val cx = scene.cam().position.x
            val cy = scene.cam().position.y
            val sunElevation = (climateGenerator.sunElevation(
                cam.position.x,
                cam.position.y
            ) * RAD_2_DEG)
            val sunAzimuth = (climateGenerator.sunAzimuth(
                cam.position.x,
                cam.position.y
            ) * RAD_2_DEG)
            val skyLight = clamp(
                sunElevation / 50.0 + 0.4, 0.0,
                1.0
            ).toFloat()
            val skyboxLight = skyLight * fogBrightness
            val sunlightNormal = world.environment.sunLightNormal(cx, cy)
            val snx = sunlightNormal.x.toFloat()
            val sny = sunlightNormal.y.toFloat()
            val snz = sunlightNormal.z.toFloat()
            val weather = climateGenerator.weather(cx, cy)
            val time = climateGenerator.dayTime() +
                    (climateGenerator.day() remP 4096L).toDouble()
            if (!texturesClouds[1].enabled
                && (abs(weather - texturesClouds[0].weather) > 0.025
                        || abs(time - texturesClouds[0].time) > 0.01)) {
                texturesClouds[1].weather = weather
                texturesClouds[1].time = time
                texturesClouds[1].enabled = true

            }
            loadCloudTextures(
                texturesClouds, cloudsOffset,
                cx, cy,
                cloudTextureOffset
            )
            if (texturesClouds[1].loaded || texturesClouds[1].blend > 0.0) {
                val fade = texturesClouds[1].blend + 1.0 * delta
                if (fade >= 1.0) {
                    copy(texturesClouds[1].textures, texturesClouds[0].textures)
                    texturesClouds[0].weather = texturesClouds[1].weather
                    texturesClouds[0].time = texturesClouds[1].time
                    texturesClouds[0].blend = 1.0
                    texturesClouds[1].blend = 0.0
                    texturesClouds[1].enabled = false
                } else {
                    texturesClouds[1].blend = fade
                    texturesClouds[0].blend = 1.0 - fade
                }
            }
            // Sky
            gl.textureEmpty().bind(gl)
            gl.setAttribute4f(GL.COLOR_ATTRIBUTE, 1.0f, 1.0f, 1.0f, 1.0f)
            sSkybox.setUniform3f(gl, 4, fogR, fogG, fogB)
            sSkybox.setUniform1f(gl, 5, scene.fogDistance())
            sSkybox.setUniform4f(
                gl, 6, skyboxLight * skyboxLight, skyboxLight,
                skyboxLight, 1.0f
            )
            val sClouds = if (texturesClouds[1].enabled) {
                sClouds2.setUniform1(gl, 3, intArrayOf(0, 1, 2, 3, 4, 5, 6, 7))
                sClouds2
            } else {
                sClouds1.setUniform1(gl, 3, intArrayOf(0, 1, 2, 3))
                sClouds1
            }
            sClouds.setUniform3f(gl, 4, fogR, fogG, fogB)
            sClouds.setUniform1f(gl, 5, scene.fogDistance())
            sClouds.setUniform4f(gl, 6, 0.7f, 0.7f, 0.7f, 1.0f)
            sClouds.setUniform4f(
                gl, 7, fogR * 1.4f, fogG * 1.5f, fogB * 1.3f,
                1.0f
            )
            sClouds.setUniform3f(gl, 8, snx, sny, snz)
            sClouds.setUniform2f(
                gl, 9, cloudTextureOffset.x.toFloat(),
                cloudTextureOffset.y.toFloat()
            )
            sClouds.setUniform1(
                gl, 10,
                FloatArray(texturesClouds.size) {
                    texturesClouds[it].blend.toFloat()
                }
            )
            skyboxMesh.render(gl, sSkybox)
            gl.setBlending(BlendingMode.ADD)
            // Stars
            if (skyLight < 1.0f) {
                val random = threadLocalRandom()
                val brightness = max(
                    1.0f - skyLight - random.nextFloat() * 0.3f, 0.0f
                )
                sGlow.setUniform4f(
                    gl, 4, brightness, brightness,
                    brightness,
                    1.0f
                )
                gl.matrixStack.push { matrix ->
                    matrix.rotateAccurate(
                        sunAzimuth + 180.0, 0.0f, 0.0f,
                        1.0f
                    )
                    matrix.rotateAccurate(-sunElevation, 1.0f, 0.0f, 0.0f)
                    starMesh.render(gl, sGlow)
                }
            }
            // Sun
            gl.matrixStack.push { matrix ->
                matrix.rotateAccurate(sunAzimuth + 180.0, 0.0f, 0.0f, 1.0f)
                matrix.rotateAccurate(-sunElevation, 1.0f, 0.0f, 0.0f)
                matrix.scale(1.0f, 1.0f, 1.0f)
                sGlow.setUniform4f(
                    gl, 4, fogR * 1.0f, fogG * 1.1f,
                    fogB * 1.1f,
                    1.0f
                )
                billboardMesh.render(gl, sGlow)
                matrix.scale(0.2f, 1.0f, 0.2f)
                sGlow.setUniform4f(
                    gl, 4, fogR * 1.6f, fogG * 1.6f,
                    fogB * 1.3f,
                    1.0f
                )
                billboardMesh.render(gl, sGlow)
            }
            // Moon
            tMoon.bind(gl)
            gl.matrixStack.push { matrix ->
                matrix.rotateAccurate(sunAzimuth, 0.0f, 0.0f, 1.0f)
                matrix.rotateAccurate(sunElevation, 1.0f, 0.0f, 0.0f)
                matrix.scale(0.1f, 1.0f, 0.1f)
                billboardMesh.render(gl, sTextured)
            }
            gl.setBlending(BlendingMode.NORMAL)
            // Clouds
            if (texturesClouds[1].enabled) {
                for (i in 3 downTo 0) {
                    gl.activeTexture(i + 4)
                    (texturesClouds[1].textures[i]?.tryGet()
                            ?: cloudTextureEmpty).bind(gl)
                }
            }
            for (i in 3 downTo 0) {
                gl.activeTexture(i)
                (texturesClouds[0].textures[i]?.tryGet()
                        ?: cloudTextureEmpty).bind(gl)
            }
            cloudMesh.render(gl, sClouds)
            gl.textureEmpty().bind(gl)
            // Bottom
            skyboxBottomMesh.render(gl, sSkybox)
        }
        }
    }

    override fun dispose() {
        rainAudio?.dispose()
        windAudio?.dispose()
    }

    override fun exposure(): Float {
        return exposure.toFloat()
    }

    override fun fogR(): Float {
        return fogR
    }

    override fun fogG(): Float {
        return fogG
    }

    override fun fogB(): Float {
        return fogB
    }

    override fun fogDistance(): Float {
        return fogDistance
    }

    companion object {
        private val STAR_COLORS = arrayOf(
            hsvToRGB(Vector3d(0.00, 0.00, 1.0)),
            hsvToRGB(Vector3d(0.00, 0.25, 1.0)),
            hsvToRGB(Vector3d(0.00, 0.70, 1.0)),
            hsvToRGB(Vector3d(0.66, 0.10, 1.0)),
            hsvToRGB(Vector3d(0.66, 0.15, 1.0))
        )
        private val COLORS = arrayOf(
            hsvToRGB(Vector3d(0.62, 0.70, 0.30)),
            hsvToRGB(Vector3d(0.56, 0.80, 0.90))
        )

        private fun addStar(
            size: Double,
            color: Int,
            brightness: Double,
            starMesh: Mesh,
            random: Random
        ) {
            var pos1 = random.nextDouble() * 2.0 - 1.0
            var pos2 = random.nextDouble() * 2.0 - 1.0
            var pos3 = random.nextDouble() * 2.0 - 1.0
            val rot = random.nextDouble() * TWO_PI
            val sqrt = 1.0 / sqrt(pos1 * pos1 + pos2 * pos2 + pos3 * pos3)
            pos1 *= sqrt
            pos2 *= sqrt
            pos3 *= sqrt
            val sin = sin(rot)
            val cos = cos(rot)
            val aTan1 = atan2(pos1, pos3)
            val aTan2 = atan2(sqrt(pos1 * pos1 + pos3 * pos3), pos2)
            val cosATan1 = cos(aTan1)
            val sinATan1 = sin(aTan1)
            val cosATan2 = cos(aTan2)
            val sinATan2 = sin(aTan2)
            val starColor = STAR_COLORS[color] * brightness
            starMesh.color(starColor.x, starColor.y, starColor.z, 1.0)
            for (vertex in 0..3) {
                var xx = ((vertex and 2) - 1).toDouble()
                var yy = ((vertex + 1 and 2) - 1).toDouble()
                starMesh.texture(xx, yy)
                xx *= size
                yy *= size
                val var1 = xx * cos - yy * sin
                val var2 = yy * cos + xx * sin
                val var4 = -var1 * cosATan2
                val xShift = var4 * sinATan1 - var2 * cosATan1
                val zShift = var2 * sinATan1 + var4 * cosATan1
                val yShift = var1 * sinATan2
                starMesh.vertex(pos1 + xShift, pos2 + yShift, pos3 + zShift)
            }
        }
    }

    private fun loadCloudTextures(
        textures: Array<CloudTileLayer>,
        oldOffset: MutableVector2i,
        cameraX: Double,
        cameraY: Double,
        textureOffset: MutableVector2d? = null
    ) {
        val cx = cameraX / 2048.0
        val cy = cameraY / 2048.0
        val x = cx.floorToInt()
        val y = cy.floorToInt()
        loadCloudTextures(textures, oldOffset, x, y)
        textureOffset?.apply {
            this.x = cx - x
            this.y = cy - y
        }
    }

    private fun loadCloudTextures(
        textures: Array<CloudTileLayer>,
        oldOffset: MutableVector2i,
        x: Int,
        y: Int
    ) {
        textures.forEach { layer ->
            val tiles = layer.textures
            if (layer.enabled) {
                if (x - oldOffset.x !in -1..1 || y - oldOffset.y !in -1..1) {
                    tiles[0 + 0 * 2] = null
                    tiles[1 + 0 * 2] = null
                    tiles[0 + 1 * 2] = null
                    tiles[1 + 1 * 2] = null
                } else {
                    if (x == oldOffset.x + 1) {
                        tiles[0 + 0 * 2] = tiles[1 + 0 * 2]
                        tiles[0 + 1 * 2] = tiles[1 + 1 * 2]
                        tiles[1 + 0 * 2] = null
                        tiles[1 + 1 * 2] = null
                    } else if (x == oldOffset.x - 1) {
                        tiles[1 + 0 * 2] = tiles[0 + 0 * 2]
                        tiles[1 + 1 * 2] = tiles[0 + 1 * 2]
                        tiles[0 + 0 * 2] = null
                        tiles[0 + 1 * 2] = null
                    }
                    if (y == oldOffset.y + 1) {
                        tiles[0 + 0 * 2] = tiles[0 + 1 * 2]
                        tiles[1 + 0 * 2] = tiles[1 + 1 * 2]
                        tiles[0 + 1 * 2] = null
                        tiles[1 + 1 * 2] = null
                    } else if (y == oldOffset.y - 1) {
                        tiles[0 + 1 * 2] = tiles[0 + 0 * 2]
                        tiles[1 + 1 * 2] = tiles[1 + 0 * 2]
                        tiles[0 + 0 * 2] = null
                        tiles[1 + 0 * 2] = null
                    }
                }
                val weather = layer.weather
                val time = layer.time
                if (tiles[0 + 0 * 2] == null)
                    tiles[0 + 0 * 2] =
                            loadCloudTexture(weather, time, x + 0, y + 0)
                if (tiles[1 + 0 * 2] == null)
                    tiles[1 + 0 * 2] =
                            loadCloudTexture(weather, time, x + 1, y + 0)
                if (tiles[0 + 1 * 2] == null)
                    tiles[0 + 1 * 2] =
                            loadCloudTexture(weather, time, x + 0, y + 1)
                if (tiles[1 + 1 * 2] == null)
                    tiles[1 + 1 * 2] =
                            loadCloudTexture(weather, time, x + 1, y + 1)
            }
        }
        oldOffset.x = x
        oldOffset.y = y
    }

    private fun loadCloudTexture(
        weather: Double,
        time: Double,
        x: Int,
        y: Int
    ): Deferred<Texture> = world.game.engine.resources.load {
        world.game.engine.graphics.createTexture(
            CloudGenerator(climateGenerator.cloudSeed).generate(
                weather, time,
                x.toDouble(), y.toDouble(),
                CLOUD_TILE_SIZE
            ),
            minFilter = TextureFilter.LINEAR,
            magFilter = TextureFilter.LINEAR,
            wrapS = TextureWrap.CLAMP,
            wrapT = TextureWrap.CLAMP
        )
    }.get()
}

private class CloudTileLayer {
    var enabled = false
        set(value) {
            field = value
            textures.fill(null)
        }
    var blend = 0.0
    var weather = 0.0
    var time = 0.0
    val loaded get() = enabled && textures.all { it?.isCompleted == true }
    val textures = arrayOfNulls<Deferred<Texture>>(4)
}

private const val CLOUD_TILE_SIZE = 1024
