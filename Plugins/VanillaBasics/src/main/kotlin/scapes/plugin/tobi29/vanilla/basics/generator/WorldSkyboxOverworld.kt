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

package scapes.plugin.tobi29.vanilla.basics.generator

import org.tobi29.scapes.chunk.WorldClient
import org.tobi29.scapes.chunk.WorldSkybox
import org.tobi29.scapes.engine.graphics.*
import org.tobi29.scapes.engine.gui.GuiComponentGroup
import org.tobi29.scapes.engine.gui.GuiComponentGroupSlab
import org.tobi29.scapes.engine.gui.debug.GuiWidgetDebugValues
import org.tobi29.scapes.engine.sound.StaticAudio
import org.tobi29.scapes.engine.utils.graphics.Cam
import org.tobi29.scapes.engine.utils.io.tag.getDouble
import org.tobi29.scapes.engine.utils.math.*
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.engine.utils.math.vector.plus
import scapes.plugin.tobi29.vanilla.basics.entity.client.MobPlayerClientMainVB
import scapes.plugin.tobi29.vanilla.basics.entity.particle.ParticleEmitterRain
import scapes.plugin.tobi29.vanilla.basics.entity.particle.ParticleEmitterSnow
import scapes.plugin.tobi29.vanilla.basics.gui.GuiComponentCondition
import scapes.plugin.tobi29.vanilla.basics.gui.GuiComponentHotbar
import java.util.*
import java.util.concurrent.ThreadLocalRandom

class WorldSkyboxOverworld(private val climateGenerator: ClimateGenerator,
                           private val biomeGenerator: BiomeGenerator,
                           private val world: WorldClient) : WorldSkybox {
    private val fbo: Framebuffer
    private val billboardMesh: Model
    private val cloudTextureMesh: Model
    private val cloudMesh: Model
    private val skyboxMesh: Model
    private val skyboxBottomMesh: Model
    private val starMesh: Model
    private val shaderSkybox: Shader
    private val shaderGlow: Shader
    private val shaderClouds: Shader
    private val shaderTextured: Shader
    private val textureMoon = world.game.engine.graphics.textures["VanillaBasics:image/Moon"]
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
    private var rainGain = 0.0f
    private var fogR = 0.0f
    private var fogG = 0.0f
    private var fogB = 0.0f
    private var fogDistance = 0.0f

    init {
        val seed = world.seed
        fbo = world.game.engine.graphics.createFramebuffer(512, 512, 1,
                false, false,
                true)
        billboardMesh = createVTI(world.game.engine,
                floatArrayOf(1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f,
                        -1.0f, 1.0f, 1.0f, -1.0f, 1.0f),
                floatArrayOf(0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f),
                intArrayOf(0, 1, 2, 0, 2, 3), RenderType.TRIANGLES)
        cloudTextureMesh = createVTI(world.game.engine,
                floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f,
                        0.0f, 0.0f, 1.0f, 0.0f),
                floatArrayOf(0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f),
                intArrayOf(0, 1, 2, 0, 2, 3), RenderType.TRIANGLES)
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
        this.cloudMesh = cloudMesh.finish(world.game.engine)
        lastX = 1.0
        lastY = 0.0
        val skyboxMesh = Mesh(true)
        run {
            var dir = -22.5
            while (dir < 360.0) {
                rad = dir.toRad()
                x = cos(rad)
                y = sin(rad)
                skyboxMesh.color(COLORS[0][0], COLORS[0][1], COLORS[0][2], 1.0)
                skyboxMesh.vertex(0.0, 0.0, 0.1)
                skyboxMesh.color(COLORS[1][0], COLORS[1][1], COLORS[1][2], 1.0)
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
        this.skyboxMesh = skyboxMesh.finish(world.game.engine)
        lastX = 1.0
        lastY = 0.0
        val skyboxBottomMesh = Mesh(true)
        skyboxBottomMesh.color(COLORS[1][0], COLORS[1][1], COLORS[1][2], 1.0)
        var dir = -22.5
        while (dir < 360.0) {
            rad = dir.toRad()
            x = cos(rad)
            y = sin(rad)
            skyboxBottomMesh.vertex(lastX, lastY, -0.1)
            skyboxBottomMesh.color(COLORS[1][0], COLORS[1][1], COLORS[1][2],
                    0.0)
            skyboxBottomMesh.vertex(lastX, lastY, 0.0)
            skyboxBottomMesh.vertex(x, y, 0.0)
            skyboxBottomMesh.vertex(x, y, 0.0)
            skyboxBottomMesh.color(COLORS[1][0], COLORS[1][1], COLORS[1][2],
                    1.0)
            skyboxBottomMesh.vertex(x, y, -0.1)
            skyboxBottomMesh.vertex(lastX, lastY, -0.1)
            skyboxBottomMesh.vertex(lastX, lastY, -0.1)
            skyboxBottomMesh.vertex(x, y, -0.1)
            skyboxBottomMesh.vertex(0.0, 0.0, -1.0)
            lastX = x
            lastY = y
            dir += 22.5f
        }
        this.skyboxBottomMesh = skyboxBottomMesh.finish(world.game.engine)
        val starMesh = Mesh()
        val random = Random(seed)
        for (i in 0..999) {
            addStar(0.001 + sqr(random.nextDouble()) * 0.009,
                    random.nextInt(COLORS.size), starMesh, random)
        }
        for (i in 0..9) {
            addStar(0.01 + random.nextDouble() * 0.02, 0, starMesh, random)
        }
        this.starMesh = starMesh.finish(world.game.engine)
        val graphics = world.game.engine.graphics
        shaderSkybox = graphics.createShader("VanillaBasics:shader/Skybox")
        shaderGlow = graphics.createShader("VanillaBasics:shader/Glow")
        shaderClouds = graphics.createShader("VanillaBasics:shader/Clouds")
        shaderTextured = graphics.createShader("Engine:shader/Textured")
    }

    override fun update(delta: Double) {
        val player = world.player
        val pos = player.getCurrentPos()
        val weather = climateGenerator.weather(pos.x,
                pos.y)
        rainGainWait -= delta
        if (rainGainWait <= 0.0) {
            val emitter = world.scene.particles().emitter(
                    ParticleEmitterRain::class.java)
            val rainDrops = emitter.andResetRaindrops
            rainGainWait += 0.05
            rainGain += (rainDrops / 128.0f - rainGain) * 0.04f
            rainGain = clamp(rainGain, 0.0f, 1.0f)
            rainAudio?.setGain(rainGain)
        }
        windAudio?.setGain(
                clamp(weather * 8.0 - 6.0, 0.0, 1.0).toFloat())
        downfallWait -= delta
        while (downfallWait <= 0.0) {
            downfallWait += 0.05
            val weatherPos = player.getCurrentPos().plus(
                    Vector3d(player.speedX(), player.speedY(),
                            player.speedZ() + 16))
            val temperature = climateGenerator.temperature(weatherPos.intX(),
                    weatherPos.intY(),
                    weatherPos.intZ())
            val downfallIntensity = max(weather * 2.0 - 1.0, 0.0)
            if (temperature > 0) {
                val amount = round(downfallIntensity * 32.0)
                val emitter = world.scene.particles().emitter(
                        ParticleEmitterRain::class.java)
                for (i in 0..amount - 1) {
                    emitter.add { instance ->
                        val random = ThreadLocalRandom.current()
                        instance.pos.set(random.nextFloat() * 32.0f - 16.0f,
                                random.nextFloat() * 32.0f - 16.0f, 0.0f)
                        instance.pos.plus(weatherPos)
                        // TODO: Wind speed
                        instance.speed.set(random.nextFloat() * 0.5f - 0.25f,
                                random.nextFloat() * 0.5f - 0.25f, -10.0f)
                        instance.time = 4.0f
                    }
                }
            } else {
                val amount = round(downfallIntensity * 12.0)
                val emitter = world.scene.particles().emitter(
                        ParticleEmitterSnow::class.java)
                for (i in 0..amount - 1) {
                    emitter.add { instance ->
                        val random = ThreadLocalRandom.current()
                        instance.pos.set(random.nextFloat() * 32.0f - 16.0f,
                                random.nextFloat() * 32.0f - 16.0f, 0.0f)
                        instance.pos.plus(weatherPos)
                        instance.speed.set(random.nextFloat() - 0.5f,
                                random.nextFloat() - 0.5f, -8.0f)
                        instance.time = 4.0f
                        instance.dir = random.nextFloat() * TWO_PI.toFloat()
                    }
                }
            }
        }
        // Debug
        temperatureDebug?.setValue(
                climateGenerator.temperature(pos.intX(), pos.intY(),
                        pos.intZ()))
        humidityDebug?.setValue(
                climateGenerator.humidity(pos.intX(), pos.intY(),
                        pos.intZ()))
        weatherDebug?.setValue(weather)
        biomeDebug?.setValue(biomeGenerator[pos.x, pos.y])
        val conditionTag = player.metaData("Vanilla").structure("Condition")
        conditionTag.getDouble("Stamina")?.let { staminaDebug?.setValue(it) }
        conditionTag.getDouble("Wake")?.let { wakeDebug?.setValue(it) }
        conditionTag.getDouble("Hunger")?.let { hungerDebug?.setValue(it) }
        conditionTag.getDouble("Thirst")?.let { thirstDebug?.setValue(it) }
        conditionTag.getDouble(
                "BodyTemperature")?.let { bodyTemperatureDebug?.setValue(it) }
        conditionTag.getBoolean("Sleeping")?.let { sleepingDebug?.setValue(it) }
        exposureDebug?.setValue(exposure)
    }

    override fun init(gl: GL) {
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
                "sound.Weather", 1.0f, 0.0f)
        windAudio = engine.sounds.playStaticAudio(
                "VanillaBasics:sound/entity/particle/rain/Wind1.ogg",
                "sound.Weather", 1.0f, 0.0f)
        val player = world.player
        if (player is MobPlayerClientMainVB) {
            val hud = player.game.hud().addHori(0.0, 0.0, -1.0, -1.0,
                    ::GuiComponentGroup)
            hud.spacer()
            val hudSlab = hud.addVert(0.0, 0.0, -1.0, 76.0,
                    ::GuiComponentGroupSlab)
            val hudBar = hudSlab.addHori(0.0, 0.0, 408.0, -1.0,
                    ::GuiComponentGroup)
            hudBar.addVert(4.0, 4.0, 400.0, 20.0) {
                GuiComponentCondition(it, player)
            }
            hudBar.addVert(4.0, 4.0, 400.0, 40.0) {
                GuiComponentHotbar(it, player)
            }
        }
    }

    override fun renderUpdate(cam: Cam,
                              delta: Double) {
        val player = world.player
        val scene = world.scene
        val factor = min(1.0, delta * 10.0)
        if (world.terrain.sunLight(scene.cam().position.intX(),
                scene.cam().position.intY(), scene.cam().position.intZ()) > 0) {
            fogBrightness += ((1.0f - fogBrightness) * factor).toFloat()
        } else {
            fogBrightness -= (fogBrightness * factor).toFloat()
        }
        val skyLight = (15.0 - climateGenerator.sunLightReduction(
                scene.cam().position.intX().toDouble(),
                scene.cam().position.intY().toDouble())).toFloat() / 15.0f
        if (player.isHeadInWater) {
            val pos = player.getCurrentPos()
            val light = clamp(world.terrain.light(pos.intX(), pos.intY(),
                    floor(pos.z + 0.7)) * 0.09333f + 0.2f, 0.0f, 1.0f)
            fogR = 0.1f * light
            fogG = 0.5f * light
            fogB = 0.8f * light
            fogDistance = 0.1f
        } else {
            val latitude = climateGenerator.latitude(cam.position.doubleY())
            val elevation = climateGenerator.sunElevationD(latitude)
            val sunsetLight = abs(
                    clamp(elevation * 2.0, -1.0, 1.0)).toFloat()
            fogR = mix(1.2f, skyLight * fogBrightness, sunsetLight)
            fogG = mix(0.4f, 0.9f * skyLight * fogBrightness, sunsetLight)
            fogB = mix(0.2f, 0.9f * skyLight * fogBrightness, sunsetLight)
            fogDistance = 1.0f
        }
        val conditionTag = world.player.metaData("Vanilla").structure(
                "Condition")
        val temperature = conditionTag.getDouble("BodyTemperature") ?: 0.0
        val heatstroke = max((temperature - 37.1) * 7.5, 0.0) + 1.0
        exposure += (heatstroke * 0.3 - exposure) * factor
    }

    override fun render(gl: GL,
                        cam: Cam) {
        val player = world.player
        val pos = player.getCurrentPos()
        val scene = world.scene
        val matrixStack = gl.matrixStack()
        val skyLight = (15.0 - climateGenerator.sunLightReduction(
                scene.cam().position.intX().toDouble(),
                scene.cam().position.intY().toDouble())).toFloat() / 15.0f
        val skyboxLight = skyLight * fogBrightness
        val weather = climateGenerator.weather(pos.x, pos.y)
        val sunElevation = (climateGenerator.sunElevation(
                cam.position.doubleX(),
                cam.position.doubleY()) * RAD_2_DEG)
        val sunAzimuth = (climateGenerator.sunAzimuth(cam.position.doubleX(),
                cam.position.doubleY()) * RAD_2_DEG)
        // Sky
        gl.textures().unbind(gl)
        gl.setAttribute4f(GL.COLOR_ATTRIBUTE, 1.0f, 1.0f, 1.0f, 1.0f)
        shaderSkybox.setUniform3f(4, scene.fogR(), scene.fogG(), scene.fogB())
        shaderSkybox.setUniform1f(5, scene.fogDistance())
        shaderSkybox.setUniform4f(6, skyboxLight * skyboxLight, skyboxLight,
                skyboxLight, 1.0f)
        skyboxMesh.render(gl, shaderSkybox)
        // Stars
        if (skyLight < 1.0f) {
            val random = ThreadLocalRandom.current()
            gl.setBlending(BlendingMode.ADD)
            val brightness = max(
                    1.0f - skyLight - random.nextFloat() * 0.1f,
                    0.0f)
            shaderGlow.setUniform4f(4, brightness, brightness, brightness, 1.0f)
            val matrix = matrixStack.push()
            matrix.rotateAccurate(sunAzimuth + 180.0, 0.0f, 0.0f, 1.0f)
            matrix.rotateAccurate(-sunElevation, 1.0f, 0.0f, 0.0f)
            starMesh.render(gl, shaderGlow)
            matrixStack.pop()
        } else {
            gl.setBlending(BlendingMode.ADD)
        }
        // Sun
        var matrix = matrixStack.push()
        matrix.rotateAccurate(sunAzimuth + 180.0f, 0.0f, 0.0f, 1.0f)
        matrix.rotateAccurate(-sunElevation, 1.0f, 0.0f, 0.0f)
        matrix.scale(1.0f, 1.0f, 1.0f)
        shaderGlow.setUniform4f(4, fogR * 1.0f, fogG * 1.1f, fogB * 1.1f, 1.0f)
        billboardMesh.render(gl, shaderGlow)
        matrix.scale(0.2f, 1.0f, 0.2f)
        shaderGlow.setUniform4f(4, fogR * 1.6f, fogG * 1.6f, fogB * 1.3f, 1.0f)
        billboardMesh.render(gl, shaderGlow)
        matrixStack.pop()
        // Moon
        textureMoon.get().bind(gl)
        matrix = matrixStack.push()
        matrix.rotateAccurate(sunAzimuth, 0.0f, 0.0f, 1.0f)
        matrix.rotateAccurate(sunElevation, 1.0f, 0.0f, 0.0f)
        matrix.scale(0.1f, 1.0f, 0.1f)
        billboardMesh.render(gl, shaderTextured)
        matrixStack.pop()
        gl.setBlending(BlendingMode.NORMAL)
        // Clouds
        fbo.texturesColor[0].bind(gl)
        cloudMesh.render(gl, shaderSkybox)
        gl.textures().unbind(gl)
        // Bottom
        skyboxBottomMesh.render(gl, shaderSkybox)
        val cloudTime = System.currentTimeMillis() % 1000000 / 1000000.0f
        fbo.activate(gl)
        gl.viewport(0, 0, fbo.width(), fbo.height())
        gl.clear(0.0f, 0.0f, 0.0f, 0.0f)
        gl.setProjectionOrthogonal(0f, 0f, 1f, 1f)
        shaderClouds.setUniform1f(4, cloudTime)
        shaderClouds.setUniform1f(5, weather.toFloat())
        shaderClouds.setUniform2f(6,
                (scene.cam().position.doubleX() / 2048.0 % 1024.0).toFloat(),
                (scene.cam().position.doubleY() / 2048.0 % 1024.0).toFloat())
        cloudTextureMesh.render(gl, shaderClouds)
        fbo.deactivate(gl)
    }

    override fun dispose(gl: GL) {
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
                doubleArrayOf(0.8, 0.8, 0.8),
                doubleArrayOf(0.8, 0.5, 0.5),
                doubleArrayOf(0.8, 0.6, 0.6),
                doubleArrayOf(0.6, 0.6, 0.8),
                doubleArrayOf(0.5, 0.5, 0.8))
        private val COLORS = arrayOf(
                doubleArrayOf(0.0, 0.3, 0.8),
                doubleArrayOf(0.2, 0.7, 1.0))

        private fun addStar(size: Double,
                            color: Int,
                            starMesh: Mesh,
                            random: Random) {
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
            starMesh.color(STAR_COLORS[color][0], STAR_COLORS[color][1],
                    STAR_COLORS[color][2], 1.0)
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
}